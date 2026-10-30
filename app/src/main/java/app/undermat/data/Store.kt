package app.undermat.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** What the guest left the host on the last day: something of their own country, and a note. */
data class Gift(val dish: String = "", val film: String = "", val song: String = "", val note: String = "") {
    val empty get() = dish.isBlank() && film.isBlank() && song.isBlank() && note.isBlank()
}

/**
 * A week in someone's flat. [start] is the epoch day the key was taken; day n of the stay opens on
 * start + n - 1. [read] holds "day:spot" for every note read, [done] "day:KIND" for everything cooked,
 * watched, heard or read. [left] is the day the key went back under the mat.
 */
data class Stay(
    val host: String,
    val start: Long,
    val entered: Boolean = false,
    val read: Set<String> = emptySet(),
    val done: Set<String> = emptySet(),
    val guests: Set<Int> = emptySet(),
    val gift: Gift? = null,
    val left: Long? = null,
    val told: Int = 0,
) {
    /** The day of the stay it is now, 1..7; once over, every day stays open. */
    fun day(today: Long): Int = if (left != null) DAYS else (today - start + 1).coerceIn(1, DAYS.toLong()).toInt()

    fun isRead(day: Int, spot: String) = "$day:$spot" in read
    fun isDone(day: Int, kind: Kind) = "$day:${kind.name}" in done

    companion object {
        const val DAYS = 7
    }
}

/** A luggage sticker: [label] on it, the city's [host] colours, what it was for. */
data class Sticker(val host: String, val label: String, val kind: String, val at: Long)

data class Save(val stay: Stay? = null, val past: List<Stay> = emptyList(), val stickers: List<Sticker> = emptyList()) {
    fun visits(host: String) = past.count { it.host == host }
}

/** The whole state of the app in one small JSON file, read once and written after every change. */
class Store(private val file: File) {
    private val lock = Mutex()
    private val _state = MutableStateFlow<Save?>(null)
    val state: StateFlow<Save?> = _state

    suspend fun load(): Save = lock.withLock { _state.value ?: read().also { _state.value = it } }

    suspend fun reload() = lock.withLock { _state.value = read() }

    suspend fun update(change: (Save) -> Save) {
        lock.withLock {
            val s = change(_state.value ?: read())
            _state.value = s
            withContext(Dispatchers.IO) {
                val tmp = File(file.parentFile, file.name + ".tmp")
                tmp.writeText(encode(s).toString())
                if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
            }
        }
    }

    suspend fun stay(change: (Stay) -> Stay) = update { s -> s.stay?.let { s.copy(stay = change(it)) } ?: s }

    /** Takes the key of [host]'s flat; a stay elsewhere ends today. */
    suspend fun take(host: String, today: Long) = update { s ->
        val past = s.stay?.let { s.past + it.copy(left = it.left ?: today) } ?: s.past
        s.copy(stay = Stay(host, today), past = past)
    }

    /** Puts the key back under the mat: the stay goes to the past, with its own sticker. */
    suspend fun leave(today: Long, gift: Gift?, label: String) = update { s ->
        val st = s.stay ?: return@update s
        s.copy(
            stay = null,
            past = s.past + st.copy(left = today, gift = gift?.takeUnless { it.empty }),
            stickers = s.stickers + Sticker(st.host, label, "stay", System.currentTimeMillis()),
        )
    }

    suspend fun read(day: Int, spot: String) = stay { if ("$day:$spot" in it.read) it else it.copy(read = it.read + "$day:$spot") }

    /** Marks something done and sticks its label in the suitcase. */
    suspend fun done(day: Int, kind: Kind, host: String, label: String) = update { s ->
        val st = s.stay ?: return@update s
        val key = "$day:${kind.name}"
        if (key in st.done) s
        else s.copy(stay = st.copy(done = st.done + key), stickers = s.stickers + Sticker(host, label, kind.name, System.currentTimeMillis()))
    }

    suspend fun guest(i: Int, host: String, label: String) = update { s ->
        val st = s.stay ?: return@update s
        if (i in st.guests) s
        else s.copy(stay = st.copy(guests = st.guests + i), stickers = s.stickers + Sticker(host, label, "guest", System.currentTimeMillis()))
    }

    private fun read(): Save = runCatching { decode(JSONObject(file.readText())) }.getOrElse { Save() }

    companion object {
        fun encode(s: Save): JSONObject = JSONObject().apply {
            s.stay?.let { put("stay", stay(it)) }
            put("past", JSONArray().apply { s.past.forEach { put(stay(it)) } })
            put("stickers", JSONArray().apply {
                s.stickers.forEach { put(JSONObject().put("host", it.host).put("label", it.label).put("kind", it.kind).put("at", it.at)) }
            })
        }

        fun decode(j: JSONObject): Save = Save(
            stay = j.optJSONObject("stay")?.let(::stay),
            past = j.optJSONArray("past")?.let { a -> (0 until a.length()).map { stay(a.getJSONObject(it)) } } ?: emptyList(),
            stickers = j.optJSONArray("stickers")?.let { a ->
                (0 until a.length()).map { a.getJSONObject(it).let { o -> Sticker(o.getString("host"), o.getString("label"), o.optString("kind"), o.optLong("at")) } }
            } ?: emptyList(),
        )

        private fun strings(a: JSONArray?) = a?.let { (0 until it.length()).map { i -> it.getString(i) }.toSet() } ?: emptySet()

        private fun stay(s: Stay) = JSONObject().apply {
            put("host", s.host); put("start", s.start); put("entered", s.entered)
            put("read", JSONArray(s.read.toList())); put("done", JSONArray(s.done.toList())); put("guests", JSONArray(s.guests.toList()))
            s.gift?.let { put("gift", JSONObject().put("dish", it.dish).put("film", it.film).put("song", it.song).put("note", it.note)) }
            s.left?.let { put("left", it) }
            put("told", s.told)
        }

        private fun stay(o: JSONObject) = Stay(
            host = o.getString("host"),
            start = o.getLong("start"),
            entered = o.optBoolean("entered"),
            read = strings(o.optJSONArray("read")),
            done = strings(o.optJSONArray("done")),
            guests = o.optJSONArray("guests")?.let { a -> (0 until a.length()).map { a.getInt(it) }.toSet() } ?: emptySet(),
            gift = o.optJSONObject("gift")?.let { Gift(it.optString("dish"), it.optString("film"), it.optString("song"), it.optString("note")) },
            left = if (o.has("left")) o.getLong("left") else null,
            told = o.optInt("told"),
        )
    }
}
