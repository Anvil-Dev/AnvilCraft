"""Compare fixed Frost smithing scenes from 1.21 and 26.1."""

from pathlib import Path

import numpy as np
from PIL import Image


ROOT = Path(__file__).resolve().parents[2]
REFERENCE = ROOT / "build/porting/reference-frost-1.21/run/frost-reference/screenshots"
TARGET = ROOT / "run/port-validation/client/screenshots"


def verify():
    # Exclude world rendering, JEI item lists, the player's inventory, and the
    # borrowed-template panel; those differ between the two test setups.
    regions = {"preview": ((728, 242, 801, 350), 1.0), "main": ((518, 220, 722, 348), 0.15)}
    for cycle in range(3):
        reference = np.asarray(Image.open(REFERENCE / f"frost-smithing-1.21-{cycle}-menu.png").convert("RGB"), dtype=int)
        target = np.asarray(Image.open(TARGET / f"frost-smithing-26.1-{cycle}-menu.png").convert("RGB"), dtype=int)
        assert reference.shape == target.shape == (720, 1280, 3)
        for name, ((x0, y0, x1, y1), limit) in regions.items():
            difference = np.abs(reference[y0:y1, x0:x1] - target[y0:y1, x0:x1])
            mean = float(difference.mean())
            print(f"scene={cycle} region={name} mean_rgb_error={mean:.3f}")
            assert mean <= limit, (cycle, name, mean)


if __name__ == "__main__":
    verify()
