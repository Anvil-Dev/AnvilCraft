"""Compare real large-cauldron render submissions from the source and native clients."""
from pathlib import Path
import json
import math
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
source = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=reference, text=True).strip()
for name in ["client-large-cauldron-render-4.log", "client-large-cauldron-render-reference-1.log"]:
    text = (root / "build/porting" / name).read_text(encoding="utf-8", errors="replace")
    assert "PORT_LARGE_CAULDRON_RENDER_PASSED: 8 cases" in text and "BUILD SUCCESSFUL" in text, name
native = json.loads((root / "run/port-validation/client/large-cauldron-26.1.json").read_text(encoding="utf-8"))
expected = json.loads((reference / "run/mun-reference/large-cauldron-1.21.json").read_text(encoding="utf-8"))
assert native.keys() == expected.keys() and len(native) == 8
counts = {
    "dry-items": {"items": 960},
    "water-items": {"items": 960, "fluid": 1056},
    "layers": {"fluid": 528},
    "milk": {"fluid": 1056},
    "oil-fire": {"fluid": 1056, "fire": 16},
    "half-fire": {"fluid": 528, "fire": 16},
    "empty-fire": {"fire": 16},
    "hidden-fire": {"fluid": 1056},
}
errors = {}
vertices = 0
fluid_vertices = 0
for name, ref in expected.items():
    actual = native[name]
    assert {k: len(v) for k, v in actual["geometry"].items()} == counts[name], name
    assert list(actual["geometry"]) == list(ref["geometry"]), (name, "draw order")
    for kind, points in actual["geometry"].items():
        remaining = list(ref["geometry"][kind])
        assert len(points) == len(remaining)
        maximum = 0
        for point in points:
            index = min(range(len(remaining)), key=lambda i: math.dist(point, remaining[i]))
            distance = math.dist(point, remaining.pop(index))
            maximum = max(maximum, distance)
            assert distance <= 1e-6, (name, kind, distance)
            vertices += 1
        errors[name + ":" + kind] = maximum
    assert len(actual["fluids"]) == len(ref["fluids"])
    for a, b in zip(actual["fluids"], ref["fluids"]):
        assert a["material"] == b["material"]
        assert len(a["vertices"]) == len(b["vertices"])
        for vertex, wanted in zip(a["vertices"], b["vertices"]):
            assert all(abs(vertex[k]-wanted[k]) <= (0 if k in ["color", "light"] else 1e-6) for k in vertex), (name, vertex, wanted)
            fluid_vertices += 1
assert vertices == 7248 and fluid_vertices == 5280
assert all(batch["material"] == "cutout" for batch in native["milk"]["fluids"])
report = {
    "source_commit": source, "client_cases": 8, "matching_vertices": vertices,
    "fluid_color_light_normal_vertices": fluid_vertices, "maximum_vertex_distance": errors,
    "contracts": [
        "All eight input slots and 32 output slots render at source positions, using world-aware models and block-position seeds.",
        "Each fluid layer preserves geometry, color, light, normals, bottom faces and source material choice; milk is cutout.",
        "Full, partial and empty fire surfaces preserve the source model and unshaded full brightness.",
        "Fire veto and ordered after-render hooks work with immutable extracted extension data.",
        "State reuse cannot accumulate contents or retain main-part rendering on a child part."
    ],
    "limits": [
        "The scenes seed client-only renderer contents and explicitly force ignition for the otherwise unreachable empty-fluid boundary.",
        "Actual gameplay transfer, ignition and recipe event contracts are outside this render-only node.",
        "Native vanilla terrain, lighting and texture animation timing are retained; screenshots are inspected but not asserted pixel-identical."
    ]
}
(root / "build/porting/large-cauldron-render-report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print(json.dumps(report, indent=2))
