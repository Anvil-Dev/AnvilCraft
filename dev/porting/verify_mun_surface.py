"""Compare real Moon material/light captures while retaining native terrain sampling."""
from pathlib import Path
import json
import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
source = root / "build/porting/reference-mun-1.21/run/mun-reference/screenshots"
target = root / "run/port-validation/client/screenshots"
report = {"cases": {}, "limits": ["Standard shadow maps, history and third-party shader integration remain pending.",
                                  "Native 26.1 albedo sampling, AO vertices and render-distance fog are retained.",
                                  "OFF retains native rendering; it is not treated as a cross-version pixel-parity case."]}


def read(path):
    return np.asarray(Image.open(path).convert("RGB"), dtype=np.int16)


palette_a = read(source / "mun-lightmap-1.21.png")
palette_b = read(target / "mun-lightmap-26.1.png")
assert palette_a.shape == palette_b.shape == (16, 16, 3)
assert np.array_equal(palette_a, palette_b), "Moon lightmap palette differs"
report["lightmap_equal_pixels"] = 256
for name in ["client-mun-surface-source-2.log", "client-mun-surface-target-final.log"]:
    log = (root / "build/porting" / name).read_text(encoding="utf8", errors="replace")
    assert "PORT_MUN_SURFACE_PASSED" in log and "All dimensions are saved" in log and "BUILD SUCCESSFUL" in log, name
    if "target" in name:
        assert "PORT_MUN_SURFACE_SCOPE_REUSE_PASSED" in log

for case in ["day", "night", "block-light", "ao-off", "off", "day-again", "large-coordinate"]:
    a = read(source / f"mun-surface-1.21-{case}.png")
    b = read(target / f"mun-surface-26.1-{case}.png")
    delta = np.abs(a - b)
    entry = {"material_region_mean_rgb_difference": float(delta[260:700, 200:1000].mean()),
             "max_rgb_difference": int(delta.max())}
    white = np.abs(a[310:350, 545:570].mean((0, 1)) - b[310:350, 545:570].mean((0, 1)))
    chest = np.abs(a[398:414, 625:647].mean((0, 1)) - b[398:414, 625:647].mean((0, 1)))
    entry["white_column_mean_color_difference"] = white.tolist()
    entry["chest_mean_color_difference"] = chest.tolist()
    if case != "off":
        assert white.max() < 0.3, (case, "directional lighting diverged", white)
        assert chest.max() < 1.5, (case, "block entity lighting diverged", chest)
        assert entry["material_region_mean_rgb_difference"] < 5.0, (case, entry)
    report["cases"][case] = entry
for folder, version in [(source, "1.21"), (target, "26.1")]:
    day = read(folder / f"mun-surface-{version}-day.png")[450:650, 300:500]
    night = read(folder / f"mun-surface-{version}-night.png")[450:650, 300:500]
    lamp = read(folder / f"mun-surface-{version}-block-light.png")[370:460, 600:690]
    unlit = read(folder / f"mun-surface-{version}-night.png")[370:460, 600:690]
    assert day.mean() - night.mean() > 20, (version, "day/night illumination missing")
    assert lamp.mean() - unlit.mean() > 10, (version, "block light missing")
output = root / "build/porting/mun-surface-comparison.json"
output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf8")
print(json.dumps(report, indent=2))
