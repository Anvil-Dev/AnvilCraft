"""Verify storage behavior runtime evidence against the local source branch."""
import argparse
import json
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser()
parser.add_argument("--client-log", default="build/porting/client-storage-behavior-2.log")
parser.add_argument("--test-log", default="build/porting/tests-storage-behavior-3.log")
args = parser.parse_args()
client = (ROOT / args.client_log).read_text(encoding="utf-8", errors="replace")
tests = (ROOT / args.test_log).read_text(encoding="utf-8", errors="replace")
assert "PORT_STORAGE_BEHAVIOR_CLIENT_PASSED" in client
assert "BUILD SUCCESSFUL" in client and "BUILD SUCCESSFUL" in tests
assert "All 4 required tests passed" in tests
blocks = ["CrateBlock", "LargeCrateBlock", "ShulkerContainerBlock", "HyperdimensionStorageStationBlock"]
sounds = ["BARREL_OPEN", "BARREL_OPEN", "SHULKER_BOX_OPEN", "ENDER_CHEST_OPEN"]
base = "src/main/java/dev/dubhe/anvilcraft/block/container/storage/"
source = subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=ROOT, text=True).strip()
for index, (block, sound) in enumerate(zip(blocks, sounds)):
    ref = subprocess.check_output(["git", "show", f"{source}:{base}{block}.java"], cwd=ROOT).decode("utf-8")
    native = (ROOT / base / f"{block}.java").read_text(encoding="utf-8")
    expression = rf"level.playSound\(player, pos, SoundEvents.{sound}, SoundSource.BLOCKS, 1.0F, 1.0F\)"
    assert re.search(expression, ref) and re.search(expression, native), block
    assert "StorageBlockEntity.applyPickStorageId" in ref and "StorageBlockEntity.applyPickStorageId" in native
    assert (ROOT / f"run/port-validation/client/screenshots/storage-behavior-26.1-{index}.png").is_file()
assert client.count("PORT_STORAGE_BEHAVIOR_CLIENT: block=") == 4
report = {
    "source_commit": source,
    "focused_tests": 4,
    "client_blocks": 4,
    "client_log": args.client_log,
    "test_log": args.test_log,
    "contracts": [
        "Weighted ceil(fullness * 15) signals, transactional rollback and committed insert/extract.",
        "Loaded aliases update together; unloaded aliases stop notifications and reload restores them.",
        "Large crate comparator outputs update at three heights on all four horizontal sides.",
        "Actual server pick packets preserve main storage ID from child parts, including subsequent placement.",
        "Real client opens each storage with the source sound once, plain pick unbound and Ctrl pick bound."
    ]
}
(ROOT / "build/porting/storage-behavior-report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print(json.dumps(report, indent=2))
