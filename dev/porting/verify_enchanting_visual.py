"""Compare bounded mod GUI/rendering regions; preserve native world and Jade host layout."""
import argparse
import json
from pathlib import Path
import re

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser()
parser.add_argument("--source-log", default="client-enchanting-parity-source-5.log")
parser.add_argument("--target-log", default="client-enchanting-parity-target-7.log")
args = parser.parse_args()
DIRS = {"1.21": ROOT / "build/porting/reference-frost-1.21/run/frost-reference/screenshots",
        "26.1": ROOT / "run/port-validation/client/screenshots"}


def picture(version, scene):
    return np.asarray(Image.open(DIRS[version] / f"enchanting-parity-{version}-{scene}.png").convert("RGB"))


def ghost(version, scene):
    return picture(version, scene)[298:330, 478:510].astype(float)


def bounds(mask):
    y, x = np.nonzero(mask)
    return [int(x.min()), int(y.min()), int(x.max()), int(y.max())]


def dilate(mask):
    padded = np.pad(mask, 1)
    return np.logical_or.reduce([padded[y:y + mask.shape[0], x:x + mask.shape[1]] for y in range(3) for x in range(3)])


def book_mask(version, scene):
    r, g, b = picture(version, scene)[170:290, 540:740].astype(float).transpose(2, 0, 1)
    return (r > b * 1.1) & (g > r * 0.5) & (r > 70)


def progress(version):
    picture_data = picture(version, "jade-progress")[:160]
    r, g, b = picture_data.transpose(2, 0, 1)
    mask = (b > 200) & (r > 100) & (r < 210) & (g < 150)
    rows = np.flatnonzero(mask.sum(axis=1) > 100)
    first = int(rows.min())
    last = first
    while last + 1 in rows:
        last += 1
    columns = np.flatnonzero(mask[first:last + 1].any(axis=0))
    return picture_data[first:last + 1, columns.min()].astype(float), [int(columns.min()), first, int(columns.max()), last]


report = {"poses": {}, "ghosts": [], "glint": {}, "books": [], "fluids": {}}
for version, log in [("1.21", args.source_log), ("26.1", args.target_log)]:
    content = (ROOT / "build/porting" / log).read_text(encoding="utf-8", errors="replace")
    if f"PORT_ENCHANTING_PARITY_PASSED {version}: 17 scenes" not in content:
        raise RuntimeError(f"Incomplete parity run: {version}")
    poses = {}
    for name, time, opened in re.findall(r"PORT_ENCHANTING_POSE " + re.escape(version)
                                       + r" ([\w-]+): BookPose\[frame=\d+, time=([\d.]+), open=([\d.]+)", content):
        poses[name] = {"time": float(time), "open": float(opened)}
    report["poses"][version] = poses
    report["fluids"][version] = {variant: {"tint": tint, "layers": json.loads(layers)}
                                  for variant, tint, layers in re.findall(r"PORT_ENCHANTING_FLUID " + re.escape(version)
                                      + r" (\d+): ([a-f0-9]+) (\[[^\]\r\n]+\])", content)}
    report["glint"][version] = {}
    for item in ("pickaxe", "book"):
        ghost_delta = ghost(version, f"primer-{item}") - ghost(version, f"{item}-no-glint")
        solid_delta = ghost(version, f"{item}-solid") - ghost(version, f"{item}-solid-no-glint")
        significant = solid_delta > 4
        report["glint"][version][item] = {"ghost_delta_sum": float(ghost_delta[significant].sum()),
                                            "solid_delta_sum": float(solid_delta[significant].sum()),
                                            "relative_glint": float(ghost_delta[significant].sum() / solid_delta[significant].sum())}
for name in ("primer-pickaxe", "primer-book", "pickaxe-no-glint", "book-no-glint"):
    a, b = ghost("1.21", name), ghost("26.1", name)
    report["ghosts"].append({"scene": name, "mean_rgb_error": float(np.abs(a - b).mean()),
                             "max_rgb_error": float(np.abs(a - b).max())})
for name in ("world-open", "world-closed", "world-curse"):
    a, b = book_mask("1.21", name), book_mask("26.1", name)
    report["books"].append({"scene": name, "source_bounds": bounds(a), "target_bounds": bounds(b),
                            "mask_iou": float((a & b).sum() / (a | b).sum()),
                            "outside_one_pixel_tolerance": int((a & ~dilate(b)).sum() + (b & ~dilate(a)).sum())})
a, source_bounds = progress("1.21")
b, target_bounds = progress("26.1")
report["jade"] = {"source_bounds": source_bounds, "target_bounds": target_bounds,
                   "gradient_max_rgb_error": float(np.abs(a - b).max()) if a.shape == b.shape else None}
output = ROOT / "build/porting/enchanting-visual-comparison.json"
output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print(json.dumps(report, indent=2))
if report["poses"]["1.21"] != report["poses"]["26.1"] or len(report["poses"]["1.21"]) != 4:
    raise RuntimeError("Book poses are not matched")
for row in report["ghosts"]:
    if "no-glint" in row["scene"] and row["max_rgb_error"] > 3:
        raise RuntimeError(f"Base ghost rendering differs: {row['scene']}")
for row in report["books"]:
    if row["outside_one_pixel_tolerance"]:
        raise RuntimeError(f"Book contour differs: {row['scene']}")
if report["jade"]["gradient_max_rgb_error"] is None or report["jade"]["gradient_max_rgb_error"] > 1:
    raise RuntimeError("Jade gradient differs")

for item in ("pickaxe", "book"):
    expected = report["glint"]["1.21"][item]["relative_glint"]
    actual = report["glint"]["26.1"][item]["relative_glint"]
    if not np.isfinite(actual) or abs(actual - expected) > 0.02:
        raise RuntimeError(f"Ghost glint composition differs: {item}")

if len(report["fluids"]["1.21"]) != 8 or report["fluids"]["1.21"] != report["fluids"]["26.1"]:
    raise RuntimeError("Liquid enchantment tint/layer composition differs")
