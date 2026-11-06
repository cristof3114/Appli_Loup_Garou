package fr.loupgarou.multi

import fr.loupgarou.engine.Role

/** La note explicative de chaque rôle, toujours consultable sur le téléphone du joueur. */
object Fiches {
    fun camp(role: Role) = if (role.estLoup) "Camp des Loups-Garous" else "Camp des Villageois"

    fun fiche(role: Role): List<String> = when (role) {
        Role.LOUP_GAROU -> listOf(
            "Chaque nuit, avec les autres loups, tu choisis une victime sur ton téléphone. La victime n'est désignée que quand tous les loups sont d'accord.",
            "Le jour, fais-toi passer pour un Villageois. Mens bien, mens souvent.",
            "Si tu surprends la Petite fille en train d'espionner, désigne-la : elle meurt à la place de la victime.",
            "Tu gagnes quand il ne reste plus que des loups en vie.",
        )
        Role.VILLAGEOIS -> listOf(
            "Tu n'as aucun pouvoir, seulement ton flair.",
            "Le jour, débats et vote pour éliminer ceux que tu soupçonnes d'être des loups.",
            "Tu gagnes quand tous les loups sont morts.",
        )
        Role.VOYANTE -> listOf(
            "Chaque nuit, tu choisis un joueur et ton téléphone te montre son vrai rôle.",
            "Aide le village sans te faire démasquer : les loups adoreraient te dévorer.",
            "Tu gagnes avec le village, quand tous les loups sont morts.",
        )
        Role.CHASSEUR -> listOf(
            "Si tu meurs dévoré par les loups ou éliminé par le vote, tu tires une dernière balle : tu emportes le joueur de ton choix.",
            "Empoisonné par la Sorcière ou mort de chagrin, tu n'as pas le temps de tirer.",
            "Tu gagnes avec le village, quand tous les loups sont morts.",
        )
        Role.CUPIDON -> listOf(
            "La première nuit, tu désignes deux Amoureux (tu peux te choisir toi-même).",
            "Si l'un des Amoureux meurt, l'autre meurt aussitôt de chagrin.",
            "Ensuite, tu joues comme un Villageois et tu gagnes avec le village.",
        )
        Role.SORCIERE -> listOf(
            "Chaque nuit, ton téléphone te montre la victime des loups.",
            "Tu as une potion de guérison pour la sauver (toi comprise) et une potion de poison pour tuer qui tu veux. Chacune ne sert qu'une fois dans la partie, et tu peux utiliser les deux la même nuit.",
            "Tu gagnes avec le village, quand tous les loups sont morts.",
        )
        Role.VOLEUR -> listOf(
            "La première nuit, ton téléphone te montre les deux cartes restées au centre. Tu peux échanger ta carte contre l'une d'elles, et tu joues ensuite ce rôle.",
            "Si les deux cartes sont des Loups-Garous, tu dois en prendre une.",
        )
        Role.PETITE_FILLE -> listOf(
            "Pendant le tour des loups, tu peux entrouvrir les yeux pour les espionner.",
            "Si un loup te surprend, tu meurs à sa place de victime. Discrétion !",
            "Tu gagnes avec le village, quand tous les loups sont morts.",
        )
    }

    val amoureux = "Si ton Amoureux meurt, tu meurs de chagrin. Si vous êtes de camps opposés, vous gagnez ensemble en éliminant tous les autres."
    val capitaine = "Tu es Capitaine : ta voix compte double, et en cas d'égalité c'est toi qui tranches. Si tu meurs, tu désignes ton successeur."

    /** Le nom de l'image d'emblème (dans le décor gravé de l'appli) pour un rôle. */
    fun embleme(role: Role) = when (role) {
        Role.LOUP_GAROU -> "embleme_loups"
        Role.VILLAGEOIS -> "embleme_village"
        Role.VOYANTE -> "embleme_voyante"
        Role.CHASSEUR -> "embleme_chasseur"
        Role.CUPIDON -> "embleme_cupidon"
        Role.SORCIERE -> "embleme_sorciere"
        Role.VOLEUR -> "embleme_voleur"
        Role.PETITE_FILLE -> "embleme_village"
    }
}
