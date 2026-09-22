"""Verify chest equipment resources against the local 1.21 source."""

import json

from verify_headgear_resources import ROOT, geometry, normalize, source


def verify():
    for name in ("ionocraft_backpack", "two_to_one_smithing/weatherproof_spacesuit_chestplate"):
        path = f"src/generated/resources/data/anvilcraft/recipe/{name}.json"
        assert normalize(json.loads(source(path))) == normalize(json.loads((ROOT / path).read_bytes())), path
    names = ("entity/equipment/spacesuit", "entity/equipment/spacesuit_off",
             "entity/equipment/weatherproof_spacesuit", "entity/equipment/weatherproof_spacesuit_off",
             "item/ionocraft_backpack", "item/ionocraft_backpack_off",
             "item/weatherproof_spacesuit_chestplate", "item/weatherproof_spacesuit_chestplate_off")
    for name in names:
        path = f"src/main/resources/assets/anvilcraft/textures/{name}.png"
        assert source(path) == (ROOT / path).read_bytes(), path
    original = source("src/main/java/dev/dubhe/anvilcraft/client/renderer/entity/model/EquipmentModels.java").decode()
    original = original.split("private static LayerDefinition ionocraftBackpack()", 1)[1].split("private static LayerDefinition pocketsLeggings()", 1)[0]
    native = (ROOT / "src/main/java/dev/dubhe/anvilcraft/client/renderer/entity/model/EquipmentChestModel.java").read_text(encoding="utf-8")
    assert geometry(original) == geometry(native), "Chest geometry or UV differs"
    assert original.count(".mirror()") == native.count(".mirror()"), "Chest mirroring differs"
    for item in ("ionocraft_backpack", "weatherproof_spacesuit_chestplate"):
        definition = json.loads((ROOT / f"src/generated/resources/assets/anvilcraft/items/{item}.json").read_bytes())["model"]
        assert definition["property"] == "anvilcraft:equipment_powered"
        assert definition["on_true"]["model"] == f"anvilcraft:item/{item}_on"
        assert definition["on_false"]["model"] == f"anvilcraft:item/{item}"
    print("Verified 2 recipes, 8 textures, chest geometry/UV and powered item model branches")


if __name__ == "__main__":
    verify()
