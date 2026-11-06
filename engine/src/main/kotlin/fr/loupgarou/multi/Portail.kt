package fr.loupgarou.multi

import fr.loupgarou.engine.Action

/**
 * Les adresses que visitent les téléphones. [executer] fait passer chaque demande sur le fil de
 * la tablette (là où vivent le moteur et la voix) ; [fichier] fournit la page et ses images ;
 * [jouer] reçoit les actions des joueurs ; [change] prévient la tablette qu'il faut redessiner.
 */
class Portail(
    private val salon: Salon,
    private val executer: (() -> Reponse) -> Reponse,
    private val fichier: (String) -> Reponse?,
    private val jouer: (Action) -> Unit,
    private val change: () -> Unit,
) {
    fun traiter(r: Requete): Reponse = when {
        r.chemin == "/" || r.chemin == "/index.html" -> fichier("index.html") ?: Reponse.introuvable()
        r.chemin.startsWith("/f/") -> {
            val nom = r.chemin.removePrefix("/f/")
            if (nom.contains("..") || nom.contains('/')) Reponse.introuvable() else fichier(nom) ?: Reponse.introuvable()
        }
        r.chemin == "/etat" -> executer { Reponse.json(salon.vue(r.parametres["j"])) }
        r.chemin == "/rejoindre" && r.methode == "POST" -> executer {
            val res = salon.rejoindre(r.parametres["nom"].orEmpty())
            change()
            Reponse.json(
                res.fold(
                    { Json.ecrire(mapOf("jeton" to it.jeton)) },
                    { Json.ecrire(mapOf("erreur" to it.message)) },
                ),
            )
        }
        r.chemin == "/action" && r.methode == "POST" -> executer {
            val choix = r.parametres["c"].orEmpty().split(',').mapNotNull { it.toIntOrNull() }
            val action = salon.recevoir(r.parametres["j"], r.parametres["cle"], r.parametres["a"].orEmpty(), choix)
            if (action != null) jouer(action)
            change()
            Reponse.json(salon.vue(r.parametres["j"]))
        }
        else -> Reponse.introuvable()
    }
}
