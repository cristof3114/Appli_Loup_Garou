package fr.loupgarou.multi

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.URLDecoder
import java.util.concurrent.Executors

/** Une requête reçue d'un téléphone. Les paramètres mêlent ceux de l'adresse et du corps. */
class Requete(val methode: String, val chemin: String, val parametres: Map<String, String>)

class Reponse(val statut: Int, val type: String, val corps: ByteArray, val garder: Boolean = false) {
    companion object {
        fun json(texte: String) = Reponse(200, "application/json; charset=utf-8", texte.toByteArray())
        fun introuvable() = Reponse(404, "text/plain; charset=utf-8", "Introuvable".toByteArray())
    }
}

/**
 * Petit serveur web sans dépendance, pour le réseau Wi-Fi de la maison. Chaque connexion est
 * traitée par [traiter], appelé depuis un fil du serveur.
 */
class ServeurHttp(private val port: Int, private val traiter: (Requete) -> Reponse) {
    private var socket: ServerSocket? = null
    private val fils = Executors.newCachedThreadPool { r -> Thread(r, "salon-http").apply { isDaemon = true } }

    val portEcoute: Int get() = socket?.localPort ?: port

    fun demarrer() {
        if (socket != null) return
        val s = ServerSocket()
        s.reuseAddress = true
        s.bind(java.net.InetSocketAddress(InetAddress.getByName("0.0.0.0"), port))
        socket = s
        fils.execute {
            while (!s.isClosed) {
                val client = try { s.accept() } catch (_: SocketException) { break }
                fils.execute { servir(client) }
            }
        }
    }

    fun arreter() {
        socket?.close()
        socket = null
    }

    private fun servir(client: Socket) {
        client.use { c ->
            c.soTimeout = 15_000
            try {
                val entree = BufferedInputStream(c.getInputStream())
                val ligne = lireLigne(entree) ?: return
                val morceaux = ligne.split(' ')
                if (morceaux.size < 2) return
                var longueur = 0
                while (true) {
                    val entete = lireLigne(entree) ?: return
                    if (entete.isEmpty()) break
                    val (nom, valeur) = entete.split(':', limit = 2).let { it[0].trim().lowercase() to it.getOrElse(1) { "" }.trim() }
                    if (nom == "content-length") longueur = valeur.toIntOrNull()?.coerceIn(0, 64_000) ?: 0
                }
                val corps = ByteArray(longueur)
                var lus = 0
                while (lus < longueur) {
                    val n = entree.read(corps, lus, longueur - lus)
                    if (n < 0) break
                    lus += n
                }
                val (chemin, requeteUrl) = morceaux[1].split('?', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
                val parametres = decoder(requeteUrl) + decoder(String(corps, 0, lus))
                val reponse = try {
                    traiter(Requete(morceaux[0], URLDecoder.decode(chemin, "UTF-8"), parametres))
                } catch (e: Exception) {
                    Reponse(500, "text/plain; charset=utf-8", (e.message ?: "Erreur").toByteArray())
                }
                val sortie = c.getOutputStream()
                val entetes = "HTTP/1.1 ${reponse.statut} ${if (reponse.statut == 200) "OK" else "Erreur"}\r\n" +
                    "Content-Type: ${reponse.type}\r\n" +
                    "Content-Length: ${reponse.corps.size}\r\n" +
                    "Cache-Control: ${if (reponse.garder) "max-age=86400" else "no-store"}\r\n" +
                    "Connection: close\r\n\r\n"
                sortie.write(entetes.toByteArray())
                sortie.write(reponse.corps)
                sortie.flush()
            } catch (_: Exception) {
                // Téléphone parti en pleine requête : il réessaiera.
            }
        }
    }

    private fun lireLigne(entree: InputStream): String? {
        val tampon = ByteArrayOutputStream()
        while (true) {
            val b = entree.read()
            if (b < 0) return if (tampon.size() == 0) null else tampon.toString("UTF-8")
            if (b == '\n'.code) return tampon.toString("UTF-8").trimEnd('\r')
            if (tampon.size() > 8_000) return null
            tampon.write(b)
        }
    }

    companion object {
        fun decoder(texte: String): Map<String, String> = texte.split('&').filter { it.isNotEmpty() }.associate {
            val (k, v) = it.split('=', limit = 2).let { p -> p[0] to p.getOrElse(1) { "" } }
            URLDecoder.decode(k, "UTF-8") to URLDecoder.decode(v, "UTF-8")
        }

        /** L'adresse de la tablette sur le Wi-Fi (ou sur son propre point d'accès). */
        fun adresseLocale(): String? = java.net.NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            .filter { it.isUp && !it.isLoopback }
            .sortedBy { if (it.name.startsWith("wlan") || it.name.startsWith("ap") || it.name.startsWith("swlan")) 0 else 1 }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<java.net.Inet4Address>()
            .firstOrNull { it.isSiteLocalAddress }
            ?.hostAddress
    }
}
