"""Compare each actual sidebar interaction and viewport snapshot with source."""
import json
from pathlib import Path

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
native_run = root / "run/port-validation/client"
source_run = root / "build/porting/reference-mun-1.21/run/mun-reference"
for name in ("client-handbook-sidebar-native-final.log", "client-handbook-sidebar-reference-final.log"):
    log = (root / "build/porting" / name).read_text(encoding="utf-8", errors="replace")
    assert "PORT_HANDBOOK_SIDEBAR_PASSED: 8 scenarios" in log and "BUILD SUCCESSFUL" in log, name
native = json.loads((native_run / "handbook-sidebar-26.1.json").read_text(encoding="utf-8"))
source = json.loads((source_run / "handbook-sidebar-1.21.json").read_text(encoding="utf-8"))
assert len(native) == len(source) == 8
for stage in native:
    assert native[stage] == source[stage], (stage, native[stage], source[stage])
assert native["6"] == native["7"], "Resize must preserve the inherited state"
metrics = {}
for stage in range(8):
    actual = np.asarray(Image.open(native_run / f"screenshots/handbook-sidebar-26.1-{stage}.png").convert("RGB"), dtype=float)
    expected = np.asarray(Image.open(source_run / f"screenshots/handbook-sidebar-1.21-{stage}.png").convert("RGB"), dtype=float)
    assert actual.shape == expected.shape == (720, 1280, 3)
    metrics[stage] = {"sidebar_mean_rgb_error": float(np.abs(actual[74:646, 150:299] - expected[74:646, 150:299]).mean())}
report = {"scenarios": native, "pixel_metrics": metrics,
          "coverage": "Ordinary click collapse/expand, pinned-parent hit, child navigation preserving state, scroll-to-bottom, last-row hit, resize.",
          "limits": "Native glyph rendering and world pixels are not required to match. Scope is the AnvilCraft guide namespace."}
(root / "build/porting/handbook-sidebar-comparison.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print("Handbook sidebar parity passed: all eight source interaction/viewport snapshots match exactly.")
