package fr.loupgarou.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import fr.loupgarou.engine.TypeDePartie

@Composable
fun EcranPreparation(vm: PartieViewModel) {
    var saisie by remember { mutableStateOf("") }
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(LocalEncre.current.voile)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            GrosBouton("Chacun son téléphone (Android ou iPhone)") { vm.ouvrirSalon() }
            Text(
                "Ou bien jouez tous sur cette tablette, avec vos vraies cartes :",
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        item { Text("Joueurs (${vm.noms.size})", fontSize = 24.sp, modifier = Modifier.padding(top = 8.dp)) }
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
                TextButton(onClick = { vm.retirerJoueur(nom) }, modifier = Modifier.contourFocus()) { Text("Retirer") }
            }
        }

        editeurComposition(vm)
        item {
            GrosBouton("Distribuer les cartes, puis déclarer les rôles", actif = vm.peutCommencer()) {
                vm.commencerDeclarations()
            }
        }
        item { Text("") }
    }
}

/** Le choix du type de partie et des cartes du paquet, commun aux deux modes de jeu. */
fun LazyListScope.editeurComposition(vm: PartieViewModel) {
    val reglages = vm.reglages
    val nb = vm.noms.size
    item { Text("Type de partie", fontSize = 24.sp, modifier = Modifier.padding(top = 16.dp)) }
    item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TypeDePartie.entries.chunked(2).forEach { ligne ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ligne.forEach { type ->
                        val m = Modifier.weight(1f).contourFocus()
                        if (type == reglages.type) {
                            Button(onClick = {}, modifier = m) { Text(type.nom, fontSize = 18.sp) }
                        } else {
                            OutlinedButton(onClick = { vm.choisirType(type) }, modifier = m) { Text(type.nom, fontSize = 18.sp) }
                        }
                    }
                }
            }
            Text(reglages.type.description, fontSize = 16.sp)
        }
    }
    item {
        Interrupteur(
            "Petite partie à 1 loup (hors règles)",
            "De 4 à 6 joueurs : 1 loup, la Voyante, le Chasseur à 6, et des Simples Villageois.",
            reglages.unSeulLoup,
            vm::basculerUnSeulLoup,
        )
    }
    item {
        Interrupteur(
            "Loups enragés (Loup +1)",
            "À partir de 8 joueurs : un loup de plus que la règle.",
            reglages.loupsEnrages,
            vm::basculerLoupsEnrages,
        )
    }
    if (vm.horsRegles()) {
        item { Text("⚠ Hors règles : un seul loup", color = MaterialTheme.colorScheme.error, fontSize = 18.sp) }
    }
    item {
        Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            val avecVoleur = Role.VOLEUR in vm.paquet
            Text(
                if (vm.eventail != null) "Paquet : ${vm.paquet.size} cartes, composition cachée"
                else "Paquet : ${vm.paquet.size} / ${Setup.taillePaquet(nb, avecVoleur)} cartes",
                fontSize = 24.sp,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = vm::proposerComposition, enabled = nb >= 4, modifier = Modifier.contourFocus()) {
                Text(if (reglages.type == TypeDePartie.CHAOS) "Relancer" else "Proposer")
            }
        }
    }
    val eventail = vm.eventail
    if (eventail != null) {
        item { Text("Les joueurs savent seulement : ${eventail.texte()}.", fontSize = 18.sp) }
        // Sur une seule tablette, il faut bien que quelqu'un sorte les cartes de la boîte.
        if (vm.multi == null) {
            item {
                TextButton(onClick = { vm.cartesMontrees = !vm.cartesMontrees }, modifier = Modifier.contourFocus()) {
                    Text(if (vm.cartesMontrees) "Cacher les cartes" else "Préparateur seul : voir les cartes à sortir")
                }
            }
            if (vm.cartesMontrees) {
                items(Role.entries.filter { r -> r in vm.paquet }) { role ->
                    Text("${vm.paquet.count { it == role }} × ${role.nom}", fontSize = 18.sp)
                }
                item { Text("Mélangez sans regarder, puis cachez la liste avant de distribuer.", fontSize = 16.sp) }
            }
        }
    } else {
        items(Role.entries) { role ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(role.nom, fontSize = 18.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = { vm.retirerCarte(role) }, modifier = Modifier.contourFocus()) { Text("−", fontSize = 22.sp) }
                Text("${vm.paquet.count { it == role }}", fontSize = 18.sp)
                TextButton(onClick = { vm.ajouterCarte(role) }, modifier = Modifier.contourFocus()) { Text("+", fontSize = 22.sp) }
            }
        }
    }
    items(vm.avertissements()) { a ->
        Text(a, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun Interrupteur(titre: String, detail: String, actif: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).contourFocus().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(titre, fontSize = 18.sp)
            Text(detail, fontSize = 14.sp)
        }
        Switch(checked = actif, onCheckedChange = { onClick() })
    }
}
