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

    fun dire(repliques: List<Replique>) {
        arreter()
        file = ArrayDeque(repliques.map { it.son })
        suivante()
    }

    private fun suivante() {
        val son = file.removeFirstOrNull() ?: return
        // Un son illisible est sauté plutôt que de bloquer la suite.
        val nouveau = MediaPlayer.create(context, son) ?: return suivante()
        lecteur = nouveau.apply {
            setOnCompletionListener {
                it.release()
                lecteur = null
                // Un court silence entre deux répliques, comme un conteur qui reprend son souffle.
                handler.postDelayed(::suivante, PAUSE_MS)
            }
            start()
        }
    }

    fun arreter() {
        handler.removeCallbacksAndMessages(null)
        file.clear()
        lecteur?.release()
        lecteur = null
    }

    private companion object {
        const val PAUSE_MS = 700L
    }
}
