"""Verify source dragon rod models, textures, protection labels, guide pages and devour denylist."""
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]


def source(path):
    return subprocess.check_output(["git", "show", f"dev/1.21/1.6:{path}"], cwd=ROOT)


def verify():
    paths = subprocess.check_output(["git", "ls-tree", "-r", "--name-only", "dev/1.21/1.6"], cwd=ROOT).decode().splitlines()
    count = 0
    for path in paths:
        if not path.startswith("src/main/resources/assets/anvilcraft/") or "dragon_rod" not in path:
            continue
        if "/models/" in path:
            assert json.loads(source(path)) == json.loads((ROOT / path).read_text(encoding="utf-8")), path
            count += 1
        elif "/textures/" in path:
            assert source(path) == (ROOT / path).read_bytes(), path
        elif "/ageratum/en_us/" in path or "/ageratum/zh_cn/" in path:
            expected = source(path).decode().replace("\r\n", "\n")
            actual = (ROOT / path).read_text(encoding="utf-8").replace("categories:\n  - tools\n", "")
            assert [line.rstrip() for line in expected.splitlines()] == [line.rstrip() for line in actual.splitlines()], path
    keys = ["screen.anvilcraft.dragon_rod.protect_containers", "screen.anvilcraft.dragon_rod.devour_containers",
            "tooltip.anvilcraft.property.protect_containers", "tooltip.anvilcraft.property.protect_containers.on",
            "tooltip.anvilcraft.property.protect_containers.off"]
    for path in ("src/generated/resources/assets/anvilcraft/lang/en_us.json",
                 "src/main/resources/assets/anvilcraft/lang/zh_cn.json"):
        expected = json.loads(source(path))
        actual = json.loads((ROOT / path).read_text(encoding="utf-8"))
        for key in keys:
            assert expected[key] == actual[key], (path, key)
    path = "src/generated/resources/data/anvilcraft/tags/block/devour_denylist.json"
    assert json.loads(source(path)) == json.loads((ROOT / path).read_text(encoding="utf-8")), path
    assert not (ROOT / "src/generated/resources/data/anvilcraft/tags/block/devour_blacklist.json").exists()
    print(f"{count} dragon rod models, texture assets, two guide pages, ten translations and denylist match source")


if __name__ == "__main__":
    verify()
