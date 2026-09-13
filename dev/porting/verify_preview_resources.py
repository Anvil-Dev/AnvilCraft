"""核对已接通的预览标签和配置翻译；显式记录尚未迁移的方块。"""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-1.21"
tag = Path("src/generated/resources/data/anvilcraft/tags/block/placement_preview.json")
source_values = set(json.loads((reference / tag).read_text(encoding="utf-8"))["values"])
target_values = set(json.loads((root / tag).read_text(encoding="utf-8"))["values"])
pending = set()
assert source_values - target_values == pending
assert not target_values - source_values
checked = 0
for path in [Path("src/main/resources/assets/anvilcraft/lang/zh_cn.json"),
             Path("src/generated/resources/assets/anvilcraft/lang/en_us.json")]:
    source = json.loads((reference / path).read_text(encoding="utf-8"))
    target = json.loads((root / path).read_text(encoding="utf-8"))
    for key, value in source.items():
        if "multi_part_preview" in key:
            assert target.get(key) == value, (key, target.get(key), value)
            checked += 1
print(f"Preview resources: {len(target_values)} shared tag entries, {checked} translations; pending: {sorted(pending)}")
