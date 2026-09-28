"""Run the same Ruins foundation scenario against the local source branch."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
assert subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root).strip() == subprocess.check_output(
    ['git', 'rev-parse', 'HEAD'], cwd=reference).strip()
folder = reference / 'src/main/java/dev/dubhe/anvilcraft/porting'
scene = (root / 'dev/porting/java/dev/dubhe/anvilcraft/porting/RuinsClientScene.java').read_text(encoding='utf-8')
scene = scene.replace('.item.block.RuinsBlockItem', '.block.item.RuinsBlockItem')
scene = scene.replace('client.resizeGui()', 'client.resizeDisplay()')
scene = scene.replace('client.getMainRenderTarget(), 1,', 'client.getMainRenderTarget(),')
scene = scene.replace('GuiGraphicsExtractor', 'GuiGraphics').replace('extractRenderState(', 'render(')
scene = scene.replace('.pushMatrix()', '.pushPose()').replace('.popMatrix()', '.popPose()')
scene = scene.replace('this.height / 2.0F - 32)', 'this.height / 2.0F - 32, 0)').replace('.scale(4, 4)', '.scale(4, 4, 4)')
scene = scene.replace('graphics.item(', 'graphics.renderItem(')
scene = re.sub(r'import net.minecraft.client.input.MouseButton(?:Event|Info);\n', '', scene)
scene = re.sub(r'        var event = new MouseButtonEvent.*?screen.mouseReleased\(event\);',
               '        screen.mouseClicked(x, y, 0);\n        screen.mouseReleased(x, y, 0);', scene, flags=re.S)
scene = scene.replace('ruins-foundation-26.1-', 'ruins-foundation-1.21-')
(folder / 'RuinsClientScene.java').write_text(scene, encoding='utf-8', newline='\r\n')
wrapper = (folder / 'MonolithReferenceScene.java').read_text(encoding='utf-8')
wrapper = wrapper.replace('MonolithReferenceScene', 'RuinsReferenceScene').replace('MonolithClientScene', 'RuinsClientScene')
wrapper = wrapper.replace('portMonolithScene', 'portRuinsScene')
(folder / 'RuinsReferenceScene.java').write_text(wrapper, encoding='utf-8', newline='\r\n')
p = reference / 'build.gradle'
s = p.read_text(encoding='utf-8')
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", s)
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portRuinsScene', 'true' }\n"
p.write_text(s, encoding='utf-8', newline='\r\n')
