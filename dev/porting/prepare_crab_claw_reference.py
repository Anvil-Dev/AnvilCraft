"""Run the same first-person claw fixture on the latest local source checkout."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
assert subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root).strip() == subprocess.check_output(
    ['git', 'rev-parse', 'HEAD'], cwd=reference).strip()
folder = reference / 'src/main/java/dev/dubhe/anvilcraft/porting'
scene = (root / 'dev/porting/java/dev/dubhe/anvilcraft/porting/CrabClawClientScene.java').read_text(encoding='utf-8')
scene = scene.replace('import dev.dubhe.anvilcraft.item.tool.HeavyHalberdMode;\n', '')
scene = scene.replace('dev.dubhe.anvilcraft.item.tool.HeavyHalberdItem', 'dev.dubhe.anvilcraft.item.HeavyHalberdItem')
for mode in ['SPEAR', 'MACE', 'SWORD']:
    scene = scene.replace('HeavyHalberdMode.' + mode, 'HeavyHalberdItem.' + mode + '_MODE')
scene = scene.replace('client.getToastManager()', 'client.getToasts()')
start = scene.index('        client.level.setTimeFromServer(500);')
end = scene.index('        client.options.hideGui = false;', start)
scene = scene[:start] + '        client.level.setGameTime(500);\n        client.level.setDayTime(6000);\n        client.level.updateSkyBrightness();\n        client.gameRenderer.lightTexture().tick();\n' + scene[end:]

scene = scene.replace('CrabClawRenderProbe.verifyThrowing(client);', 'AnvilCraft.LOGGER.info("PORT_CRAB_SOURCE_THROW_REFERENCE");')
scene = scene.replace('CrabClawRenderProbe.verify(client);', 'AnvilCraft.LOGGER.info("PORT_CRAB_SOURCE_REFERENCE");')
scene = scene.replace('client.getMainRenderTarget(), 1,', 'client.getMainRenderTarget(),').replace('crab-26.1-', 'crab-1.21-')
scene = scene.replace('if (stage == 0) AnvilCraft.LOGGER.info("PORT_CRAB_SOURCE_REFERENCE");', 'if (stage == 0) AnvilCraft.LOGGER.info("PORT_CRAB_SOURCE_REFERENCE sky={} dark={} time={} gamma={} height={} pos={}", client.level.getBrightness(net.minecraft.world.level.LightLayer.SKY, client.player.blockPosition()), client.level.getSkyDarken(0), client.level.getDayTime(), client.options.gamma().get(), client.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, 0, 0), client.player.blockPosition());')
(folder / 'CrabClawClientScene.java').write_text(scene, encoding='utf-8', newline='\r\n')
lighting = (root / 'dev/porting/java/dev/dubhe/anvilcraft/porting/ReferenceHandLighting.java').read_text(encoding='utf-8')
lighting = lighting.replace('SubmitNodeCollector', 'MultiBufferSource')
(folder / 'ReferenceHandLighting.java').write_text(lighting, encoding='utf-8', newline='\r\n')
wrapper = (folder / 'MonolithReferenceScene.java').read_text(encoding='utf-8')
wrapper = wrapper.replace('MonolithReferenceScene', 'CrabClawReferenceScene').replace('MonolithClientScene', 'CrabClawClientScene')
wrapper = wrapper.replace('portMonolithScene', 'portCrabClawScene')
(folder / 'CrabClawReferenceScene.java').write_text(wrapper, encoding='utf-8', newline='\r\n')
p = reference / 'build.gradle'
s = p.read_text(encoding='utf-8')
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", s)
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portCrabClawScene', 'true' }\n"
p.write_text(s, encoding='utf-8', newline='\r\n')
