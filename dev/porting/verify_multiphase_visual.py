"""Check alpha/beta tooltip title glyphs while preserving native GUI color conversion."""
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]


def title(path):
    pixels = np.array(Image.open(path).convert("RGB"))[340:440, 510:830]
    mask = (pixels[:, :, 0] > 230) & (pixels[:, :, 2] > 230) & (pixels[:, :, 1] < 120)
    y, x = np.where(mask)
    assert len(x) > 1000, "Expected the complete epic title"
    return mask[y.min():y.max() + 1, x.min():x.max() + 1], pixels[mask].astype(int)


def verify():
    for phase in ("alpha", "beta"):
        source, source_colors = title(ROOT / "build/porting/reference-frost-1.21/run/frost-reference/screenshots/"
                                      f"multiphase-1.21-{phase}.png")
        target, target_colors = title(ROOT / "run/port-validation/client/screenshots/"
                                      f"multiphase-26.1-{phase}.png")
        assert np.array_equal(source, target), f"{phase}: title glyphs differ"
        difference = np.abs(source_colors - target_colors).max()
        assert difference <= 3, f"{phase}: epic title color changed by {difference}"
        print(f"{phase}: {source.sum()} title pixels have identical glyph positions, maximum RGB difference {difference}")


if __name__ == "__main__":
    verify()
