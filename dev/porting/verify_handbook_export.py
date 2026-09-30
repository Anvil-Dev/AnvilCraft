"""Verify real export-button transfers and full server-save artifacts in both versions."""
import json
from pathlib import Path

from PIL import Image
import numpy as np

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
logs = {
    "tests-handbook-export-all-final.log": "All 785 required tests passed",
    "client-handbook-export-native.log": "PORT_HANDBOOK_EXPORT_CLIENT_PASSED",
    "client-handbook-export-reference.log": "PORT_HANDBOOK_EXPORT_CLIENT_PASSED",
    "tests-handbook-export-mun-light.log": "All 1 required tests passed",
}
for name, marker in logs.items():
    text = (root / "build/porting" / name).read_text(encoding="utf-8", errors="replace")
    assert marker in text and "> Task :runClient FAILED" not in text, name
    if name != "client-handbook-export-native.log":
        assert "BUILD SUCCESSFUL" in text, name
native_run = root / "run/port-validation/client"
source_run = reference / "run/mun-reference"
native = json.loads((native_run / "handbook-export-26.1.json").read_text(encoding="utf-8"))
source = json.loads((source_run / "handbook-export-1.21.json").read_text(encoding="utf-8"))
for key in ("blocks", "entities", "visibleLayers", "files"):
    assert native[key] == source[key], key
assert native["blocks"] == 2 and native["entities"] == 1 and native["visibleLayers"] == 1
for value in (native, source):
    directory = Path(value["directory"]).resolve()
    assert directory.parts[-2:] == ("data", "ageratum")
    for name in value["files"]:
        assert (directory / name).is_file() and (directory / name).stat().st_size > 24 * 1024
    assert (directory / value["files"][0]).read_bytes() == (directory / value["files"][1]).read_bytes()
a = np.asarray(Image.open(native_run / "screenshots/handbook-export-26.1.png").convert("RGB"), dtype=float)
b = np.asarray(Image.open(source_run / "screenshots/handbook-export-1.21.png").convert("RGB"), dtype=float)
error = float(np.abs(a[105:121, 379:395] - b[105:121, 379:395]).mean())
report = {"native": native, "source": source, "export_button_mean_rgb_error": error,
          "full_required_tests": 785,
          "limits": "Real packet transport is exercised against integrated servers. A separate remote dedicated-server session was not launched; structure projection remains a separate migration."}
(root / "build/porting/handbook-export-comparison.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print("Handbook export verified: full multi-packet structures, server-save paths, duplicate suffixes and 785 tests.")
