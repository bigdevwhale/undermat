package app.undermat.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

/** A line in both languages. */
data class L(val ru: String, val en: String) {
    fun of(ru: Boolean) = if (ru) this.ru else en

    companion object {
        val EMPTY = L("", "")
        fun of(o: JSONObject?) = if (o == null) EMPTY else L(o.optString("ru"), o.optString("en"))
    }
}

/** A list in both languages: ingredients, or swaps as "what they use → what to use instead". */
data class LL<T>(val ru: List<T>, val en: List<T>) {
    fun of(ru: Boolean) = if (ru) this.ru else en
}

/** The four things a host leaves for the guest to do, one each a day. */
enum class Kind { KITCHEN, TV, RECORD, BOOKS }

/** Something to cook, watch, hear or read, as the host's note describes it. */
data class Item(
    val kind: Kind,
    val title: L,
    val orig: String,
    val by: L,
    val year: String,
    val note: L,
    val why: L,
    val query: String,
    val sticker: String,
    val album: Boolean = false,
    val ingr: LL<String> = LL(emptyList(), emptyList()),
    val swaps: LL<Pair<String, String>> = LL(emptyList(), emptyList()),
)

data class Phrase(val say: String, val latin: String, val text: L)

data class Day(
    val kitchen: Item,
    val tv: Item,
    val record: Item,
    val books: Item,
    val fact: L,
    val phrase: Phrase,
    val rule: L,
    val letter: L,
) {
    fun item(k: Kind) = when (k) {
        Kind.KITCHEN -> kitchen
        Kind.TV -> tv
        Kind.RECORD -> record
        Kind.BOOKS -> books
    }
}

data class Holiday(val m: Int, val d: Int, val name: L, val text: L)
data class Neighbour(val who: L, val gift: L, val note: L, val how: L, val sticker: String)
data class Hour(val h: Int, val text: L)

/** One flat abroad: who lives there, what they left in every corner, a week of notes. */
data class Host(
    val id: String,
    val stamp: String,
    val lang: String,
    val city: L,
    val country: L,
    val address: L,
    val name: L,
    val sign: L,
    val about: L,
    val listing: L,
    val away: L,
    val doorplate: String,
    val mat: String,
    val word: String,
    val wordText: L,
    val toast: String,
    val toastText: L,
    val hours: List<Hour>,
    val holidays: List<Holiday>,
    val neighbours: List<Neighbour>,
    val days: List<Day>,
) {
    fun hourText(h: Int): L = hours.last { it.h <= h }.text

    companion object {
        fun parse(input: InputStream): Host = parse(JSONObject(input.bufferedReader().use { it.readText() }))

        fun parse(j: JSONObject): Host {
            fun l(k: String) = L.of(j.optJSONObject(k))
            val word = j.getJSONObject("word")
            val toast = j.getJSONObject("toast")
            return Host(
                id = j.getString("id"),
                stamp = j.getString("stamp"),
                lang = j.getString("lang"),
                city = l("city"), country = l("country"), address = l("address"), name = l("host"), sign = l("sign"),
                about = l("about"), listing = l("listing"), away = l("away"),
                doorplate = j.getString("doorplate"), mat = j.getString("mat"),
                word = word.getString("word"), wordText = L.of(word.optJSONObject("text")),
                toast = toast.getString("say"), toastText = L.of(toast.optJSONObject("text")),
                hours = j.getJSONArray("hours").objects().map { Hour(it.getInt("h"), L(it.optString("ru"), it.optString("en"))) }.sortedBy { it.h },
                holidays = j.getJSONArray("holidays").objects().map {
                    Holiday(it.getInt("m"), it.getInt("d"), L.of(it.optJSONObject("name")), L.of(it.optJSONObject("text")))
                },
                neighbours = j.getJSONArray("neighbours").objects().map {
                    Neighbour(L.of(it.optJSONObject("who")), L.of(it.optJSONObject("gift")), L.of(it.optJSONObject("note")), L.of(it.optJSONObject("how")), it.optString("sticker"))
                },
                days = j.getJSONArray("days").objects().map { day(it) },
            )
        }

        private fun day(o: JSONObject) = Day(
            kitchen = item(Kind.KITCHEN, o.getJSONObject("kitchen")),
            tv = item(Kind.TV, o.getJSONObject("tv")),
            record = item(Kind.RECORD, o.getJSONObject("record")),
            books = item(Kind.BOOKS, o.getJSONObject("books")),
            fact = L.of(o.optJSONObject("fact")),
            phrase = o.getJSONObject("phrase").let { Phrase(it.getString("say"), it.optString("latin"), L.of(it.optJSONObject("text"))) },
            rule = L.of(o.optJSONObject("rule")),
            letter = L.of(o.optJSONObject("letter")),
        )

        private fun item(kind: Kind, o: JSONObject): Item {
            fun strings(a: JSONArray?) = a?.let { (0 until it.length()).map { i -> it.getString(i) } } ?: emptyList()
            fun pairs(a: JSONArray?) = a?.let { (0 until it.length()).map { i -> it.getJSONArray(i).let { p -> p.getString(0) to p.getString(1) } } } ?: emptyList()
            val ingr = o.optJSONObject("ingr")
            val swaps = o.optJSONObject("swaps")
            return Item(
                kind = kind,
                title = L.of(o.optJSONObject("title")),
                orig = o.optString("orig"),
                by = L.of(o.optJSONObject("by")),
                year = o.optString("year"),
                note = L.of(o.optJSONObject("note")),
                why = L.of(o.optJSONObject("why")),
                query = o.optString("query"),
                sticker = o.optString("sticker"),
                album = o.optString("kind") == "album",
                ingr = LL(strings(ingr?.optJSONArray("ru")), strings(ingr?.optJSONArray("en"))),
                swaps = LL(pairs(swaps?.optJSONArray("ru")), pairs(swaps?.optJSONArray("en"))),
            )
        }

        private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
    }
}
