"""核对仓储流体界面的提示、字体和浮窗底图资源。"""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-1.21"
keys = ["screen.anvilcraft.storage.fluid_amount", "screen.anvilcraft.storage.fluid.bucket_missing",
        "screen.anvilcraft.storage.fluid.not_enough"]
for path in [Path("src/main/resources/assets/anvilcraft/lang/zh_cn.json"),
             Path("src/generated/resources/assets/anvilcraft/lang/en_us.json")]:
    source = json.loads((reference / path).read_text(encoding="utf-8"))
    target = json.loads((root / path).read_text(encoding="utf-8"))
    for key in keys:
        assert target[key] == source[key], key
for path in [Path("src/main/resources/assets/anvilcraft/textures/gui/sprites/flex_button/shaded_1px.png"),
             Path("src/main/resources/assets/anvilcraft/textures/font/small.png")]:
    assert (root / path).read_bytes() == (reference / path).read_bytes(), path
for path in [Path("src/main/resources/assets/anvilcraft/textures/gui/sprites/flex_button/shaded_1px.png.mcmeta"),
             Path("src/main/resources/assets/anvilcraft/font/small.json")]:
    assert json.loads((root / path).read_text()) == json.loads((reference / path).read_text()), path
print("Storage fluid UI: translations, small font and nine-slice notice resources match source")
