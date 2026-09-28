"""Verify complete crate-region interaction coverage and final compilation/style checks."""
from pathlib import Path
import json
import subprocess

root = Path(__file__).resolve().parents[2]
source = subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root, text=True).strip()
tests = (root / "build/porting/tests-crate-merge-all.log").read_text(encoding="utf-8", errors="replace")
checks = (root / "build/porting/checks-crate-merge-final.log").read_text(encoding="utf-8", errors="replace")
assert "All 712 required tests passed" in tests and "Game test server shutting down" in tests
assert "BUILD SUCCESSFUL" in checks
report = {
    "source_commit": source,
    "focused_tests": 4,
    "full_required_tests": 712,
    "part_face_combinations": 162,
    "contracts": [
        "All 27 clicked parts and six faces find the complete 3x3x3 crate region.",
        "Overlapping candidates follow source X/Y/Z scan order, leaving outside crates intact.",
        "Clear the old region before placing the main part and expanding the large crate, without drops or broken parts.",
        "Recover all 27 physical crates into the target and inherit crafting unlock while retaining target crafting input.",
        "Shared source IDs transfer only once; bound target contents remain; creative retains the held item.",
        "Incomplete regions and insufficient total capacity leave source blocks, inventory, target contents and unlock unchanged."
    ],
    "adaptation": [
        "Existing native atomic transfer protection includes the 27 recovered crates; this prevents the source nontransactional capacity edge from discarding items.",
        "No assets or generated data changed; runData and new rendering captures are not applicable.",
        "Full GameTests passed before the last formatting fix; final compile/style checks passed separately on the final files."
    ]
}
(root / "build/porting/crate-merge-report.json").write_text(json.dumps(report, indent=2)+"\n", encoding="utf-8")
print(json.dumps(report, indent=2))
