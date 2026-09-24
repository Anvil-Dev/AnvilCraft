"""Compare actual chart regions and authoritative interaction results on both versions."""
from pathlib import Path
import json
import re

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-frost-1.21/run/frost-reference/screenshots"
target = root / "run/port-validation/client/screenshots"
pattern = r"PORT_CFA_MAP_SERVER (\d+): (\[[^\]]+\])"
logs = {}
for version, name in [("source", "client-cfa-map-source-1.log"), ("target", "client-cfa-map-target-4.log")]:
    text = (root / "build/porting" / name).read_text(encoding="utf-8", errors="replace")
    assert "PORT_CFA_MAP_CAPTURED:" in text and "BUILD SUCCESSFUL" in text, name
    logs[version] = re.findall(pattern, text)
assert len(logs["source"]) == 9 and logs["source"] == logs["target"], logs
samples = []
for stage in (0, 3, 8):
    a = np.array(Image.open(reference / f"cfa-map-1.21-{stage}.png").convert("RGB"))[184:365, 542:780].astype(int)
    b = np.array(Image.open(target / f"cfa-map-26.1-{stage}.png").convert("RGB"))[184:365, 542:780].astype(int)
    delta = np.abs(a - b)
    row = {"stage": stage, "max_rgb_delta": int(delta.max()), "mean_rgb_delta": float(delta.mean())}
    samples.append(row)
    assert row["max_rgb_delta"] <= 4, row
report = {"source": "dev/1.21/1.6", "region": [542, 184, 780, 365], "samples": samples, "server_steps": logs["target"]}
output = root / "build/porting/cfa-map-visual-comparison.json"
output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print(json.dumps(samples))
print(output)
