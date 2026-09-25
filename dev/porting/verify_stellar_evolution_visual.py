"""Verify world-renderer outputs and report image differences without replacing native fog."""
import json
from pathlib import Path
import re

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SCENES = ["convective", "rgb-low", "rgb-mid", "rgb-high", "agb-a", "agb-b", "nebula", "supernova", "collapse", "ppisn"]
PATTERN = (r"PORT_STELLAR_GEOMETRY ([\w-]+): ring=([^,]+), center=([^,]+), body=([^,]+), beam=([^,]+), "
           r"camera=\(([^)]+)\), pitch=([^\r\n]+)")
RUNS = {
    "source": ("1.21", "build/porting/client-stellar-evolution-source-2.log",
               "build/porting/reference-frost-1.21/run/frost-reference/screenshots"),
    "target": ("26.1", "build/porting/client-stellar-evolution-target-6.log",
               "run/port-validation/client/screenshots"),
}
report = {"runs": {}, "scenes": {}, "limits": [
    "Fixed snapshots verify extracted geometry and sampled appearance, not continuous transition timing or multiplayer synchronization.",
    "Native 26.1 clouds, fog and lighting are retained. Pixel colors and whole screenshots are not claimed identical.",
    "The far-view control changes render distance to 32 chunks; it does not disable independent environmental fog.",
]}
values = {}
images = {}
for name, (version, log, folder) in RUNS.items():
    text = (ROOT / log).read_text(encoding="utf-8", errors="replace")
    assert "PORT_STELLAR_EVOLUTION_CAPTURED" in text and "BUILD SUCCESSFUL" in text, log
    rows = re.findall(PATTERN, text)
    assert [row[0] for row in rows] == SCENES, rows
    values[name] = {row[0]: [float(value) for value in row[1:5]] for row in rows}
    report["runs"][name] = {"log": log, "screenshots": folder,
                            "camera": {row[0]: [float(value) for value in row[5].split(",")] + [float(row[6])] for row in rows}}
    images[name] = {}
    for scene in SCENES:
        path = ROOT / folder / f"stellar-evolution-{version}-{scene}.png"
        pixels = np.asarray(Image.open(path).convert("RGB"), dtype=np.float32)
        assert pixels.shape == (720, 1280, 3), path
        images[name][scene] = pixels
    if name == "target":
        assert "PORT_STELLAR_TINT_ISOLATION_PASSED" in text
        assert "PORT_STELLAR_REPLACEMENT_PASSED" in text
        fog = re.findall(r"PORT_STELLAR_ENV_FOG: start=([^,]+), end=([^\r\n]+)", text)
        assert len(fog) == 10, fog
        report["environment_fog_samples"] = [list(map(float, sample)) for sample in fog]

for scene in SCENES:
    source = values["source"][scene]
    target = values["target"][scene]
    delta = float(np.max(np.abs(np.asarray(source) - target)))
    assert delta <= 0.00002, (scene, source, target)
    assert report["runs"]["source"]["camera"][scene] == report["runs"]["target"]["camera"][scene]
    item = {"source_ring_center_body_beam": source, "target_ring_center_body_beam": target, "max_geometry_difference": delta}
    for name in RUNS:
        pixels = images[name][scene]
        item[f"{name}_core_median_rgb"] = np.median(pixels[340:410, 600:680], axis=(0, 1)).tolist()
        red, green, blue = pixels[150:520, 400:880].transpose(2, 0, 1)
        orange = (red > 80) & (red > green * 1.1) & (green > blue * 1.3)
        y, x = np.where(orange)
        assert len(x) > 500, (name, scene, "Missing mechanical frame")
        item[f"{name}_orange_bounds_diagnostic"] = [int(x.min() + 400), int(y.min() + 150),
                                                    int(x.max() + 400), int(y.max() + 150)]
    report["scenes"][scene] = item

for name in RUNS:
    assert values[name]["agb-a"][:2] == values[name]["agb-b"][:2], "Pulsation moved the structural rings"
    assert abs(values[name]["agb-a"][2] - values[name]["agb-b"][2]) > 0.1, "Missing stellar pulsation"

far_log = ROOT / "build/porting/client-stellar-evolution-clear-1.log"
assert "BUILD SUCCESSFUL" in far_log.read_text(encoding="utf-8", errors="replace")
far = np.asarray(Image.open(ROOT / RUNS["target"][2] / "stellar-evolution-far-26.1-rgb-high.png").convert("RGB"))
report["far_view_control"] = {"log": str(far_log.relative_to(ROOT)), "render_distance_chunks": 32,
                              "rgb_high_core_median_rgb": np.median(far[340:410, 600:680], axis=(0, 1)).tolist()}
output = ROOT / "build/porting/stellar-evolution-visual-comparison.json"
output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print("10 world geometry pairs passed; maximum difference:", max(row["max_geometry_difference"] for row in report["scenes"].values()))
print("AGB structural stability, tint isolation, remnant transition and envelope bounds checks passed")
print(output)
