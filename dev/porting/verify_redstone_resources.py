"""Check the first port batch against its fixed 1.21 asset and recipe baseline."""

import json
from pathlib import Path
import subprocess


ROOT = Path(__file__).resolve().parents[2]
SOURCE = "eb2dadbc4d7fb3a0476765f6313e1667a758ed66"
NAMES = ("big_red_button", "redstone_dice")


def source(path):
    return subprocess.check_output(["git", "show", f"{SOURCE}:{path}"], cwd=ROOT)


def normalize(value):
    if isinstance(value, list):
        return [normalize(entry) for entry in value]
    if isinstance(value, dict):
        if "id" in value and set(value) <= {"id", "count"}:
            return {"id": value["id"], "count": value.get("count", 1)}
        if set(value) == {"item"}:
            return value["item"]
        if set(value) == {"tag"}:
            return "#" + value["tag"]
        return {key: normalize(entry) for key, entry in value.items()}
    return value


def main():
    count = 0
    paths = subprocess.check_output(
        ["git", "ls-tree", "-r", "--name-only", SOURCE, "src/main/resources"], cwd=ROOT
    ).decode().splitlines()
    for path in paths:
        if any(name in path for name in NAMES):
            assert (ROOT / path).read_bytes() == source(path), f"Asset differs: {path}"
            count += 1
    for name in NAMES:
        blockstate = f"assets/anvilcraft/blockstates/{name}.json"
        assert json.loads((ROOT / "src/main/resources" / blockstate).read_bytes()) == json.loads(
            source("src/generated/resources/" + blockstate)
        ), blockstate
        item = json.loads((ROOT / f"src/generated/resources/assets/anvilcraft/items/{name}.json").read_bytes())
        assert item["model"] == {"type": "minecraft:model", "model": f"anvilcraft:block/{name}"}, name
        for folder in ("recipe", "loot_table/blocks"):
            path = f"src/generated/resources/data/anvilcraft/{folder}/{name}.json"
            assert normalize(json.loads((ROOT / path).read_bytes())) == normalize(json.loads(source(path))), path
        for lang, location in (("zh_cn", "main"), ("en_us", "generated")):
            path = f"src/{location}/resources/assets/anvilcraft/lang/{lang}.json"
            original = json.loads(source(path))
            target = json.loads((ROOT / path).read_bytes())
            for key in original:
                if name in key:
                    assert target[key] == original[key], (lang, key)
        count += 6
    print(f"PASS: {count} asset/resource groups match the fixed source baseline")


if __name__ == "__main__":
    main()
