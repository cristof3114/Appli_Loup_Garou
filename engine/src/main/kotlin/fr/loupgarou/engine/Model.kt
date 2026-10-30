package fr.loupgarou.engine

/** Les rôles de la règle. Le Capitaine n'est pas un rôle : c'est une charge en plus du rôle. */
enum class Role(val nom: String, val estLoup: Boolean = false) {
    LOUP_GAROU("Loup-Garou", estLoup = true),
    VILLAGEOIS("Simple Villageois"),
    VOYANTE("Voyante"),
    CHASSEUR("Chasseur"),
    CUPIDON("Cupidon"),
    SORCIERE("Sorcière"),
    VOLEUR("Voleur"),
    PETITE_FILLE("Petite fille"),
}

class Joueur(
    val id: Int,
    val nom: String,
    var role: Role,
) {
    var vivant: Boolean = true
        internal set
    var amoureuxDe: Int? = null
        internal set

    override fun toString() = "$nom ($id, ${role.nom}${if (vivant) "" else ", mort"})"
}

enum class CauseDeMort {
    /** Victime des loups, ou Petite fille surprise à sa place. */
    LOUPS,
    POISON,
    CHAGRIN,
    VOTE,
    CHASSEUR,
}

data class Mort(val joueurId: Int, val nom: String, val role: Role, val cause: CauseDeMort)

enum class Camp { VILLAGEOIS, LOUPS, AMOUREUX, PERSONNE }
