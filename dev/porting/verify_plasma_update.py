"""核对喷流增量的配置文本与空锅接口入口。行为由 PlasmaJetPortTests 验证。"""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-1.21"
path = Path("src/generated/resources/assets/anvilcraft/lang/en_us.json")
source = json.loads((reference / path).read_text(encoding="utf-8"))
target = json.loads((root / path).read_text(encoding="utf-8"))
keys = [key for key in source if key.startswith("anvilcraft.configuration.plasma_jets_")]
keys.append("config.jade.plugin_anvilcraft.overflow_disposal_fluid_tank")
for key in keys:
    assert target.get(key) == source[key], key
injections = json.loads((root / "src/main/resources/interface_injections.json").read_text())
assert "dev/dubhe/anvilcraft/api/block/IEmptyCauldron" in injections["net/minecraft/world/level/block/CauldronBlock"]
mixins = json.loads((root / "src/main/resources/anvilcraft.mixins.json").read_text())
assert "EmptyCauldronMixin" in mixins["mixins"]
print(f"Plasma update: {len(keys)} source text entries and empty-cauldron injection verified")
