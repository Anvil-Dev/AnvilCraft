"""Compare the placement rule data and cauldron assets with the fixed source."""

import json
from pathlib import Path

from verify_redstone_resources import ROOT, source


PENDING_BLOCKS = {"giant_monolith_core", "glass_pipe_corner", "glass_pipe_node", "glass_pipe_straight"}
NATIVE_ADDITIONS = {
    "pale_oak_door", "pale_oak_wall_hanging_sign", "pale_oak_wall_sign", "potted_closed_eyeblossom",
    "potted_golden_dandelion", "potted_open_eyeblossom", "potted_pale_oak_sapling", "resin_clump", "wildflowers",
}


def rules(root):
    return {p.relative_to(root).as_posix(): json.loads(p.read_bytes()) for p in root.rglob("*.json")
            if "/block_placement_rules/" in p.as_posix()}


def main():
    # 参考检出的正式生成数据未被验证场景改动。
    baseline = ROOT / "build/porting/reference-1.21/src/generated/resources/data"
    target = ROOT / "src/generated/resources/data"
    original, current = rules(baseline), rules(target)
    for path in original.keys() & current.keys():
        assert original[path] == current[path], f"Rule changed: {path}"
    pending = original.keys() - current.keys()
    for path in pending:
        assert Path(path).stem in PENDING_BLOCKS, f"Untracked missing rule: {path}"
    for path in current.keys() - original.keys():
        assert path.startswith("minecraft/") and Path(path).stem in NATIVE_ADDITIONS, f"Unexpected rule: {path}"
    paths = [f"src/main/resources/assets/anvilcraft/blockstates/{name}_cauldron.json" for name in ("oil", "fire")]
    paths += [f"src/main/resources/assets/anvilcraft/models/block/fire_cauldron_fire{level}.json" for level in range(1, 5)]
    for path in paths:
        assert json.loads((ROOT / path).read_bytes()) == json.loads(source(path)), f"Asset changed: {path}"
    print(f"PASS: {len(original.keys() & current.keys())} shared rules and {len(paths)} cauldron assets match")
    print(f"Pending source rules: {len(pending)}; target-native additions: {len(current.keys() - original.keys())}")


if __name__ == "__main__":
    main()
