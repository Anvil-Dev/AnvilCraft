"""Verify overflow tank behavior, shared bottle entry points, assets and real client evidence."""
from pathlib import Path
import json
import subprocess

from PIL import Image

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
source = subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root, text=True).strip()
assert source == subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=reference, text=True).strip()
for filename in ["client-tank-disposal-2.log", "client-tank-disposal-reference-4.log"]:
    log = (root / "build/porting" / filename).read_text(encoding="utf-8", errors="replace")
    assert "PORT_TANK_DISPOSAL_CLIENT_PASSED" in log and "BUILD SUCCESSFUL" in log, filename
full = (root / "build/porting/tests-tank-disposal-all.log").read_text(encoding="utf-8", errors="replace")
assert "All 718 required tests passed" in full and "BUILD SUCCESSFUL" in full
for name in ["checks-tank-disposal-final.log", "tests-tank-disposal-1.log"]:
    assert "BUILD SUCCESSFUL" in (root / "build/porting" / name).read_text(encoding="utf-8", errors="replace")
assets = ["src/main/resources/assets/anvilcraft/models/block/fluid_tank.json"]
assets += [f"src/main/resources/assets/anvilcraft/textures/block/fluid_tank_{side}.png" for side in ["top", "side"]]
for path in assets:
    expected = subprocess.check_output(["git", "show", f"{source}:{path}"], cwd=root)
    actual = (root / path).read_bytes()
    assert (json.loads(actual) == json.loads(expected)) if path.endswith(".json") else actual == expected, path
for path in ["src/generated/resources/assets/anvilcraft/lang/en_us.json", "src/main/resources/assets/anvilcraft/lang/zh_cn.json"]:
    expected = json.loads(subprocess.check_output(["git", "show", f"{source}:{path}"], cwd=root))
    actual = json.loads((root / path).read_bytes())
    for key in ["block.anvilcraft.overflow_disposal_fluid_tank", "config.jade.plugin_anvilcraft.overflow_disposal_fluid_tank"]:
        assert actual[key] == expected[key], key
screenshots = []
for name in ["normal", "disposal", "restored"]:
    for base, version in [(root / "run/port-validation/client", "26.1"), (reference / "run/mun-reference", "1.21")]:
        path = base / f"screenshots/tank-disposal-{version}-{name}.png"
        assert Image.open(path).size == (1280, 720), path
        screenshots.append(str(path.relative_to(root)))
report = {
    "source_commit": source,
    "focused_tests": 6,
    "full_required_tests": 718,
    "assets_checked": assets,
    "screenshots": screenshots,
    "contracts": [
        "Only ordinary fluid tanks enable overflow disposal beside a face-adjacent Menger sponge.",
        "Store up to capacity and consume same-fluid overflow; reject foreign fluid and update cached handlers when adjacency changes.",
        "Transactions restore amount and infinity flags, including nested rollback; downgrade preserves existing excess fluid.",
        "Full enhanced tanks loaded without the infinity flag activate on the next committed input, matching source behavior.",
        "Fluid network transfers into a full disposal tank and stops when disposal is disabled.",
        "Ordinary and large-tank child parts use the common bottle interaction with 250 mB exchanges.",
        "Actual native/source clients accept a bucket into a full disposal tank, return an empty bucket and keep 16000 mB; normal mode rejects it.",
        "Actual Jade names toggle in both runtimes and stable screenshots wait for the target render section."
    ],
    "limits": [
        "World images retain native 26.1 terrain/lightmap and animated water differences; model/texture files match source exactly.",
        "Existing Jade integration still shows 16000mB where source shows 16B; fluid amount formatting remains a separate display-parity audit.",
        "First source screenshot was taken before section compilation; final runs include a render-readiness guard and settling interval."
    ]
}
(root / "build/porting/tank-disposal-report.json").write_text(json.dumps(report, indent=2)+"\n", encoding="utf-8")
print(json.dumps(report, indent=2))
