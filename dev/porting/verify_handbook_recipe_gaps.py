"""Check generated source recipes and their live-client handbook availability."""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
paths = [
    "anvilcraft/recipe/charger_charging/magnet_block.json",
    "anvilcraft/recipe/disk_to_structure_disk.json",
    "anvilcraft/recipe/exp_collector_alt.json",
    "anvilcraft/recipe/infinite_collector.json",
    "anvilcraft/recipe/smithing/frost_dragon_rod.json",
    "anvilcraft/recipe/super_heating/heated_netherite_block.json",
    "minecraft/recipe/netherrack.json",
]


def normalized(value):
    if isinstance(value, list):
        return [normalized(item) for item in value]
    if not isinstance(value, dict):
        return value
    if set(value) == {"item"}:
        return value["item"]
    return {key: normalized(item) for key, item in value.items() if not (key == "count" and item == 1)}


for name in paths:
    relative = Path("src/generated/resources/data") / name
    assert normalized(json.loads((root / relative).read_bytes())) == normalized(json.loads((reference / relative).read_bytes())), name
for obsolete in ("anvilcraft/recipe/structure_disk.json", "anvilcraft/recipe/netherrack.json"):
    assert not (root / "src/generated/resources/data" / obsolete).exists(), obsolete
for name, marker in (
    ("data-handbook-recipe-gaps-2.log", "BUILD SUCCESSFUL"),
    ("tests-handbook-recipe-gaps-focused-3.log", "All 5 required tests passed"),
    ("tests-handbook-recipe-gaps-all-client.log", "All 775 required tests passed"),
    ("checks-handbook-recipe-gaps-final.log", "BUILD SUCCESSFUL"),
):
    text = (root / "build/porting" / name).read_text(encoding="utf-8", errors="replace")
    assert marker in text, name
    if name.startswith(("data-", "checks-")):
        assert "BUILD SUCCESSFUL" in text, name
    if "all-client" in name:
        assert "PORT_HANDBOOK_PAGES_PASSED" in text and "> Task :runClient FAILED" not in text
client = json.loads((root / "run/port-validation/client/handbook-pages-26.1.json").read_text(encoding="utf-8"))
restored = {name.split("/recipe/")[0] + ":" + name.split("/recipe/")[1][:-5] for name in paths}
restored.add("anvilcraft:smoking_warp_dough_2_bread")
assert restored.isdisjoint(client["missingRecipes"])
assert client["documents"] == 416 and client["components"] == 4816 and not client["missingItems"]
report = {"source_matched_generated_recipes": paths, "restored_handbook_references": sorted(restored),
          "full_required_tests": 775, "remaining_missing_references": client["missingRecipes"],
          "limits": "Jewel crafting format/output/curse behavior remains a separate migration; no synthetic replacement recipes were added."}
(root / "build/porting/handbook-recipe-gaps-comparison.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print("Seven generated recipes match source; eight handbook references restored; 775 tests passed.")
