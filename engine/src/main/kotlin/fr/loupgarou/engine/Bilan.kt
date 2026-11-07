package fr.loupgarou.engine

/**
 * Les mots du bilan d'une annonce, communs à la tablette, à la télé et aux téléphones : un titre
 * qui dit tout de suite combien de joueurs sont morts, puis une ligne par mort.
 */
object Bilan {
    fun titre(a: Etape.Annonce): String = when (a.moment) {
        Moment.MATIN -> when (a.morts.size) {
            0 -> "Personne n'est mort cette nuit"
            1 -> "Cette nuit : 1 mort"
            else -> "Cette nuit : ${a.morts.size} morts"
        }
        Moment.VOTE -> "Le village a tranché"
        Moment.TIR_DU_CHASSEUR -> "Le Chasseur a tiré"
    }

    /** Dit que la Sorcière a sauvé la victime des loups, sans dire qui était visé. */
    fun sauvetage(a: Etape.Annonce): String? =
        if (a.sauvee) "Les loups ont frappé, mais leur victime a été sauvée." else null

    fun ligne(m: Mort): String = "${m.nom} était ${article(m.role)}" + when (m.cause) {
        CauseDeMort.CHAGRIN -> ", mort de chagrin"
        CauseDeMort.CHASSEUR -> ", abattu par le Chasseur"
        else -> ""
    }

    fun article(r: Role) = when (r) {
        Role.LOUP_GAROU -> "un Loup-Garou"
        Role.VILLAGEOIS -> "un Simple Villageois"
        Role.VOYANTE -> "la Voyante"
        Role.CHASSEUR -> "le Chasseur"
        Role.CUPIDON -> "Cupidon"
        Role.SORCIERE -> "la Sorcière"
        Role.VOLEUR -> "le Voleur"
        Role.PETITE_FILLE -> "la Petite fille"
    }
}
