"""Compare source/native Moon skies without substituting isolated shader output for game captures."""
from pathlib import Path
import argparse
import json
import numpy as np
from PIL import Image

parser = argparse.ArgumentParser()
parser.add_argument("--target-log", default="client-mun-sky-target-final-2.log")
args = parser.parse_args()
root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
source = reference / "run/mun-reference/screenshots"
target = root / "run/port-validation/client/screenshots"
cases = ["earth-new", "earth-quarter", "earth-full", "boundary", "far-side", "sun", "potato-sun", "off-earth", "off-sun", "reloaded"]
report = {"cases": {}, "limits": ["This comparison covers sky rendering; surface lighting and shadows require separate checks."]}
for log in ["client-mun-sky-source-1.log", args.target_log]:
    text = (root / "build/porting" / log).read_text(encoding="utf8", errors="replace")
    assert "PORT_MUN_SKY_PASSED" in text and "All dimensions are saved" in text and "BUILD SUCCESSFUL" in text, log
    if log == args.target_log:
        assert "PORT_MUN_SKY_BUFFER_REUSE_PASSED" in text

for name in cases:
    a = np.asarray(Image.open(source / f"mun-sky-1.21-{name}.png").convert("RGB"), dtype=np.int16)
    b = np.asarray(Image.open(target / f"mun-sky-26.1-{name}.png").convert("RGB"), dtype=np.int16)
    assert a.shape == b.shape == (720, 1280, 3)
    delta = np.abs(a - b)
    # Retain maximum errors and their counts rather than hiding sparse texture-boundary differences in the mean.
    entry = {"mean_rgb_difference": float(delta.mean()), "max_rgb_difference": int(delta.max()),
             "pixels_above_16": int((delta.max(axis=2) > 16).sum())}
    assert entry["mean_rgb_difference"] < 0.02, (name, entry)
    assert entry["pixels_above_16"] < 100, (name, entry)
    if name not in ["boundary", "far-side"]:
        mask_a = a[240:480, 500:780].max(axis=2) > 8
        mask_b = b[240:480, 500:780].max(axis=2) > 8
        union = (mask_a | mask_b).sum()
        assert union > 1000, (name, "celestial body is missing")
        entry["body_mask_iou"] = float((mask_a & mask_b).sum() / union)
        assert entry["body_mask_iou"] > 0.995, (name, entry)
    report["cases"][name] = entry


def shader_body(path):
    text = path.read_text(encoding="utf8")
    return "\n".join(line for line in text.splitlines()
                     if line.strip() and not line.startswith(("uniform ", "#version", "#moj_import")))


for name in ["core/mun/mun_sky.vsh", "core/mun/mun_sky.fsh", "include/mun/mun_sun.glsl", "include/mun/mun_sun_render.glsl"]:
    path = "src/main/resources/assets/anvilcraft/shaders/" + name
    assert shader_body(root / path) == shader_body(reference / path), name
for name in ["planet_overworld.png", "star.png"]:
    path = "src/main/resources/assets/anvilcraft/textures/block/celestial_body/" + name
    assert (root / path).read_bytes() == (reference / path).read_bytes(), name
for folder, version in [(source, "1.21"), (target, "26.1")]:
    for left, right in [("sun", "potato-sun"), ("earth-full", "reloaded")]:
        a = np.asarray(Image.open(folder / f"mun-sky-{version}-{left}.png"), dtype=np.int16)
        b = np.asarray(Image.open(folder / f"mun-sky-{version}-{right}.png"), dtype=np.int16)
        assert np.abs(a - b).mean() < 0.02, (version, left, right)
output = root / "build/porting/mun-sky-comparison.json"
output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf8")
print(json.dumps(report, indent=2))
