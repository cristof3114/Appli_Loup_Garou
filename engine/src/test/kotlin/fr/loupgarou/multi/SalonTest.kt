package fr.loupgarou.multi

import fr.loupgarou.engine.Action
import fr.loupgarou.engine.Camp
import fr.loupgarou.engine.Composition
import fr.loupgarou.engine.Reglages
import fr.loupgarou.engine.TypeDePartie
import fr.loupgarou.engine.Etape
import fr.loupgarou.engine.Role
import fr.loupgarou.engine.Setup
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SalonTest {

    private val noms = listOf("Ana", "Bob", "Chloé", "Dan", "Eve", "Fanny")
    private val paquet = listOf(
        Role.LOUP_GAROU, Role.LOUP_GAROU, Role.VOYANTE, Role.SORCIERE, Role.CHASSEUR, Role.CUPIDON,
    )

    /** Un salon où tout le monde a rejoint, vu son rôle, et où la première nuit a commencé. */
    private fun salonPret(graine: Int = 1): Pair<Salon, Map<String, String>> {
        val salon = Salon(Random(graine))
        val jetons = noms.associateWith { salon.rejoindre(it).getOrThrow().jeton }
        salon.distribuer(paquet)
        jetons.values.forEach {
            salon.recevoir(it, "revelation", "reveler", emptyList())
            salon.recevoir(it, "revelation", "cacher", emptyList())
        }
        assertTrue(salon.toutLeMondeEstPret())
        salon.commencer()
        return salon to jetons
    }

    private fun Salon.jeton(id: Int) = places.first { it.id == id }.jeton
    private fun Salon.cle(jeton: String) = vueMap(jeton)["cle"] as String

    /** Joue l'action d'un téléphone sur l'écran qu'il voit, comme le ferait la tablette. */
    private fun Salon.appui(jeton: String, action: String, vararg choix: Int): Action? {
        val a = recevoir(jeton, cle(jeton), action, choix.toList())
        if (a != null) partie!!.jouer(a)
        return a
    }

    @Test
    fun `le tirage donne une carte par joueur et deux au centre avec le Voleur`() {
        val avecVoleur = paquet + listOf(Role.VOLEUR, Role.VILLAGEOIS, Role.VILLAGEOIS)
        val (roles, centre) = Setup.tirerRoles(avecVoleur, 7, Random(3))
        assertEquals(7, roles.size)
        assertEquals(2, centre.size)
        assertEquals(avecVoleur.sorted(), (roles + centre).sorted())
    }

    @Test
    fun `inscription refuse les doublons et ferme quand la partie commence`() {
        val salon = Salon(Random(1))
        assertTrue(salon.rejoindre("Ana").isSuccess)
        assertTrue(salon.rejoindre(" ana ").isFailure)
        assertTrue(salon.rejoindre("  ").isFailure)
        noms.drop(1).forEach { salon.rejoindre(it) }
        salon.distribuer(paquet)
        assertTrue(salon.rejoindre("Zoé").isFailure)
        // Un téléphone inconnu ne voit pas le formulaire une fois la partie lancée.
        assertNull(salon.vueMap("inconnu")["inscription"])
    }

    @Test
    fun `chacun découvre son rôle et sa fiche reste disponible`() {
        val salon = Salon(Random(1))
        val jeton = salon.rejoindre("Ana").getOrThrow().jeton
        noms.drop(1).forEach { salon.rejoindre(it) }
        salon.distribuer(paquet)
        assertNull(salon.vueMap(jeton)["moi"], "le rôle reste caché tant qu'on ne l'a pas découvert")
        salon.recevoir(jeton, "revelation", "reveler", emptyList())
        @Suppress("UNCHECKED_CAST")
        val moi = salon.vueMap(jeton)["moi"] as Map<String, Any?>
        assertNotNull(moi["role"])
        assertTrue((moi["fiche"] as List<*>).isNotEmpty())
        assertTrue((moi["composition"] as String).contains("Voyante"))
        assertFalse(salon.toutLeMondeEstPret())
    }

    @Test
    fun `seul le joueur appelé voit son action la nuit`() {
        val (salon, _) = salonPret()
        val p = salon.partie!!
        val e = p.etape as Etape.AppelCupidon
        p.joueurs.forEach { j ->
            val vue = salon.vueMap(salon.jeton(j.id))
            if (j.id == e.cupidon) {
                assertTrue((vue["boutons"] as List<*>).isNotEmpty())
            } else {
                assertEquals(true, vue["sombre"])
                assertTrue((vue["boutons"] as List<*>).isEmpty())
            }
        }
        // Un autre joueur ne peut pas jouer à la place de Cupidon.
        val intrus = p.joueurs.first { it.id != e.cupidon }
        assertNull(salon.recevoir(salon.jeton(intrus.id), salon.cle(salon.jeton(intrus.id)), "cupidon", listOf(0, 1)))
    }

    @Test
    fun `pendant une transition personne ne peut agir`() {
        val (salon, _) = salonPret()
        val cupidon = (salon.partie!!.etape as Etape.AppelCupidon).cupidon
        salon.enTransition = true
        val j = salon.jeton(cupidon)
        assertNull(salon.recevoir(j, salon.cle(j), "cupidon", listOf(0, 1)))
        salon.enTransition = false
        assertNotNull(salon.recevoir(j, salon.cle(j), "cupidon", listOf(0, 1)))
    }

    @Test
    fun `un appui venu d'un écran périmé est ignoré`() {
        val (salon, _) = salonPret()
        val cupidon = (salon.partie!!.etape as Etape.AppelCupidon).cupidon
        val j = salon.jeton(cupidon)
        val vieilleCle = salon.cle(j)
        salon.appui(j, "cupidon", 0, 1)
        assertNull(salon.recevoir(j, vieilleCle, "cupidon", listOf(2, 3)))
    }

    @Test
    fun `partie complète à six téléphones`() {
        val (salon, _) = salonPret()
        val p = salon.partie!!
        fun idDe(r: Role) = p.joueurs.first { it.role == r }.id
        val loups = p.joueurs.filter { it.role == Role.LOUP_GAROU }.map { it.id }

        // Cupidon s'unit à la Voyante.
        val amoureux = idDe(Role.CUPIDON)
        salon.appui(salon.jeton(amoureux), "cupidon", amoureux, idDe(Role.VOYANTE))

        // Tout le monde lit son secret ; l'étape ne passe qu'au dernier.
        val e = p.etape as Etape.DecouverteAmoureux
        e.joueurs.dropLast(1).forEach { assertNull(salon.appui(salon.jeton(it), "lu")) }
        @Suppress("UNCHECKED_CAST")
        val moi = salon.vueMap(salon.jeton(amoureux))["moi"] as Map<String, Any?>
        assertNotNull(moi["amoureux"])
        assertEquals(Action.Ok, salon.appui(salon.jeton(e.joueurs.last()), "lu"))

        // La Voyante sonde un loup et le voit.
        val voyante = salon.jeton(idDe(Role.VOYANTE))
        salon.appui(voyante, "sonder", loups[0])
        assertTrue((salon.vueMap(voyante)["titre"] as String).contains("Loup-Garou"))
        salon.appui(voyante, "ok")

        // Les loups ne tombent pas d'accord, puis s'accordent sur le Chasseur.
        val chasseur = idDe(Role.CHASSEUR)
        assertNull(salon.appui(salon.jeton(loups[0]), "devorer", chasseur))
        assertNull(salon.appui(salon.jeton(loups[1]), "devorer", idDe(Role.SORCIERE)))
        assertNotNull(salon.appui(salon.jeton(loups[1]), "devorer", chasseur))

        // La Sorcière ne fait rien.
        val sorciere = salon.jeton(idDe(Role.SORCIERE))
        assertEquals(Action.ChoixSorciere(false, null), salon.appui(sorciere, "sorciere"))

        // Matin : le Chasseur est mort, il tire sur un loup.
        val annonce = p.etape as Etape.Annonce
        assertEquals(listOf(chasseur), annonce.morts.map { it.joueurId })
        p.jouer(Action.Ok)
        assertTrue(p.etape is Etape.TirDuChasseur)
        salon.appui(salon.jeton(chasseur), "tirer", loups[0])
        p.jouer(Action.Ok)

        // Élection du Capitaine : égalité, puis revote.
        assertEquals(Etape.ElectionCapitaine, p.etape)
        val vivants = p.vivants().map { it.id }
        val a = vivants[0]
        val b = vivants[1]
        vivants.forEachIndexed { i, v -> salon.appui(salon.jeton(v), "voter", if (i % 2 == 0) a else b) }
        assertEquals(Etape.ElectionCapitaine, p.etape, "égalité : on revote")
        vivants.forEach { salon.appui(salon.jeton(it), "voter", a) }
        assertEquals(a, p.capitaine)

        // Vote du village : tout le monde vote contre le dernier loup.
        assertTrue(p.etape is Etape.Vote)
        p.vivants().map { it.id }.forEach { salon.appui(salon.jeton(it), "voter", loups[1]) }
        assertTrue(salon.dernierDecompte!!.contains("voix"))
        p.jouer(Action.Ok)
        assertEquals(Etape.Fin(Camp.VILLAGEOIS), p.etape)
        assertTrue((salon.vueMap(salon.jeton(0))["titre"] as String).contains("village"))
    }

    @Test
    fun `la voix du Capitaine compte double et l'égalité passe par lui`() {
        val (salon, _) = salonPret(7)
        val p = salon.partie!!
        fun idDe(r: Role) = p.joueurs.first { it.role == r }.id
        val loups = p.joueurs.filter { it.role == Role.LOUP_GAROU }.map { it.id }
        val autres = p.joueurs.filter { !it.role.estLoup }.map { it.id }
        salon.appui(salon.jeton(idDe(Role.CUPIDON)), "cupidon", autres[0], autres[1])
        p.joueurs.forEach { salon.appui(salon.jeton(it.id), "lu") }
        salon.appui(salon.jeton(idDe(Role.VOYANTE)), "sonder", loups[0])
        salon.appui(salon.jeton(idDe(Role.VOYANTE)), "ok")
        // La Petite fille n'est pas en jeu : le bouton « surprise » est refusé.
        assertNull(salon.recevoir(salon.jeton(loups[0]), salon.cle(salon.jeton(loups[0])), "surprise", listOf(autres[2])))
        loups.forEach { salon.appui(salon.jeton(it), "devorer", idDe(Role.SORCIERE)) }
        // La Sorcière se sauve elle-même.
        val sorciere = salon.jeton(idDe(Role.SORCIERE))
        salon.appui(sorciere, "guerir")
        assertEquals(Action.ChoixSorciere(true, null), salon.appui(sorciere, "sorciere"))
        p.jouer(Action.Ok)
        // Le premier loup devient Capitaine.
        p.vivants().forEach { salon.appui(salon.jeton(it.id), "voter", loups[0]) }
        assertEquals(loups[0], p.capitaine)
        // 3 voix contre autres[2], 2 voix + Capitaine (double) contre autres[3] : 4 contre 3.
        val votants = p.vivants().map { it.id }
        votants.forEach { v ->
            val cible = if (v in loups || v == autres[2]) autres[3] else autres[2]
            salon.appui(salon.jeton(v), "voter", cible)
        }
        val annonce = p.etape as Etape.Annonce
        assertEquals(autres[3], annonce.morts.first().joueurId)
    }

    @Test
    fun `la meute discute en secret pendant son tour et garde la discussion`() {
        val (salon, _) = salonPret()
        val p = salon.partie!!
        fun idDe(r: Role) = p.joueurs.first { it.role == r }.id
        val loups = p.joueurs.filter { it.role == Role.LOUP_GAROU }.map { it.id }
        val villageois = p.joueurs.filter { !it.role.estLoup }.map { it.id }
        val l0 = salon.jeton(loups[0])
        val l1 = salon.jeton(loups[1])

        // Avant le tour des loups, personne ne peut écrire.
        assertFalse(salon.ecrireMeute(l0, "trop tôt"))
        val cupidon = idDe(Role.CUPIDON)
        // Cupidon unit les deux loups : personne ne mourra de chagrin pendant le test.
        salon.appui(salon.jeton(cupidon), "cupidon", loups[0], loups[1])
        p.joueurs.forEach { salon.appui(salon.jeton(it.id), "lu") }
        salon.appui(salon.jeton(idDe(Role.VOYANTE)), "sonder", loups[0])
        salon.appui(salon.jeton(idDe(Role.VOYANTE)), "ok")
        assertTrue(p.etape is Etape.AppelLoups)

        // Un loup écrit, l'autre lit ; un villageois ne peut ni écrire ni lire.
        assertTrue(salon.ecrireMeute(l0, "  On mange   la Sorcière ?  "))
        assertFalse(salon.ecrireMeute(l0, "   "))
        assertFalse(salon.ecrireMeute(salon.jeton(villageois[0]), "Je suis un loup"))
        assertFalse(salon.ecrireMeute("inconnu", "coucou"))
        @Suppress("UNCHECKED_CAST")
        fun messages(j: String) = (salon.vueMap(j)["meute"] as Map<String, Any?>)["messages"] as List<Map<String, Any?>>
        val vus = messages(l1)
        assertEquals(1, vus.size)
        assertEquals("On mange la Sorcière ?", vus[0]["texte"])
        assertEquals(false, vus[0]["moi"])
        assertEquals(true, messages(l0)[0]["moi"])
        assertNull(salon.vueMap(salon.jeton(villageois[0]))["meute"])
        assertTrue(salon.ecrireMeute(l1, "x".repeat(500)))
        assertEquals(Salon.MAX_MESSAGE, (messages(l0)[1]["texte"] as String).length)

        // Écrire ne change pas l'écran : un appui sur « Dévorer » reste valable.
        val cle = salon.cle(l0)
        salon.ecrireMeute(l1, "D'accord")
        assertEquals(cle, salon.cle(l0))
        assertFalse(salon.attenteTablette().joinToString().contains("Sorcière ?"))

        loups.forEach { salon.appui(salon.jeton(it), "devorer", idDe(Role.SORCIERE)) }
        assertFalse(salon.ecrireMeute(l0, "après le tour"))
        assertNull(salon.vueMap(l0)["meute"])
        salon.appui(salon.jeton(idDe(Role.SORCIERE)), "sorciere")
        // Le jour passe : un loup devient Capitaine, le village élimine Cupidon.
        while (p.etape !is Etape.AppelLoups && p.etape !is Etape.Fin) {
            val e = p.etape
            val vivants = p.vivants().map { it.id }
            when (e) {
                Etape.ElectionCapitaine -> vivants.forEach { salon.appui(salon.jeton(it), "voter", loups[0]) }
                is Etape.Vote -> vivants.forEach { salon.appui(salon.jeton(it), "voter", cupidon) }
                is Etape.AppelVoyante -> salon.appui(salon.jeton(e.voyante), "sonder", idDe(Role.CHASSEUR))
                else -> p.jouer(Action.Ok)
            }
        }
        // Deuxième nuit : la discussion de la veille est toujours là.
        assertTrue(p.etape is Etape.AppelLoups)
        assertEquals(3, messages(l0).size)
        assertEquals(1, messages(l0)[0]["nuit"])
        assertTrue(salon.ecrireMeute(l1, "Rebelote"))
        assertEquals(2, messages(l0).last()["nuit"])

        // Nouvelle partie : la discussion est effacée.
        salon.rejouer()
        assertFalse(salon.ecrireMeute(l0, "encore là ?"))
    }

    @Test
    fun `un loup seul n'a pas de messagerie`() {
        val salon = Salon(Random(2))
        val jetons = listOf("Ana", "Bob", "Chloé", "Dan").map { salon.rejoindre(it).getOrThrow().jeton }
        salon.distribuer(listOf(Role.LOUP_GAROU, Role.VILLAGEOIS, Role.VILLAGEOIS, Role.VILLAGEOIS))
        jetons.forEach {
            salon.recevoir(it, "revelation", "reveler", emptyList())
            salon.recevoir(it, "revelation", "cacher", emptyList())
        }
        val p = salon.commencer()
        while (p.etape !is Etape.AppelLoups) p.jouer(Action.Ok)
        val loup = salon.jeton(p.joueurs.first { it.role == Role.LOUP_GAROU }.id)
        assertNull(salon.vueMap(loup)["meute"])
        assertFalse(salon.ecrireMeute(loup, "Il y a quelqu'un ?"))
    }

    @Test
    fun `les téléphones passent par le petit serveur web`() {
        val salon = Salon(Random(1))
        val portail = Portail(
            salon,
            executer = { synchronized(salon) { it() } },
            fichier = { if (it == "index.html") Reponse(200, "text/html", "<html>ok</html>".toByteArray()) else null },
            jouer = { salon.partie?.jouer(it) },
            change = {},
        )
        val serveur = ServeurHttp(0, portail::traiter)
        serveur.demarrer()
        try {
            val base = "http://127.0.0.1:${serveur.portEcoute}"
            assertTrue(lire("$base/").contains("ok"))
            val rep = lire("$base/rejoindre", "nom=Ana%20Lou")
            assertTrue(rep.contains("jeton"))
            assertEquals("Ana Lou", salon.places.single().nom)
            val jeton = salon.places.single().jeton
            assertTrue(lire("$base/etat?j=$jeton").contains("Bienvenue, Ana Lou"))
            assertTrue(lire("$base/etat?j=inconnu").contains("\"inscription\":true"))
        } finally {
            serveur.arreter()
        }
    }

    private fun lire(url: String, corps: String? = null): String {
        val c = URL(url).openConnection() as HttpURLConnection
        if (corps != null) {
            c.requestMethod = "POST"
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            c.outputStream.use { it.write(corps.toByteArray()) }
        }
        return c.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
    }

    @Test
    fun `le JSON échappe les caractères spéciaux`() {
        assertEquals("""{"a":"x\"y\n","b":[1,true,null]}""", Json.ecrire(mapOf("a" to "x\"y\n", "b" to listOf(1, true, null))))
    }

    @Test
    fun `en Chaos les telephones ne voient que l'eventail, et la nuit appelle tous les roles`() {
        val salon = Salon(Random(3))
        val jetons = noms.map { salon.rejoindre(it).getOrThrow().jeton }
        val chaos = Composition.pour(noms.size, Reglages(TypeDePartie.CHAOS), Random(5))
        salon.distribuer(chaos)
        assertTrue(salon.composition().startsWith("Composition cachée"))
        assertTrue(salon.lesRoles().all { it["nombre"] == 0 })
        assertEquals(Role.entries.size + 1, salon.lesRoles().size)
        assertEquals(true, salon.vueMap(jetons[0])["compositionCachee"])
        jetons.forEach {
            salon.recevoir(it, "revelation", "reveler", emptyList())
            salon.recevoir(it, "revelation", "cacher", emptyList())
        }
        val p = salon.commencer()
        assertEquals(Role.entries.toSet(), p.rolesPossibles)
        // Le Voleur est toujours dans le paquet Chaos : la première nuit commence par lui, vrai ou fictif.
        assertTrue(p.etape is Etape.AppelVoleur || p.etape == Etape.AppelFictif(fr.loupgarou.engine.Appel.VOLEUR))
    }
}
