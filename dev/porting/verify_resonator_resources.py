"""Compare resonator geometry, display transforms, textures and charge math with the local source branch."""
from pathlib import Path
import json
import re
import subprocess

ROOT = Path(__file__).resolve().parents[2]


def source(path):
    return subprocess.check_output(["git", "show", f"dev/1.21/1.6:{path}"], cwd=ROOT)


def verify():
    paths = subprocess.check_output(["git", "ls-tree", "-r", "--name-only", "dev/1.21/1.6"], cwd=ROOT).decode().splitlines()
    count = 0
    for path in paths:
        if not path.startswith("src/main/resources/assets/anvilcraft/models/item/"):
            continue
        if not any(part in Path(path).stem for part in ("resonator", "resonance_")):
            continue
        expected = json.loads(source(path))
        actual = json.loads((ROOT / path).read_text(encoding="utf-8"))
        for key in ("elements", "textures", "parent"):
            assert expected.get(key) == actual.get(key), (path, key)
        for key, value in expected.get("display", {}).items():
            assert value == actual["display"].get(key), (path, key)
        for texture in expected.get("textures", {}).values():
            if texture.startswith("anvilcraft:"):
                texture_path = "src/main/resources/assets/anvilcraft/textures/" + texture.split(":", 1)[1] + ".png"
                assert source(texture_path) == (ROOT / texture_path).read_bytes(), texture_path
        count += 1
    path = "src/main/java/dev/dubhe/anvilcraft/client/renderer/item/ItemUseAnimationTransform.java"
    normalize = lambda value: re.sub(r"\s+", "", value.replace("ItemUseAnimationTransform.", ""))
    assert normalize(source(path).decode()) == normalize((ROOT / path).read_text(encoding="utf-8")), "charge transforms differ"
    print(f"{count} resonator models, referenced textures and charge transforms match source")


if __name__ == "__main__":
    verify()
