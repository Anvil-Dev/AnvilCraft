"""Run the uploader conversion, transfer and visual scenario against the local 1.21 source checkout."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
assert subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root).strip() == subprocess.check_output(
    ['git', 'rev-parse', 'HEAD'], cwd=reference).strip()
folder = reference / 'src/main/java/dev/dubhe/anvilcraft/porting'
scene = (root / 'dev/porting/java/dev/dubhe/anvilcraft/porting/UploaderClientScene.java').read_text(encoding='utf-8')
scene = scene.replace('client.resizeGui()', 'client.resizeDisplay()')
scene = scene.replace('import net.neoforged.neoforge.transfer.item.ItemResource;\n', '')
scene = re.sub(r'getBuffer\(\)\.set\((\d+), ItemResource\.of\(Items.GOLD_INGOT\), (\d+)\)', r'getBuffer().setStackInSlot(\1, new net.minecraft.world.item.ItemStack(Items.GOLD_INGOT, \2))', scene)
scene = scene.replace('client.getMainRenderTarget(), 1,', 'client.getMainRenderTarget(),')
scene = scene.replace('GuiGraphicsExtractor', 'GuiGraphics').replace('extractRenderState(', 'render(')
scene = scene.replace('.pushMatrix()', '.pushPose()').replace('.popMatrix()', '.popPose()')
scene = scene.replace('this.height / 2.0F - 32)', 'this.height / 2.0F - 32, 0)').replace('.scale(4, 4)', '.scale(4, 4, 4)')
scene = scene.replace('graphics.item(', 'graphics.renderItem(')
scene = scene.replace('uploader-26.1-', 'uploader-1.21-')
(folder / 'UploaderClientScene.java').write_text(scene, encoding='utf-8', newline='\r\n')
wrapper = (folder / 'MonolithReferenceScene.java').read_text(encoding='utf-8')
wrapper = wrapper.replace('MonolithReferenceScene', 'UploaderReferenceScene').replace('MonolithClientScene', 'UploaderClientScene')
wrapper = wrapper.replace('portMonolithScene', 'portUploaderScene')
(folder / 'UploaderReferenceScene.java').write_text(wrapper, encoding='utf-8', newline='\r\n')
p = reference / 'build.gradle'
s = p.read_text(encoding='utf-8')
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", s)
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portUploaderScene', 'true' }\n"
s = re.sub(r"// BEGIN_PORT_UPLOADER_ANIMATION.*?// END_PORT_UPLOADER_ANIMATION\n?", "", s, flags=re.S)
s += """
// BEGIN_PORT_UPLOADER_ANIMATION
if (providers.gradleProperty('portUploaderScene').isPresent()) {
    def output = file('build/resources/main/assets/anvilcraft/textures/block/transcendium_block_outline.png.mcmeta').absolutePath
    def original = file('src/main/resources/assets/anvilcraft/textures/block/transcendium_block_outline.png.mcmeta').absolutePath
    def restore = tasks.register('restoreUploaderOutlineAnimation') {
        doLast { new File(output).bytes = new File(original).bytes }
    }
    tasks.named('runClient') {
        doFirst { new File(output).text = '{"animation":{"frametime":10,"frames":[0]}}' }
        finalizedBy(restore)
    }
}
// END_PORT_UPLOADER_ANIMATION
"""
p.write_text(s, encoding='utf-8', newline='\r\n')
