"""Compare source/native stellar samples and visible surface colors in the fixed fixture."""
import json
from pathlib import Path
import re

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
RUNS = {
    "source": ("1.21", "build/porting/client-stellar-source-2.log",
               "build/porting/reference-frost-1.21/run/frost-reference/screenshots", "PORT_CFA_ITEM_REFERENCE_CAPTURED"),
    "target": ("26.1", "build/porting/client-stellar-target-3.log",
               "run/port-validation/client/screenshots", "PORT_CFA_ITEM_RENDER_PASSED"),
}
SCENES = ["gallery", "main-hand", "off-hand", "head", "reloaded", "vanilla-atmosphere"]
report = {"runs": {}, "limits": [
    "Fixed snapshots do not verify stellar evolution or event animation.",
    "GUI atlas resolution, backgrounds and head ring animation phase differ; no whole-image parity claim.",
    "Iris, world multi-star ordering, transparent previews and hardware fallback still require further coverage.",
]}
images = {}
samples = {}
for name, (version, log, directory, marker) in RUNS.items():
    text = (ROOT / log).read_text(encoding="utf-8", errors="replace")
    assert marker in text and "BUILD SUCCESSFUL" in text, log
    if name == "target":
        assert "Sheets loaded too early" not in text
        assert "Stellar pipeline unavailable" not in text
    samples[name] = re.findall(r"PORT_STELLAR_SAMPLE ([^\r\n]+)", text)
    assert len(samples[name]) == 10, samples[name]
    scenes = SCENES + (["forced-fallback", "recovered"] if name == "target" else [])
    views = re.findall(r"PORT_CFA_VIEW ([\w-]+): position=\(([^)]+)\), flying=(\w+)", text)
    assert [view[0] for view in views] == scenes, views
    assert all(view[1:] == ("8.5, 85.0, 12.5", "false") for view in views), views
    images[name] = {}
    for scene in scenes:
        path = ROOT / directory / f"stellar-{version}-{scene}.png"
        pixels = np.asarray(Image.open(path).convert("RGB"), dtype=np.float32)
        assert pixels.shape == (720, 1280, 3), path
        if scene not in ["main-hand", "off-hand", "head"]:
            assert np.all((pixels[10, 10] >= 20) & (pixels[10, 10] <= 50)), (name, scene, "loading overlay")
        images[name][scene] = pixels
    report["runs"][name] = {"log": log, "screenshots": directory, "scenes": scenes}
assert samples["source"] == samples["target"], samples
report["matching_temperature_color_exposure_samples"] = samples["source"]

core = {}
for name in RUNS:
    # Interior of the K-star front face; excludes frame, corona and native sky.
    core[name] = images[name]["main-hand"][285:318, 860:965].mean(axis=(0, 1)).tolist()
core["maximum_mean_channel_difference"] = float(np.max(np.abs(np.asarray(core["source"]) - core["target"])))
assert core["maximum_mean_channel_difference"] <= 3, core
report["k_star_surface"] = core

gallery = {}
for name in RUNS:
    means = {}
    for scene in ["gallery", "vanilla-atmosphere"]:
        # Small interior patch of the GUI K star, kept away from the corona.
        means[scene] = images[name][scene][145:153, 270:278].mean(axis=(0, 1)).tolist()
    assert sum(means["gallery"]) > sum(means["vanilla-atmosphere"]) + 50, means
    gallery[name] = means
report["gui_mode_surface_comparison"] = gallery
for scene, reference in [("forced-fallback", "vanilla-atmosphere"), ("recovered", "gallery")]:
    actual = images["target"][scene][145:153, 270:278].mean(axis=(0, 1))
    expected = images["target"][reference][145:153, 270:278].mean(axis=(0, 1))
    assert float(np.max(np.abs(actual - expected))) <= 5, (scene, actual, expected)
report["forced_failure_recovery"] = "Target entered the forced failure latch, rendered fallback, then rebuilt a valid pipeline on resource reload."
output = ROOT / "build/porting/stellar-visual-comparison.json"
output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print(json.dumps({"k_star_surface": core, "gui_modes": gallery}, indent=2))
print(output)
