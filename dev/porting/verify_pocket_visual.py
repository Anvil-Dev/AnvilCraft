"""Compare pocket panel frames/headers, excluding item contents and world backgrounds."""

from pathlib import Path

import numpy as np
from PIL import Image


ROOT = Path(__file__).resolve().parents[2]
SCENARIOS = (
    ("survival-six", 6, 232, 97, 176, 2, False),
    ("survival-twelve", 12, 232, 97, 176, 2, False),
    ("recipe-wide", 12, 309, 97, 176, 2, False),
    ("recipe-narrow", 12, 125, 37, 176, 3, False),
    ("creative-twelve", 12, 222, 112, 195, 2, True),
)


def verify():
    total = 0
    for name, capacity, left, top, image_width, scale, creative in SCENARIOS:
        reference = np.array(Image.open(ROOT / f"build/porting/reference-frost-1.21/run/frost-reference/screenshots/pockets-1.21-{name}.png")
                             .convert("RGB")).astype(int)
        native = np.array(Image.open(ROOT / f"run/port-validation/client/screenshots/pockets-26.1-{name}.png").convert("RGB")).astype(int)
        assert reference.shape == native.shape == (720, 1280, 3)
        width = 26 if capacity == 6 else 44
        texture_name = "pockets_leggings" if capacity == 6 else "weatherproof_spacesuit_leggings"
        texture = np.array(Image.open(ROOT / f"src/main/resources/assets/anvilcraft/textures/gui/misc/equipment/{texture_name}.png")
                           .convert("RGBA"))
        mask = texture[:, :, 3] == 255
        mask[13:69, 4:width - 4] = False
        mask = mask.repeat(scale, axis=0).repeat(scale, axis=1)
        for side in ([1] if name == "recipe-narrow" else [0, 1]):
            x = (left - width - 2 if side == 0 else left + image_width + 2) * scale
            y = (top + (40 if creative else 70)) * scale
            diff = np.abs(reference[y:y + 73 * scale, x:x + width * scale] - native[y:y + 73 * scale, x:x + width * scale])[mask]
            assert diff.max() == 0, f"{name} side {side}: maximum RGB difference {diff.max()}"
            total += int(mask.sum())
    print(f"Verified {total} opaque pocket frame/header pixels across five client layouts: zero RGB differences")


if __name__ == "__main__":
    verify()
