"""Compare the three fixed GUI-scale-2 overlay scenes and their source textures."""
import json
import subprocess
from pathlib import Path

from PIL import Image, ImageChops

ROOT = Path(__file__).resolve().parents[2]
SOURCE = "dev/1.21/1.6"
REFERENCE = ROOT / "build/porting/reference-1.21"
TARGET_IMAGES = ROOT / "run/port-validation/client/screenshots"
REFERENCE_IMAGES = REFERENCE / "run/port-visual/screenshots"


def source_file(path: str) -> bytes:
    return subprocess.check_output(["git", "show", f"{SOURCE}:{path}"], cwd=ROOT)


def main() -> None:
    shared = "src/main/resources/assets/anvilcraft/textures/gui/misc/box_selection.png"
    assert (ROOT / shared).read_bytes() == source_file(shared), "Selection texture differs from source"
    overlay = "src/main/java/dev/dubhe/anvilcraft/client/support/TerminalRemoteOverlay.java"
    reference_overlay = subprocess.check_output(["git", "show", f"HEAD:{overlay}"], cwd=REFERENCE)
    assert reference_overlay == source_file(overlay), "Refresh the reference overlay before comparing"
    results = {}
    for size in (4, 5, 6):
        texture = f"src/main/resources/assets/anvilcraft/textures/gui/misc/storage_station/remote_{size}.png"
        assert (ROOT / texture).read_bytes() == source_file(texture), texture
        reference = Image.open(REFERENCE_IMAGES / f"terminal-overlay-1.21-{size}.png").convert("RGB")
        target = Image.open(TARGET_IMAGES / f"terminal-overlay-26.1-{size}.png").convert("RGB")
        assert reference.size == target.size == (1280, 720), "Scene dimensions changed"
        # Terminal inventory slot 9, GUI scale 2: every size shares the lower-right anchor.
        box = (372 - 36 * (size - 4), 154 - 36 * (size - 4), 532, 346)
        diff = ImageChops.difference(reference.crop(box), target.crop(box))
        deltas = [max(pixel) for pixel in diff.get_flattened_data()]
        results[str(size)] = {
            "region": box,
            "changed_pixels": sum(delta > 0 for delta in deltas),
            "pixels": len(deltas),
            "max_channel_difference": max(deltas),
            "pixels_over_3": sum(delta > 3 for delta in deltas),
        }
        # Preserve the 26.1 text/item pipeline; this fixture differs by at most 3/255 in brightness.
        assert max(deltas) <= 3, f"Size {size}: layout, layering or color difference exceeds the measured baseline"
    output = ROOT / "build/porting/terminal-overlay-comparison.json"
    output.write_text(json.dumps(results, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(results, indent=2))


if __name__ == "__main__":
    main()
