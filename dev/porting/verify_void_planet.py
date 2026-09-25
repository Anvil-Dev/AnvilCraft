"""Verify the builtin void's source recipe, real travel and orbital sky captures."""
from pathlib import Path
import json
import re
import subprocess
import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
source = root / "build/porting/reference-frost-1.21/run/frost-reference/screenshots"
target = root / "run/port-validation/client/screenshots"
report = {"travel": {}, "sky": {}}
for version, log in [("1.21", "client-void-travel-source-final.log"), ("26.1", "client-void-travel-target-final.log")]:
    text = (root / "build/porting" / log).read_text(encoding="utf8", errors="replace")
    assert "PORT_TRAVEL_PLAYER_PASSED" in text and "All dimensions are saved" in text and "BUILD SUCCESSFUL" in text
    rows = re.findall(r"PORT_TRAVEL_PLAYER_(OUTBOUND|RETURNED): dimension=([^\r\n]+), position=\(([^)]+)\)", text)
    assert len(rows) == 2, rows
    report["travel"][version] = rows
assert report["travel"]["1.21"] == report["travel"]["26.1"], report["travel"]

for name in ["recipe/special_celestial_body/void_planet.json", "advancement/recipes/special_celestial_body/void_planet.json"]:
    path = "src/generated/resources/data/anvilcraft/" + name
    original = subprocess.check_output(["git", "show", "dev/1.21/1.6:" + path], cwd=root)
    assert json.loads(original) == json.loads((root / path).read_text()), path
path = "src/main/resources/data/anvilcraft/dimension/void_planet.json"
assert json.loads(subprocess.check_output(["git", "show", "dev/1.21/1.6:" + path], cwd=root)) == json.loads((root / path).read_text())


def read(folder, version, case):
    return np.asarray(Image.open(folder / f"void-sky-{version}-{case}.png").convert("RGB"), dtype=np.int16)


for case in ["base", "disabled", "rotated", "animated", "pending", "reloaded", "eclipse"]:
    a = read(source, "1.21", case)
    b = read(target, "26.1", case)
    diff = np.abs(a - b)
    report["sky"][case] = {"mean_rgb_difference": float(diff.mean()), "max_rgb_difference": int(diff.max())}
    assert diff.mean() < 0.2, (case, "unexpected source/native visual difference")
    if case not in ["disabled", "rotated"]:
        mask_a = np.max(np.abs(a - read(source, "1.21", "disabled")), axis=2) > 12
        mask_b = np.max(np.abs(b - read(target, "26.1", "disabled")), axis=2) > 12
        assert mask_a.sum() > 10000 and mask_b.sum() > 10000
        iou = float((mask_a & mask_b).sum() / (mask_a | mask_b).sum())
        assert iou > 0.995, (case, iou)
        report["sky"][case]["silhouette_iou"] = iou
for folder, version in [(source, "1.21"), (target, "26.1")]:
    base = read(folder, version, "base")
    assert np.abs(base - read(folder, version, "disabled")).mean() > 1, (version, "missing rings")
    assert np.abs(base - read(folder, version, "pending")).mean() < 0.1, (version, "void used overworld collapse state")
    assert np.abs(base - read(folder, version, "reloaded")).mean() < 0.1, (version, "reload changed sky")
    assert np.abs(base - read(folder, version, "animated")).mean() > 1, (version, "orbit did not animate")
report["limits"] = ["Native 26.1 End rendering and lighting are retained; full-scene pixel equality is not asserted.",
                    "Portal texture animation and native terrain shading are not frozen for travel captures."]
path = root / "build/porting/void-planet-comparison.json"
path.write_text(json.dumps(report, indent=2) + "\n", encoding="utf8")
print(json.dumps(report, indent=2))
