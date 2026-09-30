"""Compare handbook fonts/layout, source assets and real bookmark interaction."""
import json
from pathlib import Path
import zipfile

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
native_run = root / "run/port-validation/client"
source_run = reference / "run/mun-reference"
native = json.loads((native_run / "handbook-pages-26.1.json").read_text(encoding="utf-8"))
source = json.loads((source_run / "handbook-pages-1.21.json").read_text(encoding="utf-8"))
assert native["style"] == source["style"] == {"contentWidth": 331, "contentHeight": 286, "labelWidth": 128, "bookmarks": 1}
for key in ("documents", "components", "directories", "navigation"):
    assert native[key] == source[key], key
for name, required in (
    ("client-handbook-style-2.log", "PORT_HANDBOOK_PAGES_PASSED"),
    ("client-handbook-style-reference.log", "BUILD SUCCESSFUL"),
    ("checks-handbook-style-final.log", "BUILD SUCCESSFUL"),
):
    log = (root / "build/porting" / name).read_text(encoding="utf-8", errors="replace")
    assert required in log and "> Task :runClient FAILED" not in log, name
cache = Path.home() / ".gradle/caches/modules-2/files-2.1/dev.anvilcraft.resource/ageratum-neoforge-1.21.1"
jar = next(cache.rglob("*121.jar"))
with zipfile.ZipFile(jar) as archive:
    for name in ("guide.png", "label_primary.png", "label_secondary.png", "label_bookmark.png"):
        relative = "assets/ageratum/textures/gui/guide/" + name
        assert (root / "src/main/resources" / relative).read_bytes() == archive.read(relative), name
metrics = {}
for index in range(7):
    actual = np.asarray(Image.open(native_run / f"screenshots/handbook-pages-26.1-{index}.png").convert("RGB"), dtype=float)
    expected = np.asarray(Image.open(source_run / f"screenshots/handbook-pages-1.21-{index}.png").convert("RGB"), dtype=float)
    assert actual.shape == expected.shape == (720, 1280, 3)
    difference = np.abs(actual[68:645, 307:976] - expected[68:645, 307:976])
    mean = float(difference.mean())
    assert mean < 2, (index, mean)
    assert np.all(actual == [7, 93, 127], axis=2).sum() > 1000, index
    metrics[index] = {"content_mean_rgb_error": mean, "different_pixels": int(np.any(difference != 0, axis=2).sum())}
report = {"layout": native["style"], "pages": 7, "content_metrics": metrics,
          "coverage": "Six bilingual directory pages, source link color, real add/bookmark navigation and restoration of pre-existing bookmarks.",
          "limits": "Native glyph assets and antialiasing can differ; this does not verify all sidebar folding, scrolling or structure-export behavior."}
(root / "build/porting/handbook-style-comparison.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print("Handbook style passed: source geometry/assets, seven paired captures and real bookmark navigation.")
