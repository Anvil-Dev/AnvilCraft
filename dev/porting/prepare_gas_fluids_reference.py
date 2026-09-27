"""Prepare paired gas bucket renders with a fixed vanilla water animation frame."""
from pathlib import Path
import json
import re
import subprocess
import zipfile

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
assert subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root).strip() == subprocess.check_output(
    ['git', 'rev-parse', 'HEAD'], cwd=reference).strip()
folder = reference / 'src/main/java/dev/dubhe/anvilcraft/porting'
scene = (root / 'dev/porting/java/dev/dubhe/anvilcraft/porting/GasFluidClientScene.java').read_text(encoding='utf-8')
scene = scene.replace('import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;\n', '')
scene = scene.replace('GuiGraphicsExtractor', 'GuiGraphics').replace('extractRenderState(', 'render(')
scene = scene.replace('client.resizeGui()', 'client.resizeDisplay()')
scene = scene.replace('.pushMatrix()', '.pushPose()').replace('.popMatrix()', '.popPose()')
scene = scene.replace('.translate(x, y)', '.translate(x, y, 0)').replace('.scale(4, 4)', '.scale(4, 4, 4)')
scene = scene.replace('graphics.item(', 'graphics.renderItem(')
scene = scene.replace('client.getMainRenderTarget(), 1,', 'client.getMainRenderTarget(),')
scene = scene.replace('gas-fluids-26.1', 'gas-fluids-1.21')
scene = scene.replace('''        var model = FluidRenderHelper.getModel(client.getModelManager().getFluidStateModelSet(), fluid);
        return model.fluidTintSource().colorAsStack(new FluidStack(fluid, 1000));''',
    '''        return net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid)
            .getTintColor(new FluidStack(fluid, 1000));''')
(folder / 'GasFluidClientScene.java').write_text(scene, encoding='utf-8', newline='\r\n')
(folder / 'GasBucketMeshProbe.java').write_text(
    """package dev.dubhe.anvilcraft.porting;

public final class GasBucketMeshProbe {
    public static void verify(net.minecraft.client.Minecraft client) {
        try {
            var stack = dev.dubhe.anvilcraft.init.item.ModItems.HYDROGEN_BUCKET.asStack();
            var model = client.getItemRenderer().getModel(stack, client.level, client.player, 0);
            var result = new java.util.ArrayList<>();
            for (var pass : model.getRenderPasses(stack, false)) {
                for (var renderType : pass.getRenderTypes(stack, false)) {
                    for (var quad : pass.getQuads(null, null, net.minecraft.util.RandomSource.create(42),
                        net.neoforged.neoforge.client.model.data.ModelData.EMPTY, renderType)) {
                        var sprite = quad.getSprite();
                        var data = quad.getVertices();
                        var vertices = new java.util.ArrayList<>();
                        for (int index = 0; index < 4; index++) {
                            int offset = index * 8;
                            vertices.add(java.util.List.of(Float.intBitsToFloat(data[offset]), Float.intBitsToFloat(data[offset + 1]),
                                Float.intBitsToFloat(data[offset + 2]),
                                (Float.intBitsToFloat(data[offset + 4]) - sprite.getU0()) / (sprite.getU1() - sprite.getU0()),
                                (Float.intBitsToFloat(data[offset + 5]) - sprite.getV0()) / (sprite.getV1() - sprite.getV0())));
                        }
                        result.add(java.util.Map.of("sprite", sprite.contents().name().toString(), "direction", quad.getDirection().name(),
                            "vertices", vertices));
                    }
                }
            }
            java.nio.file.Files.writeString(client.gameDirectory.toPath().resolve("gas-bucket-mesh-1.21.json"),
                new com.google.gson.Gson().toJson(result));
        } catch (java.io.IOException error) { throw new IllegalStateException(error); }
    }
}
""", encoding='utf-8')
wrapper = (folder / 'MonolithReferenceScene.java').read_text(encoding='utf-8')
wrapper = wrapper.replace('MonolithReferenceScene', 'GasFluidReferenceScene').replace('MonolithClientScene', 'GasFluidClientScene')
wrapper = wrapper.replace('portMonolithScene', 'portGasFluidScene')
(folder / 'GasFluidReferenceScene.java').write_text(wrapper, encoding='utf-8', newline='\r\n')
p = reference / 'build.gradle'
s = p.read_text(encoding='utf-8')
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", s)
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portGasFluidScene', 'true' }\n"
if "tasks.register('clearGasFluidReferenceTextures')" not in s:
    s += '''
if (providers.gradleProperty('portGasFluidScene').isPresent()) {
    sourceSets.main.resources.srcDir('../gas-fluids-source-assets')
    def cleanup = tasks.register('clearGasFluidReferenceTextures') {
        doLast {
            ['water_still', 'water_flow'].each { name ->
                delete(file("build/resources/main/assets/minecraft/textures/block/${name}.png"))
                delete(file("build/resources/main/assets/minecraft/textures/block/${name}.png.mcmeta"))
            }
        }
    }
    tasks.named('runClient') { finalizedBy(cleanup) }
}
'''
p.write_text(s, encoding='utf-8', newline='\r\n')
for platform, archive_path in [
    ('target', root / 'build/moddev/artifacts/minecraft-patched-26.1.2.75.jar'),
    ('source', reference / 'build/moddev/artifacts/neoforge-21.1.238-client-extra-aka-minecraft-resources.jar'),
]:
    output = root / 'build/porting' / f'gas-fluids-{platform}-assets/assets/minecraft/textures/block'
    output.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(archive_path) as archive:
        for name in ['water_still', 'water_flow']:
            (output / f'{name}.png').write_bytes(archive.read(f'assets/minecraft/textures/block/{name}.png'))
            (output / f'{name}.png.mcmeta').write_text(json.dumps({'animation': {'frames': [0], 'frametime': 1}}), encoding='utf-8')
