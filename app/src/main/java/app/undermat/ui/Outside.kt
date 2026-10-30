package app.undermat.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import app.undermat.data.Item
import app.undermat.data.Kind
import java.net.URLEncoder
import java.util.Locale

/** A place to look for a film, a record or a book: the app only knows titles and hands the search over. */
data class Where(val name: String, val url: String)

object Outside {
    private fun q(s: String) = URLEncoder.encode(s, "UTF-8")

    /** What to search for: the host's own query, or the original title and who made it. */
    fun query(item: Item, ru: Boolean): String =
        item.query.ifBlank { listOf(item.orig.substringBefore(" · "), item.by.of(ru)).filter { it.isNotBlank() }.joinToString(" ") }

    fun places(item: Item, ru: Boolean): List<Where> {
        val s = q(query(item, ru))
        return when (item.kind) {
            Kind.TV -> if (ru) listOf(
                Where("Кинопоиск", "https://www.kinopoisk.ru/index.php?kp_query=$s"),
                Where("YouTube", "https://www.youtube.com/results?search_query=$s"),
                Where("Яндекс", "https://yandex.ru/search/?text=$s"),
            ) else listOf(
                Where("JustWatch", "https://www.justwatch.com/us/search?q=$s"),
                Where("YouTube", "https://www.youtube.com/results?search_query=$s"),
                Where("Google", "https://www.google.com/search?q=$s"),
            )
            Kind.RECORD -> if (ru) listOf(
                Where("Яндекс Музыка", "https://music.yandex.ru/search?text=$s"),
                Where("YouTube Music", "https://music.youtube.com/search?q=$s"),
                Where("Spotify", "https://open.spotify.com/search/$s"),
            ) else listOf(
                Where("Spotify", "https://open.spotify.com/search/$s"),
                Where("YouTube Music", "https://music.youtube.com/search?q=$s"),
                Where("Apple Music", "https://music.apple.com/search?term=$s"),
            )
            Kind.BOOKS -> if (ru) listOf(
                Where("Литрес", "https://www.litres.ru/search/?q=${q(item.title.ru)}"),
                Where("LiveLib", "https://www.livelib.ru/find/${q(item.title.ru)}"),
                Where("Яндекс", "https://yandex.ru/search/?text=$s"),
            ) else listOf(
                Where("Goodreads", "https://www.goodreads.com/search?q=$s"),
                Where("Open Library", "https://openlibrary.org/search?q=$s"),
                Where("Google", "https://www.google.com/search?q=$s"),
            )
            Kind.KITCHEN -> listOf(Where("YouTube", "https://www.youtube.com/results?search_query=${q(item.orig.substringBefore(" · "))}"))
        }
    }

    fun open(context: Context, url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun share(context: Context, text: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        runCatching { context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

/**
 * The phone's own voice, saying a phrase in the host's language. [say] reports whether that language has
 * a voice on this phone; the first call may come too early, while the engine is still waking up.
 */
class Voice(context: Context) {
    private var ready = false
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { ready = it == TextToSpeech.SUCCESS }

    fun say(text: String, lang: String): Boolean {
        if (!ready) return false
        val loc = Locale.forLanguageTag(lang)
        val ok = tts.isLanguageAvailable(loc) >= TextToSpeech.LANG_AVAILABLE
        if (!ok) return false
        tts.language = loc
        tts.setSpeechRate(0.85f)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "phrase")
        return true
    }

    fun close() = tts.shutdown()
}
