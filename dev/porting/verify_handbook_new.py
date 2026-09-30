"""Compare handbook layout/tooltip/loop contracts and report the paired screenshots."""
from pathlib import Path
import json
import subprocess

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
native = root / "run/port-validation/client"
source = root / "build/porting/reference-mun-1.21/run/mun-reference"
a = json.loads((native / "handbook-new-26.1.json").read_text(encoding="utf-8"))
b = json.loads((source / "handbook-new-1.21.json").read_text(encoding="utf-8"))
assert a == b and len(a) == 12, "Layout, tooltip, loop selection or intermediate-model references differ"
models = set()
metrics = {}
for name, data in a.items():
    models.update(value for value in data.get("models", []) if value)
    first = np.asarray(Image.open(native / f"screenshots/handbook-new-26.1-{name}.png").convert("RGB"), dtype=np.int16)
    second = np.asarray(Image.open(source / f"screenshots/handbook-new-1.21-{name}.png").convert("RGB"), dtype=np.int16)
    if name.startswith(("energy-loop", "mass-loop")):
        assert np.array_equal(first[410:446, 398:572], second[410:446, 398:572]), (name, "arrow or cycle texture")
    width, height = data["width"] * 2, data["height"] * 2
    first = first[220:220 + height, 120:120 + width]
    second = second[220:220 + height, 120:120 + width]
    first_mask = np.any(first != [241, 230, 205], axis=2)
    second_mask = np.any(second != [241, 230, 205], axis=2)
    union = first_mask | second_mask
    assert union.any(), name
    metrics[name] = {
        "foreground_iou": float((first_mask & second_mask).sum() / union.sum()),
        "mean_rgb_error": float(np.abs(first - second)[union].mean()),
    }
for model in models:
    namespace, path = model.split(":", 1)
    file = f"src/main/resources/assets/{namespace}/models/{path}.json"
    expected = subprocess.check_output(["git", "show", "dev/1.21/1.6:" + file], cwd=root)
    assert json.loads((root / file).read_text(encoding="utf-8")) == json.loads(expected), model
for name in ("procedural_process.png", "cycle.png", "arrow_long.png"):
    file = "src/main/resources/assets/anvilcraft/textures/gui/ageratum/" + name
    assert (root / file).read_bytes() == subprocess.check_output(["git", "show", "dev/1.21/1.6:" + file], cwd=root), name
report = {"cases": len(a), "referenced_models": sorted(models), "screenshots": metrics,
          "limits": "Screenshot RGB and silhouette differences are reported rather than treated as equality: ingredient cycling, the animated WIP shell and native block rendering can differ in captured phase/sampling. Loop phase is explicitly matched."}
(root / "build/porting/handbook-new-comparison.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print(f"PASS: {len(a)} source-matched layouts, tooltips and loop/model selections; exact arrow/cycle assets and model definitions")
