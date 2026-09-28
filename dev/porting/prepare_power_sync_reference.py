"""Prepare the same live power-grid payload and HUD scenario on the source checkout."""
from pathlib import Path
import re
import subprocess
root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
assert subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root).strip() == subprocess.check_output(
    ["git", "rev-parse", "HEAD"], cwd=reference).strip()
relative = Path("src/main/java/dev/dubhe/anvilcraft/api/power/PowerSyncFixture.java")
(reference / relative).write_text((root / "dev/porting/java/dev/dubhe/anvilcraft/api/power/PowerSyncFixture.java").read_text(encoding="utf-8"),
    encoding="utf-8", newline="\r\n")
folder = reference / "src/main/java/dev/dubhe/anvilcraft/porting"
scene = (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/PowerSyncClientScene.java").read_text(encoding="utf-8")
scene = scene.replace("client.resizeGui()", "client.resizeDisplay()")
scene = scene.replace("player.teleportTo(player.level(), 0.5, 163, 0.5, Set.of(), 180, 0, true)",
    "player.teleportTo(player.serverLevel(), 0.5, 163, 0.5, 180, 0)")
scene = scene.replace("PowerSyncFixture.create(player.level(), 1025)", "PowerSyncFixture.create(player.serverLevel(), 1025)")
scene = scene.replace("client.getMainRenderTarget(), 1,", "client.getMainRenderTarget(),")
scene = scene.replace("power-hud-26.1", "power-hud-1.21").replace("power-sync-26.1", "power-sync-1.21")
(folder / "PowerSyncClientScene.java").write_text(scene, encoding="utf-8", newline="\r\n")
wrapper = (folder / "MonolithReferenceScene.java").read_text(encoding="utf-8")
wrapper = wrapper.replace("MonolithReferenceScene", "PowerSyncReferenceScene").replace("MonolithClientScene", "PowerSyncClientScene")
wrapper = wrapper.replace("portMonolithScene", "portPowerSyncScene")
(folder / "PowerSyncReferenceScene.java").write_text(wrapper, encoding="utf-8", newline="\r\n")
p = reference / "build.gradle"
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", p.read_text(encoding="utf-8"))
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portPowerSyncScene', 'true' }\n"
p.write_text(s, encoding="utf-8", newline="\r\n")
