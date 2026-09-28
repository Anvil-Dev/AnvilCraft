"""Compare ordinary/creative tank and minecart render submissions with source."""
from pathlib import Path
import json
import subprocess
import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
source = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=reference, text=True).strip()
for file in ["client-single-tank-render-1.log", "client-single-tank-reference-1.log"]:
    text = (root / "build/porting" / file).read_text(encoding="utf-8", errors="replace")
    assert "PORT_SINGLE_TANK_RENDER_PASSED: 9 cases" in text and "BUILD SUCCESSFUL" in text, file
for file in ["data-single-tank-render-2.log", "tests-single-tank-render-final-2.log", "client-large-tank-single-helper-regression.log"]:
    text = (root / "build/porting" / file).read_text(encoding="utf-8", errors="replace")
    assert "BUILD SUCCESSFUL" in text, file
full = (root / "build/porting/tests-single-tank-render-final-2.log").read_text(encoding="utf-8", errors="replace")
assert "All 730 required tests passed" in full

def compare(native, expected):
    assert native.keys() == expected.keys()
    count = 0
    for name, modes in expected.items():
        for mode, batches in modes.items():
            actual = native[name][mode]
            assert len(actual) == len(batches), (name, mode)
            for a, b in zip(actual, batches):
                assert a["type"] == b["type"] and len(a["vertices"]) == len(b["vertices"]), (name, mode)
                for vertex, wanted in zip(a["vertices"], b["vertices"]):
                    assert all(abs(vertex[k]-wanted[k]) <= (0 if k in ["color", "light"] else 1e-6) for k in vertex), (name, mode)
                    count += 1
    return count

native = json.loads((root / "run/port-validation/client/single-tank-render-26.1.json").read_text())
expected = json.loads((reference / "run/mun-reference/single-tank-render-1.21.json").read_text())
assert len(native) == 9
count = compare(native, expected)
assert count == 576
large = json.loads((root / "run/port-validation/client/large-tank-layers-26.1.json").read_text())
large_ref = json.loads((reference / "run/mun-reference/large-tank-layers-1.21.json").read_text())
assert compare(large["geometry"], large_ref["geometry"]) == 4320
model = json.loads((root / "src/generated/resources/assets/anvilcraft/items/creative_fluid_tank.json").read_text())
assert model["model"]["model"]["type"] == "anvilcraft:creative_fluid_tank"
a = np.asarray(Image.open(root / "run/port-validation/client/screenshots/single-tank-26.1-items.png").convert("RGB")).astype(float)
b = np.asarray(Image.open(reference / "run/mun-reference/screenshots/single-tank-1.21-items.png").convert("RGB")).astype(float)
images = []
for i in range(9):
    x, y = 360+i%3*200, 80+i//3*192
    ac, bc = a[y:y+150, x:x+150], b[y:y+150, x:x+150]
    am, bm = np.any(ac != [32, 37, 48], axis=2), np.any(bc != [32, 37, 48], axis=2)
    iou = float((am & bm).sum()/(am | bm).sum())
    assert iou > 0.99, (i, iou)
    images.append({"index": i, "silhouette_iou": iou, "mean_rgb_difference": float(abs(ac-bc)[am | bm].mean())})
report = {
    "source_commit": source, "client_cases": 9, "render_paths": 27, "matching_vertices": count,
    "large_tank_regression_vertices": 4320, "full_required_tests": 730, "gallery": images,
    "contracts": [
        "Ordinary world/item tanks and minecart contents use exact source fill without a forced minimum and clamp over-capacity fill.",
        "Gas fills the whole interior with amount-based alpha, and milk uses cutout material.",
        "Creative world and item tanks render configured fluid at full volume; the item uses a registered special renderer.",
        "Creative fluid components take precedence over current serialized handler data and legacy infinityFluid display data.",
        "Other base-holder users keep their original minimum-fill policy; large layered tank submissions remain source-equivalent."
    ],
    "limits": [
        "Vertex tests isolate contents at fixed sky light; vanilla entity transforms, lightmaps and texture animation remain native.",
        "The first full run failed one lunar-light assertion and one Windows temporary-file move; a full retry passed all 730 tests without changing either failing subsystem."
    ]
}
(root / "build/porting/single-tank-render-report.json").write_text(json.dumps(report, indent=2)+"\n", encoding="utf-8")
print(json.dumps(report, indent=2))
