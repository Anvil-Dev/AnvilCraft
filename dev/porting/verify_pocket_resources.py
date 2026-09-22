"""Verify pocket leggings resources and geometry against the local 1.21 branch."""

import json

from verify_headgear_resources import ROOT, geometry, normalize, source


def verify():
    for name in ("pockets_leggings", "two_to_one_smithing/weatherproof_spacesuit_leggings"):
        path = f"src/generated/resources/data/anvilcraft/recipe/{name}.json"
        assert normalize(json.loads(source(path))) == normalize(json.loads((ROOT / path).read_bytes())), path
    for name in ("pockets_leggings", "weatherproof_spacesuit_leggings"):
        for category in ("item", "gui/misc/equipment"):
            path = f"src/main/resources/assets/anvilcraft/textures/{category}/{name}.png"
            assert source(path) == (ROOT / path).read_bytes(), path
    for name in ("spacesuit", "weatherproof_spacesuit"):
        original = source(f"src/main/resources/assets/anvilcraft/textures/entity/equipment/{name}.png")
        assert original == (ROOT / f"src/main/resources/assets/anvilcraft/textures/entity/equipment/humanoid_leggings/{name}.png").read_bytes()
    original = source("src/main/java/dev/dubhe/anvilcraft/client/renderer/entity/model/EquipmentModels.java").decode()
    original = original.split("private static LayerDefinition pocketsLeggings()", 1)[1].split("private static LayerDefinition bufferBoots()", 1)[0]
    native = (ROOT / "src/main/java/dev/dubhe/anvilcraft/client/renderer/entity/model/EquipmentLeggingsModel.java").read_text(encoding="utf-8")
    assert geometry(original) == geometry(native), "Leggings geometry or UV differs"
    assert original.count(".mirror()") == native.count(".mirror()"), "Leggings UV mirroring differs"
    print("Verified 2 recipes, 6 texture mappings, leggings geometry and UV mirroring against local 1.21")


if __name__ == "__main__":
    verify()
