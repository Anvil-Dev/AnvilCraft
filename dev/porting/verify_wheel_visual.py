"""Compare live wheel backgrounds and check that sustained display does not accumulate GUI feedback."""
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]


def load(path):
    return np.array(Image.open(path).convert("RGB")).astype(int)


def disc_mask(pixels):
    height, width = pixels.shape[:2]
    y, x = np.indices((height, width))
    return (x - width / 2) ** 2 + (y - height / 2) ** 2 < (min(width, height) * 0.33 - 4) ** 2


def verify():
    for name in ("early", "settled", "resized", "restored", "reopened"):
        source = load(ROOT / "build/porting/reference-frost-1.21/run/frost-reference/screenshots/"
                      f"wheel-1.21-{name}.png")
        target = load(ROOT / f"run/port-validation/client/screenshots/wheel-26.1-{name}.png")
        assert source.shape == target.shape, (name, source.shape, target.shape)
        difference = np.abs(source - target)[disc_mask(source)]
        mean = difference.mean()
        percentile = np.percentile(difference, 95)
        assert mean < 3, f"{name}: wheel mean RGB difference {mean}"
        assert percentile <= 8, f"{name}: wheel 95th percentile RGB difference {percentile}"
        print(f"{name}: mean RGB difference {mean:.4f}, 95th percentile {percentile:.1f}, max {difference.max()}")
    early = load(ROOT / "run/port-validation/client/screenshots/wheel-26.1-early.png")
    settled = load(ROOT / "run/port-validation/client/screenshots/wheel-26.1-settled.png")
    temporal = np.abs(early - settled)[disc_mask(early)].mean()
    assert temporal < 1, f"Wheel changes after settling: {temporal}"
    print(f"Sustained display mean RGB change: {temporal:.4f}")


if __name__ == "__main__":
    verify()
