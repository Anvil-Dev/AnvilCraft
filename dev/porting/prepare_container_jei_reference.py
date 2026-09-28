"""Run the container upgrade recipe scenario against the local 1.21 source checkout."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
assert subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root).strip() == subprocess.check_output(
    ['git', 'rev-parse', 'HEAD'], cwd=reference).strip()
folder = reference / 'src/main/java/dev/dubhe/anvilcraft/porting'
scene = (root / 'dev/porting/java/dev/dubhe/anvilcraft/porting/ContainerUpgradeJeiScene.java').read_text(encoding='utf-8')
scene = scene.replace('TooltipDedupProbe.verifyDeferred(graphics);', '')
scene = scene.replace('client.resizeGui()', 'client.resizeDisplay()')
scene = scene.replace('Identifier', 'ResourceLocation').replace('createCraftingStationLookup', 'createRecipeCatalystLookup')
scene = scene.replace('client.getMainRenderTarget(), 1,', 'client.getMainRenderTarget(),')
scene = scene.replace('GuiGraphicsExtractor', 'GuiGraphics').replace('extractRenderState(', 'render(')
scene = scene.replace('.pushMatrix()', '.pushPose()').replace('.popMatrix()', '.popPose()')
scene = scene.replace('this.height / 2.0F - 32)', 'this.height / 2.0F - 32, 0)').replace('.scale(4, 4)', '.scale(4, 4, 4)')
scene = scene.replace('graphics.item(', 'graphics.renderItem(')
scene = scene.replace('container-jei-26.1-', 'container-jei-1.21-')
(folder / 'ContainerUpgradeJeiScene.java').write_text(scene, encoding='utf-8', newline='\r\n')
wrapper = (folder / 'MonolithReferenceScene.java').read_text(encoding='utf-8')
wrapper = wrapper.replace('MonolithReferenceScene', 'ContainerUpgradeJeiReferenceScene').replace('MonolithClientScene', 'ContainerUpgradeJeiScene')
wrapper = wrapper.replace('portMonolithScene', 'portContainerJeiScene')
(folder / 'ContainerUpgradeJeiReferenceScene.java').write_text(wrapper, encoding='utf-8', newline='\r\n')
p = reference / 'build.gradle'
s = p.read_text(encoding='utf-8')
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", s)
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portContainerJeiScene', 'true' }\n"
s = re.sub(r"// BEGIN_PORT_CONTAINER_JEI_ANIMATION.*?// END_PORT_CONTAINER_JEI_ANIMATION\n?", "", s, flags=re.S)
s += """
// BEGIN_PORT_CONTAINER_JEI_ANIMATION
if (providers.gradleProperty('portContainerJeiScene').isPresent()) {
    def output = file('build/resources/main/assets/anvilcraft/textures/block/transcendium_block_outline.png.mcmeta').absolutePath
    def original = file('src/main/resources/assets/anvilcraft/textures/block/transcendium_block_outline.png.mcmeta').absolutePath
    def restore = tasks.register('restoreContainerJeiOutlineAnimation') {
        doLast { new File(output).bytes = new File(original).bytes }
    }
    tasks.named('runClient') {
        doFirst { new File(output).text = '{"animation":{"frametime":10,"frames":[0]}}' }
        finalizedBy(restore)
    }
}
// END_PORT_CONTAINER_JEI_ANIMATION
"""
p.write_text(s, encoding='utf-8', newline='\r\n')
