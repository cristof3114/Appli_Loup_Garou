package fr.loupgarou.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import fr.loupgarou.app.R
import fr.loupgarou.engine.Appel
import fr.loupgarou.engine.Camp
import fr.loupgarou.engine.Etape
import fr.loupgarou.engine.Mort

/*
 * Le décor dessiné du village (style « gravure à l'encre ») : le village de nuit ou de jour,
 * le médaillon du rôle appelé à la place de la lune ou du soleil, et une tombe avec un petit
 * fantôme pour chaque mort, dans l'ordre des décès. Les dessins sources sont dans art/.
 *
 * Les images sont dessinées sur une planche de 390 × 844 unités, calée en bas de l'écran et
 * rognée sur les côtés si besoin : toutes les positions ci-dessous sont dans ces unités.
 */

private const val LARGEUR = 390f
private const val HAUTEUR = 844f

val Gravure = FontFamily(
    Font(R.font.im_fell),
    Font(R.font.im_fell_italique, style = FontStyle.Italic),
)

enum class Ciel { NUIT, JOUR }

data class Ambiance(val ciel: Ciel, @DrawableRes val embleme: Int? = null)

/** Couleurs du décor courant : l'encre du texte et le voile posé derrière les panneaux. */
data class Encre(val texte: Color, val pale: Color, val voile: Color, val fond: Color)

private val ENCRE_NUIT = Encre(Color(0xFFE8DCC0), Color(0xFFB5A785), Color(0xE0161A26), Color(0xFF161A26))
private val ENCRE_JOUR = Encre(Color(0xFF2B2218), Color(0xFF5A4A36), Color(0xE6E8DAB9), Color(0xFFE8DAB9))

val LocalEncre = staticCompositionLocalOf { ENCRE_NUIT }

fun encreDe(ciel: Ciel) = if (ciel == Ciel.NUIT) ENCRE_NUIT else ENCRE_JOUR

/** Le décor qui va avec une étape de la partie (null : préparation, avant la première nuit). */
fun ambianceDe(etape: Etape?, enTransition: Boolean): Ambiance {
    val nuit = Ambiance(Ciel.NUIT)
    val jour = Ambiance(Ciel.JOUR)
    if (etape == null) return nuit
    val ambiance = when (etape) {
        is Etape.AppelVoleur -> nuit.copy(embleme = R.drawable.embleme_voleur)
        is Etape.AppelCupidon -> nuit.copy(embleme = R.drawable.embleme_cupidon)
        is Etape.ReveilAmoureux -> nuit.copy(embleme = R.drawable.embleme_amoureux)
        is Etape.AppelVoyante, is Etape.VisionVoyante -> nuit.copy(embleme = R.drawable.embleme_voyante)
        is Etape.AppelLoups -> nuit.copy(embleme = R.drawable.embleme_loups)
        is Etape.AppelSorciere -> nuit.copy(embleme = R.drawable.embleme_sorciere)
        is Etape.AppelFictif -> nuit.copy(embleme = emblemeDe(etape.appel))
        is Etape.Annonce -> jour
        is Etape.TirDuChasseur -> jour.copy(embleme = R.drawable.embleme_chasseur)
        is Etape.SuccessionCapitaine, Etape.ElectionCapitaine, is Etape.DepartageCapitaine ->
            jour.copy(embleme = R.drawable.embleme_capitaine)
        is Etape.Vote -> jour.copy(embleme = R.drawable.embleme_vote)
        is Etape.Fin -> when (etape.gagnant) {
            Camp.VILLAGEOIS -> jour.copy(embleme = R.drawable.embleme_village)
            Camp.LOUPS -> nuit.copy(embleme = R.drawable.embleme_meute)
            Camp.AMOUREUX -> nuit.copy(embleme = R.drawable.embleme_amoureux)
            Camp.PERSONNE -> nuit.copy(embleme = R.drawable.embleme_personne)
        }
    }
    // Pendant une transition, le village dort ou s'éveille : pas encore de médaillon.
    return if (enTransition) ambiance.copy(embleme = null) else ambiance
}

private fun emblemeDe(appel: Appel) = when (appel) {
    Appel.VOLEUR -> R.drawable.embleme_voleur
    Appel.CUPIDON -> R.drawable.embleme_cupidon
    Appel.AMOUREUX -> R.drawable.embleme_amoureux
    Appel.VOYANTE -> R.drawable.embleme_voyante
    Appel.LOUPS -> R.drawable.embleme_loups
    Appel.SORCIERE -> R.drawable.embleme_sorciere
}

/** Place d'une tombe sur la planche : coin haut gauche et échelle. */
private class Place(val x: Float, val y: Float, val echelle: Float)

/**
 * Les tombes se rangent sur trois rangées, de la plus proche à la plus lointaine. Au-delà de
 * 19 morts, les suivantes se glissent entre les premières.
 */
private fun placeDe(index: Int): Place {
    val rangees = listOf(
        Triple(640f, 1f, List(6) { 6f + it * 64f }),
        Triple(568f, 0.8f, List(6) { 30f + it * 56f }),
        Triple(514f, 0.62f, List(7) { 14f + it * 52f }),
    )
    var i = index
    for ((y, echelle, xs) in rangees) {
        if (i < xs.size) return Place(xs[i], y, echelle)
        i -= xs.size
    }
    val (y, echelle, xs) = rangees[0]
    return Place(xs[i % xs.size] + 30f, y + 12f, echelle)
}

@Composable
fun Decor(ambiance: Ambiance, morts: List<Mort>, contenu: @Composable () -> Unit) {
    val encre = encreDe(ambiance.ciel)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Taille d'une unité de la planche, et position de la planche (calée en bas, centrée).
        val u = maxOf(maxWidth / LARGEUR, maxHeight / HAUTEUR)
        val gauche = (maxWidth - u * LARGEUR) / 2
        val haut = maxHeight - u * HAUTEUR

        Crossfade(ambiance.ciel, animationSpec = tween(1200), label = "ciel") { ciel ->
            Image(
                painterResource(if (ciel == Ciel.NUIT) R.drawable.scene_nuit else R.drawable.scene_jour),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomCenter,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Le médaillon du rôle prend la place de la lune (ou du soleil).
        val d = u * 132
        AnimatedContent(
            ambiance.embleme,
            transitionSpec = { fadeIn(tween(900)) togetherWith fadeOut(tween(600)) },
            modifier = Modifier.offset(gauche + u * 195 - d / 2, haut + u * 300 - d / 2).size(d),
            label = "embleme",
        ) { embleme ->
            if (embleme != null) {
                Image(painterResource(embleme), contentDescription = null, modifier = Modifier.fillMaxSize())
            } else {
                Box(Modifier.fillMaxSize())
            }
        }

        morts.forEachIndexed { index, mort ->
            Tombe(mort.nom, ambiance.ciel, placeDe(index), index, u, gauche, haut)
        }

        CompositionLocalProvider(LocalEncre provides encre, LocalContentColor provides encre.texte) {
            MaterialTheme(colorScheme = couleursDe(encre)) { contenu() }
        }
    }
}

@Composable
private fun Tombe(nom: String, ciel: Ciel, place: Place, index: Int, u: Dp, gauche: Dp, haut: Dp) {
    val l = u * 60 * place.echelle
    val h = u * 70 * place.echelle
    val x = gauche + u * place.x
    val y = haut + u * place.y
    val encre = encreDe(ciel)
    Box(Modifier.offset(x, y).size(l, h)) {
        Image(
            painterResource(if (ciel == Ciel.NUIT) R.drawable.tombe_nuit else R.drawable.tombe_jour),
            contentDescription = "Tombe de $nom",
            modifier = Modifier.fillMaxSize(),
        )
        // Le prénom gravé sur la pierre, plus petit s'il est long.
        val taille = min(u * 13, u * 92 / nom.length.coerceAtLeast(1)) * place.echelle
        with(androidx.compose.ui.platform.LocalDensity.current) {
            Text(
                nom,
                color = encre.texte,
                fontFamily = Gravure,
                fontSize = taille.toSp(),
                maxLines = 1,
                overflow = TextOverflow.Clip,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = h * 0.5f, start = l * 0.12f, end = l * 0.12f),
            )
        }
    }
    // Le fantôme flotte doucement au-dessus de sa tombe, chacun à son rythme.
    val flotte = rememberInfiniteTransition(label = "fantome")
    val dy by flotte.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800 + (index % 4) * 350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flotte",
    )
    val lf = u * 34 * place.echelle
    Image(
        painterResource(R.drawable.fantome),
        contentDescription = null,
        modifier = Modifier
            .offset(x + (l - lf) / 2, y - lf * 1.05f)
            .size(lf, lf * 56 / 48)
            .graphicsLayer { translationY = dy * 3.dp.toPx(); alpha = 0.92f },
    )
}

private fun couleursDe(e: Encre) = androidx.compose.material3.darkColorScheme(
    primary = e.texte,
    onPrimary = e.fond,
    secondary = e.pale,
    background = Color.Transparent,
    surface = e.fond,
    onBackground = e.texte,
    onSurface = e.texte,
    onSurfaceVariant = e.texte,
    outline = e.pale,
    error = Color(0xFFC0392B),
)
