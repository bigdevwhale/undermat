package app.undermat.data

import android.content.Context
import app.undermat.AppContainer

/**
 * The `-Pdemo` build starts four days into a week in Naples, with two weeks behind it (Kyoto and Bergen)
 * and a suitcase already well stickered.
 */
object DemoSeed {
    suspend fun seedIfEmpty(context: Context, c: AppContainer) {
        val s = c.store.load()
        if (s.stay != null || s.past.isNotEmpty()) return
        c.settings.update { it.copy(onboarded = true) }
        val today = c.today()
        val stickers = mutableListOf<Sticker>()
        fun week(id: String, start: Long, upTo: Int, skip: Set<String> = emptySet(), gift: Gift? = null, left: Long? = null): Stay {
            val host = c.host(id) ?: return Stay(id, start)
            val read = mutableSetOf<String>()
            val done = mutableSetOf<String>()
            for (d in 1..upTo) {
                val spots = listOf("word", "window", "calendar", "rules", "fridge", "record", "drawer", "tv", "kitchen", "books")
                for (sp in spots) if (d < upTo || sp in setOf("drawer", "fridge", "kitchen", "window")) read += "$d:$sp"
                for (k in Kind.entries) {
                    val key = "$d:${k.name}"
                    if (key in skip || (d == upTo && left == null)) continue
                    if ((d + k.ordinal) % 3 == 2) continue
                    done += key
                    stickers += Sticker(id, host.days[d - 1].item(k).sticker, k.name, 0)
                }
            }
            val guests = (0 until host.neighbours.size).filter { (it + 1) * 2 < upTo || left != null }.toSet()
            for (g in guests) stickers += Sticker(id, host.neighbours[g].sticker, "guest", 0)
            if (left != null) stickers += Sticker(id, "", "stay", 0)
            return Stay(id, start, entered = true, read = read, done = done, guests = guests, gift = gift, left = left, told = upTo)
        }
        val kyoto = week("kyoto", today - 60, 7, gift = Gift("Борщ с пампушками", "Ирония судьбы", "«Подмосковные вечера»", "Спасибо за вьюнки — они цвели всю неделю."), left = today - 53)
        val bergen = week("bergen", today - 30, 7, gift = Gift("Сырники", "", "Кино — «Звезда по имени Солнце»", ""), left = today - 23)
        val naples = week("naples", today - 3, 4)
        c.store.update { Save(stay = naples, past = listOf(kyoto, bergen), stickers = stickers) }
    }
}
