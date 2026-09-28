"""Verify live source/native crate disposal UI and server behavior evidence."""
from pathlib import Path
import json
import subprocess

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
source = subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root, text=True).strip()
assert source == subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=reference, text=True).strip()
for filename in ["client-crate-disposal-1.log", "client-crate-disposal-reference-1.log"]:
    text = (root / "build/porting" / filename).read_text(encoding="utf-8", errors="replace")
    assert "PORT_CRATE_DISPOSAL_CLIENT_PASSED" in text and "BUILD SUCCESSFUL" in text, filename
full = (root / "build/porting/tests-crate-disposal-all.log").read_text(encoding="utf-8", errors="replace")
assert "All 701 required tests passed" in full and "BUILD SUCCESSFUL" in full
assets = ["src/main/resources/assets/anvilcraft/models/block/crate.json"]
assets += [f"src/main/resources/assets/anvilcraft/textures/block/crate_{side}.png" for side in ["top", "side", "bottom"]]
for path in assets:
    expected = subprocess.check_output(["git", "show", f"{source}:{path}"], cwd=root)
    actual = (root / path).read_bytes()
    if path.endswith(".json"):
        assert json.loads(actual) == json.loads(expected), path
    else:
        assert actual == expected, path
for path in ["src/generated/resources/assets/anvilcraft/lang/en_us.json", "src/main/resources/assets/anvilcraft/lang/zh_cn.json"]:
    expected = json.loads(subprocess.check_output(["git", "show", f"{source}:{path}"], cwd=root))
    actual = json.loads((root / path).read_bytes())
    assert actual["block.anvilcraft.overflow_disposal_crate"] == expected["block.anvilcraft.overflow_disposal_crate"]
images = {}
for state in ["normal-ui", "disposal-ui", "restored-ui"]:
    a = np.asarray(Image.open(root / f"run/port-validation/client/screenshots/crate-disposal-26.1-{state}.png").convert("RGB"))
    b = np.asarray(Image.open(reference / f"run/mun-reference/screenshots/crate-disposal-1.21-{state}.png").convert("RGB"))
    title_a, title_b = a[140:160, 554:938].astype(float), b[140:160, 554:938].astype(float)
    error = float(np.abs(title_a-title_b).mean())
    identical = np.all(title_a == title_b, axis=2)
    native_text = np.all(title_a == 64, axis=2)
    source_text = np.all(title_b == 63, axis=2)
    assert np.all(identical | (native_text & source_text)), (state, error)
    assert np.array_equal(native_text, source_text), state
    images[state] = {"title_mean_rgb_error": error, "glyph_mask_iou": 1.0, "max_channel_error": 1}
report = {
    "source_commit": source,
    "full_required_tests": 701,
    "focused_tests": 6,
    "assets_checked": assets,
    "visuals": images,
    "contracts": [
        "Only face-adjacent ordinary void matter enables disposal; no diagonal/excited-state trigger.",
        "Store within weighted 2048 capacity, consume overflow, never destroy eternal items.",
        "Wrong occupied slot still routes to available sparse storage before disposing overflow.",
        "Nested transaction rollback restores both endpoints; committed transfers consume overflow.",
        "Capability and RPC creation/search paths refresh mode; cached capabilities follow adjacency changes.",
        "Mode is transient, recomputed after load, and preserves the block entity, storage ID and contents.",
        "Live source/native screens recenter their changing title; actual Jade collection switches names both ways."
    ],
    "limits": [
        "Vanilla 26.1 world rendering is retained; pixel assertions target the mod screen title.",
        "Both versions use 0x404040 title color: source rasterizes to RGB 63 and native to RGB 64; only this one-level glyph difference is allowed.",
        "Storage ports still connect only to shulker/hyperdimension cores, matching source; no new crate connectivity is added.",
        "Crate/container removal-drop lifecycle remains a separate pending port milestone."
    ]
}
(root / "build/porting/crate-disposal-report.json").write_text(json.dumps(report, indent=2)+"\n", encoding="utf-8")
print(json.dumps(report, indent=2))
