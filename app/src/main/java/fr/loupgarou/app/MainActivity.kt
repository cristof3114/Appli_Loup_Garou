package fr.loupgarou.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.loupgarou.app.ui.EcranDeclaration
import fr.loupgarou.app.ui.EcranJeu
import fr.loupgarou.app.ui.EcranPreparation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // L'écran ne doit pas s'éteindre pendant la nuit.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFC62828),
                    background = Color(0xFF0E0E14),
                    surface = Color(0xFF0E0E14),
                ),
            ) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val vm: PartieViewModel = viewModel()
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
