package fr.loupgarou.engine

/** Préparation de la partie : composition du paquet et contrôle des rôles déclarés en secret. */
object Setup {

    /** Nombre de cartes du paquet : une par joueur, plus 2 au centre si le Voleur est en jeu. */
    fun taillePaquet(nbJoueurs: Int, paquetAvecVoleur: Boolean) =
        if (paquetAvecVoleur) nbJoueurs + 2 else nbJoueurs

    /** Composition proposée par défaut selon le nombre de joueurs (sans Voleur). */
    fun compositionProposee(nbJoueurs: Int): List<Role> {
        require(nbJoueurs >= 4) { "Il faut au moins 4 joueurs" }
        val nbLoups = when {
            nbJoueurs <= 7 -> 2
            nbJoueurs <= 11 -> 3
            else -> 4
        }
        val speciaux = listOf(
            Role.VOYANTE, Role.SORCIERE, Role.CHASSEUR, Role.CUPIDON, Role.PETITE_FILLE,
        ).take((nbJoueurs - nbLoups - 1).coerceAtLeast(1))
        val villageois = nbJoueurs - nbLoups - speciaux.size
        return List(nbLoups) { Role.LOUP_GAROU } + speciaux + List(villageois) { Role.VILLAGEOIS }
    }

    /** Problèmes de composition par rapport à la version jouable minimum de la règle. */
    fun avertissements(paquet: List<Role>, nbJoueurs: Int): List<String> = buildList {
        val avecVoleur = Role.VOLEUR in paquet
        if (paquet.size != taillePaquet(nbJoueurs, avecVoleur)) {
            add("Le paquet doit compter ${taillePaquet(nbJoueurs, avecVoleur)} cartes pour $nbJoueurs joueurs")
        }
        if (paquet.count { it == Role.LOUP_GAROU } < 2) add("Il faut au moins 2 Loups-Garous")
        if (Role.VOYANTE !in paquet) add("Il faut une Voyante")
        Role.entries.filter { it != Role.LOUP_GAROU && it != Role.VILLAGEOIS }
            .filter { r -> paquet.count { it == r } > 1 }
            .forEach { add("Il ne peut y avoir qu'une carte ${it.nom}") }
    }

    /**
     * Vérifie que les rôles déclarés correspondent au paquet. Renvoie les 2 cartes du centre
     * (vide sans Voleur), ou null si les déclarations ne collent pas au paquet.
     */
    fun cartesDuCentre(paquet: List<Role>, declares: List<Role>): List<Role>? {
        val reste = paquet.toMutableList()
        for (r in declares) if (!reste.remove(r)) return null
        val attendu = if (Role.VOLEUR in paquet) 2 else 0
        return if (reste.size == attendu) reste else null
    }
}
