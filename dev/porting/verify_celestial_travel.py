"""Verify live player round-trip evidence and source assets, disclosing reference shutdown failure."""
from pathlib import Path
import json
import re

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
source = root / "build/porting/reference-frost-1.21"
logs = {}
for version, name in (("1.21", "client-travel-source-1.log"), ("26.1", "client-travel-1.log")):
    text = (root / "build/porting" / name).read_text(encoding="utf8", errors="replace")
    assert "PORT_TRAVEL_PLAYER_PASSED" in text, name
    rows = re.findall(r"PORT_TRAVEL_PLAYER_(OUTBOUND|RETURNED): dimension=([^\r\n]+), position=\(([^)]+)\)", text)
    assert len(rows) == 2, rows
    logs[version] = rows
    if version == "26.1":
        assert "All dimensions are saved" in text and "BUILD SUCCESSFUL" in text
    else:
        assert "Saving worlds" in text and "BUILD FAILED" in text
assert logs["1.21"] == logs["26.1"], logs
for name in ("travel-source-shutdown-threads.log", "travel-source-shutdown-threads-2.log"):
    text = (root / "build/porting" / name).read_text(encoding="utf8", errors="replace")
    assert "ChunkMap.processUnloads" in text and "MinecraftServer.stopServer" in text
for name in ("assets/anvilcraft/blockstates/celestial_back_gate.json", "assets/anvilcraft/models/block/back_gate.json",
             "assets/anvilcraft/textures/block/celestial_forging_anvil_gate_inner.png",
             "assets/anvilcraft/textures/block/celestial_forging_anvil_gate_inner.png.mcmeta"):
    assert (root / "src/main/resources" / name).read_bytes() == (source / "src/main/resources" / name).read_bytes(), name
masks = []
bounds = []
for version, directory in (("1.21", source / "run/frost-reference/screenshots"),
                            ("26.1", root / "run/port-validation/client/screenshots")):
    for scene in ("return-gate", "source-gate"):
        image = np.asarray(Image.open(directory / f"celestial-travel-{version}-{scene}.png").convert("RGB"), dtype=float)
        assert image.shape == (720, 1280, 3)
        if scene == "return-gate":
            red, green, blue = image[240:490, 510:770].transpose(2, 0, 1)
            mask = ((blue > red * 1.2) & (red > green * 1.2)) | ((red < 100) & (green < 80) & (blue < 120))
            y, x = np.where(mask)
            assert len(x) > 20000
            bounds.append([int(x.min() + 510), int(y.min() + 240), int(x.max() + 510), int(y.max() + 240)])
            masks.append(mask)
assert bounds[0] == bounds[1], bounds
report = {"round_trip": logs, "return_gate_bounds": bounds[0],
          "return_gate_mask_iou": float((masks[0] & masks[1]).sum() / (masks[0] | masks[1]).sum()),
          "target_shutdown": "all dimensions saved and exited successfully",
          "source_shutdown": "reference process stopped after repeated busy unload-queue snapshots; clean shutdown not verified",
          "limits": ["Native environmental rendering and independent portal animation phases are retained.",
                     "Builtin special dimensions and overworld-like collapse/reset entry guards are not implemented by this node."]}
assert report["return_gate_mask_iou"] > 0.99
output = root / "build/porting/celestial-travel-comparison.json"
output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf8")
print("Live round-trip coordinates and return-gate silhouette match; native save/exit passed; source shutdown limitation recorded")
print(output)
