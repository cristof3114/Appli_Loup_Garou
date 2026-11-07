package fr.loupgarou.engine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TypeDePartieTest {

    private fun List<Role>.loups() = count { it.estLoup }

    @Test
    fun `le Classique reste la composition de la regle`() {
        assertEquals(
            listOf(Role.LOUP_GAROU, Role.LOUP_GAROU, Role.VOYANTE, Role.SORCIERE, Role.CHASSEUR, Role.VILLAGEOIS),
            Composition.pour(6, Reglages()).paquet,
        )
        assertEquals(Setup.compositionProposee(9), Composition.pour(9, Reglages()).paquet)
        assertEquals(mapOf(4 to 2, 7 to 2, 8 to 3, 11 to 3, 12 to 4), listOf(4, 7, 8, 11, 12).associateWith { Composition.pour(it, Reglages()).paquet.loups() })
    }

    @Test
    fun `la Decouverte n'a que Voyante, Chasseur et Sorciere des 8 joueurs`() {
        val six = Composition.pour(6, Reglages(TypeDePartie.DECOUVERTE)).paquet
        assertEquals(setOf(Role.LOUP_GAROU, Role.VOYANTE, Role.CHASSEUR, Role.VILLAGEOIS), six.toSet())
        assertEquals(2, six.loups())
        val huit = Composition.pour(8, Reglages(TypeDePartie.DECOUVERTE)).paquet
        assertEquals(setOf(Role.LOUP_GAROU, Role.VOYANTE, Role.CHASSEUR, Role.SORCIERE, Role.VILLAGEOIS), huit.toSet())
        assertEquals(8, huit.size)
    }

    @Test
    fun `un seul loup de 4 a 6 joueurs, avec seulement les roles prevus`() {
        val r = Reglages(unSeulLoup = true)
        assertEquals(listOf(Role.LOUP_GAROU, Role.VOYANTE, Role.VILLAGEOIS, Role.VILLAGEOIS), Composition.pour(4, r).paquet)
        assertEquals(listOf(Role.LOUP_GAROU, Role.VOYANTE, Role.VILLAGEOIS, Role.VILLAGEOIS, Role.VILLAGEOIS), Composition.pour(5, r).paquet)
        assertEquals(
            listOf(Role.LOUP_GAROU, Role.VOYANTE, Role.CHASSEUR, Role.VILLAGEOIS, Role.VILLAGEOIS, Role.VILLAGEOIS),
            Composition.pour(6, r).paquet,
        )
        assertTrue(Composition.pour(5, r).horsRegles)
        // Dès 7 joueurs, l'interrupteur ne compte plus : on revient à 2 loups.
        assertEquals(2, Composition.pour(7, r).paquet.loups())
        assertEquals(emptyList(), Setup.avertissements(Composition.pour(5, r).paquet, 5, unSeulLoupPermis = true))
        assertTrue("Il faut au moins 2 Loups-Garous" in Setup.avertissements(Composition.pour(5, r).paquet, 5))
    }

    @Test
    fun `les loups enrages ajoutent un loup a partir de 8 joueurs`() {
        val r = Reglages(loupsEnrages = true)
        assertEquals(2, Composition.pour(7, r).paquet.loups())
        assertEquals(4, Composition.pour(8, r).paquet.loups())
        assertEquals(5, Composition.pour(12, r).paquet.loups())
        assertEquals(8, Composition.pour(8, r).paquet.size)
        assertTrue(Role.VILLAGEOIS in Composition.pour(8, r).paquet)
    }

    @Test
    fun `le Chaos est cache, avec le Voleur et un nombre de loups qui varie`() {
        val vus = mutableSetOf<Int>()
        for (graine in 0 until 300) {
            for (nb in 4..14) {
                val c = Composition.pour(nb, Reglages(TypeDePartie.CHAOS), Random(graine))
                val e = assertNotNull(c.eventail)
                assertEquals(nb + 2, c.paquet.size)
                assertTrue(Role.VOLEUR in c.paquet)
                assertTrue(Role.VILLAGEOIS in c.paquet)
                assertTrue(c.paquet.loups() in e.loupsMin..e.loupsMax)
                assertTrue(e.loupsMin >= 2 && e.loupsMax <= maxOf(2, nb / 2 - 1))
                assertTrue(c.rolesPossibles.containsAll(c.paquet))
                Role.entries.filter { it != Role.LOUP_GAROU && it != Role.VILLAGEOIS }
                    .forEach { r -> assertTrue(c.paquet.count { it == r } <= 1) }
                if (nb == 10) vus += c.paquet.loups()
            }
        }
        assertEquals(setOf(2, 3, 4), vus)
    }

    @Test
    fun `le Chaos a 1 loup ne tire que les roles qui ne l'ecrasent pas`() {
        for (graine in 0 until 200) {
            val c = Composition.pour(4, Reglages(TypeDePartie.CHAOS, unSeulLoup = true), Random(graine))
            assertEquals(1, c.paquet.loups())
            assertEquals(4, c.paquet.size)
            assertTrue(Role.VOYANTE in c.paquet)
            assertTrue(Composition.ROLES_UN_LOUP.containsAll(c.rolesPossibles))
        }
        val six = (0 until 200).map { Composition.pour(6, Reglages(TypeDePartie.CHAOS, unSeulLoup = true), Random(it)) }
        assertEquals(setOf(1, 2), six.map { it.paquet.loups() }.toSet())
        six.filter { it.paquet.loups() == 1 }.forEach { assertTrue(Composition.ROLES_UN_LOUP.containsAll(it.paquet)) }
    }

    @Test
    fun `en composition cachee la nuit appelle aussi les roles absents`() {
        // Ni Cupidon, ni Sorcière, ni Voleur dans le paquet, mais ils font partie de l'éventail.
        val roles = listOf(Role.LOUP_GAROU, Role.LOUP_GAROU, Role.VOYANTE, Role.VILLAGEOIS, Role.VILLAGEOIS)
        val joueurs = roles.mapIndexed { i, r -> Joueur(i, "J$i", r) }
        val p = Partie(joueurs, roles, rolesPossibles = Role.entries.toSet())
        assertEquals(Etape.AppelFictif(Appel.VOLEUR), p.etape)
        p.jouer(Action.Ok)
        assertEquals(Etape.AppelFictif(Appel.CUPIDON), p.etape)
        p.jouer(Action.Ok)
        assertIs<Etape.DecouverteAmoureux>(p.etape)
        p.jouer(Action.Ok)
        assertIs<Etape.AppelVoyante>(p.etape)
        p.jouer(Action.Designation(0))
        p.jouer(Action.Ok)
        assertIs<Etape.AppelLoups>(p.etape)
        p.jouer(Action.ChoixLoups(3))
        assertEquals(Etape.AppelFictif(Appel.SORCIERE), p.etape)
        p.jouer(Action.Ok)
        assertIs<Etape.Annonce>(p.etape)
    }

    @Test
    fun `composition publique, la nuit n'appelle que les roles du paquet`() {
        val roles = listOf(Role.LOUP_GAROU, Role.LOUP_GAROU, Role.VOYANTE, Role.VILLAGEOIS, Role.VILLAGEOIS)
        val p = Partie(roles.mapIndexed { i, r -> Joueur(i, "J$i", r) }, roles)
        assertIs<Etape.AppelVoyante>(p.etape)
        assertNull(Composition(roles).eventail)
    }
}
