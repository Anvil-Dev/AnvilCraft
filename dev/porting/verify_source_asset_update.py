"""核对源增量提交的十五个素材；不将素材到位视为未移植装备玩法已完成。"""
import json
import subprocess
from pathlib import Path

root = Path(__file__).resolve().parents[2]
revision = "6c7b3f5b87705c5626a17143064064a0b855b187"
paths = subprocess.check_output(["git", "diff-tree", "--no-commit-id", "--name-only", "-r", revision], cwd=root).decode().splitlines()
for path in paths:
    original = subprocess.check_output(["git", "show", f"{revision}:{path}"], cwd=root)
    assert (root / path).read_bytes() == original, path
    if path.endswith(".json"):
        model = json.loads(original)
        for texture in model.get("textures", {}).values():
            if texture.startswith("#"):
                continue
            namespace, location = texture.split(":", 1)
            assert (root / f"src/main/resources/assets/{namespace}/textures/{location}.png").exists(), texture
inactive = json.loads((root / "src/main/resources/assets/anvilcraft/models/item/ionocraft_backpack_exhausted.json").read_text())
assert inactive["textures"]["layer0"] == "anvilcraft:item/ionocraft_backpack_off"
print(f"Source asset increment: {len(paths)} files match, model dependencies and backpack inactive texture verified")
