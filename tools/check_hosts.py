"""Checks host files against tools/CONTENT.md: python tools/check_hosts.py [files...] (default: all hosts)."""
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
HOSTS = ROOT / "app/src/main/assets/hosts"


def tr(errs, where, v, lo=0, hi=10_000):
    if not isinstance(v, dict) or not isinstance(v.get("ru"), str) or not isinstance(v.get("en"), str):
        errs.append(f"{where}: needs ru and en")
        return
    for k in ("ru", "en"):
        if not v[k].strip():
            errs.append(f"{where}.{k}: empty")
    n = len(v["ru"])
    if n < lo * 0.8 or n > hi * 1.25:
        errs.append(f"{where}.ru: {n} chars, want {lo}–{hi}")


def check(path):
    errs = []
    try:
        h = json.loads(pathlib.Path(path).read_text(encoding="utf-8"))
    except Exception as e:
        return [f"not JSON: {e}"]
    for k in ("id", "stamp", "lang", "doorplate", "mat"):
        if not isinstance(h.get(k), str) or not h[k]:
            errs.append(f"{k}: missing")
    if h.get("id") != pathlib.Path(path).stem:
        errs.append("id must equal the file name")
    for k in ("city", "country", "address", "host", "sign", "about"):
        tr(errs, k, h.get(k))
    tr(errs, "listing", h.get("listing"), 40, 160)
    tr(errs, "away", h.get("away"), 40, 200)
    w = h.get("word", {})
    if not w.get("word"):
        errs.append("word.word missing")
    tr(errs, "word.text", w.get("text"), 80, 320)
    t = h.get("toast", {})
    if not t.get("say"):
        errs.append("toast.say missing")
    tr(errs, "toast.text", t.get("text"))
    hours = h.get("hours", [])
    if not 6 <= len(hours) <= 8 or hours[0].get("h") != 0 or [x["h"] for x in hours] != sorted(x["h"] for x in hours):
        errs.append("hours: 6–8 entries, ascending, first h=0")
    for i, x in enumerate(hours):
        tr(errs, f"hours[{i}]", x, 40, 120)
    hol = h.get("holidays", [])
    if not 8 <= len(hol) <= 12:
        errs.append(f"holidays: {len(hol)}, want 8–12")
    for i, x in enumerate(hol):
        if not (1 <= x.get("m", 0) <= 12 and 1 <= x.get("d", 0) <= 31):
            errs.append(f"holidays[{i}]: bad date")
        tr(errs, f"holidays[{i}].name", x.get("name"))
        tr(errs, f"holidays[{i}].text", x.get("text"), 60, 260)
    nb = h.get("neighbours", [])
    if len(nb) != 3:
        errs.append(f"neighbours: {len(nb)}, want 3")
    for i, x in enumerate(nb):
        for k in ("who", "gift"):
            tr(errs, f"neighbours[{i}].{k}", x.get(k))
        tr(errs, f"neighbours[{i}].note", x.get("note"), 80, 260)
        tr(errs, f"neighbours[{i}].how", x.get("how"), 80, 240)
        st = x.get("sticker", "")
        if not st or len(st) > 18:
            errs.append(f"neighbours[{i}].sticker: 1–18 chars")
    days = h.get("days", [])
    if len(days) != 7:
        errs.append(f"days: {len(days)}, want 7")
    for d, day in enumerate(days, 1):
        p = f"days[{d}]"
        for kind in ("kitchen", "tv", "record", "books"):
            it = day.get(kind)
            if not isinstance(it, dict):
                errs.append(f"{p}.{kind}: missing")
                continue
            tr(errs, f"{p}.{kind}.title", it.get("title"))
            tr(errs, f"{p}.{kind}.note", it.get("note"), 220, 420)
            if not it.get("orig"):
                errs.append(f"{p}.{kind}.orig missing")
            st = it.get("sticker", "")
            if not st or len(st) > 18:
                errs.append(f"{p}.{kind}.sticker: 1–18 chars, got {st!r}")
            if kind != "kitchen":
                tr(errs, f"{p}.{kind}.by", it.get("by"))
                if not isinstance(it.get("year"), str):
                    errs.append(f"{p}.{kind}.year must be a string")
            if kind in ("tv", "record") and not it.get("query"):
                errs.append(f"{p}.{kind}.query missing")
            if kind == "tv":
                tr(errs, f"{p}.tv.why", it.get("why"), 80, 220)
            if kind == "record" and it.get("kind") not in ("album", "song"):
                errs.append(f"{p}.record.kind: album|song")
            if kind == "kitchen":
                for k in ("ingr", "swaps"):
                    v = it.get(k, {})
                    if not isinstance(v, dict) or not v.get("ru") or not v.get("en"):
                        errs.append(f"{p}.kitchen.{k}: needs ru and en lists")
                    elif len(v["ru"]) != len(v["en"]):
                        errs.append(f"{p}.kitchen.{k}: ru and en lengths differ")
                for lang in ("ru", "en"):
                    for pair in it.get("swaps", {}).get(lang, []):
                        if not (isinstance(pair, list) and len(pair) == 2):
                            errs.append(f"{p}.kitchen.swaps.{lang}: pairs of two")
        tr(errs, f"{p}.fact", day.get("fact"), 120, 280)
        ph = day.get("phrase", {})
        if not ph.get("say") or "latin" not in ph:
            errs.append(f"{p}.phrase: say and latin")
        tr(errs, f"{p}.phrase.text", ph.get("text"))
        tr(errs, f"{p}.rule", day.get("rule"), 60, 180)
        tr(errs, f"{p}.letter", day.get("letter"), 250, 520)
    return errs


def main():
    files = sys.argv[1:] or sorted(str(p) for p in HOSTS.glob("*.json"))
    bad = 0
    for f in files:
        e = check(f)
        print(f"{pathlib.Path(f).name}: {'ok' if not e else str(len(e)) + ' problems'}")
        for x in e:
            print("   ", x)
        bad += bool(e)
    sys.exit(1 if bad else 0)


if __name__ == "__main__":
    main()
