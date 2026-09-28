"""Compare actual Jade progress rows and legacy fluid formatting across both clients."""
from pathlib import Path
import json
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
source = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=reference, text=True).strip()
for filename in ["client-jade-fluid-parity-2.log", "client-jade-fluid-parity-reference-1.log"]:
    text = (root / "build/porting" / filename).read_text(encoding="utf-8", errors="replace")
    assert "PORT_JADE_FLUID_PARITY_PASSED: 9 cases" in text and "BUILD SUCCESSFUL" in text, filename
full = (root / "build/porting/tests-jade-fluid-parity-final.log").read_text(encoding="utf-8", errors="replace")
assert "All 724 required tests passed" in full and "BUILD SUCCESSFUL" in full
assert "BUILD SUCCESSFUL" in (root / "build/porting/data-jade-fluid-parity-1.log").read_text(encoding="utf-8", errors="replace")
a = json.loads((root / "run/port-validation/client/jade-fluid-parity-26.1.json").read_text(encoding="utf-8"))
b = json.loads((reference / "run/mun-reference/jade-fluid-parity-1.21.json").read_text(encoding="utf-8"))
assert a["formats"] == b["formats"] and len(a["formats"]) == 11
assert a["cases"].keys() == b["cases"].keys() and len(a["cases"]) == 9
for name, expected in b["cases"].items():
    actual = a["cases"][name]
    assert len(actual) == len(expected), (name, actual, expected)
    for row, ref in zip(actual, expected):
        assert row["text"] == ref["text"], (name, row, ref)
        assert abs(row["progress"] - ref["progress"]) < 1e-7, (name, row, ref)
for path in ["src/generated/resources/assets/anvilcraft/lang/en_us.json", "src/main/resources/assets/anvilcraft/lang/zh_cn.json"]:
    expected = json.loads(subprocess.check_output(["git", "show", f"{source}:{path}"], cwd=root))
    actual = json.loads((root / path).read_bytes())
    assert actual["tooltip.anvilcraft.infinity"] == expected["tooltip.anvilcraft.infinity"]
report = {
    "source_commit": source,
    "actual_client_cases": 9,
    "formatted_amounts": a["formats"],
    "source_matching_rows": a["cases"],
    "full_required_tests": 724,
    "contracts": [
        "Finite storage uses the source Jade bucket/millibucket and compact-prefix formatting, including empty capacity hints.",
        "Infinite storage displays localized Infinity rather than the old native infinity symbol.",
        "Mixed infinite tanks show finite amount/capacity using the existing shared UnitUtil formatter.",
        "Finite multi-fluid progress uses the source per-slot capacities and preserves the enhanced empty virtual slot.",
        "Native narration remains attached to the native fluid quantity component while visible labels match source."
    ],
    "limits": [
        "World lighting, animated fluid frames and framework tooltip padding remain native to each Minecraft/Jade version.",
        "Comparisons assert actual rendered progress-element text and raw progress values; screenshots provide visual inspection."
    ]
}
(root / "build/porting/jade-fluid-parity-report.json").write_text(json.dumps(report, indent=2)+"\n", encoding="utf-8")
print(json.dumps(report, indent=2))
