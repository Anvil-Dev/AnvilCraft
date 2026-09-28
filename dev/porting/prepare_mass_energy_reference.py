"""Run the common creative laser menu scenario against the local 1.21 source checkout."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
assert subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root).strip() == subprocess.check_output(
    ['git', 'rev-parse', 'HEAD'], cwd=reference).strip()
folder = reference / 'src/main/java/dev/dubhe/anvilcraft/porting'
scene = (root / 'dev/porting/java/dev/dubhe/anvilcraft/porting/MassEnergyInverterClientScene.java').read_text(encoding='utf-8')
scene = scene.replace('dev.dubhe.anvilcraft.block.laser.MassEnergyInverterBlock', 'dev.dubhe.anvilcraft.block.MassEnergyInverterBlock')
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
scene = scene.replace('mass-energy-inverter-26.1-', 'mass-energy-inverter-1.21-')
(folder / 'MassEnergyInverterClientScene.java').write_text(scene, encoding='utf-8', newline='\r\n')
wrapper = (folder / 'MonolithReferenceScene.java').read_text(encoding='utf-8')
wrapper = wrapper.replace('MonolithReferenceScene', 'MassEnergyInverterReferenceScene').replace('MonolithClientScene', 'MassEnergyInverterClientScene')
wrapper = wrapper.replace('portMonolithScene', 'portMassEnergyScene')
(folder / 'MassEnergyInverterReferenceScene.java').write_text(wrapper, encoding='utf-8', newline='\r\n')
p = reference / 'build.gradle'
s = p.read_text(encoding='utf-8')
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", s)
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portMassEnergyScene', 'true' }\n"
p.write_text(s, encoding='utf-8', newline='\r\n')

# Lock only the built test resource to a shared animation frame, then restore it.
s = p.read_text(encoding='utf-8')
s = re.sub(r'// PORT_MASS_ENERGY_WIP_ANIMATION.*?// END_PORT_MASS_ENERGY_WIP_ANIMATION\n?', '', s, flags=re.S)
s += """
// PORT_MASS_ENERGY_WIP_ANIMATION
if (providers.gradleProperty('portMassEnergyScene').isPresent()) {
    def output = file('build/resources/main/assets/anvilcraft/textures/block/wip_block.png.mcmeta').absolutePath
    def original = file('src/main/resources/assets/anvilcraft/textures/block/wip_block.png.mcmeta').absolutePath
    def restore = tasks.register('restoreMassEnergyWipAnimation') {
        doLast { new File(output).bytes = new File(original).bytes }
    }
    tasks.named('runClient') {
        doFirst { new File(output).text = '{"animation":{"frametime":2,"frames":[0]}}' }
        finalizedBy(restore)
    }
}
// END_PORT_MASS_ENERGY_WIP_ANIMATION
"""
p.write_text(s, encoding='utf-8', newline='\r\n')
