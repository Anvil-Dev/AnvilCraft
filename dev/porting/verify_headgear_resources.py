"""Verify headgear recipes, textures and helmet geometry against local 1.21."""

import json
from pathlib import Path
import re
import subprocess


ROOT = Path(__file__).resolve().parents[2]
SOURCE = "dev/1.21/1.6"


def source(path):
    return subprocess.check_output(["git", "show", f"{SOURCE}:{path}"], cwd=ROOT)


def normalize(value):
    if isinstance(value, list):
        return [normalize(item) for item in value]
    if isinstance(value, dict):
        value = {key: item for key, item in value.items() if not (key == "count" and item == 1)}
        if set(value) in ({"id"}, {"item"}):
            return next(iter(value.values()))
        if set(value) == {"tag"}:
            return "#" + value["tag"]
        return {key: normalize(item) for key, item in value.items()}
    return value


def geometry(code):
    boxes = re.findall(r"\.addBox\((.*?)\)\)", code, re.S)
    coordinates = [[float(value) for value in re.findall(r"-?\d+(?:\.\d+)?", box)] for box in boxes]
    textures = re.findall(r"\.texOffs\((\d+), (\d+)\)", code)
    return coordinates, textures


def verify():
    for name in ("breathing_helmet", "weatherproof_core", "two_to_one_smithing/weatherproof_spacesuit_helmet"):
        path = f"src/generated/resources/data/anvilcraft/recipe/{name}.json"
        assert normalize(json.loads(source(path))) == normalize(json.loads((ROOT / path).read_bytes())), path
    for name in ("entity/equipment/spacesuit", "entity/equipment/weatherproof_spacesuit",
                 "item/breathing_helmet", "item/weatherproof_spacesuit_helmet", "item/weatherproof_core"):
        path = f"src/main/resources/assets/anvilcraft/textures/{name}.png"
        assert source(path) == (ROOT / path).read_bytes(), path
    original = source("src/main/java/dev/dubhe/anvilcraft/client/renderer/entity/model/EquipmentModels.java").decode()
    original = original.split("private static LayerDefinition breathingHelmet()", 1)[1].split(
        "private static LayerDefinition ionocraftBackpack()", 1)[0]
    native = (ROOT / "src/main/java/dev/dubhe/anvilcraft/client/renderer/entity/model/EquipmentHelmetModel.java").read_text(encoding="utf-8")
    assert geometry(original) == geometry(native), "Helmet cubes or UV coordinates differ"
    print("Verified 3 recipes, 5 textures, and helmet cube/UV geometry against local 1.21")


if __name__ == "__main__":
    verify()
