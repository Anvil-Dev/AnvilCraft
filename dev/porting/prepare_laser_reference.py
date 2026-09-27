"""Prepare the same live laser scene on the latest source branch."""
from pathlib import Path
import re
import subprocess
import sys
root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
assert subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root).strip() == subprocess.check_output(
    ['git', 'rev-parse', 'HEAD'], cwd=reference).strip()
folder = reference / 'src/main/java/dev/dubhe/anvilcraft/porting'
scene = (root / 'dev/porting/java/dev/dubhe/anvilcraft/porting/LaserClientScene.java').read_text(encoding='utf-8')
scene = scene.replace('dev.dubhe.anvilcraft.block.laser.CreativeLaserBlock', 'dev.dubhe.anvilcraft.block.CreativeLaserBlock')
scene = scene.replace('dev.dubhe.anvilcraft.block.laser.LensBlock', 'dev.dubhe.anvilcraft.block.LensBlock')
scene = scene.replace('client.getMainRenderTarget(), 1,', 'client.getMainRenderTarget(),')
scene = scene.replace('laser-components-26.1', 'laser-components-1.21')
scene = scene.replace('laser-components-bloom-26.1', 'laser-components-bloom-1.21')
scene = scene.replace('dev.anvilcraft.lib.v2.rendering.cachedber.pipeline.CachedBlockEntityRenderingPipeline',
    'dev.dubhe.anvilcraft.api.rendering.CacheableBERenderingPipeline').replace('CachedBlockEntityRenderingPipeline', 'CacheableBERenderingPipeline')
scene = scene.replace('.update(laser, true)', '.update(laser)')
(folder / 'LaserClientScene.java').write_text(scene, encoding='utf-8', newline='\r\n')
wrapper = (folder / 'MonolithReferenceScene.java').read_text(encoding='utf-8')
wrapper = wrapper.replace('MonolithReferenceScene', 'LaserReferenceScene').replace('MonolithClientScene', 'LaserClientScene')
wrapper = wrapper.replace('portMonolithScene', 'portLaserScene')
(folder / 'LaserReferenceScene.java').write_text(wrapper, encoding='utf-8', newline='\r\n')
p = reference / 'build.gradle'
s = p.read_text(encoding='utf-8')
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", s)
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portLaserScene', 'true' }\n"
if '--creative' in sys.argv:
    s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portCreativeLaserBeam', 'true' }\n"
p.write_text(s, encoding='utf-8', newline='\r\n')
