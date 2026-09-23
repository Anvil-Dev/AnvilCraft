"""Measure matched processing-table captures; pixel equality is not an acceptance claim."""
from pathlib import Path
import json
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "build/porting/reference-frost-1.21/run/frost-reference/screenshots"
TARGET = ROOT / "run/port-validation/client/screenshots"
REGIONS = {
    "idle": (405, 345, 880, 455),
    "active": (405, 345, 880, 455),
    "scattered": (405, 345, 880, 455),
    "blocked": (405, 310, 880, 455),
    "door-active": (595, 355, 685, 445),
    "door-idle": (595, 355, 685, 445),
    "wheel-active": (550, 235, 730, 425),
    "wheel-idle": (550, 235, 730, 425),
}


def pixels(directory, version, name, box):
    return np.array(Image.open(directory / f"processing-{version}-{name}.png").convert("RGB").crop(box)).astype(int)


report = {"crops": {}, "motion": {}}
for name, box in REGIONS.items():
    difference = np.abs(pixels(SOURCE, "1.21", name, box) - pixels(TARGET, "26.1", name, box))
    report["crops"][name] = {
        "box": box,
        "mean_rgb_error": round(float(difference.mean()), 3),
        "p95_channel_error": int(np.percentile(difference, 95)),
    }
for part in ("door", "wheel"):
    masks = []
    entry = {}
    for version, directory in (("1.21", SOURCE), ("26.1", TARGET)):
        box = REGIONS[part + "-active"]
        delta = np.abs(pixels(directory, version, part + "-active", box)
                       - pixels(directory, version, part + "-idle", box))
        mask = delta.max(axis=2) > 8
        masks.append(mask)
        y, x = np.where(mask)
        entry[version] = {"changed_pixels": len(x), "bounds": None if not len(x)
                          else [int(x.min()), int(y.min()), int(x.max()), int(y.max())]}
    entry["mask_iou"] = round(float(np.logical_and(*masks).sum() / max(1, np.logical_or(*masks).sum())), 4)
    report["motion"][part] = entry
output = ROOT / "build/porting/processing-visual-comparison.json"
output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print(json.dumps(report, indent=2))
