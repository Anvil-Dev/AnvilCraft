"""Run the common creative laser menu scenario against the local 1.21 source checkout."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
assert subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root).strip() == subprocess.check_output(
    ['git', 'rev-parse', 'HEAD'], cwd=reference).strip()
folder = reference / 'src/main/java/dev/dubhe/anvilcraft/porting'
scene = (root / 'dev/porting/java/dev/dubhe/anvilcraft/porting/CreativeLaserClientScene.java').read_text(encoding='utf-8')
scene = scene.replace('dev.dubhe.anvilcraft.block.laser.CreativeLaserBlock', 'dev.dubhe.anvilcraft.block.CreativeLaserBlock')
scene = scene.replace('SliderWidget', 'Slider').replace('field("sliderWidget")', 'field("slider")')
scene = scene.replace('import net.minecraft.client.input.MouseButtonEvent;\n', '')
scene = scene.replace('import net.minecraft.client.input.MouseButtonInfo;\n', '')
scene = scene.replace('client.resizeGui()', 'client.resizeDisplay()').replace('screen.resize(screen.width, screen.height)',
    'screen.resize(client, screen.width, screen.height)')
scene = scene.replace('''        var event = new MouseButtonEvent((screen.width - 176) / 2.0 + x, (screen.height - 77) / 2 + y, new MouseButtonInfo(0, 0));
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);''', '''        double mouseX = (screen.width - 176) / 2.0 + x;
        double mouseY = (screen.height - 77) / 2 + y;
        screen.mouseClicked(mouseX, mouseY, 0);
        screen.mouseReleased(mouseX, mouseY, 0);''')
scene = scene.replace('client.getMainRenderTarget(), 1,', 'client.getMainRenderTarget(),')
scene = scene.replace('GuiGraphicsExtractor', 'GuiGraphics').replace('extractRenderState(', 'render(')
scene = scene.replace('.pushMatrix()', '.pushPose()').replace('.popMatrix()', '.popPose()')
scene = scene.replace('this.height / 2.0F - 32)', 'this.height / 2.0F - 32, 0)').replace('.scale(4, 4)', '.scale(4, 4, 4)')
scene = scene.replace('graphics.item(', 'graphics.renderItem(')
scene = scene.replace('creative-laser-26.1-', 'creative-laser-1.21-')
(folder / 'CreativeLaserClientScene.java').write_text(scene, encoding='utf-8', newline='\r\n')
wrapper = (folder / 'MonolithReferenceScene.java').read_text(encoding='utf-8')
wrapper = wrapper.replace('MonolithReferenceScene', 'CreativeLaserReferenceScene').replace('MonolithClientScene', 'CreativeLaserClientScene')
wrapper = wrapper.replace('portMonolithScene', 'portCreativeLaserScene')
(folder / 'CreativeLaserReferenceScene.java').write_text(wrapper, encoding='utf-8', newline='\r\n')
p = reference / 'build.gradle'
s = p.read_text(encoding='utf-8')
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", s)
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portCreativeLaserScene', 'true' }\n"
p.write_text(s, encoding='utf-8', newline='\r\n')
