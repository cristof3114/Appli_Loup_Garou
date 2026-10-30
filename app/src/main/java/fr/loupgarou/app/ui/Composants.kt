package fr.loupgarou.app.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.loupgarou.engine.Joueur

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
