package fr.loupgarou.app

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import fr.loupgarou.engine.Action
import fr.loupgarou.engine.Appel
import fr.loupgarou.engine.Etape
import fr.loupgarou.engine.Joueur
import fr.loupgarou.engine.Partie
import fr.loupgarou.engine.Role
import fr.loupgarou.engine.Setup

enum class Ecran { PREPARATION, DECLARATION, JEU }

class PartieViewModel(application: Application) : AndroidViewModel(application) {
    private val voix = Voix(application)

    init {
        Narration.init(application.getSharedPreferences("narration", Context.MODE_PRIVATE))
    }

    var ecran by mutableStateOf(Ecran.PREPARATION)
        private set

    val noms = mutableStateListOf<String>()
    val paquet = mutableStateListOf<Role>()

    /** Rôle déclaré par chaque joueur, dans l'ordre de [noms]. */
    val declarations = mutableStateListOf<Role>()
    var erreurDeclaration by mutableStateOf<String?>(null)
        private set

    var partie by mutableStateOf<Partie?>(null)
        private set

    /** Ce que le narrateur vient de dire pour l'étape en cours, affiché à l'écran. */
    var paroles by mutableStateOf<List<Replique>>(emptyList())
        private set

    /**
     * Vrai pendant qu'un joueur se rendort : l'écran reste neutre, sans bouton, le temps que la
     * voix le renvoie au lit et qu'un silence passe avant l'appel suivant.
     */
    var enTransition by mutableStateOf(false)
        private set
    private val minuterie = Handler(Looper.getMainLooper())
    private var jetonTransition = 0

    /** Incrémenté à chaque action pour redessiner l'écran de jeu. */
    var version by mutableIntStateOf(0)
        private set

    fun ajouterJoueur(nom: String) {
        val propre = nom.trim()
        if (propre.isNotEmpty() && propre !in noms) noms.add(propre)
    }

    fun retirerJoueur(nom: String) {
        noms.remove(nom)
    }

    fun proposerComposition() {
        if (noms.size < 4) return
        paquet.clear()
        paquet.addAll(Setup.compositionProposee(noms.size))
    }

    fun ajouterCarte(role: Role) {
        paquet.add(role)
    }

    fun retirerCarte(role: Role) {
        paquet.remove(role)
    }

    fun avertissements() = Setup.avertissements(paquet.toList(), noms.size)

    fun peutCommencer(): Boolean {
        if (noms.size < 4) return false
        val avecVoleur = Role.VOLEUR in paquet
        if (paquet.size != Setup.taillePaquet(noms.size, avecVoleur)) return false
        return Role.entries.filter { it != Role.LOUP_GAROU && it != Role.VILLAGEOIS }
            .none { r -> paquet.count { it == r } > 1 }
    }

    fun commencerDeclarations() {
        declarations.clear()
        erreurDeclaration = null
        ecran = Ecran.DECLARATION
    }

    fun declarer(role: Role) {
        declarations.add(role)
        if (declarations.size < noms.size) return
        val centre = Setup.cartesDuCentre(paquet.toList(), declarations.toList())
        if (centre == null) {
            declarations.clear()
            erreurDeclaration = "Les rôles déclarés ne correspondent pas au paquet. " +
                "Quelqu'un s'est trompé : on recommence la déclaration."
            return
        }
        val joueurs = noms.mapIndexed { i, nom -> Joueur(i, nom, declarations[i]) }
        val nouvelle = Partie(joueurs, paquet.toList(), centre)
        partie = nouvelle
        narrer(null, nouvelle.etape)
        version++
        ecran = Ecran.JEU
    }

    fun jouer(action: Action) {
        val p = partie ?: return
        val avant = p.etape
        p.jouer(action)
        narrer(avant, p.etape)
        version++
    }

    /**
     * Joue [action] seulement si la partie est encore sur [etape]. Sert aux minuteries (vision de
     * la Voyante, appel fictif) : si un joueur a déjà avancé à la main, elles ne doivent pas
     * valider l'écran suivant à sa place.
     */
    fun jouerSi(etape: Etape, action: Action) {
        if (partie?.etape === etape) jouer(action)
    }

    /** Relance le débat, sauf si le narrateur est déjà en train de parler. */
    fun relancerDebat() {
        if (!voix.parle) voix.dire(listOf(Narration.relanceDebat()))
    }

    fun repeter() {
        voix.dire(paroles)
    }

    /**
     * Fait parler le narrateur en passant de l'étape [avant] à l'étape [apres] : on renvoie au
     * lit celui qui vient d'agir, on endort le village à la tombée de la nuit, puis on annonce.
     */
    private fun narrer(avant: Etape?, apres: Etape) {
        val dit = mutableListOf<Replique>()
        if (avant != null && deNuit(avant) && apres !is Etape.VisionVoyante) dit += Narration.rendormir()
        val appel = appelDe(apres)
        paroles = when {
            appel != null -> {
                if (avant == null || !deNuit(avant)) dit += Narration.endort()
                listOf(Narration.appel(appel))
            }
            apres is Etape.VisionVoyante -> emptyList()
            else -> Narration.jour(apres)
        }
        commencerTransition(dit, paroles)
    }

    private fun commencerTransition(transition: List<Replique>, ensuite: List<Replique>) {
        val jeton = ++jetonTransition
        val finir = {
            // Seule la transition en cours peut se terminer, et une seule fois.
            if (jeton == jetonTransition && enTransition) enTransition = false
        }
        enTransition = transition.isNotEmpty()
        voix.dire(transition, PAUSE_TRANSITION_MS, ensuite) { finir() }
        // Filet de sécurité si le son ne se termine jamais : l'écran ne reste pas bloqué.
        if (enTransition) minuterie.postDelayed({ finir() }, SECURITE_TRANSITION_MS)
    }

    private fun deNuit(etape: Etape) = appelDe(etape) != null || etape is Etape.VisionVoyante

    private fun appelDe(etape: Etape): Appel? = when (etape) {
        is Etape.AppelVoleur -> Appel.VOLEUR
        is Etape.AppelCupidon -> Appel.CUPIDON
        is Etape.ReveilAmoureux -> Appel.AMOUREUX
        is Etape.AppelVoyante -> Appel.VOYANTE
        is Etape.AppelLoups -> Appel.LOUPS
        is Etape.AppelSorciere -> Appel.SORCIERE
        is Etape.AppelFictif -> etape.appel
        else -> null
    }

    override fun onCleared() {
        minuterie.removeCallbacksAndMessages(null)
        voix.arreter()
    }

    fun nouvellePartie() {
        jetonTransition++
        enTransition = false
        voix.arreter()
        paroles = emptyList()
        partie = null
        ecran = Ecran.PREPARATION
    }

    private companion object {
        /** Silence entre « rendors-toi » et l'appel suivant. */
        const val PAUSE_TRANSITION_MS = 2_500L
        const val SECURITE_TRANSITION_MS = 20_000L
    }
}
