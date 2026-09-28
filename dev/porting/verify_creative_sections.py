"""Compare creative catalogs, effective display data, banners and supporting food behavior."""
from pathlib import Path
import json
import re
import subprocess

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
source = subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root, text=True).strip()
assert source == subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=reference, text=True).strip()


def read(path):
    return json.loads(path.read_text(encoding="utf-8"))


def log(name, marker):
    text = (root / "build/porting" / name).read_text(encoding="utf-8", errors="replace")
    assert marker in text and "BUILD SUCCESSFUL" in text, name


def normalize(value):
    if isinstance(value, list):
        return [normalize(x) for x in value]
    if isinstance(value, str):
        # 26.1 already uses color-prefixed concrete registry names; keep those save-compatible names.
        return re.sub(r"^anvilcraft:(\w+)_reinforced_concrete(.*)$", r"anvilcraft:reinforced_concrete_\1\2", value)
    if not isinstance(value, dict):
        return value
    value = {k: normalize(x) for k, x in value.items()}
    energy = value.get("anvilcraft:stored_energy")
    if isinstance(energy, dict):
        value["anvilcraft:stored_energy"] = energy["energy"]
    contents = value.get("anvilcraft:filter_contents")
    if contents is not None and "black_list" in contents:
        contents["deny_list"] = contents.pop("black_list")
    entity = value.get("anvilcraft:saved_entity")
    if entity is not None and "type" in entity:
        entity["tag"]["id"] = entity.pop("type")
    enchantments = value.get("minecraft:enchantments")
    if enchantments is not None:
        value["minecraft:enchantments"] = enchantments.get("levels", enchantments)
    food = value.get("minecraft:food")
    if food is not None:
        # Eating duration/effects moved to Consumable in 26.1; food behavior is checked at runtime.
        value["minecraft:food"] = {k: food.get(k, False if k == "can_always_eat" else 0)
                                   for k in ["nutrition", "saturation", "can_always_eat"]}
    return value


summary = {}
for mode, native_log, source_log, tabs, captures in [
    ("sectioned", "client-creative-sections-3.log", "client-creative-sections-reference-4.log", 3, 26),
    ("legacy", "client-creative-legacy-2.log", "client-creative-legacy-reference-2.log", 4, 8),
]:
    marker = f"PORT_CREATIVE_SECTIONS_PASSED: {mode} {tabs} tabs, {captures} screenshots"
    log(native_log, marker)
    log(source_log, marker)
    native = read(root / f"run/port-validation/client/creative-sections-26.1-{mode}.json")
    expected = read(reference / f"run/mun-reference/creative-sections-1.21-{mode}.json")
    assert native.keys() == expected.keys() and native["tab_order"] == expected["tab_order"]
    modes = {}
    for key, actual in native.items():
        if not isinstance(actual, dict):
            continue
        wanted = expected[key]
        for field in ["display", "search", "sections", "effective"]:
            assert normalize(actual[field]) == normalize(wanted[field]), (mode, key, field)
        modes[key] = {"display_cells": len(actual["display"]), "search_entries": len(actual["search"]),
                      "sections": len(actual["sections"])}
    summary[mode] = modes

banner_errors = {}
native = read(root / "run/port-validation/client/creative-sections-26.1-sectioned.json")
for tab in ["items", "building_blocks", "functional_blocks"]:
    data = native[tab + "/expanded"]
    maximum = max(0, (len(data["display"]) + 8) // 9 - 5)
    for index, section in enumerate(data["sections"]):
        row = section["index"] // 9 - min(maximum, section["index"] // 9)
        box = (460, 258 + row*36, 568, 294 + row*36)
        actual = np.array(Image.open(root / f"run/port-validation/client/screenshots/creative-sections-26.1-sectioned-{tab}-0-{index}.png")
                          .convert("RGB").crop(box)).astype(int)
        expected = np.array(Image.open(reference / f"run/mun-reference/screenshots/creative-sections-1.21-sectioned-{tab}-0-{index}.png")
                            .convert("RGB").crop(box)).astype(int)
        error = float(np.abs(actual-expected).mean())
        assert error < 1, (tab, index, error)
        banner_errors[f"{tab}/{index}"] = error
assert len(banner_errors) == 23
assets = Path("src/main/resources/assets/anvilcraft/textures/gui/creative_inventory/section")
for source_file in (reference / assets).rglob("*.png"):
    assert source_file.read_bytes() == (root / assets / source_file.relative_to(reference / assets)).read_bytes()
log("client-cursed-apple-2.log", "PORT_CURSED_APPLE_PASSED: 5")
log("client-cursed-apple-reference-1.log", "PORT_CURSED_APPLE_PASSED: 5")
assert read(root / "run/port-validation/client/cursed-apple-26.1.json") == read(reference / "run/mun-reference/cursed-apple-1.21.json")
recipe = Path("src/generated/resources/data/anvilcraft/recipe/cursed_golden_apple.json")
a, b = read(root / recipe), read(reference / recipe)
assert a["pattern"] == b["pattern"] and a["result"]["id"] == b["result"]["id"]
assert a["key"] == {k: v["item"] for k, v in b["key"].items()}
texture = Path("src/main/resources/assets/anvilcraft/textures/item/cursed_golden_apple.png")
assert (root / texture).read_bytes() == (reference / texture).read_bytes()
log("tests-creative-sections-final-all.log", "All 744 required tests passed")
log("data-creative-sections-verified.log", "All providers took")
for project, game in [(root, "run/port-validation/client"), (reference, "run/mun-reference")]:
    assert (project / game / "config/anvilcraft-client.toml").read_bytes() == (
        project / "build/porting/creative-sections-client-config-backup.toml").read_bytes(), "Fixture must restore client config"
report = {
    "source_commit": source, "catalogs": summary, "banner_mean_rgb_difference": banner_errors,
    "client_screenshots_per_version": 34, "apple_real_player_cases_per_version": 5, "full_required_tests": 744,
    "contracts": [
        "Default three sectioned tabs and restart-controlled four legacy tabs preserve source titles, ordering and complete expanded/folded catalogs.",
        "All 23 section banners retain source textures, alignment, text ranges, row boundaries and empty non-item cells.",
        "Search catalogs keep every color while category folding follows source boundaries; cement bucket ordering is deterministic.",
        "Energy, enchantments, filter defaults, displayed food, saved mobs and food metadata match after explicit registry/codec normalization.",
        "Cursed golden apples preserve the source recipe, texture, food values, positive/negative dimension scaling, End respawn and consumption rules.",
        "Canned food has source default stew, sixteen-tick consumption, copied custom contents and localized quantity tooltips."
    ],
    "limits": [
        "Concrete IDs retain existing 26.1 color-prefix naming; renamed IDs and codec layouts are explicitly normalized, not rewritten in saves.",
        "Banner comparisons isolate the UI artwork/text; ordinary item lighting and vanilla inventory chrome remain native.",
        "Live apple tests use controlled safe platforms; the source landing search/fallback algorithms were carried over with height/teleport API adaptations."
    ]
}
(root / "build/porting/creative-sections-report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print(json.dumps(report, indent=2))
