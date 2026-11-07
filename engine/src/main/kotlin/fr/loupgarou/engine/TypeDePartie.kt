package fr.loupgarou.engine

import kotlin.random.Random

/** Les façons de composer le paquet, au choix avant la partie. */
enum class TypeDePartie(val nom: String, val description: String) {
    CLASSIQUE("Classique", "La composition de la règle, avec tous les rôles qui tiennent dans le village."),
    DECOUVERTE("Découverte", "Pour les débutants : la Voyante et le Chasseur (et la Sorcière dès 8 joueurs), des nuits courtes."),
    CHAOS("Chaos", "Composition tirée au hasard et cachée : personne ne sait combien de loups rôdent."),
    PERSONNALISE("Personnalisé", "Vous choisissez vous-mêmes les cartes du paquet."),
}

/** Les réglages choisis avant la partie. Les interrupteurs ne s'appliquent qu'au nombre de joueurs prévu. */
data class Reglages(
    val type: TypeDePartie = TypeDePartie.CLASSIQUE,
    /** Hors règles : un seul loup, de 4 à 6 joueurs, avec seulement les rôles qui ne l'écrasent pas. */
    val unSeulLoup: Boolean = false,
    /** Un loup de plus, à partir de 8 joueurs. */
    val loupsEnrages: Boolean = false,
) {
    fun unSeulLoupActif(nbJoueurs: Int) = unSeulLoup && nbJoueurs in Composition.JOUEURS_UN_LOUP
    fun loupsEnragesActifs(nbJoueurs: Int) = loupsEnrages && nbJoueurs >= Composition.JOUEURS_ENRAGES
}

/**
 * Ce que tout le monde peut savoir du paquet quand la composition est cachée (mode Chaos) :
 * un nombre de loups possible et les rôles qui peuvent être en jeu.
 */
data class Eventail(val loupsMin: Int, val loupsMax: Int, val roles: Set<Role>) {
    fun texte(): String {
        val loups = if (loupsMin == loupsMax) "$loupsMin" else "entre $loupsMin et $loupsMax"
        val autres = roles.filter { !it.estLoup && it != Role.VILLAGEOIS }.sortedBy { it.ordinal }.joinToString(", ") { it.nom }
        return "$loups Loups-Garous, et des rôles tirés au hasard parmi : $autres, Simple Villageois"
    }
}

/**
 * Un paquet prêt à jouer. [eventail] est non nul quand la composition est cachée : c'est alors
 * lui qu'on montre, et la nuit appelle tous les rôles qu'il contient pour ne rien trahir.
 */
data class Composition(val paquet: List<Role>, val eventail: Eventail? = null) {
    val cachee get() = eventail != null

    /** Les rôles qui peuvent être en jeu, aux yeux des joueurs. */
    val rolesPossibles: Set<Role> get() = eventail?.roles ?: paquet.toSet()

    /** Joue « hors règles » : un seul loup est possible. */
    val horsRegles get() = (eventail?.loupsMin ?: paquet.count { it.estLoup }) < 2

    companion object {
        val JOUEURS_UN_LOUP = 4..6
        const val JOUEURS_ENRAGES = 8

        /** L'ordre dans lequel on ajoute les rôles spéciaux quand la place le permet. */
        private val ORDRE_SPECIAUX = listOf(Role.VOYANTE, Role.SORCIERE, Role.CHASSEUR, Role.CUPIDON, Role.PETITE_FILLE)

        /**
         * Les seuls rôles qui n'écrasent pas un loup solitaire. Exclus : la Petite fille (elle verrait
         * le seul loup), la Sorcière (sa potion de vie annule sa seule attaque, son poison le tue),
         * le Voleur (le loup pourrait rester au centre) et Cupidon (2 morts d'un coup à 4 ou 5).
         */
        val ROLES_UN_LOUP = setOf(Role.LOUP_GAROU, Role.VOYANTE, Role.CHASSEUR, Role.VILLAGEOIS)

        /** Nombre de loups de la règle (Classique et Découverte). */
        fun nbLoups(nbJoueurs: Int, reglages: Reglages): Int {
            if (reglages.unSeulLoupActif(nbJoueurs)) return 1
            val base = when {
                nbJoueurs <= 7 -> 2
                nbJoueurs <= 11 -> 3
                else -> 4
            }
            return if (reglages.loupsEnragesActifs(nbJoueurs)) base + 1 else base
        }

        /**
         * La composition pour [nbJoueurs] joueurs. En Personnalisé, on part de la composition
         * Classique, que les joueurs modifient ensuite.
         */
        fun pour(nbJoueurs: Int, reglages: Reglages, hasard: Random = Random): Composition {
            require(nbJoueurs >= 4) { "Il faut au moins 4 joueurs" }
            return when (reglages.type) {
                TypeDePartie.CLASSIQUE, TypeDePartie.PERSONNALISE -> Composition(classique(nbJoueurs, reglages))
                TypeDePartie.DECOUVERTE -> Composition(decouverte(nbJoueurs, reglages))
                TypeDePartie.CHAOS -> chaos(nbJoueurs, reglages, hasard)
            }
        }

        private fun avecVillageois(nbJoueurs: Int, nbLoups: Int, speciaux: List<Role>, cartes: Int = nbJoueurs) =
            List(nbLoups) { Role.LOUP_GAROU } + speciaux + List(cartes - nbLoups - speciaux.size) { Role.VILLAGEOIS }

        private fun unLoup(nbJoueurs: Int): List<Role> {
            val speciaux = if (nbJoueurs >= 6) listOf(Role.VOYANTE, Role.CHASSEUR) else listOf(Role.VOYANTE)
            return avecVillageois(nbJoueurs, 1, speciaux)
        }

        private fun classique(nbJoueurs: Int, reglages: Reglages): List<Role> {
            if (reglages.unSeulLoupActif(nbJoueurs)) return unLoup(nbJoueurs)
            val loups = nbLoups(nbJoueurs, reglages)
            // On garde au moins un Simple Villageois.
            val speciaux = ORDRE_SPECIAUX.take((nbJoueurs - loups - 1).coerceAtLeast(1))
            return avecVillageois(nbJoueurs, loups, speciaux)
        }

        private fun decouverte(nbJoueurs: Int, reglages: Reglages): List<Role> {
            if (reglages.unSeulLoupActif(nbJoueurs)) return unLoup(nbJoueurs)
            val loups = nbLoups(nbJoueurs, reglages)
            val voulus = if (nbJoueurs >= 8) listOf(Role.VOYANTE, Role.CHASSEUR, Role.SORCIERE) else listOf(Role.VOYANTE, Role.CHASSEUR)
            return avecVillageois(nbJoueurs, loups, voulus.take((nbJoueurs - loups - 1).coerceAtLeast(1)))
        }

        /**
         * Chaos : le Voleur est toujours là (2 cartes au centre), le nombre de loups varie d'un
         * autour de la règle (sans dépasser la moitié du village moins un), et les autres rôles spéciaux sont tirés au hasard pour ne laisser que
         * peu de Simples Villageois. Avec un seul loup, seuls les rôles qui ne l'écrasent pas sont tirés
         * (et donc pas de Voleur).
         */
        private fun chaos(nbJoueurs: Int, reglages: Reglages, hasard: Random): Composition {
            val unLoupPossible = reglages.unSeulLoupActif(nbJoueurs)
            val base = nbLoups(nbJoueurs, reglages.copy(unSeulLoup = false))
            val min = if (unLoupPossible) 1 else (base - 1).coerceAtLeast(2)
            // Jamais plus de loups que la moitié du village moins un : sinon la partie est jouée d'avance.
            val max = (base + 1).coerceAtMost(nbJoueurs / 2 - 1).coerceAtLeast(min)
            val loups = hasard.nextInt(min, max + 1)
            val paquet = if (loups == 1) {
                // La Voyante reste : sans elle, une seule erreur au vote suffit au loup.
                val speciaux = if (hasard.nextBoolean()) listOf(Role.VOYANTE, Role.CHASSEUR) else listOf(Role.VOYANTE)
                avecVillageois(nbJoueurs, 1, speciaux)
            } else {
                val cartes = taillePaquet(nbJoueurs, avecVoleur = true)
                // Le Voleur, les loups, et au moins un Simple Villageois.
                val place = (cartes - loups - 2).coerceIn(0, ORDRE_SPECIAUX.size)
                val nb = if (place == 0) 0 else hasard.nextInt((place + 1) / 2, place + 1)
                val speciaux = listOf(Role.VOLEUR) + ORDRE_SPECIAUX.shuffled(hasard).take(nb)
                avecVillageois(nbJoueurs, loups, speciaux, cartes)
            }
            val roles = Role.entries.toSet().let { if (max == 1) it intersect ROLES_UN_LOUP else it }
            return Composition(paquet, Eventail(min, max, roles))
        }

        fun taillePaquet(nbJoueurs: Int, avecVoleur: Boolean) = Setup.taillePaquet(nbJoueurs, avecVoleur)
    }
}
