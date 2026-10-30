package fr.loupgarou.engine

/** Les appels de nuit, dans l'ordre de la règle. */
enum class Appel(val premiereNuitSeulement: Boolean = false) {
    VOLEUR(true), CUPIDON(true), AMOUREUX(true), VOYANTE, LOUPS, SORCIERE
}

/** Ce que l'appli doit montrer ou demander maintenant. Chaque étape attend une [Action]. */
sealed interface Etape {
    data class AppelVoleur(val voleur: Int, val cartes: List<Role>, val doitPrendreLoup: Boolean) : Etape
    data class AppelCupidon(val cupidon: Int) : Etape
    data class ReveilAmoureux(val a: Int, val b: Int) : Etape
    data class AppelVoyante(val voyante: Int) : Etape
    data class VisionVoyante(val cible: Int, val role: Role) : Etape
    data class AppelLoups(val loups: List<Int>) : Etape
    data class AppelSorciere(
        val sorciere: Int,
        val victime: Int?,
        val peutGuerir: Boolean,
        val peutEmpoisonner: Boolean,
    ) : Etape

    /** Rôle du paquet que personne ne tient (carte au centre) : on l'appelle quand même pour ne rien trahir. */
    data class AppelFictif(val appel: Appel) : Etape
    data class Annonce(val morts: List<Mort>, val moment: Moment) : Etape
    data class TirDuChasseur(val chasseur: Int) : Etape
    data class SuccessionCapitaine(val capitaine: Int) : Etape
    data object ElectionCapitaine : Etape
    data class Vote(val candidats: List<Int>, val revote: Boolean) : Etape
    data class DepartageCapitaine(val capitaine: Int, val exAequo: List<Int>) : Etape
    data class Fin(val gagnant: Camp) : Etape
}

enum class Moment { MATIN, VOTE, TIR_DU_CHASSEUR }

sealed interface Action {
    data object Ok : Action
    data class Designation(val joueur: Int) : Action
    data class ChoixVoleur(val carte: Role?) : Action
    data class ChoixCupidon(val a: Int, val b: Int) : Action
    data class ChoixLoups(val victime: Int, val petiteFilleSurprise: Int? = null) : Action
    data class ChoixSorciere(val guerir: Boolean, val empoisonner: Int?) : Action
    data class Elimination(val joueur: Int) : Action
    data class Egalite(val exAequo: List<Int>) : Action
}

/**
 * Moteur d'une partie. Il ne connaît ni l'écran ni la voix : l'interface lit [etape],
 * montre ce qu'il faut, puis appelle [jouer] avec le choix des joueurs.
 */
class Partie(
    val joueurs: List<Joueur>,
    val paquet: List<Role>,
    cartesDuCentre: List<Role> = emptyList(),
) {
    var etape: Etape = Etape.Fin(Camp.PERSONNE)
        private set
    var nuit = 0
        private set
    var capitaine: Int? = null
        private set
    var potionGuerison = true
        private set
    var potionPoison = true
        private set
    val historique = mutableListOf<Mort>()

    private val centre = cartesDuCentre.toMutableList()
    private var appelsRestants = ArrayDeque<Appel>()
    private var victimeLoups: Int? = null
    private var empoisonne: Int? = null
    private var guerie = false
    private val reactions = ArrayDeque<Etape>()
    private var suite: () -> Unit = {}

    init {
        require(joueurs.map { it.id }.toSet().size == joueurs.size) { "Identifiants de joueurs en double" }
        commencerNuit()
    }

    fun joueur(id: Int) = joueurs.first { it.id == id }
    fun vivants() = joueurs.filter { it.vivant }

    fun jouer(action: Action) {
        when (val e = etape) {
            is Etape.AppelVoleur -> {
                val a = action as Action.ChoixVoleur
                if (e.doitPrendreLoup) require(a.carte == Role.LOUP_GAROU) { "Le Voleur doit prendre un Loup-Garou" }
                a.carte?.let { carte ->
                    require(centre.remove(carte)) { "Cette carte n'est pas au centre" }
                    centre.add(Role.VOLEUR)
                    joueur(e.voleur).role = carte
                }
                appelSuivant()
            }
            is Etape.AppelCupidon -> {
                val a = action as Action.ChoixCupidon
                require(a.a != a.b) { "Il faut deux amoureux différents" }
                joueur(a.a).amoureuxDe = a.b
                joueur(a.b).amoureuxDe = a.a
                appelSuivant()
            }
            is Etape.AppelVoyante -> {
                val cible = (action as Action.Designation).joueur
                require(joueur(cible).vivant && cible != e.voyante) { "Cible invalide" }
                etape = Etape.VisionVoyante(cible, joueur(cible).role)
            }
            is Etape.AppelLoups -> {
                val a = action as Action.ChoixLoups
                require(joueur(a.victime).vivant) { "La victime doit être vivante" }
                val surprise = a.petiteFilleSurprise?.let(::joueur)
                victimeLoups = if (surprise != null && surprise.vivant && surprise.role == Role.PETITE_FILLE) {
                    surprise.id
                } else {
                    a.victime
                }
                appelSuivant()
            }
            is Etape.AppelSorciere -> {
                val a = action as Action.ChoixSorciere
                if (a.guerir) {
                    require(e.peutGuerir) { "Potion de guérison indisponible" }
                    potionGuerison = false
                    guerie = true
                }
                a.empoisonner?.let {
                    require(e.peutEmpoisonner && joueur(it).vivant) { "Empoisonnement impossible" }
                    potionPoison = false
                    empoisonne = it
                }
                appelSuivant()
            }
            is Etape.ReveilAmoureux, is Etape.VisionVoyante, is Etape.AppelFictif -> {
                require(action == Action.Ok)
                appelSuivant()
            }
            is Etape.Annonce -> {
                require(action == Action.Ok)
                traiterReactions()
            }
            is Etape.TirDuChasseur -> {
                val cible = (action as Action.Designation).joueur
                require(joueur(cible).vivant) { "La cible doit être vivante" }
                annoncer(listOf(cible to CauseDeMort.CHASSEUR), Moment.TIR_DU_CHASSEUR)
            }
            is Etape.SuccessionCapitaine -> {
                val successeur = (action as Action.Designation).joueur
                require(joueur(successeur).vivant) { "Le successeur doit être vivant" }
                capitaine = successeur
                traiterReactions()
            }
            Etape.ElectionCapitaine -> {
                val elu = (action as Action.Designation).joueur
                require(joueur(elu).vivant)
                capitaine = elu
                etape = Etape.Vote(vivants().map { it.id }, revote = false)
            }
            is Etape.Vote -> when (action) {
                is Action.Elimination -> {
                    require(action.joueur in e.candidats) { "Ce joueur n'est pas candidat" }
                    eliminerParVote(action.joueur)
                }
                is Action.Egalite -> {
                    require(action.exAequo.size >= 2 && e.candidats.containsAll(action.exAequo))
                    val cap = capitaine?.takeIf { joueur(it).vivant }
                    etape = if (cap != null) {
                        Etape.DepartageCapitaine(cap, action.exAequo)
                    } else {
                        Etape.Vote(action.exAequo, revote = true)
                    }
                }
                else -> error("Action inattendue pendant le vote : $action")
            }
            is Etape.DepartageCapitaine -> {
                val choisi = (action as Action.Designation).joueur
                require(choisi in e.exAequo) { "Le Capitaine doit choisir parmi les ex-aequo" }
                eliminerParVote(choisi)
            }
            is Etape.Fin -> error("La partie est terminée")
        }
    }

    // --- Nuit ---

    private fun commencerNuit() {
        nuit++
        victimeLoups = null
        empoisonne = null
        guerie = false
        appelsRestants = ArrayDeque(Appel.entries.filter { nuit == 1 || !it.premiereNuitSeulement })
        appelSuivant()
    }

    private fun appelSuivant() {
        while (appelsRestants.isNotEmpty()) {
            val prochaine = etapePour(appelsRestants.removeFirst())
            if (prochaine != null) {
                etape = prochaine
                return
            }
        }
        leverLeJour()
    }

    /** L'étape d'un appel, ou null s'il ne faut pas appeler ce rôle. */
    private fun etapePour(appel: Appel): Etape? {
        val roleAppele = when (appel) {
            Appel.VOLEUR -> Role.VOLEUR
            Appel.CUPIDON, Appel.AMOUREUX -> Role.CUPIDON
            Appel.VOYANTE -> Role.VOYANTE
            Appel.LOUPS -> Role.LOUP_GAROU
            Appel.SORCIERE -> Role.SORCIERE
        }
        if (roleAppele !in paquet) return null
        val titulaires = joueurs.filter { it.role == roleAppele }
        val vivants = titulaires.filter { it.vivant }
        // Rôle d'un mort : sa carte a été révélée, inutile de l'appeler.
        if (titulaires.isNotEmpty() && vivants.isEmpty()) return null
        if (appel == Appel.AMOUREUX) {
            val a = joueurs.firstOrNull { it.amoureuxDe != null } ?: return Etape.AppelFictif(appel)
            return Etape.ReveilAmoureux(a.id, a.amoureuxDe!!)
        }
        // Carte restée au centre : appel fictif.
        if (vivants.isEmpty()) return Etape.AppelFictif(appel)
        val titulaire = vivants.first().id
        return when (appel) {
            Appel.VOLEUR -> Etape.AppelVoleur(
                titulaire,
                centre.toList(),
                doitPrendreLoup = centre.size == 2 && centre.all { it == Role.LOUP_GAROU },
            )
            Appel.CUPIDON -> Etape.AppelCupidon(titulaire)
            Appel.VOYANTE -> Etape.AppelVoyante(titulaire)
            Appel.LOUPS -> Etape.AppelLoups(vivants.map { it.id })
            Appel.SORCIERE -> Etape.AppelSorciere(
                titulaire,
                victimeLoups,
                peutGuerir = potionGuerison && victimeLoups != null,
                peutEmpoisonner = potionPoison,
            )
            Appel.AMOUREUX -> error("traité plus haut")
        }
    }

    private fun leverLeJour() {
        val morts = buildList {
            victimeLoups?.takeIf { !guerie }?.let { add(it to CauseDeMort.LOUPS) }
            empoisonne?.let { add(it to CauseDeMort.POISON) }
        }
        suite = ::apresLeReveil
        annoncer(morts, Moment.MATIN)
    }

    // --- Jour ---

    private fun apresLeReveil() {
        etape = if (capitaine == null) Etape.ElectionCapitaine else Etape.Vote(vivants().map { it.id }, revote = false)
    }

    private fun eliminerParVote(id: Int) {
        suite = ::commencerNuit
        annoncer(listOf(id to CauseDeMort.VOTE), Moment.VOTE)
    }

    // --- Morts et réactions en chaîne ---

    private fun annoncer(morts: List<Pair<Int, CauseDeMort>>, moment: Moment) {
        val annonce = mutableListOf<Mort>()
        morts.forEach { (id, cause) -> tuer(id, cause, annonce) }
        historique += annonce
        etape = Etape.Annonce(annonce, moment)
    }

    private fun tuer(id: Int, cause: CauseDeMort, annonce: MutableList<Mort>) {
        val j = joueur(id)
        if (!j.vivant) return
        j.vivant = false
        annonce += Mort(j.id, j.nom, j.role, cause)
        if (j.role == Role.CHASSEUR && (cause == CauseDeMort.LOUPS || cause == CauseDeMort.VOTE)) {
            reactions.addLast(Etape.TirDuChasseur(j.id))
        }
        if (capitaine == j.id) reactions.addLast(Etape.SuccessionCapitaine(j.id))
        j.amoureuxDe?.let { tuer(it, CauseDeMort.CHAGRIN, annonce) }
    }

    private fun traiterReactions() {
        vainqueur()?.let {
            reactions.clear()
            etape = Etape.Fin(it)
            return
        }
        while (reactions.isNotEmpty()) {
            val r = reactions.removeFirst()
            // Le Capitaine a pu être remplacé entre-temps, ou plus personne ne peut lui succéder.
            if (r is Etape.SuccessionCapitaine && capitaine != r.capitaine) continue
            if (r is Etape.TirDuChasseur && vivants().isEmpty()) continue
            etape = r
            return
        }
        suite()
    }

    /** Le camp gagnant, ou null si la partie continue. */
    fun vainqueur(): Camp? {
        val vivants = vivants()
        if (vivants.isEmpty()) return Camp.PERSONNE
        val couple = joueurs.filter { it.amoureuxDe != null }
        val coupleMixte = couple.size == 2 && couple.count { it.role.estLoup } == 1
        if (coupleMixte && vivants.size == 2 && vivants.all { it in couple }) return Camp.AMOUREUX
        if (vivants.none { it.role.estLoup }) return Camp.VILLAGEOIS
        if (vivants.all { it.role.estLoup }) return Camp.LOUPS
        return null
    }
}
