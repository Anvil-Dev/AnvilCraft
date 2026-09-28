"""Compare real fish tank renderer submissions from both clients."""
from pathlib import Path
import json
import math
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
source = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=reference, text=True).strip()
for name in ["client-fish-contents-4.log", "client-fish-contents-reference-2.log"]:
    log = (root / "build/porting" / name).read_text(encoding="utf-8", errors="replace")
    assert "PORT_FISH_CONTENTS_PASSED: 7 cases" in log and "BUILD SUCCESSFUL" in log, name
full = (root / "build/porting/tests-fish-contents-all.log").read_text(encoding="utf-8", errors="replace")
assert "All 730 required tests passed" in full
assert "BUILD SUCCESSFUL" in (root / "build/porting/client-fish-contents-4.log").read_text(encoding="utf-8", errors="replace")
native = json.loads((root / "run/port-validation/client/fish-contents-26.1.json").read_text(encoding="utf-8"))
expected = json.loads((reference / "run/mun-reference/fish-contents-1.21.json").read_text(encoding="utf-8"))
assert native.keys() == expected.keys() and len(native) == 7
counts = {
    "dry-items": {"items": 120},
    "wet-items": {"items": 120, "fluid": 24},
    "fish": {"fish": 480, "fluid": 24},
    "mixed": {"items": 120, "fish": 480, "fluid": 24},
    "empty-fire": {"fire": 16},
    "wet-fire": {"fluid": 24, "fire": 16},
    "hidden-fire": {"fluid": 24},
}
vertices = 0
errors = {}
for name, ref in expected.items():
    actual = native[name]
    assert actual["seed"] == ref["seed"] and actual["ticks"] == ref["ticks"], name
    assert {k: len(v) for k, v in actual["geometry"].items()} == counts[name], name
    assert list(actual["geometry"]) == list(ref["geometry"]), (name, "submission order")
    for kind, points in actual["geometry"].items():
        unmatched = list(ref["geometry"][kind])
        assert len(points) == len(unmatched), (name, kind)
        maximum = 0
        # Block/item quad ordering changed; compare their vertex multisets. Fish model vertex order is unchanged.
        for point in points:
            index = 0 if kind == "fish" else min(range(len(unmatched)), key=lambda i: math.dist(point, unmatched[i]))
            distance = math.dist(point, unmatched.pop(index))
            maximum = max(maximum, distance)
            # Native tropical-fish body/tail animation retains 26.1 Mth double indexing.
            assert distance <= (0.001 if kind == "fish" else 1e-6), (name, kind, distance)
            vertices += 1
        errors[name + ":" + kind] = maximum
assert vertices == 1472
report = {
    "source_commit": source, "client_cases": 7, "compared_vertices": vertices,
    "full_required_tests": 730, "maximum_vertex_distance": errors,
    "contracts": [
        "Inventory-derived item seeds, source bob timing and source fish orbit math survive extract/submit separation.",
        "The original fire model, fill-relative height, full-bright unshaded material and extended culling bounds are restored.",
        "Render submissions preserve items/fish/fluid/fire/hooks order and fish world lighting.",
        "External hooks can veto fire and submit extracted extension data in registration order.",
        "Reused render states clear removed items/fish and level-less tanks use the client level."
    ],
    "limits": [
        "Vanilla 26.1 tropical-fish body/tail animation remains native; its double-precision trig lookup accounts for sub-0.001-block vertex differences.",
        "Screenshots retain native vanilla terrain, lighting and texture animation timing; they are not asserted pixel-identical.",
        "The full GameTest run passed before final client-render-only corrections; final compile, Checkstyle and both real client scenes passed."
    ]
}
(root / "build/porting/fish-contents-report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print(json.dumps(report, indent=2))
