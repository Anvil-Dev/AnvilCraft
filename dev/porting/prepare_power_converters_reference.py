"""Prepare identical power-grid and converter visual checks in the source checkout."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
assert subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root).strip() == subprocess.check_output(
    ['git', 'rev-parse', 'HEAD'], cwd=reference).strip()
folder = reference / 'src/main/java/dev/dubhe/anvilcraft/porting'
scene = (root / 'dev/porting/java/dev/dubhe/anvilcraft/porting/PowerConverterClientScene.java').read_text(encoding='utf-8')
scene = scene.replace('GuiGraphicsExtractor', 'GuiGraphics').replace('extractRenderState(', 'render(')
scene = scene.replace('client.resizeGui()', 'client.resizeDisplay()')
scene = scene.replace('.pushMatrix()', '.pushPose()').replace('.popMatrix()', '.popPose()')
scene = scene.replace('this.height / 2 - 32)', 'this.height / 2 - 32, 0)').replace('.scale(4, 4)', '.scale(4, 4, 4)')
scene = scene.replace('graphics.item(', 'graphics.renderItem(').replace('client.getMainRenderTarget(), 1,', 'client.getMainRenderTarget(),')
scene = scene.replace('power-converters-26.1', 'power-converters-1.21')
(folder / 'PowerConverterClientScene.java').write_text(scene, encoding='utf-8', newline='\r\n')
probe = (root / 'dev/porting/java/dev/dubhe/anvilcraft/porting/PowerConverterModelProbe.java').read_text(encoding='utf-8')
probe = probe.replace('dev.dubhe.anvilcraft.block.power.converter.BasePowerConverterBlock', 'dev.dubhe.anvilcraft.block.BasePowerConverterBlock')
probe = probe.replace('import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;\n', '')
probe = probe.replace('        var parts = new ArrayList<BlockStateModelPart>();\n', '')
start = probe.index('                parts.clear();')
end = probe.index('                    for (var quad : all) {', start)
probe = probe[:start] + '''                var model = client.getBlockRenderer().getBlockModel(state);
                int quads = 0;
                {
                    var all = new ArrayList<>(model.getQuads(state, null, RandomSource.create(42)));
                    for (Direction side : Direction.values()) all.addAll(model.getQuads(state, side, RandomSource.create(42)));
''' + probe[end:]
probe = probe.replace('quad.materialInfo().sprite()', 'quad.getSprite()').replace('power-converter-models-26.1', 'power-converter-models-1.21')
probe = probe.replace('var point = quad.position(vertex);', '''var data = quad.getVertices();
                            var point = new org.joml.Vector3f(Float.intBitsToFloat(data[vertex * 8]),
                                Float.intBitsToFloat(data[vertex * 8 + 1]), Float.intBitsToFloat(data[vertex * 8 + 2]));''')
(folder / 'PowerConverterModelProbe.java').write_text(probe, encoding='utf-8', newline='\r\n')
wrapper = (folder / 'MonolithReferenceScene.java').read_text(encoding='utf-8')
wrapper = wrapper.replace('MonolithReferenceScene', 'PowerConverterReferenceScene').replace('MonolithClientScene', 'PowerConverterClientScene')
wrapper = wrapper.replace('portMonolithScene', 'portPowerConverterScene')
(folder / 'PowerConverterReferenceScene.java').write_text(wrapper, encoding='utf-8', newline='\r\n')
p = reference / 'build.gradle'
s = p.read_text(encoding='utf-8')
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", s)
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portPowerConverterScene', 'true' }\n"
p.write_text(s, encoding='utf-8', newline='\r\n')
