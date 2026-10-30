package app.undermat.data

import androidx.compose.ui.graphics.Color
import java.time.ZoneId

enum class Floor { TERRACOTTA, PARQUET, TATAMI, PLANKS, CEMENT, CHECKER }
enum class Tiles { MAJOLICA, AZULEJO, TALAVERA, SUBWAY, IZNIK, WOOD, HEX, CHECKER }
enum class Win { SHUTTERS, SHOJI, SASH, LOUVRES, ARCH }
enum class Lamp { PENDANT, PAPER, FAN, GLASS, BRASS }

/** The one thing in each flat that could only be there. */
enum class Sign { CORNICELLO, FURIN, PAPEL_PICADO, CANDLES, STICKY_NOTES, BAGUETTE, KANTSI, FAN_ALPANA, FITAS, SARDINE, BRIGID, NAZAR }

/**
 * How a host's flat looks and where it is: everything about it that isn't words.
 * [at] and [inCity] are the host and the city in the forms a sentence needs ("у Нонны Лючии", "в Неаполе").
 */
data class Look(
    val zone: ZoneId,
    val lat: Double,
    val lon: Double,
    val at: L,
    val inCity: L,
    val wall: Color,
    val wallLow: Color?,
    val floor: Floor,
    val floorA: Color,
    val floorB: Color,
    val tiles: Tiles,
    val tileA: Color,
    val tileB: Color,
    val tileBg: Color,
    val window: Win,
    val frame: Color,
    val shutter: Color,
    val wood: Color,
    val fridge: Color,
    val rug: Color,
    val rugEdge: Color,
    val lamp: Lamp,
    val lampColor: Color,
    val door: Color,
    val sign: Sign,
    /** The colours of this city's luggage stickers. */
    val stickers: List<Color>,
    /** The neighbour lowers a basket past the window instead of knocking. */
    val basket: Boolean = false,
)

private fun c(hex: Long) = Color(0xFF000000 or hex)

object Looks {
    val all: Map<String, Look> = mapOf(
        "naples" to Look(
            ZoneId.of("Europe/Rome"), 40.853, 14.250, L("у Нонны Лючии", "at Nonna Lucia's"), L("в Неаполе", "in Naples"),
            wall = c(0xE8C590), wallLow = null, floor = Floor.TERRACOTTA, floorA = c(0xB9633E), floorB = c(0x97492B),
            tiles = Tiles.MAJOLICA, tileA = c(0x2C5D8A), tileB = c(0xE2B13C), tileBg = c(0xF4ECD8),
            window = Win.SHUTTERS, frame = c(0xF1E6CF), shutter = c(0x3F6B4F), wood = c(0x7A4B2E), fridge = c(0xEFE6D2),
            rug = c(0xA33B2F), rugEdge = c(0xE2B13C), lamp = Lamp.PENDANT, lampColor = c(0x2F5A43), door = c(0x2F5A43),
            sign = Sign.CORNICELLO, stickers = listOf(c(0xC84A32), c(0x2C5D8A), c(0x3F6B4F), c(0xE2B13C), c(0x7A4B2E)), basket = true,
        ),
        "kyoto" to Look(
            ZoneId.of("Asia/Tokyo"), 35.030, 135.748, L("у госпожи Оты", "at Mrs Ōta's"), L("в Киото", "in Kyoto"),
            wall = c(0xE9DFC8), wallLow = c(0x8C6A48), floor = Floor.TATAMI, floorA = c(0xC9C08A), floorB = c(0x9D9462),
            tiles = Tiles.WOOD, tileA = c(0x6E4E33), tileB = c(0x8C6A48), tileBg = c(0xA9865F),
            window = Win.SHOJI, frame = c(0x5A3E2A), shutter = c(0xF6F1E2), wood = c(0x5A3E2A), fridge = c(0xF2F0EA),
            rug = c(0x2E3B55), rugEdge = c(0xC9A64A), lamp = Lamp.PAPER, lampColor = c(0xF3E9CF), door = c(0x5A3E2A),
            sign = Sign.FURIN, stickers = listOf(c(0xB0353A), c(0x2E3B55), c(0x5E7B4C), c(0xC9A64A), c(0x6E4E33)),
        ),
        "oaxaca" to Look(
            ZoneId.of("America/Mexico_City"), 17.066, -96.719, L("у доньи Тере", "at Doña Tere's"), L("в Оахаке", "in Oaxaca"),
            wall = c(0x3F8FA0), wallLow = c(0xE7B33F), floor = Floor.CEMENT, floorA = c(0xC1553B), floorB = c(0xE8D9B5),
            tiles = Tiles.TALAVERA, tileA = c(0x1F4E9A), tileB = c(0xE7B33F), tileBg = c(0xF6F0E1),
            window = Win.ARCH, frame = c(0xE65A7A), shutter = c(0x2F7A5A), wood = c(0x8A4B2A), fridge = c(0xF2EBDD),
            rug = c(0xD6455B), rugEdge = c(0x2F7A5A), lamp = Lamp.GLASS, lampColor = c(0x2F7A5A), door = c(0x7A2E3A),
            sign = Sign.PAPEL_PICADO, stickers = listOf(c(0xD6455B), c(0x1F4E9A), c(0x2F7A5A), c(0xE7B33F), c(0x7A2E3A)),
        ),
        "bergen" to Look(
            ZoneId.of("Europe/Oslo"), 60.395, 5.312, L("у Арне", "at Arne's"), L("в Бергене", "in Bergen"),
            wall = c(0xDCE3DF), wallLow = c(0xA9BDB6), floor = Floor.PLANKS, floorA = c(0xC9AE86), floorB = c(0xA88C64),
            tiles = Tiles.SUBWAY, tileA = c(0xF4F4F0), tileB = c(0xCFD6D4), tileBg = c(0xE9EDEB),
            window = Win.SASH, frame = c(0xFAFAF6), shutter = c(0x9E3B32), wood = c(0x8E6F4E), fridge = c(0xF4F4F0),
            rug = c(0x2F4B6E), rugEdge = c(0xF1EDE4), lamp = Lamp.BRASS, lampColor = c(0x2B2E33), door = c(0x9E3B32),
            sign = Sign.CANDLES, stickers = listOf(c(0x9E3B32), c(0x2F4B6E), c(0x4E6B5A), c(0xC9A04A), c(0x2B2E33)),
        ),
        "seoul" to Look(
            ZoneId.of("Asia/Seoul"), 37.556, 126.906, L("у Джи Хо", "at Ji-ho's"), L("в Сеуле", "in Seoul"),
            wall = c(0xF1EDE6), wallLow = null, floor = Floor.PLANKS, floorA = c(0xD9B98C), floorB = c(0xC4A273),
            tiles = Tiles.HEX, tileA = c(0xF6F4EF), tileB = c(0x9FB7C9), tileBg = c(0xE6E3DC),
            window = Win.SASH, frame = c(0xE9E9E9), shutter = c(0x9FB7C9), wood = c(0xC6A47A), fridge = c(0xE7ECEF),
            rug = c(0xF2C14E), rugEdge = c(0x4A6FA5), lamp = Lamp.GLASS, lampColor = c(0xF2C14E), door = c(0x4A6FA5),
            sign = Sign.STICKY_NOTES, stickers = listOf(c(0x4A6FA5), c(0xE0525B), c(0xF2C14E), c(0x3E9C8F), c(0x2D2D3A)),
        ),
        "lyon" to Look(
            ZoneId.of("Europe/Paris"), 45.774, 4.832, L("у Матильды", "at Mathilde's"), L("в Лионе", "in Lyon"),
            wall = c(0xE7DCC7), wallLow = c(0xB7A585), floor = Floor.PARQUET, floorA = c(0xA9764A), floorB = c(0x8C5E37),
            tiles = Tiles.CHECKER, tileA = c(0x2C2C2C), tileB = c(0xF2EEE4), tileBg = c(0xF2EEE4),
            window = Win.SHUTTERS, frame = c(0xF3EFE6), shutter = c(0x7D8F96), wood = c(0x6B4A33), fridge = c(0xEAE6DC),
            rug = c(0x8E2F3C), rugEdge = c(0xD8B25A), lamp = Lamp.BRASS, lampColor = c(0x3A2A1E), door = c(0x2E4057),
            sign = Sign.BAGUETTE, stickers = listOf(c(0x8E2F3C), c(0x2E4057), c(0xD8B25A), c(0x5B7A5A), c(0x3A2A1E)),
        ),
        "tbilisi" to Look(
            ZoneId.of("Asia/Tbilisi"), 41.690, 44.800, L("у Дато", "at Dato's"), L("в Тбилиси", "in Tbilisi"),
            wall = c(0xE6D3B3), wallLow = c(0x6E8F7A), floor = Floor.PARQUET, floorA = c(0x8E5A3A), floorB = c(0x74462A),
            tiles = Tiles.WOOD, tileA = c(0x6E4A30), tileB = c(0x8E5A3A), tileBg = c(0x9C6B48),
            window = Win.ARCH, frame = c(0x4F7564), shutter = c(0x4F7564), wood = c(0x6E4A30), fridge = c(0xEDE5D5),
            rug = c(0x8B2232), rugEdge = c(0x1F3B5C), lamp = Lamp.BRASS, lampColor = c(0x8E5A3A), door = c(0x6E4A30),
            sign = Sign.KANTSI, stickers = listOf(c(0x8B2232), c(0x4F7564), c(0x1F3B5C), c(0xC99A3C), c(0x6E4A30)),
        ),
        "kolkata" to Look(
            ZoneId.of("Asia/Kolkata"), 22.600, 88.372, L("у Румы", "at Ruma's"), L("в Калькутте", "in Kolkata"),
            wall = c(0xBFD9C8), wallLow = c(0xE9E1CF), floor = Floor.CHECKER, floorA = c(0xEDE6D6), floorB = c(0x9E3A2E),
            tiles = Tiles.HEX, tileA = c(0xE9E1CF), tileB = c(0x9E3A2E), tileBg = c(0xD9CFB9),
            window = Win.LOUVRES, frame = c(0xE9E1CF), shutter = c(0x2F6B4A), wood = c(0x5C3A26), fridge = c(0xD8E3E0),
            rug = c(0xC0392B), rugEdge = c(0xF2C14E), lamp = Lamp.FAN, lampColor = c(0x3A3A3A), door = c(0x2F6B4A),
            sign = Sign.FAN_ALPANA, stickers = listOf(c(0xC0392B), c(0x2F6B4A), c(0xF2C14E), c(0x1F4E79), c(0x5C3A26)),
        ),
        "salvador" to Look(
            ZoneId.of("America/Bahia"), -12.966, -38.505, L("у местре Тониньо", "at Mestre Toninho's"), L("в Салвадоре", "in Salvador"),
            wall = c(0xF2C14E), wallLow = c(0x2E86AB), floor = Floor.CHECKER, floorA = c(0xF4EAD5), floorB = c(0x2E86AB),
            tiles = Tiles.AZULEJO, tileA = c(0x1F5FA8), tileB = c(0x1F5FA8), tileBg = c(0xF7F3E8),
            window = Win.SHUTTERS, frame = c(0xF7F3E8), shutter = c(0x2E86AB), wood = c(0x6B3E26), fridge = c(0xF3EEE2),
            rug = c(0x2A9D5B), rugEdge = c(0xE63946), lamp = Lamp.GLASS, lampColor = c(0xE63946), door = c(0x2E86AB),
            sign = Sign.FITAS, stickers = listOf(c(0x2A9D5B), c(0x2E86AB), c(0xE63946), c(0xF2A541), c(0x6B3E26)),
        ),
        "lisbon" to Look(
            ZoneId.of("Europe/Lisbon"), 38.711, -9.130, L("у Инеш", "at Inês's"), L("в Лиссабоне", "in Lisbon"),
            wall = c(0xF0E6D6), wallLow = null, floor = Floor.PLANKS, floorA = c(0xB88B5A), floorB = c(0x9C7246),
            tiles = Tiles.AZULEJO, tileA = c(0x1C4E8C), tileB = c(0x1C4E8C), tileBg = c(0xF8F5EC),
            window = Win.SHUTTERS, frame = c(0xF8F5EC), shutter = c(0x2E5E4E), wood = c(0x7A5233), fridge = c(0xEDE7DA),
            rug = c(0x1C4E8C), rugEdge = c(0xF2C14E), lamp = Lamp.PENDANT, lampColor = c(0xC8553D), door = c(0xC8553D),
            sign = Sign.SARDINE, stickers = listOf(c(0x1C4E8C), c(0xC8553D), c(0xF2C14E), c(0x2E5E4E), c(0x7A5233)),
        ),
        "galway" to Look(
            ZoneId.of("Europe/Dublin"), 53.268, -9.057, L("у Шивон", "at Siobhán's"), L("в Голуэе", "in Galway"),
            wall = c(0xE9E2CF), wallLow = c(0x6D8A6A), floor = Floor.PLANKS, floorA = c(0x8C6A4A), floorB = c(0x74563A),
            tiles = Tiles.SUBWAY, tileA = c(0xEDE9DD), tileB = c(0xC9C2AE), tileBg = c(0xE2DCCB),
            window = Win.SASH, frame = c(0xF4F1E8), shutter = c(0xB23A48), wood = c(0x5B4130), fridge = c(0xEFEBDF),
            rug = c(0x3E6B48), rugEdge = c(0xC9A04A), lamp = Lamp.PENDANT, lampColor = c(0x3E6B48), door = c(0xB23A48),
            sign = Sign.BRIGID, stickers = listOf(c(0x3E6B48), c(0xB23A48), c(0xC9A04A), c(0x2F4B6E), c(0x5B4130)),
        ),
        "istanbul" to Look(
            ZoneId.of("Europe/Istanbul"), 41.032, 29.031, L("у Несрин-ханым", "at Nesrin Hanım's"), L("в Стамбуле", "in Istanbul"),
            wall = c(0xEFE2CC), wallLow = c(0xA0522D), floor = Floor.PARQUET, floorA = c(0x9A6A43), floorB = c(0x80542F),
            tiles = Tiles.IZNIK, tileA = c(0x1F6FA8), tileB = c(0xC0392B), tileBg = c(0xF7F3EA),
            window = Win.SASH, frame = c(0xF3ECDD), shutter = c(0x7A4B2E), wood = c(0x6B4226), fridge = c(0xF1ECE2),
            rug = c(0x9B2335), rugEdge = c(0x1F3A5F), lamp = Lamp.GLASS, lampColor = c(0x1F6FA8), door = c(0x7A4B2E),
            sign = Sign.NAZAR, stickers = listOf(c(0x9B2335), c(0x1F6FA8), c(0x2E7D6B), c(0xD4A017), c(0x6B4226)),
        ),
    )

    /** In the order they hang on the board. */
    val order = listOf("naples", "kyoto", "oaxaca", "bergen", "seoul", "lyon", "tbilisi", "kolkata", "salvador", "lisbon", "galway", "istanbul")

    fun of(id: String): Look = all.getValue(id)
}
