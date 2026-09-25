"""Measure fixed-frame CFA previews and depth-composited slot ghosts against local 1.21."""
import json
from pathlib import Path
import re

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SCENES = ["star", "rocky", "atmosphere", "white-dwarf", "neutron-slow", "neutron-fast", "black-hole", "flesh", "ghost-no-foil"]
RUNS = {
    "source": ("1.21", "build/porting/client-cfa-preview-source-3.log",
               "build/porting/reference-frost-1.21/run/frost-reference/screenshots"),
    "target": ("26.1", "build/porting/client-cfa-preview-target-10.log", "run/port-validation/client/screenshots"),
}
report = {"runs": {}, "previews": {}, "ghost_slots": {}, "limits": [
    "Preview RGB samples are diagnostics, not a whole-image pixel-parity claim.",
    "Neutron-jet low-alpha tails, black-hole fine texture/cutout edges and some atmosphere pixels still differ.",
    "The preview ROI is 59x59 GUI units; black-hole overflow beyond it is not included in the RGB metric.",
    "Texture animation is frozen to its first frame in the test fixture only; production animation remains active.",
    "General CFA information text, count-entry controls, other special bodies and resource-pack/platform coverage remain separate work.",
]}
images = {}
for name, (version, log, folder) in RUNS.items():
    text = (ROOT / log).read_text(encoding="utf-8", errors="replace")
    assert "PORT_CFA_PREVIEWS_CAPTURED" in text and "BUILD SUCCESSFUL" in text, log
    assert "PORT_CFA_PREVIEW_ATLAS_FROZEN" in text, log
    rows = re.findall(r"PORT_STELLAR_UI ([\w-]+): scroll=", text)
    assert rows == SCENES, rows
    images[name] = {}
    for scene in SCENES:
        path = ROOT / folder / f"cfa-preview-{version}-{scene}.png"
        pixels = np.asarray(Image.open(path).convert("RGB"), dtype=np.int16)
        assert pixels.shape == (720, 1280, 3), path
        images[name][scene] = pixels
    report["runs"][name] = {"log": log, "screenshots": folder, "scenes": rows}

for scene in SCENES[:-1]:
    source = images["source"][scene][182:300, 492:610]
    target = images["target"][scene][182:300, 492:610]
    difference = np.abs(source - target)
    entry = {"maximum_rgb_difference": int(difference.max()), "mean_absolute_rgb_difference": float(difference.mean())}
    for name, pixels in [("source", source), ("target", target)]:
        y, x = np.where(pixels.max(axis=2) > 15)
        assert len(x) > 100, (name, scene, "Missing celestial preview")
        entry[f"{name}_bounds"] = [int(x.min()), int(y.min()), int(x.max()), int(y.max())]
    if not scene.startswith("neutron"):
        assert entry["source_bounds"] == entry["target_bounds"], (scene, entry)
    if scene == "rocky":
        assert entry["maximum_rgb_difference"] <= 2, entry
    if scene == "atmosphere":
        assert entry["maximum_rgb_difference"] <= 4, entry
    if scene in ["star", "white-dwarf", "flesh"]:
        assert entry["mean_absolute_rgb_difference"] < 1, entry
    report["previews"][scene] = entry

for scene in ["star", "ghost-no-foil"]:
    entries = []
    for slot in range(5):
        x = 314 if slot < 4 else 830
        y = 228 + slot * 36 if slot < 4 else 394
        a = images["source"][scene][y:y + 32, x:x + 32]
        b = images["target"][scene][y:y + 32, x:x + 32]
        difference = np.abs(a - b)
        entry = {"slot": slot, "maximum_rgb_difference": int(difference.max()),
                 "mean_absolute_rgb_difference": float(difference.mean())}
        assert entry["mean_absolute_rgb_difference"] < 1, (scene, entry)
        if slot == 4:
            assert entry["maximum_rgb_difference"] <= 2, (scene, entry)
        entries.append(entry)
    report["ghost_slots"][scene] = entries

output = ROOT / "build/porting/cfa-preview-visual-comparison.json"
output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print("9 captures per version verified; 6 non-jet preview bounds match")
print("Rocky preview max RGB error <=2; star/white-dwarf/flesh and all ghost-slot mean errors <1/255")
print(output)
