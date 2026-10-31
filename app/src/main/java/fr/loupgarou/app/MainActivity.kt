package fr.loupgarou.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.loupgarou.app.ui.EcranDeclaration
import fr.loupgarou.app.ui.EcranJeu
import fr.loupgarou.app.ui.EcranPreparation
import fr.loupgarou.app.ui.Decor
import fr.loupgarou.app.ui.Gravure
import fr.loupgarou.app.ui.ambianceDe
import fr.loupgarou.engine.Etape

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // L'écran ne doit pas s'éteindre pendant la nuit.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            MaterialTheme(typography = typographieGravure()) {
                val vm: PartieViewModel = viewModel()
                // Lire la version fait suivre au décor chaque étape de la partie.
                vm.version
                val partie = vm.partie.takeIf { vm.ecran == Ecran.JEU }
                val etape = partie?.etape
                var morts = partie?.historique.orEmpty().toList()
                // Les morts de la nuit ne reçoivent leur tombe qu'une fois annoncés.
                if (vm.enTransition && etape is Etape.Annonce) morts = morts.dropLast(etape.morts.size)
                Decor(ambianceDe(etape, vm.enTransition), morts) {
                    when (vm.ecran) {
                        Ecran.PREPARATION -> EcranPreparation(vm)
                        Ecran.DECLARATION -> EcranDeclaration(vm)
                        Ecran.JEU -> EcranJeu(vm)
                    }
                }
            }
        }
    }
}

/** Toute l'appli écrit avec la police « gravure » (IM Fell English). */
private fun typographieGravure(): Typography {
    val t = Typography()
    return Typography(
        displayLarge = t.displayLarge.copy(fontFamily = Gravure),
        displayMedium = t.displayMedium.copy(fontFamily = Gravure),
        displaySmall = t.displaySmall.copy(fontFamily = Gravure),
        headlineLarge = t.headlineLarge.copy(fontFamily = Gravure),
        headlineMedium = t.headlineMedium.copy(fontFamily = Gravure),
        headlineSmall = t.headlineSmall.copy(fontFamily = Gravure),
        titleLarge = t.titleLarge.copy(fontFamily = Gravure),
        titleMedium = t.titleMedium.copy(fontFamily = Gravure),
        titleSmall = t.titleSmall.copy(fontFamily = Gravure),
        bodyLarge = t.bodyLarge.copy(fontFamily = Gravure),
        bodyMedium = t.bodyMedium.copy(fontFamily = Gravure),
        bodySmall = t.bodySmall.copy(fontFamily = Gravure),
        labelLarge = t.labelLarge.copy(fontFamily = Gravure),
        labelMedium = t.labelMedium.copy(fontFamily = Gravure),
        labelSmall = t.labelSmall.copy(fontFamily = Gravure),
    )
}
