"""Compare large tank renderer submissions and item-gallery geometry against 1.21."""
from pathlib import Path
import json
import subprocess

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
source = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=reference, text=True).strip()
for name in ["client-large-tank-layers-2.log", "client-large-tank-layers-reference-2.log"]:
    text = (root / "build/porting" / name).read_text(encoding="utf-8", errors="replace")
    assert "PORT_LARGE_TANK_LAYERS_PASSED: 13 cases" in text and "BUILD SUCCESSFUL" in text, name
full = (root / "build/porting/tests-large-tank-layers-final.log").read_text(encoding="utf-8", errors="replace")
assert "All 727 required tests passed" in full and "BUILD SUCCESSFUL" in full
native = json.loads((root / "run/port-validation/client/large-tank-layers-26.1.json").read_text(encoding="utf-8"))
ref = json.loads((reference / "run/mun-reference/large-tank-layers-1.21.json").read_text(encoding="utf-8"))
assert native["geometry"].keys() == ref["geometry"].keys() and len(ref["geometry"]) == 9
vertices = 0
for name, modes in ref["geometry"].items():
    for mode, batches in modes.items():
        actual = native["geometry"][name][mode]
        assert len(actual) == len(batches), (name, mode)
        for a, b in zip(actual, batches):
            assert a["type"] == b["type"] and len(a["vertices"]) == len(b["vertices"]), (name, mode)
            for vertex, expected in zip(a["vertices"], b["vertices"]):
                vertices += 1
                assert vertex["color"] == expected["color"] and vertex["light"] == expected["light"], (name, mode)
                assert all(abs(vertex[k]-expected[k]) <= 1e-6 for k in ["x", "y", "z", "nx", "ny", "nz"]), (name, mode)
assert vertices == 4320
images = []
a = np.asarray(Image.open(root / "run/port-validation/client/screenshots/large-tank-layers-26.1-items.png").convert("RGB")).astype(float)
b = np.asarray(Image.open(reference / "run/mun-reference/screenshots/large-tank-layers-1.21-items.png").convert("RGB")).astype(float)
for index in range(9):
    x, y = 360+index%3*200, 80+index//3*192
    ac, bc = a[y:y+150, x:x+150], b[y:y+150, x:x+150]
    am, bm = np.any(ac != [32, 37, 48], axis=2), np.any(bc != [32, 37, 48], axis=2)
    iou = float((am & bm).sum()/(am | bm).sum())
    error = float(abs(ac-bc)[am | bm].mean())
    assert iou > 0.99, (index, iou)
    images.append({"index": index, "silhouette_iou": iou, "mean_rgb_difference": error})
report = {
    "source_commit": source,
    "client_scenes": 13,
    "world_and_item_cases": 18,
    "matching_vertices": vertices,
    "full_required_tests": 727,
    "gallery": images,
    "contracts": [
        "World and item forms share sorting, normalization and layer construction; equal amounts sort by fluid registry ID.",
        "World gas occupies the full interior with share-based alpha; item gas retains source layer geometry.",
        "Enhanced totals above the single-fluid threshold preserve source normalization; milk uses cutout material.",
        "Current flat fluid lists and legacy nested item display data decode with registry context and their infinity flags.",
        "Actual renderer submissions match source positions, normals, RGBA, light and material type; filled items render in a real GUI."
    ],
    "limits": [
        "Vertex comparison uses fixed sky light to isolate mod geometry and color from native lightmaps.",
        "Texture UV packing and animation phase are not asserted numerically; paired world/item images were inspected.",
        "Gallery RGB differences include animated textures and native renderer shading; silhouettes exceed 0.99 IoU.",
        "Single-tank/base-holder gas opacity and minimum-fill behavior still need a separate source audit."
    ]
}
(root / "build/porting/large-tank-layers-report.json").write_text(json.dumps(report, indent=2)+"\n", encoding="utf-8")
print(json.dumps(report, indent=2))
