"""Verify exact source assets and the native client's bake/reload report."""
from pathlib import Path
import hashlib
import json
import subprocess

root = Path(__file__).resolve().parents[2]
commit = "3e80800061a28dbf639ccd013408e3a6b3e71b77"
report = json.loads((root / "run/port-validation/client/civilization-assets-26.1.json").read_text(encoding="utf-8"))
assert report["loads"] >= 2 and report["faces"] == 136, report
for relative, actual_hash in report["resources"].items():
    path = "src/main/resources/assets/anvilcraft/" + relative
    expected = subprocess.check_output(["git", "show", commit + ":" + path], cwd=root)
    assert (root / path).read_bytes() == expected, path
    assert actual_hash == hashlib.sha256(expected).hexdigest(), path
assert set(report["textures"]) == {"anvilcraft:block/celestial_forging_anvil_top", "anvilcraft:block/celestial_forging_anvil_rings"}
assert set(report["dimensions"]) == {"textures/block/celestial_forging_anvil_rings.png", "textures/item/civilization_catalyst.png"}
print("PASS: three byte-identical source resources, 136 baked faces, both PNGs decoded, initial load and resource reload")
