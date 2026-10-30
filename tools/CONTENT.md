# Writing a host for «Ключ под ковриком» / Under the Mat

The app: the user swaps flats for a week with a person abroad. The app *is* that person's flat;
the host left handwritten notes in every corner. Each of the 7 days unlocks one new set of notes.
The point is **what every local knows, not what tourists know**: the film the whole country watches
at New Year, the book from the school syllabus, the song sung at weddings, the dish grandma cooks.

One host = one file `app/src/main/assets/hosts/<id>.json`, UTF-8, valid JSON (no comments, no trailing commas).

## Hard rules

1. **Everything real must be real.** Films, books, albums, songs: real titles, real authors/directors/performers,
   correct years. If you are not sure of a year, leave the year out (`"year": ""`) rather than guess.
   Facts on the fridge must be true and checkable; prefer well-documented ones. No invented quotes put in
   the mouths of real people. Holidays: correct fixed dates only (skip movable feasts like Easter).
2. **The host is fictional**; their life, family and letters are invented — that's fine. They must not be a
   real or famous person.
3. **Russian first, and native.** Write `ru` as a native Russian writer would — no calques from English,
   no «это про», no «окей», normal Russian word order and idiom. Then write `en` as natural English
   (not a word-for-word translation; same meaning, same warmth).
4. The host's **voice**: one distinct character — age, temper, humour, little habits — talking to a guest
   they've never met, in the second person (ты / you). Notes are handwritten and personal, not encyclopedic.
   Short, warm, specific, a bit bossy or funny. Show culture through their life, not through lectures.
5. No stereotypes as jokes, no politics, no religion as preaching (customs are fine), nothing gloomy.
6. Lengths (Russian, characters): `note` 220–420, `letter` 250–520, `fact` 120–280, `rule` 60–180,
   `why` 80–220, `how` 100–240, hours texts 40–120.

## Over the 7 days

- **kitchen** (7 dishes): real home cooking. Mix: at least one quick weeknight dish (≤ 30 min), one
  weekend project, one sweet or bake, one street food or snack, one festive dish. `ingr` 4–8 short lines with
  amounts. `swaps` 1–3 pairs `[what they use, what to use if you can't find it]` — assume an ordinary
  supermarket in Russia/Europe.
- **tv** (7): films, TV plays, series or cartoons locals all know. Include a childhood cartoon or children's
  film, a holiday classic, a comedy everybody quotes, one serious classic, one from the last 20 years.
  `why`: why every local knows it.
- **record** (7): albums or songs. Mix eras; include a song sung at weddings/parties/football, one modern artist.
- **books** (7): include the school-syllabus classic, a fairy tale/children's book, poetry, a modern novel.
- **fact** (7): surprising, true, about everyday life or the city/country.
- **phrase** (7): an everyday word or phrase in the local language (or dialect) with meaning and when it is said.
- **rule** (7): one unwritten house/etiquette rule or superstition, with the host's comment.
- **letter** (7): the host's letters form a small arc across the week — why they left (said in `away`), their
  family, a memory, a little secret or confession on day 5–6; day 7 is the farewell: thanks, and an invitation
  to leave something from the guest's own country (a recipe, a film, a song) in return.

## Schema

```json
{
  "id": "naples",
  "stamp": "NAPOLI",                       // the city as locals write it, Latin capitals (for stickers)
  "lang": "it-IT",                         // BCP-47 for text-to-speech of phrases
  "city": {"ru": "Неаполь", "en": "Naples"},
  "country": {"ru": "Италия", "en": "Italy"},
  "address": {"ru": "квартал Санита, 3-й этаж", "en": "Rione Sanità, third floor"},
  "host": {"ru": "Нонна Лючия", "en": "Nonna Lucia"},
  "sign": {"ru": "— Нонна", "en": "— Nonna"},
  "about": {"ru": "78 лет, вдова, сорок лет держала …", "en": "…"},          // 1–2 sentences, third person, for the listing
  "listing": {"ru": "Сдаю на неделю комнату с видом на Везувий. Кот не мой.", "en": "…"},  // the ad on the board, host's voice, ≤ 160
  "away": {"ru": "Я у дочки в Турине до воскресенья. Квартира твоя. Записки — по всему дому.", "en": "…"}, // note under the mat, ≤ 200
  "doorplate": "FAM. ESPOSITO",            // what's on the door, local script/Latin
  "mat": "BENVENUTI",                      // what's woven into the doormat ("welcome" in the local language, Latin letters)
  "word": {"word": "arrangiarsi", "text": {"ru": "…", "en": "…"}},     // an untranslatable word
  "toast": {"say": "Cent'anni!", "text": {"ru": "«Сто лет!» — …", "en": "…"}},
  "hours": [                                // what is happening outside right now, by the host's LOCAL hour; first entry hour 0
    {"h": 0,  "ru": "…", "en": "…"},
    {"h": 6,  "ru": "…", "en": "…"}
  ],                                        // 6–8 entries, hours ascending, true to local rhythm (meal times, siesta…)
  "holidays": [                             // 8–12, fixed dates across the whole year
    {"m": 11, "d": 2, "name": {"ru": "День поминовения", "en": "All Souls' Day"}, "text": {"ru": "…", "en": "…"}}
  ],
  "neighbours": [                           // exactly 3 surprise visits
    {"who": {"ru": "Кончетта, 4-й этаж", "en": "Concetta, fourth floor"},
     "gift": {"ru": "Сфольятелла", "en": "A sfogliatella"},
     "note": {"ru": "neighbour's own voice, gossip about the host, ≤ 260", "en": "…"},
     "how": {"ru": "the custom behind it, ≤ 240", "en": "…"},
     "sticker": "Sfogliatella"}
  ],
  "days": [                                 // exactly 7
    {
      "kitchen": {"title": {"ru": "Паста с картошкой", "en": "Pasta with potatoes"},
                  "orig": "Pasta e patate con la provola",
                  "note": {"ru": "…", "en": "…"},
                  "ingr": {"ru": ["картошка — 3 шт.", "…"], "en": ["3 potatoes", "…"]},
                  "swaps": {"ru": [["провола", "копчёный сулугуни"]], "en": [["provola", "smoked mozzarella"]]},
                  "sticker": "Pasta e patate"},
      "tv":     {"title": {"ru": "…", "en": "…"}, "orig": "Natale in casa Cupiello",
                 "by": {"ru": "Эдуардо Де Филиппо, телеспектакль", "en": "Eduardo De Filippo, TV play"}, "year": "1977",
                 "note": {…}, "why": {…}, "query": "Natale in casa Cupiello 1977", "sticker": "Cupiello"},
      "record": {"title": {"ru": "Terra mia", "en": "Terra mia"}, "orig": "Terra mia",
                 "by": {"ru": "Пино Даниэле", "en": "Pino Daniele"}, "year": "1977", "kind": "album",   // album | song
                 "note": {…}, "query": "Pino Daniele Terra mia", "sticker": "Pino Daniele"},
      "books":  {"title": {"ru": "Сказка сказок", "en": "The Tale of Tales"}, "orig": "Lo cunto de li cunti",
                 "by": {"ru": "Джамбаттиста Базиле", "en": "Giambattista Basile"}, "year": "1634–1636",
                 "note": {…}, "sticker": "Lo cunto"},
      "fact":   {"ru": "…", "en": "…"},
      "phrase": {"say": "Uè!", "latin": "", "text": {"ru": "Привет! А ещё — удивление, окрик…", "en": "…"}},
      "rule":   {"ru": "…", "en": "…"},
      "letter": {"ru": "…", "en": "…"}
    }
  ]
}
```

- `orig`: the original title. For non-Latin scripts write it romanized, then ` · ` and the native script,
  e.g. `"Tonari no Totoro · となりのトトロ"`. `phrase.say` is in the native script; `phrase.latin` is the
  romanization (empty for Latin-script languages).
- `title.ru` is the title as known in Russian (official Russian release title if one exists), `title.en`
  likewise in English.
- `sticker`: 1–3 words, Latin letters, ≤ 18 characters — the label on a luggage sticker.
- `query`: what to type into a streaming/search box to find it.

Validate when done: `python -c "import json,sys; json.load(open(sys.argv[1],encoding='utf-8'))" <file>` and
check that there are exactly 7 days, 3 neighbours, every `ru`/`en` pair present.
