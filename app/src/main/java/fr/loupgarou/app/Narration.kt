package fr.loupgarou.app

import android.content.SharedPreferences
import fr.loupgarou.engine.Appel
import fr.loupgarou.engine.Camp
import fr.loupgarou.engine.CauseDeMort
import fr.loupgarou.engine.Etape
import fr.loupgarou.engine.Moment

/**
 * Choisit ce que dit le narrateur à chaque étape, en puisant dans [Repliques]. Les répliques
 * entendues récemment sont mémorisées d'une partie à l'autre pour éviter les redites.
 */
object Narration {
    private const val CLE = "dejaDites"
    private const val MEMOIRE = 60

    private var prefs: SharedPreferences? = null
    private val dejaDites = ArrayDeque<Int>()

    fun init(prefs: SharedPreferences) {
        this.prefs = prefs
        dejaDites.clear()
        prefs.getString(CLE, "")!!.split(',').mapNotNull { it.toIntOrNull() }.forEach(dejaDites::addLast)
    }

    /** Choisit une variante en évitant les répliques entendues récemment. */
    private fun une(variantes: List<Replique>): Replique {
        val fraiches = variantes.filter { it.son !in dejaDites }.ifEmpty { variantes }
        val choix = fraiches.random()
        dejaDites.remove(choix.son)
        dejaDites.addLast(choix.son)
        while (dejaDites.size > MEMOIRE) dejaDites.removeFirst()
        prefs?.edit()?.putString(CLE, dejaDites.joinToString(","))?.apply()
        return choix
    }

    fun appel(appel: Appel): Replique = une(
        when (appel) {
            Appel.VOLEUR -> Repliques.voleur
            Appel.CUPIDON -> Repliques.cupidon
            Appel.AMOUREUX -> Repliques.amoureux
            Appel.VOYANTE -> Repliques.voyante
            Appel.LOUPS -> Repliques.loups
            Appel.SORCIERE -> Repliques.sorciere
        },
    )

    fun endort() = une(Repliques.endort)

    fun rendormir() = une(Repliques.rendormir)

    fun relanceDebat() = une(Repliques.relanceDebat)

    /** Ce que dit le narrateur en arrivant sur une étape de jour (hors appels de nuit). */
    fun jour(etape: Etape): List<Replique> = when (etape) {
        is Etape.Annonce -> annonce(etape)
        is Etape.TirDuChasseur -> listOf(une(Repliques.chasseur))
        is Etape.SuccessionCapitaine -> listOf(une(Repliques.successionCapitaine))
        Etape.ElectionCapitaine -> listOf(une(Repliques.electionCapitaine))
        is Etape.Vote -> listOf(une(if (etape.revote) Repliques.egalite else Repliques.debutVote))
        is Etape.DepartageCapitaine -> listOf(une(Repliques.departageCapitaine))
        is Etape.Fin -> listOf(fin(etape.gagnant))
        else -> emptyList()
    }

    private fun annonce(etape: Etape.Annonce): List<Replique> {
        val intro = when (etape.moment) {
            Moment.MATIN -> une(if (etape.morts.isEmpty()) Repliques.matinPersonne else Repliques.matinMorts)
            Moment.VOTE -> une(Repliques.condamne)
            Moment.TIR_DU_CHASSEUR -> une(Repliques.tirDuChasseur)
        }
        val morts = etape.morts.flatMap { m ->
            listOfNotNull(
                if (m.cause == CauseDeMort.CHAGRIN) une(Repliques.chagrin) else null,
                une(Repliques.revelation.getValue(m.role)),
            )
        }
        return listOf(intro) + morts
    }

    fun fin(camp: Camp): Replique = une(Repliques.fin.getValue(camp))
}
