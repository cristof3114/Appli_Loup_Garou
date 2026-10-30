package fr.loupgarou.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import fr.loupgarou.app.PartieViewModel
import fr.loupgarou.engine.Role

/** Chaque joueur, à tour de rôle, déclare en secret la carte qu'il a reçue. */
@Composable
fun EcranDeclaration(vm: PartieViewModel) {
    val index = vm.declarations.size
    val nom = vm.noms.getOrNull(index) ?: return
    // Étapes locales : 0 = passer l'appareil, 1 = choisir son rôle, 2 = confirmer.
    var phase by remember(index, vm.erreurDeclaration) { mutableStateOf(0) }
    var choisi by remember(index) { mutableStateOf<Role?>(null) }

    Page {
        when (phase) {
            0 -> {
                vm.erreurDeclaration?.takeIf { index == 0 }?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                Titre("Passe l'appareil à $nom")
                Recit("Les autres détournent le regard.")
                GrosBouton("Je suis $nom") { phase = 1 }
            }
            1 -> {
                Titre("$nom, quelle carte as-tu reçue ?")
                vm.paquet.distinct().forEach { role ->
                    GrosBouton(role.nom) { choisi = role; phase = 2 }
                }
            }
            else -> {
                Titre("Tu es ${choisi?.nom} ?")
                GrosBouton("Oui, c'est ma carte") {
                    choisi?.let(vm::declarer)
                }
                GrosBouton("Non, je me suis trompé") { phase = 1 }
            }
        }
    }
}
