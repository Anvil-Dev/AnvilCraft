"""Prepare the real-player cursed apple route and consumption scenario on 1.21."""
from pathlib import Path
import re
import subprocess
root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
assert subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root).strip() == subprocess.check_output(
    ["git", "rev-parse", "HEAD"], cwd=reference).strip()
folder = reference / "src/main/java/dev/dubhe/anvilcraft/porting"
scene = (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/CursedAppleClientScene.java").read_text(encoding="utf-8")
scene = scene.replace(".dimension().identifier()", ".dimension().location()")
scene = scene.replace("player.teleportTo(source, origin, 180, origin, Set.of(), 37, 11, true)", "player.teleportTo(source, origin, 180, origin, 37, 11)")
scene = re.sub(r"    private static void respawn\(ServerPlayer player\) \{.*?\n    \}",
    "    private static void respawn(ServerPlayer player) {\n        player.setRespawnPosition(Level.OVERWORLD, new BlockPos(160, 201, 160), 90, true, false);\n    }", scene, flags=re.S)
scene = scene.replace("getMaxY()", "getMaxBuildHeight()").replace("getMinY()", "getMinBuildHeight()")
scene = scene.replace("cursed-apple-26.1", "cursed-apple-1.21")
(folder / "CursedAppleClientScene.java").write_text(scene, encoding="utf-8", newline="\r\n")
wrapper = (folder / "MonolithReferenceScene.java").read_text(encoding="utf-8")
wrapper = wrapper.replace("MonolithReferenceScene", "CursedAppleReferenceScene").replace("MonolithClientScene", "CursedAppleClientScene")
wrapper = wrapper.replace("portMonolithScene", "portCursedAppleScene")
(folder / "CursedAppleReferenceScene.java").write_text(wrapper, encoding="utf-8", newline="\r\n")
p = reference / "build.gradle"
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", p.read_text(encoding="utf-8"))
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portCursedAppleScene', 'true' }\n"
p.write_text(s, encoding="utf-8", newline="\r\n")
