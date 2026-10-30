package fr.loupgarou.app

import fr.loupgarou.engine.Appel
import fr.loupgarou.engine.Camp
import fr.loupgarou.engine.Etape
import fr.loupgarou.engine.Moment

/**
 * Textes du narrateur. Version provisoire en texte à l'écran : l'étape 3 les remplacera
 * par une banque de répliques plus fournie et lue par une voix pré-générée.
 */
object Narration {
    private val dejaDites = ArrayDeque<String>()

    /** Choisit une variante en évitant les répliques entendues récemment. */
    private fun une(vararg variantes: String): String {
        val fraiches = variantes.filter { it !in dejaDites }.ifEmpty { variantes.toList() }
        val choix = fraiches.random()
        dejaDites.addLast(choix)
        if (dejaDites.size > 30) dejaDites.removeFirst()
        return choix
    }

    fun appel(appel: Appel): String = when (appel) {
        Appel.VOLEUR -> une(
            "Le Voleur se réveille. Les poches vides, la conscience aussi.",
            "Voleur, ouvre les yeux. Deux cartes t'attendent, sers-toi sans honte.",
        )
        Appel.CUPIDON -> une(
            "Cupidon se réveille. Choisis deux cœurs à lier… jusqu'à ce que la mort les sépare. Ensemble.",
            "Cupidon, ouvre les yeux. Ce soir, tu fais des malheureux. Deux, précisément.",
        )
        Appel.AMOUREUX -> une(
            "Les Amoureux se réveillent et se découvrent. Profitez-en, ça ne durera peut-être pas.",
            "Amoureux, ouvrez les yeux et regardez-vous. Désormais, si l'un tombe, l'autre suit.",
        )
        Appel.VOYANTE -> une(
            "La Voyante se réveille. Choisis l'âme que tu veux sonder.",
            "Voyante, ouvre les yeux. Ta boule de cristal a faim de vérité.",
            "La Voyante s'éveille. Qui cache quoi, ce soir ?",
        )
        Appel.LOUPS -> une(
            "Les Loups-Garous se réveillent. Le dîner est servi, il reste à choisir le plat.",
            "Loups-Garous, ouvrez les yeux. Le village dort, et il sent bon.",
            "Les Loups-Garous s'éveillent. Concertez-vous en silence : qui ne verra pas l'aube ?",
        )
        Appel.SORCIERE -> une(
            "La Sorcière se réveille. Deux fioles, un cadavre tout frais : à toi de voir.",
            "Sorcière, ouvre les yeux. Sauver, empoisonner, ou retourner te coucher ?",
        )
    }

    fun rendormir(): String = une(
        "Rendors-toi, et repose l'appareil au centre.",
        "Ferme les yeux. Repose l'appareil sans bruit.",
    )

    fun annonce(etape: Etape.Annonce): String = when {
        etape.moment == Moment.MATIN && etape.morts.isEmpty() -> une(
            "Le village se réveille… au complet. Les loups ont eu une nuit difficile.",
            "Le soleil se lève et, miracle, personne ne manque à l'appel.",
        )
        etape.moment == Moment.MATIN -> une(
            "Le village se réveille. Enfin, presque tout le village.",
            "Le soleil se lève sur une scène que personne n'oubliera. Surtout pas la victime.",
            "Ce matin, le village pleure l'un des siens. Ou plusieurs.",
        )
        etape.moment == Moment.VOTE -> une(
            "Le village a parlé. La corde aussi.",
            "La sentence tombe. Le condamné aussi.",
        )
        else -> une(
            "Le Chasseur a tiré. Il ne part pas seul.",
            "Un dernier coup de fusil, et un voisin de moins.",
        )
    }

    fun fin(camp: Camp): String = when (camp) {
        Camp.VILLAGEOIS -> "Les Villageois l'emportent ! Le dernier Loup-Garou a rejoint ses victimes."
        Camp.LOUPS -> "Les Loups-Garous l'emportent. Le village est calme, désormais. Très calme."
        Camp.AMOUREUX -> "Les Amoureux l'emportent, seuls au monde. Ils l'ont bien cherché."
        Camp.PERSONNE -> "Plus personne. Le village est désert, et les corbeaux ont gagné."
    }
}
