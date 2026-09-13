"""Verify migrated dynamic models and their direct texture references."""

import json
from pathlib import Path

from verify_redstone_resources import ROOT, source


MODELS = (
    "advanced_comparator_indicator", "pump_piston_1", "pump_piston_2", "heliostats_head", "heliostats_head_sunflower",
    "charge_collector_head", "heat_collector_head", "infinite_collector_head", "void_energy_collector_head",
    "creative_generator_head", "fe_collector_head", "control_valve_handwheel", "check_valve_arm",
)


def main():
    textures = set()
    for name in MODELS:
        path = f"src/main/resources/assets/anvilcraft/models/block/{name}.json"
        original = json.loads(source(path))
        target = json.loads((ROOT / path).read_bytes())
        if name == "creative_generator_head":
            # 目标版本已有的粒子纹理补全；模型各面的真实纹理不变。
            assert target["textures"]["particle"] == target["textures"]["head"]
            target["textures"].pop("particle")
        assert target == original, f"Model differs: {name}"
        textures.update(value for value in target.get("textures", {}).values() if value.startswith("anvilcraft:"))
    for texture in textures:
        path = "src/main/resources/assets/anvilcraft/textures/" + texture.split(":", 1)[1] + ".png"
        assert (ROOT / path).read_bytes() == source(path), f"Texture differs: {texture}"
    print(f"PASS: {len(MODELS)} dynamic models and {len(textures)} textures match source")


if __name__ == "__main__":
    main()
