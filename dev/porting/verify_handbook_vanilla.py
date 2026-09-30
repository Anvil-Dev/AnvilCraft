"""Compare actual standard-recipe data, hover hits and paired rendered widgets."""
import json
from pathlib import Path

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
native_run = root / "run/port-validation/client"
source_run = root / "build/porting/reference-mun-1.21/run/mun-reference"
for name in ("client-handbook-vanilla-native-final.log", "client-handbook-vanilla-reference-final.log"):
    log = (root / "build/porting" / name).read_text(encoding="utf-8", errors="replace")
    assert "PORT_HANDBOOK_VANILLA_PASSED: 10 cases" in log and "BUILD SUCCESSFUL" in log, name
native = json.loads((native_run / "handbook-vanilla-26.1.json").read_text(encoding="utf-8"))
source = json.loads((source_run / "handbook-vanilla-1.21.json").read_text(encoding="utf-8"))
assert native == source and len(native) == 10
assert [i for i, slot in enumerate(native["sparse"]["slots"]) if slot] == [0, 3, 4]
assert [i for i, slot in enumerate(native["column"]["slots"]) if slot] == [0, 3, 6]
assert [i for i, slot in enumerate(native["row"]["slots"]) if slot] == [0, 1, 2]
assert native["sparse"]["count"] == 3 and native["sparse"]["hoverTooltips"] == 0
for kind, icon in (("smelting", "furnace"), ("blasting", "blast_furnace"), ("smoking", "smoker"), ("campfire_cooking", "campfire")):
    assert native[kind]["station"] == "minecraft:" + icon
metrics = {}
for index, name in enumerate(native):
    actual = np.asarray(Image.open(native_run / f"screenshots/handbook-vanilla-26.1-{index}.png").convert("RGB"), dtype=float)
    expected = np.asarray(Image.open(source_run / f"screenshots/handbook-vanilla-1.21-{index}.png").convert("RGB"), dtype=float)
    assert actual.shape == expected.shape == (720, 1280, 3)
    error = float(np.abs(actual[80:300, 80:464] - expected[80:300, 80:464]).mean())
    if name in {"sparse", "column", "row", "shapeless"}:
        assert error == 0, (name, error)
    metrics[name] = {"widget_mean_rgb_error": error}
report = {"recipes": native, "visual_metrics": metrics,
          "coverage": "Three shaped grids, shapeless crafting, four cooking devices, stonecutting and the real stamping-platform recipe.",
          "limits": "Native item lighting/texture sampling remains in use. Static standard crafting recipes are covered; dynamic special-recipe outputs are not inferred."}
(root / "build/porting/handbook-vanilla-comparison.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print("Standard handbook recipe parity passed: ten widgets, exact slot/result/count/device metadata and hover behavior.")
