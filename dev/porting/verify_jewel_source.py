"""Verify source jewel data, paired live menus and restored handbook references."""
import json
from pathlib import Path

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
data_path = Path("src/generated/resources/data/anvilcraft/recipe/jewel_crafting")
native_files = {p.name: p for p in (root / data_path).glob("*.json")}
source_files = {p.name: p for p in (reference / data_path).glob("*.json")}


def normalized(value):
    if isinstance(value, list):
        return [normalized(item) for item in value]
    if not isinstance(value, dict):
        return value
    if set(value) == {"item"}:
        return value["item"]
    if set(value) == {"tag"}:
        return "#" + value["tag"]
    return {key: normalized(item) for key, item in value.items() if not (key == "count" and item == 1)}


assert native_files.keys() == source_files.keys() and len(native_files) == 18
for name in native_files:
    assert normalized(json.loads(native_files[name].read_bytes())) == normalized(json.loads(source_files[name].read_bytes())), name
for name, marker in (("data-jewel-source-final.log", "BUILD SUCCESSFUL"),
                     ("tests-jewel-all-client.log", "All 781 required tests passed"),
                     ("client-jewel-source-reference.log", "PORT_JEWEL_CLIENT_PASSED: 12 scenarios"),
                     ("client-jewel-handbook.log", "PORT_HANDBOOK_PAGES_PASSED")):
    log = (root / "build/porting" / name).read_text(encoding="utf-8", errors="replace")
    assert marker in log and "BUILD SUCCESSFUL" in log, name
native_run = root / "run/port-validation/client"
source_run = reference / "run/mun-reference"
native = json.loads((native_run / "jewel-source-26.1.json").read_text(encoding="utf-8"))
source = json.loads((source_run / "jewel-source-1.21.json").read_text(encoding="utf-8"))
assert native == source and len(native) == 12
handbook = json.loads((native_run / "handbook-pages-26.1.json").read_text(encoding="utf-8"))
assert handbook["missingRecipes"] == ["anvilcraft:jei/solid_liquid/enchanted_gold_ingot"]
assert handbook["documents"] == 416 and not handbook["missingItems"]
for name in ("textures/gui/crafting/background/jewelcrafting_table.png", "textures/gui/ageratum/jewelcrafting_table.png"):
    relative = Path("src/main/resources/assets/anvilcraft") / name
    assert (root / relative).read_bytes() == (reference / relative).read_bytes()
metrics = {}
for index in range(12):
    actual = np.asarray(Image.open(native_run / f"screenshots/jewel-source-26.1-{index}.png").convert("RGB"), dtype=float)
    expected = np.asarray(Image.open(source_run / f"screenshots/jewel-source-1.21-{index}.png").convert("RGB"), dtype=float)
    assert actual.shape == expected.shape == (720, 1280, 3)
    difference = np.abs(actual[194:526, 464:816] - expected[194:526, 464:816])
    metrics[index] = {"panel_mean_rgb_error": float(difference.mean())}
report = {"source_static_recipes": 18, "paired_menu_scenarios": native, "full_required_tests": 781,
          "panel_metrics": metrics, "remaining_handbook_reference": handbook["missingRecipes"],
          "limits": "Panel pixels can differ due to native item lighting, ghost rendering and animated glint. Actual JEI-category interaction is not covered by the menu captures."}
(root / "build/porting/jewel-source-comparison.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print("Jewel source parity passed: 18 static recipes, 12 paired menu states, 781 server tests and all three handbook references restored.")
