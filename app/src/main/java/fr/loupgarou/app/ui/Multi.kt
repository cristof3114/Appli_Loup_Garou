package fr.loupgarou.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import fr.loupgarou.app.BuildConfig
import fr.loupgarou.app.PartieViewModel
import fr.loupgarou.engine.Action
import fr.loupgarou.engine.Etape
import fr.loupgarou.multi.Salon
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Le salon du mode « chacun son téléphone » : les joueurs scannent le QR code, apparaissent
 * ici, on choisit la composition, puis l'appli distribue les rôles sur les téléphones.
 */
@Composable
fun EcranSalon(vm: PartieViewModel) {
    vm.versionSalon
    val hote = vm.multi ?: return
    val salon = hote.salon
    if (salon.phase == Salon.Phase.REVELATION) {
        Page {
            Titre("Chacun découvre son rôle, en secret")
            Recit("Sur votre téléphone : « Découvrir mon rôle », puis « Je cache mon écran ».")
            Recit("Prêts : ${salon.nbPrets()} sur ${salon.places.size}")
            val manquent = salon.pasEncorePrets()
            if (manquent.isNotEmpty()) Recit("On attend : ${manquent.joinToString(", ")}")
            GrosBouton("Que la nuit tombe", actif = salon.toutLeMondeEstPret()) { vm.commencerNuitMulti() }
        }
        return
    }
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(LocalEncre.current.voile)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { Text("Chacun son téléphone", fontSize = 28.sp, modifier = Modifier.padding(top = 24.dp)) }
        item {
            val adresse = hote.adresse
            if (adresse != null) {
                CodeQr(adresse)
                Recit("Scannez ce code avec l'appareil photo du téléphone, ou tapez cette adresse dans le navigateur :")
                Text(adresse, fontSize = 24.sp, textAlign = TextAlign.Center)
                Recit("Tous les téléphones doivent être sur le même Wi-Fi que la tablette.")
            } else {
                Recit(
                    "La tablette n'est connectée à aucun Wi-Fi. Connectez-la au Wi-Fi de la maison, " +
                        "ou activez son point d'accès pour que les téléphones s'y connectent.",
                )
            }
            TextButton(onClick = vm::actualiserAdresse, modifier = Modifier.contourFocus()) { Text("Actualiser l'adresse") }
        }
        item { Text("Joueurs connectés (${salon.places.size})", fontSize = 24.sp, modifier = Modifier.padding(top = 8.dp)) }
        items(salon.places.toList(), key = { it.id }) { place ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(place.nom, fontSize = 18.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = { vm.retirerDuSalon(place.id) }, modifier = Modifier.contourFocus()) { Text("Retirer") }
            }
        }
        editeurComposition(vm)
        item {
            GrosBouton("Distribuer les rôles au hasard", actif = vm.peutCommencer()) { vm.distribuerAuxTelephones() }
        }
        // La télé ne sait faire que le meneur : pas de retour au jeu sur un seul appareil.
        if (!BuildConfig.TELE) {
            item { TextButton(onClick = vm::fermerSalon, modifier = Modifier.contourFocus()) { Text("Jouer plutôt sur une seule tablette") } }
        }
        item { Text("") }
    }
}

@Composable
private fun CodeQr(texte: String) {
    val image = remember(texte) {
        val m = QRCodeWriter().encode(texte, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 1))
        val bmp = Bitmap.createBitmap(m.width, m.height, Bitmap.Config.ARGB_8888)
        for (x in 0 until m.width) for (y in 0 until m.height) {
            bmp.setPixel(x, y, if (m[x, y]) 0xFF161A26.toInt() else 0xFFF4EAD2.toInt())
        }
        bmp.asImageBitmap()
    }
    Image(
        bitmap = image,
        contentDescription = "Code QR de la partie",
        filterQuality = androidx.compose.ui.graphics.FilterQuality.None,
        modifier = Modifier.size(240.dp).clip(RoundedCornerShape(8.dp)),
    )
}

/**
 * La tablette pendant une partie à plusieurs téléphones : elle parle, montre ce qui est public
 * et attend les choix faits sur les téléphones. En secours (téléphone éteint, joueur perdu),
 * l'étape peut se jouer sur la tablette comme dans le mode à une seule tablette.
 */
@Composable
fun EcranJeuMulti(vm: PartieViewModel) {
    val partie = vm.partie ?: return
    vm.versionSalon
    val etape = remember(vm.version) { partie.etape }
    val salon = vm.multi?.salon ?: return
    var secours by remember(vm.version) { mutableStateOf(false) }

    // Ce que la tablette faisait déjà seule : appel fictif d'une carte au centre, vision qui se
    // referme, relances du débat.
    when (etape) {
        is Etape.AppelFictif -> LaunchedEffect(vm.version) {
            delay(Random.nextLong(14_000, 22_000))
            vm.jouerSi(etape, Action.Ok)
        }
        is Etape.VisionVoyante -> LaunchedEffect(vm.version) {
            delay(12_000)
            vm.jouerSi(etape, Action.Ok)
        }
        is Etape.Vote -> if (!etape.revote) LaunchedEffect(vm.version) {
            repeat(4) {
                delay(75_000)
                vm.relancerDebat()
            }
        }
        else -> {}
    }

    if (vm.enTransition || etape is Etape.Annonce || etape is Etape.Fin) {
        EcranJeu(vm)
        return
    }
    if (secours) {
        Box(Modifier.fillMaxSize()) {
            EcranJeu(vm)
            TextButton(onClick = { secours = false }, modifier = Modifier.align(Alignment.TopEnd)) {
                Text("Retour aux téléphones")
            }
        }
        return
    }
    val nuit = when (etape) {
        is Etape.AppelVoleur, is Etape.AppelCupidon, is Etape.DecouverteAmoureux, is Etape.AppelVoyante,
        is Etape.VisionVoyante, is Etape.AppelLoups, is Etape.AppelSorciere, is Etape.AppelFictif -> true
        else -> false
    }
    Page(bas = {
        TextButton(onClick = { secours = true }, modifier = Modifier.contourFocus()) {
            Text("Un téléphone ne répond plus ? Jouer cette étape sur la tablette", textAlign = TextAlign.Center)
        }
    }) {
        if (nuit) {
            Titre("Chut… le village dort.")
            vm.paroles.forEach { Recit(it.texte) }
            if (etape is Etape.DecouverteAmoureux) {
                Recit("Tout le monde ouvre les yeux sur son téléphone et lit son secret, en silence.")
            }
        } else {
            Paroles(vm)
            Titre(
                when (etape) {
                    is Etape.TirDuChasseur -> "${partie.joueur(etape.chasseur).nom}, Chasseur, vise sur ton téléphone"
                    is Etape.SuccessionCapitaine -> "${partie.joueur(etape.capitaine).nom} désigne son successeur sur son téléphone"
                    Etape.ElectionCapitaine -> "Élisez votre Capitaine sur vos téléphones"
                    is Etape.Vote -> if (etape.revote) "Revotez entre les ex-æquo" else "Débattez, puis votez sur vos téléphones"
                    is Etape.DepartageCapitaine -> "Égalité : ${partie.joueur(etape.capitaine).nom}, Capitaine, tranche sur ton téléphone"
                    else -> ""
                },
            )
        }
        salon.attenteTablette().forEach { Recit(it) }
    }
}
