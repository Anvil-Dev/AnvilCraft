"""Compare the fifteen inventory icons captured in the two live client scenes."""
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]


def verify():
    source = np.array(Image.open(ROOT / "build/porting/reference-frost-1.21/run/frost-reference/screenshots/"
                                "resonator-1.21-mode-models.png").convert("RGB")).astype(int)
    target = np.array(Image.open(ROOT / "run/port-validation/client/screenshots/"
                                "resonator-26.1-mode-models.png").convert("RGB")).astype(int)
    assert source.shape == target.shape == (720, 1280, 3)
    for slot in range(15):
        x = 480 + 36 * (slot if slot < 9 else slot - 9)
        y = 478 if slot < 9 else 362
        difference = np.abs(source[y:y + 32, x:x + 32] - target[y:y + 32, x:x + 32])
        mean = difference.mean()
        assert mean < 1, f"slot {slot}: mean RGB difference {mean}"
        print(f"slot {slot}: mean RGB difference {mean:.4f}, maximum edge/channel difference {difference.max()}")


if __name__ == "__main__":
    verify()
