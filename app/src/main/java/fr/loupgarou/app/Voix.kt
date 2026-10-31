package fr.loupgarou.app

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper

/**
 * Lit les répliques enregistrées, l'une après l'autre. Une nouvelle demande coupe ce qui
 * était en cours : la voix suit toujours l'écran affiché.
 */
class Voix(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private var lecteur: MediaPlayer? = null
    private var file = ArrayDeque<Int>()

    val parle: Boolean get() = lecteur != null || file.isNotEmpty()

    private var apresTransition: List<Replique> = emptyList()
    private var auMilieu: (() -> Unit)? = null

    fun dire(repliques: List<Replique>) {
        arreter()
        file = ArrayDeque(repliques.map { it.son })
        suivante()
    }

    /**
     * Dit [transition] (le joueur se rendort, le village s'endort), marque un silence de
     * [pauseMs], appelle [milieu], puis dit [ensuite]. Sans transition, [milieu] est appelé
     * tout de suite.
     */
    fun dire(transition: List<Replique>, pauseMs: Long, ensuite: List<Replique>, milieu: () -> Unit) {
        if (transition.isEmpty()) {
            milieu()
            dire(ensuite)
            return
        }
        arreter()
        file = ArrayDeque(transition.map { it.son })
        apresTransition = ensuite
        auMilieu = {
            handler.postDelayed({
                milieu()
                file = ArrayDeque(apresTransition.map { it.son })
                apresTransition = emptyList()
                suivante()
            }, pauseMs)
        }
        suivante()
    }

    private fun suivante() {
        val son = file.removeFirstOrNull() ?: return finDeFile()
        // Un son illisible est sauté plutôt que de bloquer la suite.
        val nouveau = MediaPlayer.create(context, son) ?: return suivante()
        lecteur = nouveau.apply {
            setOnCompletionListener {
                it.release()
                lecteur = null
                if (file.isEmpty()) {
                    finDeFile()
                } else {
                    // Un court silence entre deux répliques, comme un conteur qui reprend son souffle.
                    handler.postDelayed(::suivante, PAUSE_MS)
                }
            }
            start()
        }
    }

    private fun finDeFile() {
        val milieu = auMilieu ?: return
        auMilieu = null
        milieu()
    }

    fun arreter() {
        handler.removeCallbacksAndMessages(null)
        auMilieu = null
        apresTransition = emptyList()
        file.clear()
        lecteur?.release()
        lecteur = null
    }

    private companion object {
        const val PAUSE_MS = 1_000L
    }
}
