"""Check gateway projection, source GUI submission diagnosis, item fidelity and cache reuse."""
from pathlib import Path
import json
import re

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
source = root / "build/porting/reference-frost-1.21/run/frost-reference/screenshots"
target = root / "run/port-validation/client/screenshots"
report = {"ui": {}, "world": {}, "items": {}, "limits": [
    "Unmodified source GUI produced a blank body region. A separate fixture immediately flushes the original private draw method.",
    "The source control changes submission timing only; production source in the reference worktree is unchanged.",
    "Native world fog, sky and clouds are retained; maximum pixel identity is not claimed for procedural projection or world views."
]}

def log(name, marker):
    text = (root / "build/porting" / name).read_text(encoding="utf8", errors="replace")
    assert "BUILD SUCCESSFUL" in text and marker in text, name
    return text


def pixels(folder, name):
    image = np.asarray(Image.open(folder / name).convert("RGB"), dtype=int)
    assert image.shape == (720, 1280, 3), name
    return image

log("client-gateway-ui-source-1.log", "PORT_GATEWAY_UI_CAPTURED")
log("client-gateway-ui-control-source-2.log", "PORT_GATEWAY_UI_CONTROL_CAPTURED")
log("client-gateway-ui-target-3.log", "PORT_GATEWAY_UI_CAPTURED")
for name in ("gateway", "gateway-offset", "gateway-time"):
    x = 492 + (60 if name == "gateway-offset" else 0)
    old = pixels(source, f"gateway-ui-1.21-{name}.png")[182:300, x:x + 118]
    a = pixels(source, f"gateway-ui-control-1.21-{name}.png")[182:300, x:x + 118]
    b = pixels(target, f"gateway-ui-26.1-{name}.png")[182:300, x:x + 118]
    assert not old.any(), "The baseline submission diagnosis changed"
    assert np.array_equal(a.max(axis=2) > 0, b.max(axis=2) > 0), (name, "Gateway silhouette differs")
    delta = np.abs(a - b)
    correlation = float(np.corrcoef(a.flatten(), b.flatten())[0, 1])
    assert float(delta.mean()) < 1 and correlation > 0.85, (name, delta.mean(), correlation)
    report["ui"][name] = {"baseline_nonzero_values": int(np.count_nonzero(old)), "max_rgb_difference": int(delta.max()),
                            "mean_rgb_difference": float(delta.mean()), "correlation": correlation}
for version, folder in (("1.21", source), ("26.1", target)):
    prefix = "gateway-ui-control" if version == "1.21" else "gateway-ui"
    base = pixels(folder, f"{prefix}-{version}-gateway.png")[182:300, 492:610]
    moved = pixels(folder, f"{prefix}-{version}-gateway-offset.png")[182:300, 552:670]
    later = pixels(folder, f"{prefix}-{version}-gateway-time.png")[182:300, 492:610]
    assert np.count_nonzero(np.abs(base - moved) > 10) > 500, "Projection is incorrectly anchored to the PIP texture"
    assert np.count_nonzero(np.abs(base - later) > 10) > 500, "Gateway animation stopped"
pattern = (r"PORT_SPECIAL_WORLD_GEOMETRY ([\w-]+): ring=([^,]+), center=([^,]+), body=([^,]+), beam=([^,]+), "
           r"camera=\(([^)]+)\), pitch=([^\r\n]+)")
world = {}
for version, name, folder in (("1.21", "client-gateway-world-source-1.log", source),
                               ("26.1", "client-gateway-world-target-1.log", target)):
    text = log(name, "PORT_GATEWAY_WORLD_CAPTURED")
    rows = re.findall(pattern, text)
    assert [row[0] for row in rows] == ["gateway", "gateway-angle", "gateway-time"]
    world[version] = {row[0]: [float(v) for v in row[1:5]] + [float(v) for v in row[5].split(",")] + [float(row[6])] for row in rows}
    if version == "26.1":
        assert text.count("PORT_GATEWAY_RENDER_STATE_PASSED") == 3
    for scene in world[version]:
        region = pixels(folder, f"gateway-world-{version}-{scene}.png")[380:455, 605:675]
        assert int(((region[:, :, 0] < 80) & (region[:, :, 2] < 150)).sum()) > 1000, "Missing world gateway surface"
for scene in world["1.21"]:
    difference = float(np.max(np.abs(np.asarray(world["1.21"][scene]) - world["26.1"][scene])))
    assert difference < 0.00002, (scene, difference)
    report["world"][scene] = {"geometry_and_camera": world["26.1"][scene], "max_difference": difference}
for scene in world["1.21"]:
    a = pixels(source, f"gateway-world-1.21-{scene}.png")[350:480, 570:710]
    b = pixels(target, f"gateway-world-26.1-{scene}.png")[350:480, 570:710]
    source_mask = a[:, :, 0] < 80
    target_mask = b[:, :, 0] < 80
    overlap = float((source_mask & target_mask).sum() / (source_mask | target_mask).sum())
    assert overlap > 0.99, (scene, "Central dark-region geometry diverged", overlap)
    report["world"][scene]["central_dark_mask_iou"] = overlap
for version, folder in (("1.21", source), ("26.1", target)):
    base = pixels(folder, f"gateway-world-{version}-gateway.png")[350:480, 570:710]
    angle = pixels(folder, f"gateway-world-{version}-gateway-angle.png")[350:480, 570:710]
    later = pixels(folder, f"gateway-world-{version}-gateway-time.png")[350:480, 570:710]
    assert np.count_nonzero((base[:, :, 0] < 80) != (angle[:, :, 0] < 80)) > 100, "Rotated scene did not rotate"
    mask = (base[:, :, 0] < 80) & (later[:, :, 0] < 80)
    assert np.count_nonzero(np.abs(base - later)[mask] > 10) > 500, "World projection animation stopped"
log("client-gateway-item-source-1.log", "PORT_CFA_ITEM_REFERENCE_CAPTURED")
text = log("client-gateway-item-target-3.log", "PORT_CFA_ITEM_RENDER_PASSED")
assert "PORT_GATEWAY_GUI_PROJECTION_PASSED" in text
cache = re.search(r"PORT_GATEWAY_CACHE_REUSE_PASSED: (\d+) frames, (\d+) renderers, (\d+) atlas allocations", text)
assert cache and int(cache[1]) >= 3
report["cache_reuse"] = dict(zip(("frames", "renderers", "atlas_allocations"), map(int, cache.groups())))
for scene in ("gallery", "main-hand", "off-hand", "head", "reloaded", "vanilla-atmosphere"):
    a = pixels(source, f"gateway-item-1.21-{scene}.png")
    b = pixels(target, f"gateway-item-26.1-{scene}.png")
    if scene in ("gallery", "reloaded", "vanilla-atmosphere"):
        values = []
        for index in range(3):
            x = 50 + 168 * index
            delta = np.abs(a[170:280, x:x + 110] - b[170:280, x:x + 110])
            values.append(float(delta.mean()))
        assert max(values) < 1, (scene, values)
        report["items"][scene] = {"mean_icon_rgb_differences": values}
    else:
        report["items"][scene] = {"captured": True, "comparison": "visually inspected; native scene/background differences retained"}
log("client-gateway-storage-regression.log", "PORT_STORAGE_ITEM_SCENE_PASSED")
baselines = list((root / "build/porting/gateway-storage-baseline").glob("*.png"))
assert len(baselines) == 8
for before in baselines:
    assert np.array_equal(pixels(before.parent, before.name), pixels(target, before.name)), before.name
report["storage_icon_regression"] = "All eight native screenshots unchanged; cache bounds and reuse checks passed"
output = root / "build/porting/gateway-visual-comparison.json"
output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf8")
print("Gateway GUI, world, item, cache reuse and storage icon regression checks passed")
print(output)
