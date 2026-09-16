"""核对分配器实际模型、纹理、文本及配方的跨版本内容。"""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-1.21"
files = [p.relative_to(reference) for p in reference.glob("src/main/resources/assets/anvilcraft/**/*item_splitter*") if p.is_file()]
for path in files:
    assert (root / path).read_bytes() == (reference / path).read_bytes(), path
for path in [Path("src/main/resources/assets/anvilcraft/lang/zh_cn.json"),
             Path("src/generated/resources/assets/anvilcraft/lang/en_us.json")]:
    source = json.loads((reference / path).read_text(encoding="utf-8"))
    target = json.loads((root / path).read_text(encoding="utf-8"))
    for key, value in source.items():
        if "item_splitter" in key:
            assert target.get(key) == value, key
path = Path("src/generated/resources/data/anvilcraft/recipe/item_splitter.json")
source = json.loads((reference / path).read_text())
target = json.loads((root / path).read_text())
assert source["pattern"] == target["pattern"]
assert {key: value["item"] for key, value in source["key"].items()} == target["key"]
assert source["result"]["id"] == target["result"]["id"]
assert source["result"].get("count", 1) == target["result"].get("count", 1)
print(f"Item splitter: {len(files)} visual resources, 6 translations and recipe verified")
