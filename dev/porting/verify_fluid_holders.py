"""Verify fluid-holder render geometry, materials and still/flowing texture selection."""
from pathlib import Path
import json
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
source = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=reference, text=True).strip()
for name in ["client-fluid-holders-1.log", "client-fluid-holders-reference-1.log"]:
    text = (root / "build/porting" / name).read_text(encoding="utf-8", errors="replace")
    assert "PORT_FLUID_HOLDERS_PASSED: 12 cases" in text and "BUILD SUCCESSFUL" in text, name
full = (root / "build/porting/tests-fluid-holders-all.log").read_text(encoding="utf-8", errors="replace")
assert "All 730 required tests passed" in full
assert "BUILD SUCCESSFUL" in (root / "build/porting/checks-fluid-holders-final.log").read_text(encoding="utf-8", errors="replace")
native = json.loads((root / "run/port-validation/client/fluid-holders-26.1.json").read_text(encoding="utf-8"))
expected = json.loads((reference / "run/mun-reference/fluid-holders-1.21.json").read_text(encoding="utf-8"))
assert native.keys() == expected.keys() and len(native) == 12
vertices = 0
for name, modes in expected.items():
    for mode, batches in modes.items():
        actual = native[name][mode]
        assert len(actual) == len(batches), (name, mode)
        for a, b in zip(actual, batches):
            assert a["type"] == b["type"] and a["textures"] == b["textures"], (name, mode)
            assert "unknown" not in a["textures"] and len(a["vertices"]) == len(b["vertices"]), (name, mode)
            for vertex, ref in zip(a["vertices"], b["vertices"]):
                assert all(abs(vertex[k]-ref[k]) <= (0 if k in ["color", "light"] else 1e-6) for k in vertex), (name, mode)
                vertices += 1
for name in ["column-water", "column-lava", "column-milk"]:
    assert len(native[name]["secondary"]) == 1
    column = native[name]["world"][1]
    assert set(column["textures"]) == {"still", "flowing"}
    for vertex, texture in zip(column["vertices"], column["textures"]):
        assert texture == ("still" if abs(vertex["ny"]) > 0 else "flowing")
assert vertices == 588
report = {
    "source_commit": source, "client_cases": 12, "matching_vertices": vertices, "full_required_tests": 730,
    "contracts": [
        "Storage fluid port world/item rendering preserves its two-pixel extra inset, gas alpha and source milk cutout material.",
        "Collector, fish tank and drain fluid levels follow source values without the old forced minimum.",
        "Fish-tank fluid retains source height layering; drain gas uses full tank volume with opacity.",
        "Drain column sides use flowing sprites, top uses still sprite and bottom stays omitted.",
        "Reusing a drain render state after stopping the column clears its previous extent."
    ],
    "limits": [
        "Client-only visual fixtures seed block-entity render data to isolate renderer behavior from fluid transfer ticks.",
        "Fish/fire/item animation and external fish-render hooks are not covered by this fluid-only comparison.",
        "Full GameTests passed before formatting-only cleanup; final compilation and Checkstyle passed separately."
    ]
}
(root / "build/porting/fluid-holders-report.json").write_text(json.dumps(report, indent=2)+"\n", encoding="utf-8")
print(json.dumps(report, indent=2))
