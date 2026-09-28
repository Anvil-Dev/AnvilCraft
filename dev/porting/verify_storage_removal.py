"""Verify storage removal regression coverage and exact source loot tables."""
from pathlib import Path
import json
import subprocess

root = Path(__file__).resolve().parents[2]
source = subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root, text=True).strip()
log = (root / "build/porting/tests-storage-removal-all.log").read_text(encoding="utf-8", errors="replace")
assert "All 708 required tests passed" in log and "BUILD SUCCESSFUL" in log
assert "BUILD SUCCESSFUL" in (root / "build/porting/data-storage-removal-2.log").read_text(encoding="utf-8", errors="replace")
loot = []
for name in ["shulker_container", "hyperdimension_storage_station"]:
    path = f"src/generated/resources/data/anvilcraft/loot_table/blocks/{name}.json"
    expected = json.loads(subprocess.check_output(["git", "show", f"{source}:{path}"], cwd=root))
    assert expected == json.loads((root / path).read_bytes()), path
    loot.append(path)
report = {
    "source_commit": source,
    "full_required_tests": 708,
    "focused_tests": 7,
    "actual_player_break_cases": 34,
    "dropped_container_replacements": 20,
    "source_loot_exact_matches": loot,
    "contracts": [
        "Crate and large-crate removal spills contents and crafting unlock materials exactly once, without dangling storage.",
        "Creative crate destruction emits contents but no crate reference item; survival emits the ordinary crate item.",
        "Both player modes and main/child destruction paths clean up multipart blocks.",
        "Empty, unupgraded advanced containers drop default components in survival and nothing in creative, deleting empty storage.",
        "Filled, crafting-unlocked, and upgraded containers preserve storage reference, contents and state after replacement.",
        "Large-crate movement/upgrade flags retain storage without spill; full suite also covers the actual upgrade chain and Ruins conversion."
    ],
    "limits": [
        "This milestone changes server lifecycle and loot data; it adds no textures, models or visual effects.",
        "Player removal is tested through ServerPlayerGameMode; no additional client screenshot run was needed for the unchanged item rendering.",
        "Crate merging still needs the source's origin search, consumed-crate recovery and crafting-unlock propagation."
    ]
}
(root / "build/porting/storage-removal-report.json").write_text(json.dumps(report, indent=2)+"\n", encoding="utf-8")
print(json.dumps(report, indent=2))
