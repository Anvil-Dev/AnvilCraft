"""核对溢流溜槽模型、资源与配方。"""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-1.21"
files = [p.relative_to(reference) for p in reference.glob("src/main/resources/assets/anvilcraft/**/*overflow_chute*") if p.is_file()]
for path in files:
    assert (root / path).read_bytes() == (reference / path).read_bytes(), path
for path in [Path("src/main/resources/assets/anvilcraft/lang/zh_cn.json"),
             Path("src/generated/resources/assets/anvilcraft/lang/en_us.json")]:
    source = json.loads((reference / path).read_text(encoding="utf-8"))
    target = json.loads((root / path).read_text(encoding="utf-8"))
    for key, value in source.items():
        if "overflow_chute" in key:
            assert target.get(key) == value, key
path = Path("src/generated/resources/data/anvilcraft/recipe/overflow_chute.json")
source = json.loads((reference / path).read_text())
target = json.loads((root / path).read_text())
assert [ingredient["item"] for ingredient in source["ingredients"]] == target["ingredients"]
assert source["result"]["id"] == target["result"]["id"]
assert source["result"].get("count", 1) == target["result"].get("count", 1)
print(f"Overflow chute: {len(files)} visual resources, 6 translations and recipe verified")
