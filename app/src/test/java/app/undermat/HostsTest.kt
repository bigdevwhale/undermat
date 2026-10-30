package app.undermat

import app.undermat.data.Gift
import app.undermat.data.Host
import app.undermat.data.Kind
import app.undermat.data.Looks
import app.undermat.data.Save
import app.undermat.data.Stay
import app.undermat.data.Sticker
import app.undermat.data.Store
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Every host file reads, has a full week, and has a flat drawn for it; the saved state round-trips. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HostsTest {
    private val dir = File("src/main/assets/hosts")

    private fun all() = dir.listFiles { f -> f.extension == "json" }!!.sortedBy { it.name }.map { f -> f.inputStream().use { Host.parse(it) } }

    @Test fun everyHostIsComplete() {
        val hosts = all()
        assertEquals(Looks.order.toSet(), hosts.map { it.id }.toSet())
        for (h in hosts) {
            assertEquals(h.id, 7, h.days.size)
            assertEquals(h.id, 3, h.neighbours.size)
            assertTrue(h.id, h.hours.first().h == 0)
            assertTrue(h.id, h.holidays.size >= 8)
            for ((i, d) in h.days.withIndex()) for (k in Kind.entries) {
                val it = d.item(k)
                assertTrue("${h.id} day ${i + 1} $k", it.title.ru.isNotBlank() && it.title.en.isNotBlank() && it.note.ru.isNotBlank() && it.note.en.isNotBlank())
                assertTrue("${h.id} day ${i + 1} $k sticker", it.sticker.isNotBlank() && it.sticker.length <= 18)
            }
            for (d in h.days) assertTrue(h.id, d.kitchen.ingr.ru.size == d.kitchen.ingr.en.size && d.kitchen.ingr.ru.isNotEmpty())
        }
    }

    @Test fun hoursCoverTheDay() {
        for (h in all()) for (hour in 0..23) assertTrue(h.hourText(hour).ru.isNotBlank())
    }

    @Test fun staysCountDays() {
        val s = Stay("naples", 100)
        assertEquals(1, s.day(100))
        assertEquals(4, s.day(103))
        assertEquals(7, s.day(140))
        assertEquals(7, s.copy(left = 101).day(101))
    }

    @Test fun saveRoundTrips() {
        val save = Save(
            stay = Stay("kyoto", 10, entered = true, read = setOf("1:fridge"), done = setOf("1:KITCHEN"), guests = setOf(0), told = 1),
            past = listOf(Stay("naples", 1, entered = true, gift = Gift("Борщ", "", "", "спасибо"), left = 8)),
            stickers = listOf(Sticker("naples", "Genovese", "KITCHEN", 5)),
        )
        val back = Store.decode(JSONObject(Store.encode(save).toString()))
        assertEquals(save, back)
    }
}
