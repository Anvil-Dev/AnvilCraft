"""核对仓储端口本体资源和组合物品模型；渲染仍须实景检查。"""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-1.21"
for name in ["storage_port", "hd_storage_port"]:
    for folder, extension in [("models/block", "json"), ("textures/block", "png")]:
        path = Path(f"src/main/resources/assets/anvilcraft/{folder}/{name}.{extension}")
        assert (root / path).read_bytes() == (reference / path).read_bytes(), path
state = "assets/anvilcraft/blockstates/storage_port.json"
assert json.loads((root / "src/main/resources" / state).read_text()) == json.loads(
    (reference / "src/generated/resources" / state).read_text())
for path in [Path("src/main/resources/assets/anvilcraft/lang/zh_cn.json"),
             Path("src/generated/resources/assets/anvilcraft/lang/en_us.json")]:
    source = json.loads((reference / path).read_text(encoding="utf-8"))
    target = json.loads((root / path).read_text(encoding="utf-8"))
    for key, value in source.items():
        if "storage_port" in key and "consolidator" not in key:
            assert target.get(key) == value, (key, target.get(key), value)
path = Path("src/generated/resources/data/anvilcraft/recipe/storage_port.json")
source = json.loads((reference / path).read_text())
target = json.loads((root / path).read_text())
assert source["pattern"] == target["pattern"]
assert {key: value["item"] for key, value in source["key"].items()} == target["key"]
assert source["result"] == target["result"]
definition = json.loads((root / "src/generated/resources/assets/anvilcraft/items/storage_port.json").read_text())["model"]
assert definition["type"] == "minecraft:composite"
assert definition["models"] == [
    {"type": "minecraft:model", "model": "anvilcraft:block/storage_port"},
    {"type": "minecraft:special", "base": "anvilcraft:block/storage_port", "model": {"type": "anvilcraft:storage_port"}}
]
print("Storage port: block resources, composite item model, translations and recipe verified")
