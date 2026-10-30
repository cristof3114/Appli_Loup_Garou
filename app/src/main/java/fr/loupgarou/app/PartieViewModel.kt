package fr.loupgarou.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import fr.loupgarou.engine.Action
import fr.loupgarou.engine.Joueur
import fr.loupgarou.engine.Partie
import fr.loupgarou.engine.Role
import fr.loupgarou.engine.Setup

enum class Ecran { PREPARATION, DECLARATION, JEU }

class PartieViewModel : ViewModel() {
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
        partie = Partie(joueurs, paquet.toList(), centre)
        version++
        ecran = Ecran.JEU
    }

    fun jouer(action: Action) {
        partie?.jouer(action)
        version++
    }

    fun nouvellePartie() {
        partie = null
        ecran = Ecran.PREPARATION
    }
}
