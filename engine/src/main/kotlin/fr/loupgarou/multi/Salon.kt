package fr.loupgarou.multi

import fr.loupgarou.engine.Bilan
import fr.loupgarou.engine.Action
import fr.loupgarou.engine.Camp
import fr.loupgarou.engine.CauseDeMort
import fr.loupgarou.engine.Composition
import fr.loupgarou.engine.Eventail
import fr.loupgarou.engine.Etape
import fr.loupgarou.engine.Joueur
import fr.loupgarou.engine.Partie
import fr.loupgarou.engine.Role
import fr.loupgarou.engine.Setup
import kotlin.random.Random

/**
 * Partie « chacun son téléphone ». La tablette garde le moteur et la voix ; chaque joueur se
 * connecte depuis le navigateur de son téléphone. Le salon sait ce que chaque téléphone doit
 * montrer ([vue]) et transforme les choix des joueurs en [Action] pour le moteur ([recevoir]).
 *
 * Le salon ne joue jamais lui-même : la tablette appelle [Partie.jouer] avec l'action rendue
 * par [recevoir], pour que la narration suive comme dans le mode à une tablette.
 * Toutes les méthodes s'appellent depuis le même fil (celui de la tablette).
 */
class Salon(private val hasard: Random = Random) {

    class Place(val id: Int, val nom: String, val jeton: String)

    enum class Phase { INSCRIPTION, REVELATION, JEU }

    private val _places = mutableListOf<Place>()
    val places: List<Place> get() = _places

    var phase = Phase.INSCRIPTION
        private set
    var partie: Partie? = null
        private set
    var paquet: List<Role> = emptyList()
        private set
    /** Non nul quand la composition est cachée (Chaos) : c'est tout ce que les joueurs en savent. */
    var eventail: Eventail? = null
        private set
    private var roles = mapOf<Int, Role>()
    private var centre = listOf<Role>()
    private val reveles = mutableSetOf<Int>()
    private val prets = mutableSetOf<Int>()
    private var prochainId = 0

    /** Vrai pendant que le narrateur endort quelqu'un : aucun téléphone n'agit. Réglé par la tablette. */
    var enTransition = false

    // --- Ce qui se passe pendant l'étape en cours, remis à zéro à chaque nouvelle étape ---

    private data class Proposition(val cible: Int, val petiteFille: Boolean)

    private var etapeSuivie: Etape? = null
    private var revision = 0
    private val propositions = mutableMapOf<Int, Proposition>()
    private val votes = mutableMapOf<Int, Int>()
    private var candidatsElection: List<Int>? = null
    private val lus = mutableSetOf<Int>()
    private var guerir = false

    // --- La messagerie de la meute, gardée d'une nuit à l'autre, effacée à chaque partie ---

    private class MessageMeute(val de: Int, val texte: String, val nuit: Int)

    private val meute = mutableListOf<MessageMeute>()

    /** Le décompte du dernier vote, montré à tous à l'annonce qui suit. */
    var dernierDecompte: String? = null
        private set

    // --- Inscription ---

    fun rejoindre(nomBrut: String): Result<Place> {
        val nom = nomBrut.trim().replace(Regex("\\s+"), " ").take(20)
        if (phase != Phase.INSCRIPTION) return Result.failure(IllegalStateException("La partie a déjà commencé."))
        if (nom.isEmpty()) return Result.failure(IllegalArgumentException("Écris ton prénom."))
        if (_places.any { it.nom.equals(nom, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("Ce prénom est déjà pris\u00A0: ajoute une initiale."))
        }
        if (_places.size >= MAX_JOUEURS) return Result.failure(IllegalStateException("Le village est plein."))
        val jeton = (1..24).map { ALPHABET[hasard.nextInt(ALPHABET.length)] }.joinToString("")
        val place = Place(prochainId++, nom, jeton)
        _places += place
        return Result.success(place)
    }

    fun place(jeton: String?) = _places.firstOrNull { it.jeton == jeton }

    fun retirer(id: Int) {
        if (phase == Phase.INSCRIPTION) _places.removeAll { it.id == id }
    }

    /** L'appli tire les rôles au hasard ; chacun va découvrir le sien sur son téléphone. */
    fun distribuer(paquet: List<Role>) = distribuer(Composition(paquet))

    fun distribuer(composition: Composition) {
        check(phase == Phase.INSCRIPTION)
        val paquet = composition.paquet
        eventail = composition.eventail
        val (tires, reste) = Setup.tirerRoles(paquet, _places.size, hasard)
        this.paquet = paquet
        roles = _places.mapIndexed { i, p -> p.id to tires[i] }.toMap()
        centre = reste
        reveles.clear()
        prets.clear()
        phase = Phase.REVELATION
    }

    fun nbPrets() = prets.size
    fun pasEncorePrets() = _places.filter { it.id !in prets }.map { it.nom }
    fun toutLeMondeEstPret() = phase == Phase.REVELATION && prets.size == _places.size

    /** Tout le monde a vu son rôle : la première nuit tombe. */
    fun commencer(): Partie {
        check(phase == Phase.REVELATION)
        val joueurs = _places.map { Joueur(it.id, it.nom, roles.getValue(it.id)) }
        val p = Partie(joueurs, paquet, centre, eventail?.roles ?: paquet.toSet())
        partie = p
        phase = Phase.JEU
        etapeSuivie = null
        dernierDecompte = null
        meute.clear()
        return p
    }

    /** Nouvelle partie avec les mêmes joueurs, déjà connectés. */
    fun rejouer() {
        phase = Phase.INSCRIPTION
        partie = null
        roles = emptyMap()
        etapeSuivie = null
        dernierDecompte = null
        meute.clear()
    }

    /**
     * Un loup écrit à sa meute. Possible seulement pendant le tour des loups, pour un loup vivant
     * qui n'est pas seul. Renvoie vrai si le message est gardé.
     */
    fun ecrireMeute(jeton: String?, texteBrut: String): Boolean {
        val moi = place(jeton) ?: return false
        if (phase != Phase.JEU || enTransition) return false
        val p = partie ?: return false
        val e = p.etape as? Etape.AppelLoups ?: return false
        if (moi.id !in e.loups || e.loups.size < 2) return false
        val texte = texteBrut.trim().replace(Regex("\\s+"), " ").take(MAX_MESSAGE)
        if (texte.isEmpty()) return false
        meute += MessageMeute(moi.id, texte, p.nuit)
        while (meute.size > MAX_MESSAGES) meute.removeAt(0)
        return true
    }

    /** La discussion vue par un loup : toute la partie, avec la nuit de chaque message. */
    private fun vueMeute(p: Partie, moi: Int): Map<String, Any?> = mapOf(
        "messages" to meute.map {
            mapOf("de" to p.joueur(it.de).nom, "moi" to (it.de == moi), "texte" to it.texte, "nuit" to it.nuit)
        },
        "nuit" to p.nuit,
        "rapides" to buildList {
            add("D'accord")
            add("Pas lui")
            if (Role.VOYANTE in p.rolesPossibles) add("Je pense que c'est la Voyante")
            if (Role.SORCIERE in p.rolesPossibles) add("Méfiance, la Sorcière veille")
            add("On se dépêche")
        },
    )

    // --- Ce que les téléphones envoient ---

    /**
     * Un joueur a touché un bouton. [cle] est celle de l'écran qu'il voyait : un appui venu d'un
     * écran périmé (double appui, réseau lent) est ignoré. Renvoie l'action à jouer, ou null.
     */
    fun recevoir(jeton: String?, cle: String?, action: String, choix: List<Int>): Action? {
        val moi = place(jeton) ?: return null
        synchroniser()
        if (cle != null && cle != cleCourante()) return null
        when (phase) {
            Phase.INSCRIPTION -> return null
            Phase.REVELATION -> {
                when (action) {
                    "reveler" -> reveles += moi.id
                    "cacher" -> if (moi.id in reveles) prets += moi.id
                }
                return null
            }
            Phase.JEU -> {}
        }
        if (enTransition) return null
        val p = partie ?: return null
        val j = p.joueur(moi.id)
        return when (val e = p.etape) {
            is Etape.AppelVoleur -> if (e.voleur == j.id && action.startsWith("voleur:")) {
                val carte = action.removePrefix("voleur:").takeIf { it.isNotEmpty() }?.let { Role.valueOf(it) }
                if (e.doitPrendreLoup && carte != Role.LOUP_GAROU) null
                else if (carte != null && carte !in e.cartes) null
                else Action.ChoixVoleur(carte)
            } else null
            is Etape.AppelCupidon -> if (e.cupidon == j.id && action == "cupidon" && choix.size == 2 &&
                choix[0] != choix[1] && choix.all { vivant(p, it) }
            ) Action.ChoixCupidon(choix[0], choix[1]) else null
            is Etape.DecouverteAmoureux -> {
                if (action == "lu" && j.id in e.joueurs) lus += j.id
                if (lus.containsAll(e.joueurs)) Action.Ok else null
            }
            is Etape.AppelVoyante -> if (e.voyante == j.id && action == "sonder") {
                choix.singleOrNull()?.takeIf { it != j.id && vivant(p, it) }?.let { Action.Designation(it) }
            } else null
            is Etape.VisionVoyante -> if (estVoyante(p, j) && action == "ok") Action.Ok else null
            is Etape.AppelLoups -> {
                if (j.id !in e.loups) return null
                val cible = choix.singleOrNull()?.takeIf { vivant(p, it) } ?: return null
                when (action) {
                    "devorer" -> propositions[j.id] = Proposition(cible, false)
                    "surprise" -> if (Role.PETITE_FILLE in p.rolesPossibles) propositions[j.id] = Proposition(cible, true) else return null
                    else -> return null
                }
                val accord = e.loups.map { propositions[it] }.distinct().singleOrNull()
                accord?.let { Action.ChoixLoups(it.cible, if (it.petiteFille) it.cible else null) }
            }
            is Etape.AppelSorciere -> {
                if (e.sorciere != j.id) return null
                when (action) {
                    "guerir" -> {
                        if (e.peutGuerir) guerir = !guerir
                        null
                    }
                    "sorciere" -> {
                        val poison = choix.singleOrNull()
                        if (poison != null && (!e.peutEmpoisonner || !vivant(p, poison))) null
                        else Action.ChoixSorciere(guerir && e.peutGuerir, poison)
                    }
                    else -> null
                }
            }
            is Etape.TirDuChasseur -> if (e.chasseur == j.id && action == "tirer") {
                choix.singleOrNull()?.takeIf { vivant(p, it) }?.let { Action.Designation(it) }
            } else null
            is Etape.SuccessionCapitaine -> if (e.capitaine == j.id && action == "successeur") {
                choix.singleOrNull()?.takeIf { vivant(p, it) }?.let { Action.Designation(it) }
            } else null
            Etape.ElectionCapitaine -> if (j.vivant && action == "voter") {
                val c = choix.singleOrNull()?.takeIf { it in candidatsElection(p) } ?: return null
                votes[j.id] = c
                depouillerElection(p)
            } else null
            is Etape.Vote -> if (j.vivant && action == "voter") {
                val c = choix.singleOrNull()?.takeIf { it in e.candidats } ?: return null
                votes[j.id] = c
                depouillerVote(p, e)
            } else null
            is Etape.DepartageCapitaine -> if (e.capitaine == j.id && action == "trancher") {
                choix.singleOrNull()?.takeIf { it in e.exAequo }?.let { Action.Designation(it) }
            } else null
            is Etape.AppelFictif, is Etape.Annonce, is Etape.Fin -> null
        }
    }

    private fun candidatsElection(p: Partie) = candidatsElection ?: p.vivants().map { it.id }

    /** Tous les vivants ont voté : majorité relative, sinon on revote entre les ex-æquo. */
    private fun depouillerElection(p: Partie): Action? {
        val votants = p.vivants().map { it.id }
        if (!votes.keys.containsAll(votants)) return null
        val (gagnants, texte) = compter(p, votes, double = null)
        dernierDecompte = texte
        if (gagnants.size == 1) return Action.Designation(gagnants.single())
        candidatsElection = gagnants
        votes.clear()
        revision++
        return null
    }

    private fun depouillerVote(p: Partie, e: Etape.Vote): Action? {
        val votants = p.vivants().map { it.id }
        if (!votes.keys.containsAll(votants)) return null
        val (gagnants, texte) = compter(p, votes, double = p.capitaine?.takeIf { vivant(p, it) })
        dernierDecompte = texte
        return if (gagnants.size == 1) Action.Elimination(gagnants.single()) else Action.Egalite(gagnants)
    }

    /** Les plus désignés, et le décompte lisible. La voix du Capitaine compte double. */
    private fun compter(p: Partie, votes: Map<Int, Int>, double: Int?): Pair<List<Int>, String> {
        val voix = mutableMapOf<Int, Int>()
        votes.forEach { (votant, cible) -> voix[cible] = (voix[cible] ?: 0) + if (votant == double) 2 else 1 }
        val max = voix.values.maxOrNull() ?: 0
        val gagnants = voix.filterValues { it == max }.keys.sorted()
        val texte = voix.entries.sortedByDescending { it.value }
            .joinToString(", ") { "${p.joueur(it.key).nom}\u00A0: ${it.value} voix" }
        return gagnants to texte
    }

    // --- Ce que montre chaque téléphone ---

    /** L'écran d'un téléphone, en JSON. Un jeton inconnu reçoit l'écran d'inscription. */
    fun vue(jeton: String?): String = Json.ecrire(vueMap(jeton))

    fun vueMap(jeton: String?): Map<String, Any?> {
        synchroniser()
        val moi = place(jeton) ?: return inscription()
        return when (phase) {
            Phase.INSCRIPTION -> ecran(
                "jour", "Bienvenue, ${moi.nom}",
                listOf(
                    "Tu es bien arrivé au village. La partie commencera sur la tablette quand tout le monde sera là.",
                    "Déjà là\u00A0: " + _places.joinToString(", ") { it.nom },
                ),
                image = "embleme_village",
            )
            Phase.REVELATION -> revelation(moi)
            Phase.JEU -> enJeu(moi, partie!!)
        } + ("moi" to fiche(moi)) + ("roles" to lesRoles()) + ("compositionCachee" to (eventail != null))
    }

    /**
     * L'explication de chaque rôle en jeu (tous les rôles tant que la composition n'est pas
     * choisie), consultable par tous : la composition est publique. Quand elle est cachée (Chaos),
     * on montre tous les rôles possibles, sans leur nombre.
     */
    fun lesRoles(): List<Map<String, Any?>> {
        val cache = eventail
        val enJeu = (cache?.roles?.toList() ?: paquet.distinct()).sortedBy { it.ordinal }.ifEmpty { Role.entries }
        return enJeu.map { r ->
            mapOf(
                "role" to r.nom,
                "nombre" to if (cache != null) 0 else paquet.count { it == r },
                "camp" to Fiches.camp(r),
                "image" to Fiches.embleme(r),
                "fiche" to Fiches.fiche(r),
            )
        } + mapOf(
            "role" to "Capitaine",
            "nombre" to 0,
            "camp" to "Élu par le village, en plus de son rôle",
            "image" to "embleme_capitaine",
            "fiche" to listOf("Le premier jour, le village élit un Capitaine.", Fiches.capitaine.removePrefix("Tu es Capitaine : ").replaceFirstChar { it.uppercase() }),
        )
    }

    private fun inscription(): Map<String, Any?> =
        if (phase == Phase.INSCRIPTION) mapOf("cle" to "inscription", "inscription" to true, "theme" to "jour")
        else ecran("nuit", "La partie a déjà commencé", listOf("Il faudra attendre la prochaine pour rejoindre le village."))

    private fun revelation(moi: Place): Map<String, Any?> = when (moi.id) {
        in prets -> ecran(
            "nuit", "Ton rôle est caché",
            listOf(
                "Tu peux le revoir à tout moment avec le bouton « Mon rôle », en bas de l'écran.",
                "Prêts\u00A0: ${prets.size} sur ${_places.size}. La nuit tombera depuis la tablette.",
            ),
        )
        in reveles -> {
            val r = roles.getValue(moi.id)
            ecran(
                "nuit", "Tu es ${article(r)}",
                listOf(Fiches.camp(r)) + Fiches.fiche(r),
                image = Fiches.embleme(r),
                boutons = listOf(bouton("J'ai retenu, je cache mon écran", "cacher")),
            )
        }
        else -> ecran(
            "nuit", "Ta carte t'attend",
            listOf("Cache ton écran des regards, puis découvre ton rôle."),
            boutons = listOf(bouton("Découvrir mon rôle", "reveler")),
        )
    }

    private fun enJeu(moi: Place, p: Partie): Map<String, Any?> {
        val j = p.joueur(moi.id)
        val e = p.etape
        if (enTransition) {
            return if (deNuit(e)) dodo(j) else ecran("jour", "Le village se réveille…", emptyList())
        }
        val vivants = p.vivants()
        return when (e) {
            is Etape.AppelVoleur -> if (e.voleur != j.id) dodo(j) else ecran(
                "nuit", "Voleur, voici les cartes du centre",
                listOf(e.cartes.joinToString(" et ") { it.nom }) +
                    if (e.doitPrendreLoup) listOf("Deux Loups-Garous\u00A0: tu dois en prendre un.") else emptyList(),
                image = "embleme_voleur",
                boutons = (if (e.doitPrendreLoup) emptyList() else listOf(bouton("Garder ma carte", "voleur:"))) +
                    e.cartes.distinct().map { bouton("Prendre ${it.nom}", "voleur:${it.name}") },
            )
            is Etape.AppelCupidon -> if (e.cupidon != j.id) dodo(j) else ecran(
                "nuit", "Cupidon, choisis deux Amoureux",
                listOf("Tu peux te choisir toi-même."),
                image = "embleme_cupidon",
                choix = choix(vivants, min = 2, max = 2),
                boutons = listOf(bouton("Unir ces deux-là", "cupidon", "requis")),
            )
            is Etape.DecouverteAmoureux -> if (j.id !in e.joueurs || j.id in lus) dodo(j) else {
                val aime = j.amoureuxDe?.let(p::joueur)
                ecran(
                    "nuit",
                    aime?.let { "Tu es amoureux de ${it.nom}" } ?: "La flèche de Cupidon ne t'a pas touché",
                    if (aime != null) listOf(Fiches.amoureux) else listOf("Rien de nouveau pour toi. Garde un air mystérieux."),
                    image = if (aime != null) "embleme_amoureux" else "embleme_cupidon",
                    boutons = listOf(bouton("J'ai lu, je ferme les yeux", "lu")),
                )
            }
            is Etape.AppelVoyante -> if (e.voyante != j.id) dodo(j) else ecran(
                "nuit", "Voyante, qui veux-tu sonder\u00A0?", emptyList(),
                image = "embleme_voyante",
                choix = choix(vivants.filter { it.id != j.id }),
                boutons = listOf(bouton("Sonder {nom}", "sonder", "requis", "Touche un joueur")),
            )
            is Etape.VisionVoyante -> if (!estVoyante(p, j)) dodo(j) else ecran(
                "nuit", "${p.joueur(e.cible).nom} est ${article(e.role)}", listOf("Garde ce secret pour toi… ou presque."),
                image = Fiches.embleme(e.role),
                boutons = listOf(bouton("Fermer les yeux", "ok")),
            )
            is Etape.AppelLoups -> if (j.id !in e.loups) dodo(j) else {
                val marques = e.loups.mapNotNull { l -> propositions[l]?.let { it.cible to p.joueur(l).nom + if (it.petiteFille) " (Petite fille !)" else "" } }
                    .groupBy({ it.first }, { it.second })
                val autres = e.loups.filter { it != j.id }.map { p.joueur(it).nom }
                val ecranLoups = ecran(
                    "nuit", "Loups, qui dévorez-vous\u00A0?",
                    listOf(
                        if (autres.isEmpty()) "Tu chasses seul cette nuit." else "Ta meute\u00A0: ${autres.joinToString(", ")}.",
                        "Mettez-vous d'accord\u00A0: la victime est choisie quand tous les loups désignent la même.",
                    ),
                    image = "embleme_meute",
                    choix = choix(vivants, marques = marques),
                    boutons = listOf(bouton("Dévorer {nom}", "devorer", "requis", "Touche un joueur")) +
                        if (Role.PETITE_FILLE in p.rolesPossibles) {
                            listOf(bouton("{nom} est la Petite fille, on l'a surprise\u00A0!", "surprise", "requis", "Petite fille surprise\u00A0? Touche-la", discret = true))
                        } else emptyList(),
                )
                if (autres.isEmpty()) ecranLoups else ecranLoups + ("meute" to vueMeute(p, j.id))
            }
            is Etape.AppelSorciere -> if (e.sorciere != j.id) dodo(j) else {
                val victime = e.victime?.let(p::joueur)
                ecran(
                    "nuit", victime?.let { "Les loups ont choisi ${it.nom}" } ?: "Personne n'a été attaqué",
                    buildList {
                        if (!p.potionGuerison) add("Ta potion de guérison est déjà utilisée.")
                        if (e.peutEmpoisonner) add("Pour empoisonner quelqu'un, touche-le avant de valider.")
                        else add("Ta potion de poison est déjà utilisée.")
                    },
                    image = "embleme_sorciere",
                    choix = if (e.peutEmpoisonner) choix(vivants, min = 0) else null,
                    boutons = buildList {
                        if (e.peutGuerir && victime != null) {
                            add(bouton(if (guerir) "✓ ${victime.nom} sera sauvé" else "Sauver ${victime.nom}", "guerir", choisi = guerir, bascule = true))
                        }
                        add(bouton("Valider\u00A0: empoisonner {nom}", "sorciere", "optionnel", if (guerir) "Valider" else "Ne rien faire cette nuit"))
                    },
                )
            }
            is Etape.AppelFictif -> dodo(j)
            is Etape.Annonce -> ecran(
                "jour",
                if (j.id in e.morts.map { it.joueurId }) "Tu es mort" else Bilan.titre(e),
                buildList {
                    if (j.id in e.morts.map { it.joueurId }) add(Bilan.titre(e))
                    Bilan.sauvetage(e)?.let(::add)
                    if (e.moment != fr.loupgarou.engine.Moment.MATIN) dernierDecompte?.let { add("Votes\u00A0: $it") }
                    e.morts.forEach { add(Bilan.ligne(it)) }
                    if (j.id in e.morts.map { it.joueurId }) add("Tu ne peux plus parler, mais tu peux tout regarder.")
                },
                image = if (e.morts.isEmpty()) "embleme_village" else "fantome",
            )
            is Etape.TirDuChasseur -> if (e.chasseur != j.id) attente(j, "Le Chasseur vise…") else ecran(
                "jour", "Chasseur, tu emportes quelqu'un avec toi", emptyList(),
                image = "embleme_chasseur",
                choix = choix(vivants),
                boutons = listOf(bouton("Tirer sur {nom}", "tirer", "requis", "Touche un joueur")),
            )
            is Etape.SuccessionCapitaine -> if (e.capitaine != j.id) attente(j, "Le Capitaine désigne son successeur…") else ecran(
                "jour", "Désigne le nouveau Capitaine", listOf("Dans ton dernier souffle."),
                image = "embleme_capitaine",
                choix = choix(vivants),
                boutons = listOf(bouton("{nom} devient Capitaine", "successeur", "requis", "Touche un joueur")),
            )
            Etape.ElectionCapitaine -> vote(
                j, p, "Élisez votre Capitaine", candidatsElection(p).map(p::joueur),
                if (candidatsElection != null) listOf("Égalité ! ${dernierDecompte.orEmpty()}. On revote entre les ex-æquo.")
                else listOf("Sa voix comptera double."),
                "Élire {nom}", "embleme_capitaine",
            )
            is Etape.Vote -> vote(
                j, p, if (e.revote) "Revote entre les ex-æquo" else "Le village vote", e.candidats.map(p::joueur),
                if (e.revote) listOf("Égalité\u00A0: ${dernierDecompte.orEmpty()}.") else listOf("Débattez, puis votez. Vous pouvez changer d'avis jusqu'au dernier vote."),
                "Voter contre {nom}", "embleme_vote",
            )
            is Etape.DepartageCapitaine -> if (e.capitaine != j.id) attente(j, "Égalité ! Le Capitaine tranche…", dernierDecompte) else ecran(
                "jour", "Égalité\u00A0: c'est toi qui tranches", listOfNotNull(dernierDecompte),
                image = "embleme_capitaine",
                choix = choix(e.exAequo.map(p::joueur)),
                boutons = listOf(bouton("Éliminer {nom}", "trancher", "requis", "Touche un joueur")),
            )
            is Etape.Fin -> ecran(
                "jour",
                when (e.gagnant) {
                    Camp.VILLAGEOIS -> "Le village a gagné"
                    Camp.LOUPS -> "Les Loups-Garous ont gagné"
                    Camp.AMOUREUX -> "Les Amoureux ont gagné"
                    Camp.PERSONNE -> "Personne n'a survécu"
                },
                p.joueurs.map { "${it.nom}\u00A0: ${it.role.nom}" + if (it.vivant) "" else " (mort)" },
                image = "embleme_" + when (e.gagnant) {
                    Camp.VILLAGEOIS -> "village"
                    Camp.LOUPS -> "loups"
                    Camp.AMOUREUX -> "amoureux"
                    Camp.PERSONNE -> "personne"
                },
            )
        }
    }

    private fun vote(j: Joueur, p: Partie, titre: String, candidats: List<Joueur>, textes: List<String>, libelle: String, image: String): Map<String, Any?> {
        val votants = p.vivants()
        val manquent = votants.filter { it.id !in votes }.map { it.nom }
        val marques = votes.entries.groupBy({ it.value }, { p.joueur(it.key).nom })
        val bilan = "Ont voté\u00A0: ${votes.size} sur ${votants.size}" +
            if (manquent.isNotEmpty() && manquent.size <= 3) ". On attend ${manquent.joinToString(", ")}." else "."
        if (!j.vivant) return ecran("jour", titre, textes + bilan + "Tu es mort\u00A0: tu regardes, en silence.", image = image, choix = choix(candidats, marques = marques, min = -1))
        val monVote = votes[j.id]?.let { "Ton vote\u00A0: ${p.joueur(it).nom}." }
        return ecran(
            "jour", titre, textes + listOfNotNull(monVote, bilan),
            image = image,
            choix = choix(candidats, marques = marques),
            boutons = listOf(bouton(libelle, "voter", "requis", "Touche un joueur")),
        )
    }

    private fun dodo(j: Joueur) = if (j.vivant) {
        ecran("nuit", "Le village dort", listOf("Garde les yeux fermés. Le narrateur t'appellera quand ce sera ton tour."), sombre = true)
    } else {
        ecran("nuit", "Tu es mort", listOf("Tu peux ouvrir les yeux et regarder, mais pas un mot."), image = "fantome", sombre = true)
    }

    private fun attente(j: Joueur, titre: String, texte: String? = null) =
        ecran("jour", titre, listOfNotNull(texte) + if (j.vivant) emptyList() else listOf("Tu es mort\u00A0: tu regardes, en silence."))

    /** La fiche « Mon rôle », toujours à portée de main sur le téléphone. */
    private fun fiche(moi: Place): Map<String, Any?>? {
        val p = partie
        val j = p?.joueur(moi.id)
        val role = j?.role ?: roles[moi.id]?.takeIf { moi.id in reveles } ?: return null
        val e = p?.etape
        val aime = j?.amoureuxDe?.takeIf { e !is Etape.DecouverteAmoureux || moi.id in lus }?.let { p.joueur(it).nom }
        return mapOf(
            "nom" to moi.nom,
            "role" to role.nom,
            "camp" to Fiches.camp(role),
            "image" to Fiches.embleme(role),
            "fiche" to Fiches.fiche(role),
            "amoureux" to aime?.let { "Tu es amoureux de $it. ${Fiches.amoureux}" },
            "capitaine" to if (p?.capitaine == moi.id) Fiches.capitaine else null,
            "vivant" to (j?.vivant ?: true),
            "composition" to composition(),
        )
    }

    /** La composition est publique : chacun peut savoir quels rôles sont en jeu. En Chaos, seul l'éventail est connu. */
    fun composition(): String = eventail?.let { "Composition cachée : ${it.texte()}." }
        ?: ("En jeu : " + paquet.groupingBy { it }.eachCount().entries
            .sortedBy { it.key.ordinal }
            .joinToString(", ") { (r, n) -> if (n > 1) "$n × ${r.nom}" else r.nom })

    // --- Ce que montre la tablette ---

    /** Une ligne pour la tablette : ce qu'on attend, sans rien trahir. */
    fun attenteTablette(): List<String> {
        synchroniser()
        val p = partie ?: return emptyList()
        return when (val e = p.etape) {
            is Etape.AppelLoups -> listOf("Les loups se concertent\u00A0: ${propositions.size} sur ${e.loups.size} ont désigné une victime.")
            is Etape.DecouverteAmoureux -> listOf("Ont lu leur secret\u00A0: ${lus.size} sur ${e.joueurs.size}.")
            Etape.ElectionCapitaine, is Etape.Vote -> buildList {
                val votants = p.vivants()
                add("Ont voté\u00A0: ${votes.size} sur ${votants.size}")
                if (candidatsElection != null && e == Etape.ElectionCapitaine) add("Égalité, on revote\u00A0: ${dernierDecompte.orEmpty()}")
                votes.entries.groupBy({ it.value }, { p.joueur(it.key).nom }).forEach { (cible, qui) ->
                    add("${p.joueur(cible).nom} ← ${qui.joinToString(", ")}")
                }
            }
            is Etape.Annonce -> if (e.moment != fr.loupgarou.engine.Moment.MATIN) listOfNotNull(dernierDecompte?.let { "Votes\u00A0: $it" }) else emptyList()
            else -> emptyList()
        }
    }

    // --- Outils ---

    /** Nouvelle étape du moteur : on oublie les choix en cours de l'étape précédente. */
    private fun synchroniser() {
        val e = partie?.etape ?: return
        if (e === etapeSuivie) return
        etapeSuivie = e
        revision++
        propositions.clear()
        votes.clear()
        lus.clear()
        guerir = false
        if (e != Etape.ElectionCapitaine) candidatsElection = null
    }

    private fun cleCourante() = when (phase) {
        Phase.INSCRIPTION -> "inscription"
        Phase.REVELATION -> "revelation"
        Phase.JEU -> "e$revision" + if (enTransition) "t" else ""
    }

    private fun ecran(
        theme: String,
        titre: String,
        textes: List<String>,
        image: String? = null,
        choix: Map<String, Any?>? = null,
        boutons: List<Map<String, Any?>> = emptyList(),
        sombre: Boolean = false,
    ): Map<String, Any?> = mapOf(
        "cle" to cleCourante(),
        "theme" to theme,
        "titre" to titre,
        "textes" to textes,
        "image" to image,
        "choix" to choix,
        "boutons" to boutons,
        "sombre" to sombre,
    )

    /** [min] à -1 : liste en lecture seule (pour les morts). */
    private fun choix(joueurs: List<Joueur>, min: Int = 1, max: Int = 1, marques: Map<Int, List<String>> = emptyMap()) = mapOf(
        "min" to min,
        "max" to max,
        "options" to joueurs.map { mapOf("id" to it.id, "nom" to it.nom, "marque" to marques[it.id]?.joinToString(", ")) },
    )

    /**
     * Un bouton. [texte] peut contenir {nom}, remplacé par le joueur touché. [selon] dit si le
     * bouton a besoin d'un joueur touché : « aucun », « requis » ou « optionnel » ; [sansChoix]
     * est son texte tant que personne n'est touché.
     */
    private fun bouton(
        texte: String,
        action: String,
        selon: String = "aucun",
        sansChoix: String? = null,
        choisi: Boolean = false,
        discret: Boolean = false,
        bascule: Boolean = false,
    ) = mapOf(
        "texte" to texte, "action" to action, "selon" to selon, "sansChoix" to sansChoix,
        "choisi" to choisi, "discret" to discret, "bascule" to bascule,
    )

    private fun vivant(p: Partie, id: Int) = p.joueurs.any { it.id == id && it.vivant }

    private fun estVoyante(p: Partie, j: Joueur) = j.vivant && j.role == Role.VOYANTE

    private fun deNuit(e: Etape) = when (e) {
        is Etape.AppelVoleur, is Etape.AppelCupidon, is Etape.DecouverteAmoureux, is Etape.AppelVoyante,
        is Etape.VisionVoyante, is Etape.AppelLoups, is Etape.AppelSorciere, is Etape.AppelFictif -> true
        else -> false
    }

    private fun article(r: Role) = Bilan.article(r)

    companion object {
        const val MAX_JOUEURS = 24
        const val MAX_MESSAGE = 200
        const val MAX_MESSAGES = 300
        private const val ALPHABET = "abcdefghijkmnpqrstuvwxyz23456789"
    }
}
