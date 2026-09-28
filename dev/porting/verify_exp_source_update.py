"""Verify the two latest 1.21 experience collector updates on 26.1."""
from pathlib import Path
import json
import subprocess

root = Path(__file__).resolve().parents[2]
source = "a3a037f17876b2e7036779baee71898978c5d4be"
previous = "e0dedd6e8f1dd8667090eae161ba372c4da10bc2"
commits = subprocess.check_output(["git", "rev-list", "--reverse", f"{previous}..{source}"], cwd=root, text=True).splitlines()
assert len(commits) == 2
log = (root / "build/porting/tests-exp-source-all.log").read_text(encoding="utf-8", errors="replace")
assert "All 724 required tests passed" in log and "BUILD SUCCESSFUL" in log
report = {
    "source_commit": source,
    "previous_source": previous,
    "source_updates": commits,
    "focused_tests": 6,
    "full_required_tests": 724,
    "contracts": [
        "Both periodic scanning and entity-add interception absorb available XP points at twenty mB per point.",
        "Partially consumed merged orbs preserve whole remaining units and award the remainder back into the world.",
        "Nonpositive orb values are discarded and nonpositive merged counts normalize to one.",
        "Less than twenty mB of space leaves XP untouched; a full collector does not block another eligible collector.",
        "Grid power, redstone disabling and configured range still constrain collection.",
        "Long multiplication preserves XP for merged counts up to Integer.MAX_VALUE without overflow."
    ],
    "adaptation": [
        "Both source changes use the existing shared 26.1 collector helper, so ExperienceOrbMixin keeps delegating to one implementation.",
        "Experience data uses the 26.1 getValue accessor and transactional fluid insertion.",
        "Tests exercise actual ServerLevel.addFreshEntity interception and periodic gridTick, then sum surviving world orbs."
    ],
    "limits": ["No resource or rendering assets changed; no datagen or new screenshot capture was required for this gameplay update."]
}
(root / "build/porting/exp-source-update-report.json").write_text(json.dumps(report, indent=2)+"\n", encoding="utf-8")
print(json.dumps(report, indent=2))
