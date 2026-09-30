"""Compare actual renderer output for all gas-container cases on both versions."""
from pathlib import Path
import json

import numpy as np

root = Path(__file__).resolve().parents[2]
native = json.loads((root / "run/port-validation/client/gas-container-26.1.json").read_text(encoding="utf-8"))
source = json.loads((root / "build/porting/reference-mun-1.21/run/mun-reference/gas-container-1.21.json").read_text(encoding="utf-8"))
assert native.keys() == source.keys() and len(native) == 29
report = {}
total = 0
for name in native:
    first, second = native[name], source[name]
    if isinstance(first, list):
        assert [batch["type"] for batch in first] == [batch["type"] for batch in second], name
        a = [vertex for batch in first for vertex in batch["vertices"]]
        b = [vertex for batch in second for vertex in batch["vertices"]]
    else:
        assert first["empty_vertices"] == second["empty_vertices"] == 0, name
        a, b = first["vertices"], second["vertices"]
    assert len(a) == len(b) and a, (name, len(a), len(b))
    error = 0.0
    for field in ("x", "y", "z", "nx", "ny", "nz"):
        delta = float(np.max(np.abs(np.array([point[field] for point in a]) - np.array([point[field] for point in b]))))
        assert delta <= 2e-6, (name, field, delta)
        error = max(error, delta)
    for field in ("color", "light"):
        assert [point[field] for point in a] == [point[field] for point in b], (name, field)
    report[name] = {"vertices": len(a), "max_geometry_error": error}
    total += len(a)
assert native["interface-single"] == native["reloaded-interface"]
assert source["interface-single"] == source["reloaded-interface"]
result = {"cases": len(report), "vertices": total, "results": report,
          "limits": "World render geometry, per-vertex color/alpha, normals and light are compared. Native vanilla background shading remains unchanged."}
(root / "build/porting/gas-container-comparison.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
print(f"PASS: {len(report)} cases, {total} vertices; matching geometry, RGBA, normals, light, empty-state cleanup and reload")
