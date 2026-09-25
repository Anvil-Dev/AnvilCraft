"""Validate extracted structure geometry and inspect fixed source/native world images."""
from pathlib import Path
import json
import re

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
scenes = ["brown-sphere", "brown-rotated", "red-no-amplifier", "red-amplifier", "red-small-sphere", "ordinary-large"]
pattern = (r"PORT_DYSON_GEOMETRY ([\w-]+): ring=([^,]+), center=([^,]+), body=([^,]+), beam=([^,]+), "
           r"camera=\(([^)]+)\), pitch=([^\r\n]+)")
runs = {"source": ("1.21", "client-dyson-source-4.log", "build/porting/reference-frost-1.21/run/frost-reference/screenshots"),
        "target": ("26.1", "client-dyson-target-3.log", "run/port-validation/client/screenshots")}
report = {"runs": {}, "scenes": {}, "limits": [
    "Native clouds, fog and terrain rendering retained; full-image pixel identity is not asserted.",
    "Fixed geometry and sampled rotations do not certify continuous transformation animation or all resource packs.",
    "Source world renderer hides stellar bodies without a physical amplifier, including the special red dwarf; this behavior is preserved."
]}
values = {}
images = {}
for name, (version, log, folder) in runs.items():
    text = (root / "build/porting" / log).read_text(encoding="utf8", errors="replace")
    assert "PORT_DYSON_CAPTURED" in text and "BUILD SUCCESSFUL" in text, log
    rows = re.findall(pattern, text)
    assert [row[0] for row in rows] == scenes, rows
    values[name] = {row[0]: [float(v) for v in row[1:5]] + [float(v) for v in row[5].split(",")] + [float(row[6])] for row in rows}
    if name == "target":
        assert text.count("PORT_DYSON_RENDER_STATE_PASSED") == 6
    report["runs"][name] = {"log": log, "images": folder}
    images[name] = {scene: np.asarray(Image.open(root / folder / f"dyson-{version}-{scene}.png").convert("RGB"),
                                      dtype=np.float32) for scene in scenes}
for scene in scenes:
    difference = float(np.max(np.abs(np.asarray(values["source"][scene]) - values["target"][scene])))
    assert difference < 0.00002, (scene, difference)
    row = {"source_geometry_camera": values["source"][scene], "target_geometry_camera": values["target"][scene],
           "max_difference": difference}
    for name in runs:
        pixels = images[name][scene]
        assert pixels.shape == (720, 1280, 3)
        red, green, blue = pixels[200:550, 350:930].transpose(2, 0, 1)
        orange = (red > 80) & (red > green * 1.1) & (green > blue * 1.3)
        y, x = np.where(orange)
        assert len(x) > 200, (scene, name, "Missing visible structure")
        row[name + "_orange_bounds"] = [int(x.min() + 350), int(y.min() + 200), int(x.max() + 350), int(y.max() + 200)]
        row[name + "_orange_pixels"] = len(x)
    report["scenes"][scene] = row
output = root / "build/porting/dyson-visual-comparison.json"
output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf8")
print("Six source/native geometry, camera and visible-structure checks passed")
print(output)
