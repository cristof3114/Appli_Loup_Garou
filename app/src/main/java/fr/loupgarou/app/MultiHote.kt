package fr.loupgarou.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import fr.loupgarou.engine.Action
import fr.loupgarou.multi.Portail
import fr.loupgarou.multi.Reponse
import fr.loupgarou.multi.Salon
import fr.loupgarou.multi.ServeurHttp
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Le mode « chacun son téléphone » côté tablette : le salon des joueurs et le petit serveur
 * web que leurs téléphones visitent sur le Wi-Fi. Tout ce que demandent les téléphones est
 * traité sur le fil principal, là où vivent le moteur, l'écran et la voix.
 */
class MultiHote(
    private val context: Context,
    jouer: (Action) -> Unit,
    change: () -> Unit,
) {
    val salon = Salon()
    private val principal = Handler(Looper.getMainLooper())
    private val portail = Portail(salon, ::surLeFilPrincipal, ::fichier, jouer, change)
    private var serveur: ServeurHttp? = null

    /** L'adresse à ouvrir sur les téléphones, ou null si la tablette n'est sur aucun réseau. */
    var adresse: String? = null
        private set

    fun demarrer() {
        if (serveur == null) {
            serveur = PORTS.firstNotNullOfOrNull { port ->
                runCatching { ServeurHttp(port, portail::traiter).also { it.demarrer() } }.getOrNull()
            }
        }
        actualiserAdresse()
    }

    /** Le Wi-Fi a pu changer (point d'accès activé après coup) : on relit l'adresse. */
    fun actualiserAdresse() {
        val s = serveur
        adresse = ServeurHttp.adresseLocale()?.let { ip -> s?.let { "http://$ip:${it.portEcoute}" } }
    }

    fun arreter() {
        serveur?.arreter()
        serveur = null
    }

    private fun surLeFilPrincipal(bloc: () -> Reponse): Reponse {
        if (Looper.myLooper() == Looper.getMainLooper()) return bloc()
        var reponse: Reponse? = null
        val fini = CountDownLatch(1)
        principal.post {
            reponse = runCatching(bloc).getOrElse { Reponse(500, "text/plain", (it.message ?: "Erreur").toByteArray()) }
            fini.countDown()
        }
        fini.await(5, TimeUnit.SECONDS)
        return reponse ?: Reponse(503, "text/plain", "Occupé".toByteArray())
    }

    /** La page des joueurs, la police et les gravures de l'appli, servies telles quelles. */
    private fun fichier(nom: String): Reponse? {
        val res = context.resources
        return when {
            nom == "index.html" -> context.assets.open("joueur/index.html").use {
                Reponse(200, "text/html; charset=utf-8", it.readBytes())
            }
            nom.endsWith(".ttf") -> {
                val id = res.getIdentifier(nom.removeSuffix(".ttf"), "font", context.packageName)
                if (id == 0) null else res.openRawResource(id).use { Reponse(200, "font/ttf", it.readBytes(), garder = true) }
            }
            nom.endsWith(".webp") -> {
                val id = res.getIdentifier(nom.removeSuffix(".webp"), "drawable", context.packageName)
                if (id == 0) null else res.openRawResource(id).use { Reponse(200, "image/webp", it.readBytes(), garder = true) }
            }
            else -> null
        }
    }

    private companion object {
        val PORTS = listOf(8080, 8081, 8888, 0)
    }
}
