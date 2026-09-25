"""Compare actual source/native orbital silhouettes, visibility and reload behavior."""
from pathlib import Path
import json
import subprocess
import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
source = root / "build/porting/reference-frost-1.21/run/frost-reference/screenshots"
target = root / "run/port-validation/client/screenshots"
cases = ["base", "disabled", "rotated", "animated", "pending", "reloaded", "eclipse"]
report = {"cases": {}, "assets": [], "silhouette_roi": [0, 0, 1280, 540]}


def read(folder, version, case):
    return np.asarray(Image.open(folder / f"orbital-sky-{version}-{case}.png").convert("RGB"), dtype=np.int16)


baseline_source = read(source, "1.21", "disabled")
baseline_target = read(target, "26.1", "disabled")
for case in cases:
    a = read(source, "1.21", case)
    b = read(target, "26.1", case)
    delta = np.abs(a - b)
    entry = {"mean_rgb_difference": float(delta.mean()), "max_rgb_difference": int(delta.max())}
    if case in ["base", "rotated", "animated", "reloaded"]:
        # Exclude the lower fog fade, where native sky-color rounding changes the contrast threshold.
        mask_a = np.max(np.abs(a - baseline_source), axis=2)[:540] > 12
        mask_b = np.max(np.abs(b - baseline_target), axis=2)[:540] > 12
        union = np.logical_or(mask_a, mask_b).sum()
        intersection = np.logical_and(mask_a, mask_b).sum()
        entry["silhouette_iou"] = float(intersection / union) if union else 1.0
        entry["source_ring_pixels"] = int(mask_a.sum())
        entry["target_ring_pixels"] = int(mask_b.sum())
        if case != "rotated":
            assert mask_a.sum() > 10000 and mask_b.sum() > 10000, (case, "missing orbital geometry")
        assert entry["silhouette_iou"] > 0.995, (case, entry)
    report["cases"][case] = entry

for folder, version in [(source, "1.21"), (target, "26.1")]:
    hidden = np.abs(read(folder, version, "disabled") - read(folder, version, "pending"))
    reload_delta = np.abs(read(folder, version, "base") - read(folder, version, "reloaded"))
    assert hidden.max() <= 2, (version, "pending did not hide rings")
    assert reload_delta.mean() < 0.1, (version, "resource reload changed geometry")
    assert np.abs(read(folder, version, "animated") - read(folder, version, "base")).mean() > 1
    assert read(folder, version, "eclipse").mean() < read(folder, version, "base").mean() * 0.8

assets = ["models/block/celestial_forging_anvil_ring_4.json", "models/block/celestial_forging_anvil_ring_5.json",
          "models/block/celestial_forging_anvil_ring_big.json", "textures/block/celestial_forging_anvil_top.png"]
for asset in assets:
    path = "src/main/resources/assets/anvilcraft/" + asset
    original = subprocess.check_output(["git", "show", "dev/1.21/1.6:" + path], cwd=root)
    actual = (root / path).read_bytes()
    if asset.endswith(".json"):
        assert json.loads(actual) == json.loads(original), asset
    else:
        assert actual == original, asset
    report["assets"].append(asset)
report["limits"] = ["Native 26.1 sky and fog remain; whole-image pixel equality is not required.",
                    "Void dimension activation and third-party shader packs are separate validation work."]
output = root / "build/porting/orbital-sky-comparison.json"
output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf8")
print(json.dumps(report, indent=2))
