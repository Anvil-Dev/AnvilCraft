"""Audit source handbook assets, native ID adaptations and paired directory behavior."""
import json
from pathlib import Path
import re

root = Path(__file__).resolve().parents[2]
base = root / "src/main/resources/assets/anvilcraft/ageratum"
reference = root / "build/porting/reference-mun-1.21"
source_base = reference / "src/main/resources/assets/anvilcraft/ageratum"
native_files = {p.relative_to(base).as_posix(): p for p in base.rglob("*") if p.is_file()}
source_files = {p.relative_to(source_base).as_posix(): p for p in source_base.rglob("*") if p.is_file()}
assert native_files.keys() == source_files.keys() and len(native_files) == 494
adapted = []
for name, path in native_files.items():
    expected = source_files[name].read_bytes()
    if name.endswith(".md"):
        expected = expected.replace(b'minecraft:chain"', b'minecraft:iron_chain"')
        expected = re.sub(rb"anvilcraft:reinforced_concrete_([a-z_]+)", rb"anvilcraft:\1_reinforced_concrete", expected)
        expected = expected.replace(b"../007_struct/anvil_processing.md", b"../007_struct/000_anvil_processing.md")
        if name in {"en_us/001_feature/201_properties.md", "zh_cn/000_process/004.md", "zh_cn/000_process/010.md",
                    "zh_cn/001_feature/201_properties.md", "zh_cn/005_tool/101_dragon_rod.md",
                    'en_us/002_material/122_cruse_gold.md',
                    'en_us/004_block/215_large_cauldron.md',
                    'en_us/004_block/402_teleportation.md',
                    'zh_cn/001_feature/402_gamma_laser.md',
                    'zh_cn/002_material/122_cruse_gold.md',
                    'zh_cn/004_block/311_transcendence_smithing_table.md',
                    'zh_cn/004_block/402_teleportation.md',
                    'zh_cn/008_recipe/001_auto_recipe.md'}:
            newline = b"\r\n" if b"\r\n" in expected else b"\n"
            expected = newline.join(line.rstrip(b" \t") for line in expected.splitlines()).rstrip(b"\r\n") + newline
        for target in re.findall(r"!?\[[^\]]*\]\(([^)]+)\)", path.read_text(encoding="utf-8")):
            target = target.split("#")[0].strip().split(' "')[0]
            if target and ":" not in target:
                assert (path.parent / target).exists(), (name, target)
    assert path.read_bytes() == expected, name
    if expected != source_files[name].read_bytes():
        adapted.append(name)
for name in ("client-handbook-pages-native-final.log", "client-handbook-pages-reference-final.log"):
    log = (root / "build/porting" / name).read_text(encoding="utf-8", errors="replace")
    assert "BUILD SUCCESSFUL" in log and "PORT_HANDBOOK_PAGES_PASSED" in log, name
native = json.loads((root / "run/port-validation/client/handbook-pages-26.1.json").read_text(encoding="utf-8"))
source = json.loads((reference / "run/mun-reference/handbook-pages-1.21.json").read_text(encoding="utf-8"))
for key in ("documents", "components", "directories", "navigation"):
    assert native[key] == source[key], key
assert native["documents"] == 416 and native["components"] == 4816 and len(native["directories"]) == 6
assert not native["missingItems"]
report = {
    "resource_files": 494, "documents": 416, "components": 4816,
    "adapted_files": sorted(adapted), "directory_pages": list(native["directories"]),
    "native_missing_recipes": native["missingRecipes"],
    "source_missing_recipes": source["missingRecipes"],
    "native_only_missing_recipes": sorted(set(native["missingRecipes"]) - set(source["missingRecipes"])),
    "limits": "Resource/directory parity does not close the recipe, structure-export or complete-handbook rendering audits."
}
(root / "build/porting/handbook-resources-comparison.json").write_text(
    json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"Resource and directory checks passed; {len(report['native_only_missing_recipes'])} native-only recipe references remain.")
