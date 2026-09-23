"""Compare scanner preview geometry in source/target screenshots, excluding vanilla world rendering."""
from pathlib import Path
import json
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "build/porting/reference-frost-1.21/run/frost-reference/screenshots"
TARGET = ROOT / "run/port-validation/client/screenshots"
NAMES = ["cube", "flat", "x-line", "y-line", "z-line"]


def mask(path):
    pixels = np.asarray(Image.open(path).convert("RGB"))[196:368, 658:878]
    return pixels.max(axis=2) > 65


def bounds(value):
    y, x = np.nonzero(value)
    if not len(x):
        raise RuntimeError("Missing preview geometry")
    return [int(x.min()), int(y.min()), int(x.max()), int(y.max())]


def dilate(value):
    padded = np.pad(value, 1)
    return np.logical_or.reduce([padded[y:y + value.shape[0], x:x + value.shape[1]]
                                 for y in range(3) for x in range(3)])


for version, log in [("1.21", "client-scanner-parity-source.log"), ("26.1", "client-scanner-parity-target-final.log")]:
    if f"PORT_SCANNER_PARITY_PASSED {version}: 14 scenes" not in (ROOT / "build/porting" / log).read_text(
            encoding="utf-8", errors="replace"):
        raise RuntimeError(f"Incomplete client scene: {version}")

rows = []
for name in NAMES:
    for rotation in range(2):
        key = f"{name}-{rotation}"
        a = mask(SOURCE / f"scanner-parity-1.21-{key}.png")
        b = mask(TARGET / f"scanner-parity-26.1-{key}.png")
        rows.append({"scene": key, "source_bounds": bounds(a), "target_bounds": bounds(b),
                     "geometry_iou": float((a & b).sum() / (a | b).sum()),
                     "pixels_beyond_one_pixel_tolerance": int((a & ~dilate(b)).sum() + (b & ~dilate(a)).sum())})
report = ROOT / "build/porting/scanner-visual-comparison.json"
report.write_text(json.dumps(rows, indent=2) + "\n", encoding="utf-8")
print(report)
for row in rows:
    print(row)
    if row["source_bounds"] != row["target_bounds"] or row["pixels_beyond_one_pixel_tolerance"] != 0:
        raise RuntimeError(f"Scanner geometry differs: {row['scene']}")
