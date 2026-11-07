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

    /**
     * Mode « chacun son téléphone » : on vote sur son téléphone, donc pas de répliques qui
     * demandent de voter à main levée.
     */
    var multi = false

    private val aMainLevee = setOf(R.raw.v11_05, R.raw.v13_05)

    /** Choisit une variante en évitant les répliques entendues récemment. */
    private fun une(toutes: List<Replique>): Replique {
        val variantes = if (multi) toutes.filter { it.son !in aMainLevee }.ifEmpty { toutes } else toutes
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
    fun jour(etape: Etape, villageoisDebout: Boolean = false): List<Replique> = when (etape) {
        is Etape.Annonce -> annonce(etape)
        is Etape.TirDuChasseur -> listOf(une(Repliques.chasseur))
        is Etape.SuccessionCapitaine -> listOf(une(Repliques.successionCapitaine))
        Etape.ElectionCapitaine -> listOf(une(Repliques.electionCapitaine))
        is Etape.Vote -> listOf(une(if (etape.revote) Repliques.egalite else Repliques.debutVote))
        is Etape.DepartageCapitaine -> listOf(une(Repliques.departageCapitaine))
        is Etape.Fin -> listOf(fin(etape.gagnant, villageoisDebout))
        else -> emptyList()
    }

    private fun annonce(etape: Etape.Annonce): List<Replique> {
        val intro = when (etape.moment) {
            Moment.MATIN -> buildList {
                // L'ouverture, puis le nombre de morts dit clairement, puis le sauvetage éventuel.
                add(une(if (etape.morts.isEmpty()) Repliques.matinPersonne else Repliques.matinMorts))
                Repliques.nombreDeMorts[etape.morts.size.coerceAtMost(4)]?.let { add(une(it)) }
                if (etape.sauvee) add(une(Repliques.sauvetage))
            }
            Moment.VOTE -> listOf(une(Repliques.condamne))
            Moment.TIR_DU_CHASSEUR -> listOf(une(Repliques.tirDuChasseur))
        }
        val morts = etape.morts.flatMap { m ->
            listOfNotNull(
                if (m.cause == CauseDeMort.CHAGRIN) une(Repliques.chagrin) else null,
                une(Repliques.revelation.getValue(m.role)),
            )
        }
        return intro + morts
    }

    /**
     * Quand les loups gagnent avant d'avoir mangé le dernier villageois (victoire anticipée),
     * on écarte la réplique « il ne reste plus personne à manger ».
     */
    fun fin(camp: Camp, villageoisDebout: Boolean = false): Replique {
        val toutes = Repliques.fin.getValue(camp)
        return une(if (villageoisDebout) toutes.filter { it.son != R.raw.v20_04 }.ifEmpty { toutes } else toutes)
    }
}
