"""核对仓储流体端口的方块资源及配方；物品液面与仓储 UI 待单独验收。"""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-1.21"
for name in ["storage_fluid_port", "hd_storage_fluid_port"]:
    for folder, extension in [("models/block", "json"), ("textures/block", "png")]:
        path = Path(f"src/main/resources/assets/anvilcraft/{folder}/{name}.{extension}")
        assert (root / path).read_bytes() == (reference / path).read_bytes(), path
state = "assets/anvilcraft/blockstates/storage_fluid_port.json"
assert json.loads((root / "src/main/resources" / state).read_text()) == json.loads(
    (reference / "src/generated/resources" / state).read_text())
for path in [Path("src/main/resources/assets/anvilcraft/lang/zh_cn.json"),
             Path("src/generated/resources/assets/anvilcraft/lang/en_us.json")]:
    source = json.loads((reference / path).read_text(encoding="utf-8"))
    target = json.loads((root / path).read_text(encoding="utf-8"))
    for key, value in source.items():
        if "storage_fluid_port" in key:
            assert target.get(key) == value, key
path = Path("src/generated/resources/data/anvilcraft/recipe/storage_fluid_port.json")
source = json.loads((reference / path).read_text())
target = json.loads((root / path).read_text())
assert source["pattern"] == target["pattern"]
assert {key: value["item"] for key, value in source["key"].items()} == target["key"]
assert source["result"]["id"] == target["result"]["id"]
assert source["result"].get("count", 1) == target["result"].get("count", 1)
print("Storage fluid port: block resources, translations and recipe verified; item and storage UI pending")
