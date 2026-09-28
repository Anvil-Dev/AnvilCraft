"""Verify the source cauldron event API, real posting sites and native GameTests."""
from collections import Counter
from pathlib import Path
import json
import re
import subprocess

root = Path(__file__).resolve().parents[2]
source = subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root, text=True).strip()
base = "src/main/java/dev/dubhe/anvilcraft/"


def original(path):
    return subprocess.check_output(["git", "show", source + ":" + base + path], cwd=root, text=True, encoding="utf-8")


for name in ["FishTankEvent", "LargeCauldronEvent"]:
    path = "api/event/" + name + ".java"
    expected = original(path).replace("ItemInteractionResult", "InteractionResult").replace(
        "PASS_TO_DEFAULT_BLOCK_INTERACTION", "TRY_WITH_EMPTY_HAND")
    assert expected == (root / base / path).read_text(encoding="utf-8"), path
sites = Counter()
for old, new in [
    ("block/FishTankBlock.java", "block/workstation/FishTankBlock.java"),
    ("block/LargeCauldronBlock.java", "block/LargeCauldronBlock.java"),
    ("block/entity/FishTankBlockEntity.java", "block/entity/FishTankBlockEntity.java"),
    ("block/entity/LargeCauldronBlockEntity.java", "block/entity/LargeCauldronBlockEntity.java"),
]:
    pattern = r"new ((?:FishTankEvent|LargeCauldronEvent)\.\w+)\("
    expected = Counter(re.findall(pattern, original(old)))
    actual = Counter(re.findall(pattern, (root / base / new).read_text(encoding="utf-8")))
    assert actual == expected, (new, actual, expected)
    sites.update(actual)
focused = (root / "build/porting/tests-cauldron-events-focused-1.log").read_text(encoding="utf-8", errors="replace")
assert "All 10 required tests passed" in focused
full = (root / "build/porting/tests-cauldron-events-all.log").read_text(encoding="utf-8", errors="replace")
assert "All 740 required tests passed" in full and "BUILD SUCCESSFUL" in full
report = {
    "source_commit": source, "source_equivalent_posting_sites": dict(sites),
    "focused_tests": 10, "full_required_tests": 740,
    "contracts": [
        "Fish tick events are posted before the lava-only repair guard; large-cauldron tick events follow intake, fluid effects and repair and only come from the main part.",
        "Both entity-inside events precede vanilla handling and retain the collided entity/state/position context.",
        "Cancelable UseItem returns the handler-selected native InteractionResult before any item/fluid handling.",
        "GiantAnvilImpact can replace the state used for validating and processing the landing.",
        "Burning fish-tank fluid restores source four-point damage and ignition through the native deferred collision queue; both fluid-damage hooks control actual damage.",
        "MixingOutput receives fresh copies during both capacity simulation and commit, honors empty replacements, and preserves fluid contents when transformed output cannot fit."
    ],
    "limits": [
        "Interaction and collision event tests run on the server; client-side dispatch is verified from the unconditional production call sites.",
        "The first focused run passed all gameplay assertions and failed only two test-file line-length checks; the final full run includes the corrected style checks.",
        "Rendering hooks and visual comparisons are covered by the preceding fish-tank and large-cauldron render nodes."
    ]
}
(root / "build/porting/cauldron-events-report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print(json.dumps(report, indent=2))
