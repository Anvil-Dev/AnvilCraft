"""Compare real Jade tooltip collection before and after reload in both clients."""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
native_run = root / "run/port-validation/client"
source_run = reference / "run/mun-reference"
for version in ("native", "reference"):
    log = (root / f"build/porting/client-cursed-gold-{version}-verified.log").read_text(encoding="utf-8", errors="replace")
    assert "BUILD SUCCESSFUL" in log and "PORT_CURSED_GOLD_JADE_PASSED" in log, version
    assert log.count("PORT_CURSED_GOLD_RECT:") == 2, version
native = json.loads((native_run / "cursed-gold-26.1.json").read_text(encoding="utf-8"))
source = json.loads((source_run / "cursed-gold-1.21.json").read_text(encoding="utf-8"))
expected = ["Block of Cursed Gold", "Ench Power: -1", "AnvilCraft"]
assert native == source == [expected, expected], (native, source)
key = "config.jade.plugin_anvilcraft.cursed_gold_enchant_power"
for language, folder in (("en_us", "generated"), ("en_ud", "generated"), ("zh_cn", "main")):
    relative = Path(f"src/{folder}/resources/assets/anvilcraft/lang/{language}.json")
    actual = json.loads((root / relative).read_text(encoding="utf-8"))[key]
    original = json.loads((reference / relative).read_text(encoding="utf-8"))[key]
    assert actual == original, (language, actual, original)
for version, run in (("26.1", native_run), ("1.21", source_run)):
    for index in (1, 2):
        assert (run / f"screenshots/cursed-gold-{version}-{index}.png").is_file()
report = {"tooltip_before_and_after_reload": native, "languages": ["en_us", "en_ud", "zh_cn"],
          "visual_review": "Inspect paired screenshots; vanilla world and Jade framework pixels are not required to match."}
(root / "build/porting/cursed-gold-comparison.json").write_text(
    json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("Cursed gold Jade parity passed: exact tooltip lines, reload stability, and three language entries.")
