"""Check matching chest silhouettes and HUD glyph placement across the native renderers."""

from pathlib import Path

import numpy as np
from PIL import Image


ROOT = Path(__file__).resolve().parents[2]


def verify():
    reference = np.array(Image.open(ROOT / "build/porting/reference-frost-1.21/run/frost-reference/screenshots/chest-1.21-models-hud.png")
                         .convert("RGB")).astype(int)
    native = np.array(Image.open(ROOT / "run/port-validation/client/screenshots/chest-26.1-models-hud.png")
                      .convert("RGB")).astype(int)
    assert reference.shape == native.shape == (720, 1280, 3)
    for name, left, right in (("grid-off", 274, 416), ("grid-on", 454, 590), ("stored-off", 632, 768), ("stored-on", 806, 944)):
        a = reference[344:430, left:right]
        b = native[344:430, left:right]
        expected = np.any(a != reference[344:430, 200:201], axis=2)
        actual = np.any(b != native[344:430, 200:201], axis=2)
        assert np.array_equal(expected, actual), f"{name}: model silhouette differs"
        difference = np.abs(a - b)[expected]
        print(f"{name}: silhouette matches, native lighting RGB mean difference {difference.mean():.4f}")
    for name, box in (("percent", (56, 24, 96, 44)), ("capacitor", (56, 64, 96, 84)), ("super-capacitor", (184, 64, 224, 84))):
        left, top, right, bottom = box
        a = reference[top:bottom, left:right]
        b = native[top:bottom, left:right]
        expected = (a[:, :, 0] > 245) & (a[:, :, 0] == a[:, :, 1]) & (a[:, :, 1] == a[:, :, 2])
        actual = (b[:, :, 0] > 245) & (b[:, :, 0] == b[:, :, 1]) & (b[:, :, 1] == b[:, :, 2])
        assert expected.any() and np.array_equal(expected, actual), f"{name}: glyph shape or placement differs"
        assert np.abs(a - b)[expected].max() <= 3, f"{name}: color difference exceeds native text conversion"
        print(f"{name}: glyph shape/placement matches, maximum RGB difference {np.abs(a - b)[expected].max()}")


if __name__ == "__main__":
    verify()
