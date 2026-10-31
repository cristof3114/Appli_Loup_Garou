package fr.loupgarou.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.loupgarou.engine.Joueur
import kotlinx.coroutines.delay

/** Page centrée, utilisée par tous les écrans de jeu. */
@Composable
fun Page(contenu: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        content = contenu,
    )
}

@Composable
fun Titre(texte: String) {
    Text(texte, fontSize = 28.sp, textAlign = TextAlign.Center, style = MaterialTheme.typography.headlineMedium)
}

@Composable
fun Recit(texte: String) {
    Text(texte, fontSize = 20.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun GrosBouton(texte: String, actif: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = actif,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) { Text(texte, fontSize = 20.sp, modifier = Modifier.padding(8.dp)) }
}

/** Grille de joueurs à toucher. Les joueurs [selectionnes] sont mis en avant. */
@Composable
fun ChoixJoueur(
    joueurs: List<Joueur>,
    selectionnes: Set<Int> = emptySet(),
    modifier: Modifier = Modifier,
    onChoix: (Joueur) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(joueurs, key = { it.id }) { j ->
            if (j.id in selectionnes) {
                Button(onClick = { onChoix(j) }) { Text(j.nom, fontSize = 18.sp) }
            } else {
                OutlinedButton(
                    onClick = { onChoix(j) },
                    colors = ButtonDefaults.outlinedButtonColors(),
                ) { Text(j.nom, fontSize = 18.sp) }
            }
        }
    }
}

/**
 * Ignore les touches pendant un court instant chaque fois que [cle] change. Sans ça, un double
 * appui (ou un doigt impatient) sur un bouton validait aussi le bouton de l'écran suivant,
 * placé au même endroit, avant que le joueur ait pu le lire.
 */
@Composable
fun AntiDoubleAppui(cle: Any?, delaiMs: Long = 800, contenu: @Composable () -> Unit) {
    var pret by remember(cle) { mutableStateOf(false) }
    LaunchedEffect(cle) {
        delay(delaiMs)
        pret = true
    }
    Box(Modifier.fillMaxSize()) {
        contenu()
        if (!pret) {
            Box(
                Modifier.matchParentSize().pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) awaitPointerEvent().changes.forEach { it.consume() }
                    }
                },
            )
        }
    }
}

/**
 * Désigner un joueur en deux temps : on le touche, puis on confirme avec le gros bouton.
 * Un seul appui ne suffit jamais à tuer, élire ou sonder quelqu'un par erreur.
 */
@Composable
fun DesignerJoueur(
    joueurs: List<Joueur>,
    cle: Any?,
    libelle: (Joueur) -> String,
    modifier: Modifier = Modifier,
    onValider: (Int) -> Unit,
) {
    var choisi by remember(cle) { mutableStateOf<Int?>(null) }
    ChoixJoueur(joueurs, setOfNotNull(choisi), modifier) { choisi = it.id }
    val joueur = joueurs.firstOrNull { it.id == choisi }
    GrosBouton(joueur?.let(libelle) ?: "Touche un joueur", actif = joueur != null) { onValider(joueur!!.id) }
}
