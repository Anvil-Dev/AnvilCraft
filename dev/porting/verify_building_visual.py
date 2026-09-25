"""Compare bounded hand geometry masks and source-normalized payload fits, not vanilla terrain pixels."""
from pathlib import Path
import argparse
import json
import re
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser()
parser.add_argument("--source-log", default="build/porting/client-rod-parity-source-final.log")
parser.add_argument("--target-log", default="build/porting/client-rod-parity-target-entity-fixed.log")
parser.add_argument("--output", default="build/porting/building-visual-comparison.json")
args = parser.parse_args()
NAMES = ["empty", "stone", "slab", "torch", "pole", "cfa", "disk", "off-stone", "off-pole", "off-cfa", "off-empty",
         "unpowered", "bed", "chest", "shulker", "head", "blueprint", "blueprint-rotated"]

def fits(path, version):
    text = path.read_text(encoding="utf-8", errors="replace")
    if "PORT_ROD_PARITY_PASSED " + version not in text:
        raise RuntimeError(f"Scene did not complete: {path}")
    result = {}
    for name, value in re.findall(r"PORT_ROD_PARITY " + re.escape(version) + r" ([\w-]+): ([^\r\n]+)", text):
        match = re.search(r"PayloadPose\[scale=([^,]+), heightScale=([^,]+), center=\(([^)]+)\)", value)
        result[name] = None if match is None else [float(match[1]), float(match[2]), *map(float, match[3].split(","))]
    if set(result) != set(NAMES):
        raise RuntimeError(f"Missing or extra scenarios: {set(NAMES) ^ set(result)}")
    return result

def bounds(mask):
    y, x = np.nonzero(mask)
    return None if len(x) == 0 else [int(x.min()), int(y.min()), int(x.max()), int(y.max())]

def orange(image, offhand):
    r, g, b = [image[:, :, index].astype(float) for index in range(3)]
    mask = (r > 100) & (r > g * 1.12) & (g > b * 1.45) & (r - b > 60)
    mask[:150] = False
    mask[640:] = False
    if offhand:
        mask[:, 560:] = False
    else:
        mask[:, :800] = False
    return mask

source_fits = fits(ROOT / args.source_log, "1.21")
target_fits = fits(ROOT / args.target_log, "26.1")
source_dir = ROOT / "build/porting/reference-frost-1.21/run/frost-reference/screenshots"
target_dir = ROOT / "run/port-validation/client/screenshots"
rows = []
for name in NAMES:
    source = np.asarray(Image.open(source_dir / f"rod-parity-1.21-{name}.png").convert("RGB"))
    target = np.asarray(Image.open(target_dir / f"rod-parity-26.1-{name}.png").convert("RGB"))
    if source.shape != target.shape:
        raise RuntimeError(f"Resolution mismatch in {name}")
    a, b = orange(source, name.startswith("off-")), orange(target, name.startswith("off-"))
    intersection = a & b
    union = a | b
    expected = source_fits[name]
    if expected is not None:
        expected = expected.copy()
        expected[2:] = [value - 0.5 for value in expected[2:]]
        # The source CFA item renderer adds one block in NONE; its fitting center compensates for that.
        if name in ("cfa", "off-cfa"):
            expected[3] -= 1
    actual = target_fits[name]
    error = None if expected is None or actual is None else max(abs(x - y) for x, y in zip(expected, actual))
    rows.append({"scenario": name, "orange_mask_iou": float(intersection.sum() / union.sum()) if union.any() else None,
                 "source_bounds": bounds(a), "target_bounds": bounds(b),
                 "mean_rgb_error_on_mask_overlap": float(np.abs(source.astype(float) - target)[intersection].mean()) if intersection.any() else None,
                 "source_effective_fit": expected, "target_fit": actual, "fit_max_error": error})
for row in rows:
    if row["source_bounds"] != row["target_bounds"]:
        raise RuntimeError(f"Hand geometry bounds differ: {row['scenario']}")
    if row["fit_max_error"] is not None and row["fit_max_error"] > 0.00001:
        raise RuntimeError(f"Payload fit differs: {row['scenario']}")
    if (row["source_effective_fit"] is None) != (row["target_fit"] is None):
        raise RuntimeError(f"Payload presence differs: {row['scenario']}")
report = {"scope": "18 matched fixed-camera scenes; frozen core angle, explicit GUI scale/FOV; parameter equality and orange hand-geometry masks only",
          "limits": "Not a pixel-identity or complete-rendering claim. Vanilla sky/cloud/lighting differences remain; animated texture phase may differ. Projection and non-orange payload geometry require screenshot inspection.",
          "rows": rows}
(ROOT / args.output).write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
for row in rows:
    print(row["scenario"], "IoU", round(row["orange_mask_iou"], 6), "fit error", row["fit_max_error"])
