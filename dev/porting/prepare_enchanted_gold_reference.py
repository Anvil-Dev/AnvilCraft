"""Prepare the common gold scene and a fixed glint phase on the local source checkout."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
assert subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root).strip() == subprocess.check_output(
    ['git', 'rev-parse', 'HEAD'], cwd=reference).strip()
folder = reference / 'src/main/java/dev/dubhe/anvilcraft/porting'
scene = (root / 'dev/porting/java/dev/dubhe/anvilcraft/porting/EnchantedGoldClientScene.java').read_text(encoding='utf-8')
scene = scene.replace('            EnchantedGoldJeiProbe.verify();\n', '')
scene = scene.replace('client.getMainRenderTarget(), 1,', 'client.getMainRenderTarget(),').replace('enchanted-gold-26.1-', 'enchanted-gold-1.21-')
(folder / 'EnchantedGoldClientScene.java').write_text(scene, encoding='utf-8', newline='\r\n')
clock = (root / 'dev/porting/java/dev/dubhe/anvilcraft/porting/EnchantedGoldReferenceClock.java').read_text(encoding='utf-8')
clock = clock.replace('import net.minecraft.client.renderer.rendertype.TextureTransform;',
    'import com.mojang.blaze3d.systems.RenderSystem;\nimport net.minecraft.client.renderer.RenderStateShard;\nimport net.minecraft.client.renderer.RenderType;')
clock = clock.replace('var state = ModRenderTypes.ENCHANTED_GOLD_GLINT.state;',
    'var state = ((RenderType.CompositeRenderType) ModRenderTypes.ENCHANTED_GOLD_GLINT).state();')
clock = clock.replace('getDeclaredField("textureTransform")', 'getDeclaredField("texturingState")')
clock = clock.replace('field.set(state, new TextureTransform("enchanted_gold_reference", EnchantedGoldReferenceClock::matrix));',
    'var setup = RenderStateShard.class.getDeclaredField("setupState");\n            setup.setAccessible(true);\n            setup.set(field.get(state), (Runnable) () -> RenderSystem.setTextureMatrix(matrix()));')
(folder / 'EnchantedGoldReferenceClock.java').write_text(clock, encoding='utf-8', newline='\r\n')
wrapper = (folder / 'MonolithReferenceScene.java').read_text(encoding='utf-8')
wrapper = wrapper.replace('MonolithReferenceScene', 'EnchantedGoldReferenceScene').replace('MonolithClientScene', 'EnchantedGoldClientScene')
wrapper = wrapper.replace('portMonolithScene', 'portEnchantedGoldScene')
(folder / 'EnchantedGoldReferenceScene.java').write_text(wrapper, encoding='utf-8', newline='\r\n')
p = reference / 'build.gradle'
s = p.read_text(encoding='utf-8')
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", s)
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portEnchantedGoldScene', 'true' }\n"
p.write_text(s, encoding='utf-8', newline='\r\n')
