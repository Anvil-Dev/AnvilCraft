"""Check fixed CFA comparison fixtures and report hand-frame geometry, not pixel parity."""
import json
from pathlib import Path
import re

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SCENES = ("gallery", "main-hand", "off-hand", "head", "reloaded", "vanilla-atmosphere")
RUNS = {
    "source": ("1.21", "build/porting/client-cfa-item-source-4.log",
               "build/porting/reference-frost-1.21/run/frost-reference/screenshots",
               "PORT_CFA_ITEM_REFERENCE_CAPTURED"),
    "target": ("26.1", "build/porting/client-cfa-atmosphere-4.log",
               "run/port-validation/client/screenshots", "PORT_CFA_ITEM_RENDER_PASSED"),
}
images = {}
report = {"runs": {}, "hand_frames": {}, "limitations": [
    "Frame masks only measure orange model placement; textures, lighting and atmosphere are not certified by this metric.",
    "Native GUI item atlas resolution and atmosphere camera coordinates differ from 1.21 immediate rendering.",
    "Latest stellar emission/evolution, world/inside atmosphere, Iris and dynamic/performance coverage remain pending.",
]}
for name, (version, log, directory, marker) in RUNS.items():
    text = (ROOT / log).read_text(encoding="utf-8", errors="replace")
    assert marker in text and "BUILD SUCCESSFUL" in text, log
    views = re.findall(r"PORT_CFA_VIEW ([\w-]+): position=\(([^)]+)\), flying=(\w+)", text)
    assert [view[0] for view in views] == list(SCENES), views
    assert all(view[1:] == ("8.5, 85.0, 12.5", "false") for view in views), views
    images[name] = {}
    for scene in SCENES:
        path = ROOT / directory / f"cfa-item-{version}-{scene}.png"
        array = np.asarray(Image.open(path).convert("RGB"), dtype=np.float32)
        assert array.shape == (720, 1280, 3), path
        images[name][scene] = array
    report["runs"][name] = {"log": log, "screenshots": directory, "scenes": list(SCENES)}

for scene, box in (("main-hand", (700, 150, 1170, 605)), ("off-hand", (100, 150, 580, 605))):
    masks = []
    bounds = []
    left, top, right, bottom = box
    for name in RUNS:
        red, green, blue = images[name][scene][top:bottom, left:right].transpose(2, 0, 1)
        mask = (red > 70) & (red > green * 1.08) & (green > blue * 1.25)
        y, x = np.where(mask)
        assert len(x) > 1000, (name, scene)
        masks.append(mask)
        bounds.append([int(x.min() + left), int(y.min() + top), int(x.max() + left), int(y.max() + top)])
    difference = int(np.max(np.abs(np.asarray(bounds[0]) - np.asarray(bounds[1]))))
    assert difference <= 2, (scene, bounds)
    report["hand_frames"][scene] = {
        "source_bounds": bounds[0], "target_bounds": bounds[1], "maximum_bound_difference": difference,
        "orange_mask_iou_diagnostic": float(np.count_nonzero(masks[0] & masks[1]) / np.count_nonzero(masks[0] | masks[1])),
    }

output = ROOT / "build/porting/cfa-item-visual-comparison.json"
output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print(json.dumps(report["hand_frames"], indent=2))
print(output)
