"""Prepare matching live transmitter-line captures on 1.21."""
from pathlib import Path
import re
import subprocess
import sys
root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
assert subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root).strip() == subprocess.check_output(
    ["git", "rev-parse", "HEAD"], cwd=reference).strip()
folder = reference / "src/main/java/dev/dubhe/anvilcraft/porting"
scene = (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/PowerLineClientScene.java").read_text(encoding="utf-8")
scene = scene.replace("dev.anvilcraft.lib.v2.util.client.Line", "dev.dubhe.anvilcraft.client.renderer.Line")
scene = scene.replace("dev.dubhe.anvilcraft.block.laser.CreativeLaserBlock", "dev.dubhe.anvilcraft.block.CreativeLaserBlock")
scene = scene.replace("dev.anvilcraft.lib.v2.rendering.cachedber.pipeline.CachedBlockEntityRenderingPipeline", "dev.dubhe.anvilcraft.api.rendering.CacheableBERenderingPipeline")
scene = scene.replace("CachedBlockEntityRenderingPipeline", "CacheableBERenderingPipeline").replace(".update(laser, true)", ".update(laser)")
scene = scene.replace(".dimension().identifier()", ".dimension().location()")
scene = scene.replace("new AABB(pos).inflate(16), PowerComponentType.TRANSMITTER", "PowerComponentType.TRANSMITTER, false")
scene = scene.replace("client.getMainRenderTarget(), 1,", "client.getMainRenderTarget(),")
scene = scene.replace("client.getWindow().handle()", "client.getWindow().getWindow()")
scene = scene.replace("power-lines-26.1", "power-lines-1.21")
(folder / "PowerLineClientScene.java").write_text(scene, encoding="utf-8", newline="\r\n")
wrapper = (folder / "MonolithReferenceScene.java").read_text(encoding="utf-8")
wrapper = wrapper.replace("MonolithReferenceScene", "PowerLineReferenceScene").replace("MonolithClientScene", "PowerLineClientScene")
wrapper = wrapper.replace("portMonolithScene", "portPowerLineScene")
(folder / "PowerLineReferenceScene.java").write_text(wrapper, encoding="utf-8", newline="\r\n")
p = reference / "build.gradle"
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", p.read_text(encoding="utf-8"))
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portPowerLineScene', 'true' }\n"
if "--mixed" in sys.argv:
    s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portPowerLineMixed', 'true' }\n"
p.write_text(s, encoding="utf-8", newline="\r\n")
