"""Compare colored special-body previews, info panels and actual world atmosphere draws."""
from pathlib import Path
import json
import re

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
source = root / "build/porting/reference-frost-1.21/run/frost-reference/screenshots"
target = root / "run/port-validation/client/screenshots"
ui_scenes = ["plain-cyan", "plain-magenta", "complex-cyan", "complex-magenta", "no-temperature",
             "no-temperature-details", "no-atmosphere", "no-atmosphere-details"]
world_scenes = [name for name in ui_scenes if not name.endswith("-details")]
report = {"ui": {}, "world": {}, "limits": [
    "World comparisons retain native sky, fog and clouds; complete pixel identity is not asserted.",
    "Complex GUI model edge pixels can differ; maximum and mean differences are reported separately.",
    "Travel metadata is validated here, not actual dimension travel or arbitrary resource-pack model registration."
]}
for log, marker in [("client-special-celestial-source-2.log", "PORT_SPECIAL_CELESTIAL_CAPTURED"),
                    ("client-special-celestial-target-3.log", "PORT_SPECIAL_CELESTIAL_CAPTURED"),
                    ("client-special-world-source-1.log", "PORT_SPECIAL_WORLD_CAPTURED"),
                    ("client-special-world-target-1.log", "PORT_SPECIAL_WORLD_CAPTURED")]:
    text = (root / "build/porting" / log).read_text(encoding="utf8", errors="replace")
    assert marker in text and "BUILD SUCCESSFUL" in text, log
for name in ui_scenes:
    a = np.asarray(Image.open(source / f"special-celestial-1.21-{name}.png").convert("RGB"), dtype=int)
    b = np.asarray(Image.open(target / f"special-celestial-26.1-{name}.png").convert("RGB"), dtype=int)
    delta = np.abs(a[182:300, 492:610] - b[182:300, 492:610])
    info = np.abs(a[182:302, 610:788] - b[182:302, 610:788])
    row = {"preview_max_rgb": int(delta.max()), "preview_mean_rgb": float(delta.mean()), "info_max_rgb": int(info.max())}
    assert row["preview_mean_rgb"] < 1 and row["info_max_rgb"] <= 3, (name, row)
    if not name.startswith("complex"):
        assert row["preview_max_rgb"] <= 4, (name, row)
    report["ui"][name] = row
pattern = (r"PORT_SPECIAL_WORLD_GEOMETRY ([\w-]+): ring=([^,]+), center=([^,]+), body=([^,]+), beam=([^,]+), "
           r"camera=\(([^)]+)\), pitch=([^\r\n]+)")
geometry = {}
for version, log, folder in [("1.21", "client-special-world-source-1.log", source),
                              ("26.1", "client-special-world-target-1.log", target)]:
    text = (root / "build/porting" / log).read_text(encoding="utf8", errors="replace")
    rows = re.findall(pattern, text)
    assert [row[0] for row in rows] == world_scenes
    geometry[version] = {row[0]: [float(v) for v in row[1:5]] + [float(v) for v in row[5].split(",")] + [float(row[6])] for row in rows}
    if version == "26.1":
        assert text.count("PORT_SPECIAL_WORLD_RENDER_STATE_PASSED") == 6
        assert text.count("PORT_SPECIAL_WORLD_COLOR:") == 5
    for prefix in ("plain", "complex"):
        cyan = np.asarray(Image.open(folder / f"special-world-{version}-{prefix}-cyan.png").convert("RGB"), dtype=int)
        magenta = np.asarray(Image.open(folder / f"special-world-{version}-{prefix}-magenta.png").convert("RGB"), dtype=int)
        # Central body region excludes moving sky and most of the mechanical frame.
        delta = np.abs(cyan[325:480, 565:715] - magenta[325:480, 565:715])
        changed = int((delta.max(axis=2) > 10).sum())
        assert changed > 100, (version, prefix, "Custom atmosphere did not affect the image", changed)
        report["world"][version + "_" + prefix + "_color_changed_pixels"] = changed
for name in world_scenes:
    difference = float(np.max(np.abs(np.asarray(geometry["1.21"][name]) - geometry["26.1"][name])))
    assert difference < 0.00002, (name, difference)
    report["world"][name] = {"geometry": geometry["26.1"][name], "max_geometry_difference": difference}
output = root / "build/porting/special-celestial-visual-comparison.json"
output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf8")
print("Eight UI pairs and six world geometry/color pairs passed")
print(output)
