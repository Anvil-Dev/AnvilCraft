"""Check the unchanged source animation math and bounded visual differences in three poses."""

from pathlib import Path
import re
import subprocess

import numpy as np
from PIL import Image


ROOT = Path(__file__).resolve().parents[2]


def verify():
    for name in ("EnergyWeaponReloadPose", "EnergyWeaponDrawAnimation"):
        path = f"src/main/java/dev/dubhe/anvilcraft/client/renderer/item/{name}.java"
        source = subprocess.check_output(["git", "show", f"dev/1.21/1.6:{path}"], cwd=ROOT).decode()
        target = (ROOT / path).read_text(encoding="utf-8")
        assert re.sub(r"\s+", "", source) == re.sub(r"\s+", "", target), f"{name} math differs from source"
    scenarios = (("pose-main-12", (550, 300, 1280, 590)),
                 ("pose-main-26", (260, 300, 1280, 590)),
                 ("pose-offhand-18", (0, 300, 1050, 590)))
    for name, (left, top, right, bottom) in scenarios:
        source = np.array(Image.open(ROOT / f"build/porting/reference-frost-1.21/run/frost-reference/screenshots/reload-1.21-{name}.png")
                          .convert("RGB")).astype(int)[top:bottom, left:right]
        target = np.array(Image.open(ROOT / f"run/port-validation/client/screenshots/reload-26.1-{name}.png")
                          .convert("RGB")).astype(int)[top:bottom, left:right]
        source_mask = source[:, :, 2] < 210
        target_mask = target[:, :, 2] < 210
        intersection = source_mask & target_mask
        overlap = np.count_nonzero(intersection) / np.count_nonzero(source_mask | target_mask)
        difference = np.abs(source - target)[intersection].mean()
        assert overlap > 0.995, f"{name}: foreground overlap {overlap}"
        assert difference < 2, f"{name}: mean RGB difference {difference}"
        print(f"{name}: foreground overlap {overlap:.6f}, mean native RGB difference {difference:.4f}")


if __name__ == "__main__":
    verify()
