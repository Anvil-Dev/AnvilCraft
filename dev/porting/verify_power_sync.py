"""Verify power chunk transport, per-component data and source HUD parity."""
from pathlib import Path
import json
import subprocess
import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
source = subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root, text=True).strip()
assert source == subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=reference, text=True).strip()


def text(name):
    return (root / "build/porting" / name).read_text(encoding="utf-8", errors="replace")


assert "All 5 required tests passed" in text("tests-power-sync-focused-1.log")
assert "All 749 required tests passed" in text("tests-power-sync-all.log")
assert "BUILD SUCCESSFUL" in text("tests-power-sync-all.log")
assert "PORT_POWER_SYNC_CLIENT_PASSED" in text("client-power-sync-1.log")
assert "PORT_POWER_SYNC_CLIENT_PASSED" in text("client-power-sync-reference-1.log")
assert "BUILD SUCCESSFUL" in text("client-power-sync-reference-1.log")
native = json.loads((root / "run/port-validation/client/power-sync-26.1.json").read_text(encoding="utf-8"))
expected = json.loads((reference / "run/mun-reference/power-sync-1.21.json").read_text(encoding="utf-8"))
assert native == expected, "Live transport, tooltip or transition mismatch"
assert native["remote_components"] == 1025 and native["replacement_components"] == 257
assert native["visibility_steps"] == [0.25, 0.5, 0.75, 1.0, 0.75, 0.5, 0.25, 0.0]
colors = {"normal": (85, 170, 255), "overloaded": (139, 32, 32)}
hud = {}
for name in ["normal", "overloaded", "outside", "hidden", "creative", "spectator"]:
    masks = []
    for version, folder in [("26.1", root / "run/port-validation/client/screenshots"),
                            ("1.21", reference / "run/mun-reference/screenshots")]:
        image = np.array(Image.open(folder / f"power-hud-{version}-{name}.png").convert("RGB"))
        region = image[656:678, 434:846]
        blue = np.all(region == (85, 170, 255), axis=2)
        red = np.all(region == (139, 32, 32), axis=2)
        mask = blue | red
        if name in colors:
            assert np.all(region[mask] == colors[name]), (name, version, "color")
            assert mask.sum() == 112, (name, version, mask.sum())
        else:
            assert not mask.any(), (name, version, "hidden")
        masks.append(mask)
    assert np.array_equal(*masks), (name, "source geometry")
    hud[name] = int(masks[0].sum())
report = {
    "source_commit": source, "focused_tests": 5, "full_required_tests": 749,
    "live_remote_components": 1025, "live_atomic_replacement_components": 257,
    "matching_tooltip_providers": 4, "matching_hud_pixel_counts": hud,
    "visibility_steps": native["visibility_steps"],
    "contracts": [
        "Tracking, split/broadcast and player synchronization use packets of at most 256 components; empty snapshots still produce one packet.",
        "Every component retains consumes/produces/stores/capacity/range/type and per-producer infinite power; native AABB precision is retained with doubles.",
        "Partial/out-of-order/duplicate chunks preserve the visible grid until complete; unchanged transmitter topology reuses its lines.",
        "Grid removal, world cleanup and legacy full snapshots clear pending data; invalid indices and oversized component lists are rejected.",
        "Sparse pending storage allocates according to received data, not an advertised total count.",
        "Producer, collector, charger and discharger tooltips use synchronized infinity and source unit formatting.",
        "The experience-bar indicator has source normal/overload colors, four-tick transitions and HUD/creative/spectator guards."
    ],
    "limits": [
        "Live transport uses synthetic server grid snapshots; existing power generation/balance behavior is not redefined by this node.",
        "HUD screenshots seed client attachment values to isolate rendering; the existing server attachment producer is unchanged.",
        "The first focused/client runs passed runtime assertions but failed fixture formatting; the final full run passed compilation and Checkstyle."
    ]
}
(root / "build/porting/power-sync-report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print(json.dumps(report, indent=2))
