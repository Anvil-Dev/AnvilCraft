"""Prepare the shared confectionery visual scene in the local source checkout."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
assert subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root).strip() == subprocess.check_output(
    ['git', 'rev-parse', 'HEAD'], cwd=reference).strip()
folder = reference / 'src/main/java/dev/dubhe/anvilcraft/porting'
scene = (root / 'dev/porting/java/dev/dubhe/anvilcraft/porting/ConfectioneryClientScene.java').read_text(encoding='utf-8')
scene = scene.replace('dev.dubhe.anvilcraft.block.cake.StepEffectBlock', 'dev.dubhe.anvilcraft.block.StepEffectBlock')
scene = scene.replace('GuiGraphicsExtractor', 'GuiGraphics').replace('extractRenderState(', 'render(')
scene = scene.replace('client.resizeGui()', 'client.resizeDisplay()')
scene = scene.replace('MobEffects.SPEED', 'MobEffects.MOVEMENT_SPEED').replace('MobEffects.HASTE', 'MobEffects.DIG_SPEED')
scene = scene.replace('MobEffects.JUMP_BOOST', 'MobEffects.JUMP')
scene = scene.replace('.pushMatrix()', '.pushPose()').replace('.popMatrix()', '.popPose()')
scene = scene.replace('.translate(x, y)', '.translate(x, y, 0)').replace('.scale(4, 4)', '.scale(4, 4, 4)')
scene = scene.replace('graphics.item(', 'graphics.renderItem(')
scene = scene.replace('client.getMainRenderTarget(), 1,', 'client.getMainRenderTarget(),')
scene = scene.replace('confectionery-26.1', 'confectionery-1.21')
(folder / 'ConfectioneryClientScene.java').write_text(scene, encoding='utf-8', newline='\r\n')
wrapper = (folder / 'MonolithReferenceScene.java').read_text(encoding='utf-8')
wrapper = wrapper.replace('MonolithReferenceScene', 'ConfectioneryReferenceScene').replace('MonolithClientScene', 'ConfectioneryClientScene')
wrapper = wrapper.replace('portMonolithScene', 'portConfectioneryScene')
(folder / 'ConfectioneryReferenceScene.java').write_text(wrapper, encoding='utf-8', newline='\r\n')
p = reference / 'build.gradle'
s = p.read_text(encoding='utf-8')
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", s)
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portConfectioneryScene', 'true' }\n"
p.write_text(s, encoding='utf-8', newline='\r\n')
