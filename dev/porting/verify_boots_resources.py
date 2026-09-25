"""Compare buffer boots assets, recipes and geometry with the local 1.21 source."""

import json

from verify_headgear_resources import ROOT, geometry, normalize, source


def verify():
    for name in ("buffer_boots", "two_to_one_smithing/weatherproof_spacesuit_boots"):
        path = f"src/generated/resources/data/anvilcraft/recipe/{name}.json"
        assert normalize(json.loads(source(path))) == normalize(json.loads((ROOT / path).read_bytes())), path
    for name in ("entity/equipment/spacesuit", "entity/equipment/weatherproof_spacesuit",
                 "item/buffer_boots", "item/weatherproof_spacesuit_boots"):
        path = f"src/main/resources/assets/anvilcraft/textures/{name}.png"
        assert source(path) == (ROOT / path).read_bytes(), path
    original = source("src/main/java/dev/dubhe/anvilcraft/client/renderer/entity/model/EquipmentModels.java").decode()
    original = original.split("private static LayerDefinition bufferBoots()", 1)[1]
    native = (ROOT / "src/main/java/dev/dubhe/anvilcraft/client/renderer/entity/model/EquipmentBootsModel.java").read_text(encoding="utf-8")
    assert geometry(original) == geometry(native), "Boot geometry or UV differs"
    assert original.count(".mirror()") == native.count(".mirror()"), "Boot UV mirroring differs"
    shader = "src/main/resources/assets/anvilcraft/shaders/core/equipment_charge.fsh"
    assert source(shader).decode().split("void main()", 1)[1] == (ROOT / shader).read_text().split("void main()", 1)[1]
    print("Verified 2 recipes, 4 textures, boot geometry/UV mirroring, and charge shader against local 1.21")


if __name__ == "__main__":
    verify()
