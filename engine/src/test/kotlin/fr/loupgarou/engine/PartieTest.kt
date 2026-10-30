package fr.loupgarou.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PartieTest {

    /** Crée une partie dont le joueur i reçoit roles[i], avec un paquet identique (sauf [centre]). */
    private fun partie(vararg roles: Role, centre: List<Role> = emptyList()): Partie {
        val joueurs = roles.mapIndexed { i, r -> Joueur(i, "J$i", r) }
        return Partie(joueurs, roles.toList() + centre, centre)
    }

    private fun Partie.ok() = jouer(Action.Ok)

    /** Avance jusqu'à une étape du type demandé en validant les étapes d'information. */
    private inline fun <reified T : Etape> Partie.jusqua(): T {
        repeat(20) {
            val e = etape
            if (e is T) return e
            when (e) {
                is Etape.AppelFictif, is Etape.ReveilAmoureux, is Etape.VisionVoyante, is Etape.Annonce -> ok()
                else -> error("Bloqué sur $e en attendant ${T::class.simpleName}")
            }
        }
        error("Trop d'étapes")
    }

    @Test
    fun `la nuit appelle les roles dans l'ordre de la regle`() {
        val p = partie(Role.CUPIDON, Role.VOYANTE, Role.LOUP_GAROU, Role.SORCIERE, Role.VILLAGEOIS)
        assertIs<Etape.AppelCupidon>(p.etape)
        p.jouer(Action.ChoixCupidon(1, 4))
        assertEquals(Etape.ReveilAmoureux(1, 4), p.etape)
        p.ok()
        assertIs<Etape.AppelVoyante>(p.etape)
        p.jouer(Action.Designation(2))
        assertEquals(Etape.VisionVoyante(2, Role.LOUP_GAROU), p.etape)
        p.ok()
        assertEquals(Etape.AppelLoups(listOf(2)), p.etape)
        p.jouer(Action.ChoixLoups(4))
        assertEquals(Etape.AppelSorciere(3, 4, peutGuerir = true, peutEmpoisonner = true), p.etape)
    }

    @Test
    fun `la victime des loups meurt au matin et son role est revele`() {
        val p = partie(Role.LOUP_GAROU, Role.LOUP_GAROU, Role.VOYANTE, Role.VILLAGEOIS, Role.VILLAGEOIS)
        p.jusqua<Etape.AppelVoyante>().also { p.jouer(Action.Designation(0)) }
        p.jusqua<Etape.AppelLoups>()
        p.jouer(Action.ChoixLoups(3))
        val annonce = p.etape as Etape.Annonce
        assertEquals(listOf(Mort(3, "J3", Role.VILLAGEOIS, CauseDeMort.LOUPS)), annonce.morts)
        assertEquals(Moment.MATIN, annonce.moment)
        p.ok()
        assertEquals(Etape.ElectionCapitaine, p.etape)
    }

    @Test
    fun `la sorciere peut guerir la victime et empoisonner la meme nuit, une seule fois`() {
        val p = partie(Role.LOUP_GAROU, Role.LOUP_GAROU, Role.SORCIERE, Role.VOYANTE, Role.VILLAGEOIS, Role.VILLAGEOIS)
        p.jusqua<Etape.AppelVoyante>().also { p.jouer(Action.Designation(0)) }
        p.jusqua<Etape.AppelLoups>()
        p.jouer(Action.ChoixLoups(4))
        p.jouer(Action.ChoixSorciere(guerir = true, empoisonner = 0))
        val annonce = p.etape as Etape.Annonce
        assertEquals(listOf(0), annonce.morts.map { it.joueurId })
        assertEquals(CauseDeMort.POISON, annonce.morts.single().cause)
        assertFalse(p.potionGuerison)
        assertFalse(p.potionPoison)

        p.ok()
        p.jouer(Action.Designation(2)) // Capitaine
        p.jouer(Action.Elimination(5))
        p.jusqua<Etape.AppelVoyante>().also { p.jouer(Action.Designation(1)) }
        p.jusqua<Etape.AppelLoups>()
        p.jouer(Action.ChoixLoups(3))
        assertEquals(Etape.AppelSorciere(2, 3, peutGuerir = false, peutEmpoisonner = false), p.etape)
        assertFailsWith<IllegalArgumentException> { p.jouer(Action.ChoixSorciere(guerir = true, empoisonner = null)) }
    }

    @Test
    fun `la sorciere peut se sauver elle-meme`() {
        val p = partie(Role.LOUP_GAROU, Role.LOUP_GAROU, Role.SORCIERE, Role.VILLAGEOIS, Role.VILLAGEOIS)
        p.jusqua<Etape.AppelLoups>()
        p.jouer(Action.ChoixLoups(2))
        p.jouer(Action.ChoixSorciere(guerir = true, empoisonner = null))
        assertEquals(emptyList(), (p.etape as Etape.Annonce).morts)
        assertTrue(p.joueur(2).vivant)
    }

    @Test
    fun `un amoureux meurt de chagrin`() {
        val p = partie(Role.CUPIDON, Role.LOUP_GAROU, Role.LOUP_GAROU, Role.VILLAGEOIS, Role.VILLAGEOIS, Role.VILLAGEOIS)
        p.jouer(Action.ChoixCupidon(3, 4))
        p.jusqua<Etape.AppelLoups>()
        p.jouer(Action.ChoixLoups(3))
        val morts = (p.etape as Etape.Annonce).morts
        assertEquals(listOf(3 to CauseDeMort.LOUPS, 4 to CauseDeMort.CHAGRIN), morts.map { it.joueurId to it.cause })
    }

    @Test
    fun `le chasseur tire s'il est tue par les loups ou par le vote, pas s'il est empoisonne`() {
        val p = partie(Role.LOUP_GAROU, Role.LOUP_GAROU, Role.CHASSEUR, Role.VILLAGEOIS, Role.VILLAGEOIS, Role.VILLAGEOIS)
        p.jusqua<Etape.AppelLoups>()
        p.jouer(Action.ChoixLoups(2))
        p.ok()
        assertEquals(Etape.TirDuChasseur(2), p.etape)
        p.jouer(Action.Designation(0))
        assertEquals(Mort(0, "J0", Role.LOUP_GAROU, CauseDeMort.CHASSEUR), (p.etape as Etape.Annonce).morts.single())
        p.ok()
        assertEquals(Etape.ElectionCapitaine, p.etape)

        val q = partie(Role.LOUP_GAROU, Role.LOUP_GAROU, Role.CHASSEUR, Role.SORCIERE, Role.VILLAGEOIS, Role.VILLAGEOIS)
        q.jusqua<Etape.AppelLoups>()
        q.jouer(Action.ChoixLoups(4))
        q.jouer(Action.ChoixSorciere(guerir = false, empoisonner = 2))
        q.ok()
        assertEquals(Etape.ElectionCapitaine, q.etape)
    }

    @Test
    fun `la petite fille surprise meurt a la place de la victime`() {
        val p = partie(Role.LOUP_GAROU, Role.LOUP_GAROU, Role.PETITE_FILLE, Role.VILLAGEOIS, Role.VILLAGEOIS)
        p.jusqua<Etape.AppelLoups>()
        p.jouer(Action.ChoixLoups(3, petiteFilleSurprise = 2))
        assertEquals(listOf(2), (p.etape as Etape.Annonce).morts.map { it.joueurId })

        val q = partie(Role.LOUP_GAROU, Role.LOUP_GAROU, Role.PETITE_FILLE, Role.VILLAGEOIS, Role.VILLAGEOIS)
        q.jusqua<Etape.AppelLoups>()
        q.jouer(Action.ChoixLoups(3, petiteFilleSurprise = 4)) // fausse accusation : sans effet
        assertEquals(listOf(3), (q.etape as Etape.Annonce).morts.map { it.joueurId })
    }

    @Test
    fun `le voleur doit prendre un loup si les deux cartes du centre sont des loups`() {
        val p = partie(
            Role.VOLEUR, Role.VOYANTE, Role.VILLAGEOIS, Role.VILLAGEOIS,
            centre = listOf(Role.LOUP_GAROU, Role.LOUP_GAROU),
        )
        val appel = p.etape as Etape.AppelVoleur
        assertTrue(appel.doitPrendreLoup)
        assertFailsWith<IllegalArgumentException> { p.jouer(Action.ChoixVoleur(null)) }
        p.jouer(Action.ChoixVoleur(Role.LOUP_GAROU))
        assertEquals(Role.LOUP_GAROU, p.joueur(0).role)
    }

    @Test
    fun `un role reste au centre est appele pour du faux, un role mort ne l'est plus`() {
        val p = partie(
            Role.VOLEUR, Role.LOUP_GAROU, Role.LOUP_GAROU, Role.VOYANTE, Role.VILLAGEOIS,
            centre = listOf(Role.SORCIERE, Role.VILLAGEOIS),
        )
        p.jouer(Action.ChoixVoleur(null))
        assertIs<Etape.AppelVoyante>(p.etape)
        p.jouer(Action.Designation(1)); p.ok()
        p.jouer(Action.ChoixLoups(3))
        assertEquals(Etape.AppelFictif(Appel.SORCIERE), p.etape)
        p.ok(); p.ok()
        p.jouer(Action.Designation(0))
        p.jouer(Action.Elimination(4))
        p.ok()
        // Nuit 2 : la Voyante est morte, on passe directement aux loups.
        assertIs<Etape.AppelLoups>(p.etape)
    }

    @Test
    fun `egalite au vote tranchee par le capitaine, sinon revote`() {
        val p = partie(Role.LOUP_GAROU, Role.LOUP_GAROU, Role.VILLAGEOIS, Role.VILLAGEOIS, Role.VILLAGEOIS, Role.VILLAGEOIS)
        p.jusqua<Etape.AppelLoups>()
        p.jouer(Action.ChoixLoups(5)); p.ok()
        p.jouer(Action.Designation(2))
        p.jouer(Action.Egalite(listOf(0, 3)))
        assertEquals(Etape.DepartageCapitaine(2, listOf(0, 3)), p.etape)
        p.jouer(Action.Designation(0))
        assertEquals(Mort(0, "J0", Role.LOUP_GAROU, CauseDeMort.VOTE), (p.etape as Etape.Annonce).morts.single())
    }

    @Test
    fun `le capitaine mort designe son successeur`() {
        val p = partie(Role.LOUP_GAROU, Role.LOUP_GAROU, Role.VILLAGEOIS, Role.VILLAGEOIS, Role.VILLAGEOIS, Role.VILLAGEOIS)
        p.jusqua<Etape.AppelLoups>()
        p.jouer(Action.ChoixLoups(5)); p.ok()
        p.jouer(Action.Designation(2))
        p.jouer(Action.Elimination(2)); p.ok()
        assertEquals(Etape.SuccessionCapitaine(2), p.etape)
        p.jouer(Action.Designation(3))
        assertEquals(3, p.capitaine)
        assertIs<Etape.AppelLoups>(p.etape)
        p.jouer(Action.ChoixLoups(4)); p.ok()
        p.jouer(Action.Egalite(listOf(0, 1)))
        assertEquals(Etape.DepartageCapitaine(3, listOf(0, 1)), p.etape)
    }

    @Test
    fun `victoires`() {
        val v = partie(Role.LOUP_GAROU, Role.VILLAGEOIS, Role.VILLAGEOIS, Role.VILLAGEOIS)
        v.jusqua<Etape.AppelLoups>()
        v.jouer(Action.ChoixLoups(3)); v.ok()
        v.jouer(Action.Designation(1))
        v.jouer(Action.Elimination(0)); v.ok()
        assertEquals(Etape.Fin(Camp.VILLAGEOIS), v.etape)

        val l = partie(Role.LOUP_GAROU, Role.LOUP_GAROU, Role.VILLAGEOIS, Role.VILLAGEOIS)
        l.jusqua<Etape.AppelLoups>()
        l.jouer(Action.ChoixLoups(3)); l.ok()
        l.jouer(Action.Designation(0))
        l.jouer(Action.Elimination(2)); l.ok()
        assertEquals(Etape.Fin(Camp.LOUPS), l.etape)

        val a = partie(Role.CUPIDON, Role.LOUP_GAROU, Role.LOUP_GAROU, Role.VILLAGEOIS)
        a.jouer(Action.ChoixCupidon(1, 3))
        a.jusqua<Etape.AppelLoups>()
        a.jouer(Action.ChoixLoups(0)); a.ok()
        assertNull(a.vainqueur())
        a.jouer(Action.Designation(1))
        a.jouer(Action.Elimination(2)); a.ok()
        assertEquals(Etape.Fin(Camp.AMOUREUX), a.etape)
    }

    @Test
    fun `controle des roles declares et cartes du centre`() {
        val paquet = listOf(Role.VOLEUR, Role.LOUP_GAROU, Role.LOUP_GAROU, Role.VOYANTE, Role.VILLAGEOIS, Role.SORCIERE)
        assertEquals(
            listOf(Role.VOYANTE, Role.SORCIERE),
            Setup.cartesDuCentre(paquet, listOf(Role.LOUP_GAROU, Role.VOLEUR, Role.VILLAGEOIS, Role.LOUP_GAROU)),
        )
        assertNull(Setup.cartesDuCentre(paquet, listOf(Role.LOUP_GAROU, Role.LOUP_GAROU, Role.LOUP_GAROU, Role.VOLEUR)))
        assertEquals(emptyList(), Setup.avertissements(Setup.compositionProposee(8), 8))
        assertEquals(listOf("Il faut au moins 2 Loups-Garous"), Setup.avertissements(listOf(Role.LOUP_GAROU, Role.VOYANTE, Role.VILLAGEOIS, Role.VILLAGEOIS), 4))
    }
}
