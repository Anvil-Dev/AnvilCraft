"""比较固定 GUI 场景中的大图标和普通图标，保留改动前后的像素误差。"""
import json
from pathlib import Path

from PIL import Image

root = Path(__file__).resolve().parents[2]
current = Image.open(root / "run/port-validation/client/screenshots/storage-port-item-26.1-0.png").convert("RGB")
reference = Image.open(root / "build/porting/reference-1.21/run/port-visual/screenshots/storage-port-item-1.21-0.png").convert("RGB")
before = Image.open(root / "build/porting/screenshots/storage-port-item/storage-port-item-26.1-0.png").convert("RGB")


def error(image, bounds):
    left, top, right, bottom = bounds
    differences = [abs(a - b) for a, b in zip(image.crop(bounds).tobytes(), reference.crop(bounds).tobytes())]
    return round(sum(differences) / ((right - left) * (bottom - top) * 3), 4)


metrics = []
for index in range(6):
    x = 80 + 200 * index
    entry = {"icon": index, "large_after": error(current, (x, 120, x + 96, 216)),
             "normal_after": error(current, (x, 288, x + 32, 320))}
    if index < 5:
        entry["large_before"] = error(before, (x, 120, x + 96, 216))
    metrics.append(entry)
output = root / "build/porting/scaled-gui-item-metrics.json"
output.write_text(json.dumps(metrics, indent=2) + "\n", encoding="utf-8")
print(json.dumps(metrics))
