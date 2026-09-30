"""Audit generated smear recipes against the current source branch."""
from pathlib import Path
import json
import subprocess

root = Path(__file__).resolve().parents[2]
prefix = "src/generated/resources/data/anvilcraft/recipe/block_smear/"
source_paths = subprocess.check_output(["git", "ls-tree", "-r", "--name-only", "dev/1.21/1.6", prefix], cwd=root).decode().splitlines()
native = {path.name: json.loads(path.read_text(encoding="utf-8")) for path in (root / prefix).glob("*.json")}
for path in source_paths:
    expected = json.loads(subprocess.check_output(["git", "show", "dev/1.21/1.6:" + path], cwd=root))
    assert native.get(Path(path).name) == expected, path
assert len(source_paths) == 122 and len(native) == 190
assert "grass_block.json" not in native
counts = {operation: sum(name.endswith("_" + operation + ".json") for name in native)
          for operation in ("dewax", "deoxidize", "strip")}
assert counts == {"dewax": 60, "deoxidize": 45, "strip": 23}, counts
for name, recipe in native.items():
    if not name.endswith(("_dewax.json", "_deoxidize.json", "_strip.json")):
        continue
    tool = recipe["inputs"][0]
    assert len(tool["blocks"]) == 5 and tool["properties"] == [{"face": "ceiling"}], name
    advancement = root / "src/generated/resources/data/anvilcraft/advancement/recipes/block_smear" / name
    assert advancement.is_file(), advancement
report = {"source_recipes_equal": len(source_paths), "native_recipes": len(native), "grinding_operations": counts,
          "native_additional_ids": sorted(set(native) - {Path(path).name for path in source_paths})}
(root / "build/porting/block-smear-comparison.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print(f"PASS: {len(source_paths)} exact source recipes; {len(native)} native recipes; {counts}")
