package fr.loupgarou.app.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.loupgarou.app.PartieViewModel
import fr.loupgarou.engine.Action
import fr.loupgarou.engine.CauseDeMort
import fr.loupgarou.engine.Etape
import fr.loupgarou.engine.Partie
import fr.loupgarou.engine.Role
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun EcranJeu(vm: PartieViewModel) {
    val partie = vm.partie ?: return
    // La clé de version force un nouvel état local à chaque étape.
    val etape = remember(vm.version) { partie.etape }
    val jouer: (Action) -> Unit = vm::jouer
    // Ce que le narrateur dit en arrivant sur l'étape, affiché tel quel.
    val dit = vm.paroles.firstOrNull()?.texte.orEmpty()

    if (vm.enTransition) {
        // Le temps que le joueur se rendorme et repose l'appareil avant l'appel suivant.
        Page { Titre(if (appelDeNuit(etape)) "Chut… le village dort." else "La nuit s'achève…") }
        return
    }
    AntiDoubleAppui(vm.version) { when (etape) {
        is Etape.AppelFictif -> AppelDeNuit(dit, vm.version, fictif = true, onFictifTermine = { vm.jouerSi(etape, Action.Ok) }) {}
        is Etape.AppelVoleur -> AppelDeNuit(dit, vm.version) { Voleur(etape, jouer) }
        is Etape.AppelCupidon -> AppelDeNuit(dit, vm.version) { Cupidon(partie, jouer) }
        is Etape.ReveilAmoureux -> AppelDeNuit(dit, vm.version) {
            Titre("${partie.joueur(etape.a).nom} et ${partie.joueur(etape.b).nom} sont amoureux")
            Recit("Si l'un meurt, l'autre meurt de chagrin.")
            GrosBouton("Fermer les yeux") { jouer(Action.Ok) }
        }
        is Etape.AppelVoyante -> AppelDeNuit(dit, vm.version) {
            Titre("Qui veux-tu sonder ?")
            DesignerJoueur(
                partie.vivants().filter { it.id != etape.voyante },
                vm.version,
                { "Sonder ${it.nom}" },
                Modifier.heightIn(max = 420.dp),
            ) { jouer(Action.Designation(it)) }
        }
        is Etape.VisionVoyante -> {
            // Le rôle s'affiche 10 secondes puis l'écran se referme tout seul.
            LaunchedEffect(vm.version) {
                delay(10_000)
                vm.jouerSi(etape, Action.Ok)
            }
            Page {
                Titre("${partie.joueur(etape.cible).nom} est ${etape.role.nom}")
                GrosBouton("Fermer les yeux") { jouer(Action.Ok) }
            }
        }
        is Etape.AppelLoups -> AppelDeNuit(dit, vm.version) { Loups(partie, etape, jouer) }
        is Etape.AppelSorciere -> AppelDeNuit(dit, vm.version) { Sorciere(partie, etape, jouer) }
        is Etape.Annonce -> Page {
            Paroles(vm)
            etape.morts.forEach { m ->
                val chagrin = if (m.cause == CauseDeMort.CHAGRIN) ", mort de chagrin" else ""
                Titre("${m.nom} était ${m.role.nom}$chagrin")
            }
            GrosBouton("Continuer") { jouer(Action.Ok) }
        }
        is Etape.TirDuChasseur -> Page {
            Paroles(vm)
            Titre("${partie.joueur(etape.chasseur).nom}, Chasseur, tu emportes quelqu'un avec toi")
            DesignerJoueur(partie.vivants(), vm.version, { "Tirer sur ${it.nom}" }, Modifier.heightIn(max = 420.dp)) {
                jouer(Action.Designation(it))
            }
        }
        is Etape.SuccessionCapitaine -> Page {
            Paroles(vm)
            Titre("${partie.joueur(etape.capitaine).nom} désigne le nouveau Capitaine")
            DesignerJoueur(partie.vivants(), vm.version, { "${it.nom} devient Capitaine" }, Modifier.heightIn(max = 420.dp)) {
                jouer(Action.Designation(it))
            }
        }
        Etape.ElectionCapitaine -> Page {
            Paroles(vm)
            Titre("Élisez votre Capitaine")
            Recit("Votez à main levée, puis touchez l'élu. Sa voix comptera double.")
            DesignerJoueur(partie.vivants(), vm.version, { "Élire ${it.nom}" }, Modifier.heightIn(max = 420.dp)) {
                jouer(Action.Designation(it))
            }
        }
        is Etape.Vote -> Vote(vm, partie, etape, jouer)
        is Etape.DepartageCapitaine -> Page {
            Paroles(vm)
            Titre("Égalité. ${partie.joueur(etape.capitaine).nom}, Capitaine, tranche")
            DesignerJoueur(etape.exAequo.map(partie::joueur), vm.version, { "Éliminer ${it.nom}" }) {
                jouer(Action.Designation(it))
            }
        }
        is Etape.Fin -> Page {
            Titre(dit)
            partie.joueurs.forEach { Text("${it.nom} : ${it.role.nom}", fontSize = 18.sp) }
            GrosBouton("Nouvelle partie") { vm.nouvellePartie() }
        }
    } }
}

private fun appelDeNuit(etape: Etape) = when (etape) {
    is Etape.AppelVoleur, is Etape.AppelCupidon, is Etape.ReveilAmoureux, is Etape.AppelVoyante,
    is Etape.VisionVoyante, is Etape.AppelLoups, is Etape.AppelSorciere, is Etape.AppelFictif -> true
    else -> false
}

/**
 * Écran commun aux appels de nuit : le récit s'affiche à tous (yeux fermés), puis le joueur
 * appelé touche l'écran pour ouvrir son action secrète. Un appel fictif dure un temps
 * aléatoire, comme si quelqu'un agissait.
 */
@Composable
private fun AppelDeNuit(
    texte: String,
    version: Int,
    fictif: Boolean = false,
    onFictifTermine: () -> Unit = {},
    action: @Composable () -> Unit,
) {
    var ouvert by remember(version) { mutableStateOf(false) }
    if (fictif) {
        LaunchedEffect(version) {
            delay(Random.nextLong(14_000, 22_000))
            onFictifTermine()
        }
    }
    // Un double appui sur « Touche ici » validait sinon l'écran secret avant qu'il soit lu.
    AntiDoubleAppui(ouvert) {
        if (!ouvert) {
            // Le récit en haut, le médaillon du rôle au milieu, le bouton à portée de pouce.
            val bouton: (@Composable ColumnScope.() -> Unit)? =
                if (fictif) null else { { GrosBouton("Touche ici quand tu as les yeux ouverts") { ouvert = true } } }
            Page(bas = bouton) { Titre(texte) }
        } else {
            Page { action() }
        }
    }
}

@Composable
private fun Voleur(etape: Etape.AppelVoleur, jouer: (Action) -> Unit) {
    Titre("Les cartes du centre : ${etape.cartes.joinToString(" et ") { it.nom }}")
    if (!etape.doitPrendreLoup) GrosBouton("Garder ma carte") { jouer(Action.ChoixVoleur(null)) }
    etape.cartes.distinct().forEach { carte ->
        GrosBouton("Prendre ${carte.nom}") { jouer(Action.ChoixVoleur(carte)) }
    }
    Recit("Si tu changes, échange aussi ta carte physique avec celle du centre.")
}

@Composable
private fun Cupidon(partie: Partie, jouer: (Action) -> Unit) {
    var choisis by remember { mutableStateOf(listOf<Int>()) }
    Titre("Choisis deux amoureux")
    ChoixJoueur(partie.vivants(), choisis.toSet(), Modifier.heightIn(max = 420.dp)) { j ->
        choisis = if (j.id in choisis) choisis - j.id else (choisis + j.id).takeLast(2)
    }
    GrosBouton("Unir ces deux-là", actif = choisis.size == 2) {
        jouer(Action.ChoixCupidon(choisis[0], choisis[1]))
    }
}

@Composable
private fun Loups(partie: Partie, etape: Etape.AppelLoups, jouer: (Action) -> Unit) {
    var victime by remember { mutableStateOf<Int?>(null) }
    var surprise by remember { mutableStateOf<Int?>(null) }
    var signalerPetiteFille by remember { mutableStateOf(false) }
    Recit("Loups : ${etape.loups.joinToString { partie.joueur(it).nom }}")
    if (!signalerPetiteFille) {
        Titre("Qui dévorez-vous ?")
        ChoixJoueur(partie.vivants(), setOfNotNull(victime), Modifier.heightIn(max = 380.dp)) { victime = it.id }
        if (Role.PETITE_FILLE in partie.paquet) {
            Bascule(
                surprise?.let { "Petite fille surprise : ${partie.joueur(it).nom}" } ?: "Nous avons surpris la Petite fille",
                surprise != null,
            ) { if (surprise == null) signalerPetiteFille = true else surprise = null }
        }
        GrosBouton("Valider", actif = victime != null) {
            jouer(Action.ChoixLoups(victime!!, surprise))
        }
    } else {
        Titre("Qui avez-vous surpris en train d'espionner ?")
        ChoixJoueur(partie.vivants().filter { it.id !in etape.loups }, modifier = Modifier.heightIn(max = 420.dp)) {
            surprise = it.id
            signalerPetiteFille = false
        }
        TextButton(onClick = { signalerPetiteFille = false }) { Text("Retour", fontSize = 18.sp) }
    }
}

/**
 * Écran secret de la Sorcière. Chaque potion encore pleine est un gros bouton bien visible
 * (une case à cocher, trop petite, passait souvent inaperçue), et le bouton final dit
 * clairement ce qui va se passer.
 */
@Composable
private fun Sorciere(partie: Partie, etape: Etape.AppelSorciere, jouer: (Action) -> Unit) {
    var guerir by remember { mutableStateOf(false) }
    var poison by remember { mutableStateOf<Int?>(null) }
    var choisirPoison by remember { mutableStateOf(false) }
    if (choisirPoison) {
        Titre("Qui empoisonnes-tu ?")
        ChoixJoueur(partie.vivants(), setOfNotNull(poison), Modifier.heightIn(max = 420.dp)) {
            poison = it.id
            choisirPoison = false
        }
        TextButton(onClick = { choisirPoison = false }) { Text("Retour", fontSize = 18.sp) }
        return
    }
    val victime = etape.victime?.let(partie::joueur)
    Titre(victime?.let { "Les loups ont choisi ${it.nom}" } ?: "Personne n'a été attaqué")
    when {
        etape.peutGuerir && victime != null ->
            Bascule(if (guerir) "Sauver ${victime.nom} ✓" else "Sauver ${victime.nom}", guerir) { guerir = !guerir }
        !partie.potionGuerison -> Recit("Ta potion de guérison est déjà utilisée.")
    }
    if (etape.peutEmpoisonner) {
        Bascule(poison?.let { "Empoisonner ${partie.joueur(it).nom}" } ?: "Empoisonner quelqu'un", poison != null) {
            choisirPoison = true
        }
        if (poison != null) TextButton(onClick = { poison = null }) { Text("Ne plus empoisonner", fontSize = 18.sp) }
    } else {
        Recit("Ta potion de poison est déjà utilisée.")
    }
    val rien = !guerir && poison == null
    GrosBouton(if (rien) "Ne rien faire cette nuit" else "Valider") { jouer(Action.ChoixSorciere(guerir, poison)) }
}

/** Gros bouton à deux états : plein quand il est choisi, en contour sinon. */
@Composable
private fun Bascule(texte: String, choisi: Boolean, onClick: () -> Unit) {
    val modifier = Modifier.fillMaxWidth()
    if (choisi) {
        Button(onClick = onClick, modifier = modifier) { Text(texte, fontSize = 20.sp, modifier = Modifier.padding(8.dp)) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(texte, fontSize = 20.sp, modifier = Modifier.padding(8.dp)) }
    }
}

@Composable
private fun Vote(vm: PartieViewModel, partie: Partie, etape: Etape.Vote, jouer: (Action) -> Unit) {
    val version = vm.version
    // Pendant le débat, le narrateur relance de temps en temps les indécis.
    if (!etape.revote) {
        LaunchedEffect(version) {
            repeat(4) {
                delay(75_000)
                vm.relancerDebat()
            }
        }
    }
    var modeEgalite by remember(version) { mutableStateOf(false) }
    var choisis by remember(version) { mutableStateOf(setOf<Int>()) }
    val candidats = etape.candidats.map(partie::joueur)
    Page {
        Paroles(vm)
        Titre(if (etape.revote) "Revote entre les ex-æquo" else "Le village vote")
        if (!modeEgalite) {
            Recit("Votez à main levée, puis touchez le joueur désigné.")
            ChoixJoueur(candidats, choisis, Modifier.heightIn(max = 420.dp)) { choisis = setOf(it.id) }
            GrosBouton(
                choisis.singleOrNull()?.let { "Éliminer ${partie.joueur(it).nom}" } ?: "Éliminer",
                actif = choisis.size == 1,
            ) { jouer(Action.Elimination(choisis.single())) }
            GrosBouton("Il y a égalité") { modeEgalite = true; choisis = emptySet() }
        } else {
            Recit("Touchez les joueurs à égalité.")
            ChoixJoueur(candidats, choisis, Modifier.heightIn(max = 420.dp)) {
                choisis = if (it.id in choisis) choisis - it.id else choisis + it.id
            }
            GrosBouton("Valider l'égalité", actif = choisis.size >= 2) { jouer(Action.Egalite(choisis.toList())) }
            TextButton(onClick = { modeEgalite = false; choisis = emptySet() }) { Text("Annuler") }
        }
    }
}

/** Les paroles du narrateur pour l'étape en cours, avec de quoi les réentendre. */
@Composable
private fun Paroles(vm: PartieViewModel) {
    vm.paroles.forEach { Recit(it.texte) }
    if (vm.paroles.isNotEmpty()) TextButton(onClick = vm::repeter) { Text("Répéter") }
}
