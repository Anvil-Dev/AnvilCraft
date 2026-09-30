"""Compare fixed handbook assets, model silhouettes and multipart preview states."""
from pathlib import Path
import json

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
native = root / "run/port-validation/client"
source = root / "build/porting/reference-mun-1.21/run/mun-reference"
a = json.loads((native / "handbook-recipes-26.1.json").read_text(encoding="utf-8"))
b = json.loads((source / "handbook-recipes-1.21.json").read_text(encoding="utf-8"))
assert a["helpers"] == b["helpers"], "Model-holder mappings or block metrics differ"
assert a["pages"] == b["pages"] == 6
keys = [key for key in a if key.startswith("anvilcraft:")]
assert len(keys) == 25 and all(key in b for key in keys)
for key in keys:
    assert a[key]["width"] > 0 and a[key]["height"] > 0, key

x = np.asarray(Image.open(native / "screenshots/handbook-recipes-26.1-5.png").convert("RGB"), dtype=np.int16)
y = np.asarray(Image.open(source / "screenshots/handbook-recipes-1.21-5.png").convert("RGB"), dtype=np.int16)
assert x.shape == y.shape == (720, 1280, 3)
regions = {
    "arrows": (60, 25, 650, 110),
    "explosion": (680, 20, 780, 120),
    "anvil_trail": (900, 30, 1010, 110),
    "slots_and_items": (60, 450, 400, 580),
    "stacked_blocks": (500, 430, 630, 580),
}
for index, name in enumerate(["stone", "anvil", "scaffolding", "cauldron", "giant_anvil", "amplifier_north",
                              "amplifier_east", "amplifier_south", "amplifier_west"]):
    regions[name] = (10 + index * 140, 210, 150 + index * 140, 410)
report = {"recipes": len(keys), "pages_per_client": a["pages"], "model_holder_states": sum(len(value) for value in a["helpers"].values() if isinstance(value, dict)), "regions": {}}
for name, (left, top, right, bottom) in regions.items():
    first = x[top:bottom, left:right]
    second = y[top:bottom, left:right]
    first_mask = np.any(first != x[0, 0], axis=2)
    second_mask = np.any(second != y[0, 0], axis=2)
    union = first_mask | second_mask
    assert union.any(), name
    intersection = first_mask & second_mask
    metrics = {
        "native_pixels": int(first_mask.sum()),
        "source_pixels": int(second_mask.sum()),
        "silhouette_iou": float(intersection.sum() / union.sum()),
        "mean_rgb_error": float(np.abs(first - second)[union].mean()),
        "different_pixels": int(np.any(first != second, axis=2).sum()),
    }
    report["regions"][name] = metrics
    assert np.array_equal(first_mask, second_mask), (name, metrics)
    if name in ("arrows", "explosion"):
        assert np.array_equal(first, second), (name, metrics)
report["limits"] = [
    "3D lighting, texture sampling and item colors retain the native renderer; RGB errors are reported, not hidden.",
    "The source helper background is placed behind negative-depth geometry; the original flat z=0 fixture clipped model faces.",
    "Recipe-gallery ingredients can cycle independently; exact screenshot assertions use the fixed helper page.",
]
output = root / "build/porting/handbook-visual-report.json"
output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print(json.dumps(report, indent=2))
