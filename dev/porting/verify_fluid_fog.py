"""Compare registered fluid fog hooks and paired development-scene screenshots."""
import json
from pathlib import Path

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
native_run = root / "run/port-validation/client"
source_run = root / "build/porting/reference-mun-1.21/run/mun-reference"
for name in ("client-fluid-fog-native-final.log", "client-fluid-fog-reference.log"):
    log = (root / "build/porting" / name).read_text(encoding="utf-8", errors="replace")
    assert "BUILD SUCCESSFUL" in log, name
    if name.startswith("client"):
        assert "PORT_FLUID_FOG_PASSED: 7 scenes" in log, name
    if "native" in name:
        assert "> Task :checkPortJava" in log
native = json.loads((native_run / "fluid-fog-26.1.json").read_text(encoding="utf-8"))
source = json.loads((source_run / "fluid-fog-1.21.json").read_text(encoding="utf-8"))
assert native.keys() == source.keys() and len(native) == 7
for case, expected in source.items():
    assert native[case].keys() == expected.keys() and len(expected) == 29
    for fluid, values in expected.items():
        assert np.allclose(native[case][fluid], values, rtol=0, atol=1e-7), (case, fluid, native[case][fluid], values)
assert np.allclose(native["honey"]["honey"], [1, 184 / 255, 46 / 255, 0, 2], rtol=0, atol=1e-7)
assert native["honey"] == native["honey-reloaded"]
assert native["spectator"]["honey"][3:] == [123, 234]
for key in ("powder_snow", "liquid_enchantment"):
    assert np.allclose(native["honey"][key], [0.1, 0.2, 0.3, 123, 234], rtol=0, atol=1e-7)
metrics = {}
for case in source:
    actual = np.asarray(Image.open(native_run / f"screenshots/fluid-fog-26.1-{case}.png").convert("RGB"), dtype=float)
    expected = np.asarray(Image.open(source_run / f"screenshots/fluid-fog-1.21-{case}.png").convert("RGB"), dtype=float)
    assert actual.shape == expected.shape == (720, 1280, 3)
    metrics[case] = {"mean_rgb_error": float(np.abs(actual - expected).mean()),
                     "far_wall_native_rgb": actual[300:420, 850:950].mean(axis=(0, 1)).tolist(),
                     "far_wall_source_rgb": expected[300:420, 850:950].mean(axis=(0, 1)).tolist()}
    if case not in ("no-fog", "spectator"):
        assert np.max(np.abs(actual[300:420, 850:950] - expected[300:420, 850:950])) <= 1, case
        assert np.max(np.abs(actual[40:160, 543:560] - expected[40:160, 543:560])) <= 1, (case, "sky gap")
report = {"registered_extensions": 29, "scenes": 7, "callback_cases": 203, "screenshot_metrics": metrics,
          "scope": "Development scene invokes real fog extensions at viewport events; it does not make unplaceable fluids placeable.",
          "limits": "Native world shading and fog interpolation may differ; fully fogged far-wall and sky-gap pixels must match."}
(root / "build/porting/fluid-fog-comparison.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print(json.dumps(report, indent=2))
