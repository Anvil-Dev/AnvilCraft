"""Compare the bounded evolution panel and independently exercised countdown controls."""
import json
from pathlib import Path
import re
import subprocess

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SCENES = ["running-top", "paused-top", "paused-bottom", "long-phase", "supernova-bottom", "collapse-bottom",
          "chinese-top", "chinese-bottom"]
RUNS = {
    "source": ("1.21", "build/porting/client-stellar-ui-source-2.log",
               "build/porting/reference-frost-1.21/run/frost-reference/screenshots"),
    "target": ("26.1", "build/porting/client-stellar-ui-target-2.log", "run/port-validation/client/screenshots"),
}
report = {"runs": {}, "panels": {}, "limits": [
    "The ROI covers the 89x60 GUI-unit evolution panel only; celestial previews, ghost slots, other controls and JEI are excluded.",
    "Screenshots fix the render-time clock and cached countdown; separate real screen.tick calls verify pause/run behavior before normalization.",
    "The fixture opens the real screen locally; multiplayer menu opening and long-running synchronization are not certified by this comparison.",
]}
data = {}
images = {}
pattern = r"PORT_STELLAR_UI ([\w-]+): scroll=(\d+), remaining=(\d+), phase=([^,]+), total=([^\r\n]+)"
for name, (version, log, folder) in RUNS.items():
    text = (ROOT / log).read_text(encoding="utf-8", errors="replace")
    assert "PORT_STELLAR_UI_CAPTURED" in text and "BUILD SUCCESSFUL" in text, log
    rows = re.findall(pattern, text)
    assert [row[0] for row in rows] == SCENES, rows
    countdowns = re.findall(r"PORT_STELLAR_UI_COUNTDOWN paused=(\w+): before=(\d+), after=(\d+)", text)
    assert len(countdowns) == 8
    for paused, before, after in countdowns:
        assert int(before) - int(after) == (0 if paused == "true" else 8), (name, paused, before, after)
    data[name] = (rows, countdowns)
    images[name] = {}
    for scene in SCENES:
        pixels = np.asarray(Image.open(ROOT / folder / f"stellar-ui-{version}-{scene}.png").convert("RGB"), dtype=np.int16)
        assert pixels.shape == (720, 1280, 3)
        images[name][scene] = pixels[182:302, 610:788]
    report["runs"][name] = {"log": log, "screenshots": folder, "samples": rows, "countdowns": countdowns}
assert data["source"] == data["target"], "Phase, countdown or scroll values differ"

for scene in SCENES:
    source = images["source"][scene]
    target = images["target"][scene]
    source_text = source.min(axis=2) > 200
    target_text = target.min(axis=2) > 200
    assert int(source_text.sum()) > 200, (scene, "Missing panel text")
    assert np.array_equal(source_text, target_text), (scene, "Text layout differs")
    difference = np.abs(source - target)
    maximum = int(difference.max())
    assert maximum <= 3, (scene, maximum)
    report["panels"][scene] = {"text_mask_equal": True, "maximum_rgb_channel_difference": maximum,
                               "mean_absolute_rgb_difference": float(difference.mean())}

for language, directory in [("en_us", "src/generated/resources"), ("zh_cn", "src/main/resources")]:
    relative = f"{directory}/assets/anvilcraft/lang/{language}.json"
    native = json.loads((ROOT / relative).read_text(encoding="utf-8"))
    reference = json.loads(subprocess.check_output(["git", "show", f"dev/1.21/1.6:{relative}"], cwd=ROOT))
    entries = lambda table: {key: value for key, value in table.items() if key.startswith("screen.anvilcraft.cfa.evolution.")}
    assert entries(native) == entries(reference), language
    report[f"matching_{language}_evolution_entries"] = len(entries(native))

output = ROOT / "build/porting/stellar-ui-visual-comparison.json"
output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print("8 evolution panels passed: identical text masks; maximum RGB channel difference 3/255")
print("Pause/run countdowns, scrolling, values and source language entries matched")
print(output)
