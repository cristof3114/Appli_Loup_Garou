package fr.loupgarou.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.loupgarou.app.PartieViewModel
import fr.loupgarou.engine.Role
import fr.loupgarou.engine.Setup

@Composable
fun EcranPreparation(vm: PartieViewModel) {
    var saisie by remember { mutableStateOf("") }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Text("Joueurs (${vm.noms.size})", fontSize = 24.sp, modifier = Modifier.padding(top = 24.dp)) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = saisie,
                    onValueChange = { saisie = it },
                    label = { Text("Prénom, dans l'ordre de la table") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { vm.ajouterJoueur(saisie); saisie = "" },
                    modifier = Modifier.padding(start = 8.dp),
                ) { Text("Ajouter") }
            }
        }
        items(vm.noms.toList()) { nom ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(nom, fontSize = 18.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = { vm.retirerJoueur(nom) }) { Text("Retirer") }
            }
        }

        item {
            Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                val avecVoleur = Role.VOLEUR in vm.paquet
                Text(
                    "Paquet : ${vm.paquet.size} / ${Setup.taillePaquet(vm.noms.size, avecVoleur)} cartes",
                    fontSize = 24.sp,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = vm::proposerComposition, enabled = vm.noms.size >= 4) { Text("Proposer") }
            }
        }
        items(Role.entries) { role ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(role.nom, fontSize = 18.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = { vm.retirerCarte(role) }) { Text("−", fontSize = 22.sp) }
                Text("${vm.paquet.count { it == role }}", fontSize = 18.sp)
                TextButton(onClick = { vm.ajouterCarte(role) }) { Text("+", fontSize = 22.sp) }
            }
        }
        items(vm.avertissements()) { a ->
            Text(a, color = MaterialTheme.colorScheme.error)
        }
        item {
            GrosBouton("Distribuer les cartes, puis déclarer les rôles", actif = vm.peutCommencer()) {
                vm.commencerDeclarations()
            }
        }
        item { Text("") }
    }
}
