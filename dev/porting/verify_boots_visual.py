"""Compare opaque charge-bar pixels in the matching 1.21 and 26.1 client scenes."""

from io import BytesIO
from pathlib import Path
from zipfile import ZipFile

import numpy as np
from PIL import Image


ROOT = Path(__file__).resolve().parents[2]


def verify():
    reference = np.array(Image.open(ROOT / "build/porting/reference-frost-1.21/run/frost-reference/screenshots/boots-1.21-models-bars.png")
                         .convert("RGB")).astype(int)
    native = np.array(Image.open(ROOT / "run/port-validation/client/screenshots/boots-26.1-models-bars.png")
                      .convert("RGB")).astype(int)
    assert reference.shape == native.shape == (720, 1280, 3)
    with ZipFile(ROOT / "build/moddev/artifacts/neoforge-21.1.238-client-extra-aka-minecraft-resources.jar") as archive:
        background = Image.open(BytesIO(archive.read("assets/minecraft/textures/gui/sprites/hud/experience_bar_background.png")))
        mask = np.array(background.convert("RGBA"))[:, :, 3] == 255
    mask = mask.repeat(2, axis=0).repeat(2, axis=1)
    assert mask.shape == (10, 364)
    for index, name in enumerate(("quarter", "half", "three-quarter", "held")):
        top = 662 - (index + 4) * 24
        diff = np.abs(reference[top:top + 10, 458:822] - native[top:top + 10, 458:822])[mask]
        assert diff.max() <= 1, f"{name}: maximum RGB difference {diff.max()}"
        print(f"{name}: {mask.sum()} opaque pixels, max RGB difference {diff.max()}, mean {diff.mean():.5f}")


if __name__ == "__main__":
    verify()
