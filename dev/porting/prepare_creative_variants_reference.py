"""Run the shared creative-picker fixture against the local 1.21 source branch."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
assert subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root).strip() == subprocess.check_output(
    ['git', 'rev-parse', 'HEAD'], cwd=reference).strip()
folder = reference / 'src/main/java/dev/dubhe/anvilcraft/porting'
scene = (root / 'dev/porting/java/dev/dubhe/anvilcraft/porting/CreativeVariantClientScene.java').read_text(encoding='utf-8')
scene = scene.replace('import net.minecraft.client.input.MouseButtonEvent;\n', '')
scene = scene.replace('import net.minecraft.client.input.MouseButtonInfo;\n', '')
scene = scene.replace('ContainerInput', 'ClickType').replace('hasInfiniteMaterials()', 'isCreative()')
scene = scene.replace('client.resizeGui()', 'client.resizeDisplay()')
scene = scene.replace('client.getWindow().handle()', 'client.getWindow().getWindow()')
scene = scene.replace('User32.SendMessage(null, window,', 'User32.SendMessage(window,')
scene = scene.replace('client.hasShiftDown()', 'net.minecraft.client.gui.screens.Screen.hasShiftDown()')
scene = scene.replace('.getValue(CreativeModeTabs.INVENTORY)', '.get(CreativeModeTabs.INVENTORY)')
scene = scene.replace('''        var mouse = new MouseButtonEvent(x, y, new MouseButtonInfo(button, modifiers));
        screen.mouseClicked(mouse, false);
        screen.mouseReleased(mouse);''', '''        screen.mouseClicked(x, y, button);
        screen.mouseReleased(x, y, button);''')
scene = scene.replace('client.getMainRenderTarget(), 1,', 'client.getMainRenderTarget(),')
scene = scene.replace('creative-variants-26.1', 'creative-variants-1.21')
(folder / 'CreativeVariantClientScene.java').write_text(scene, encoding='utf-8', newline='\r\n')
wrapper = (folder / 'MonolithReferenceScene.java').read_text(encoding='utf-8')
wrapper = wrapper.replace('MonolithReferenceScene', 'CreativeVariantReferenceScene').replace('MonolithClientScene', 'CreativeVariantClientScene')
wrapper = wrapper.replace('portMonolithScene', 'portCreativeVariantScene')
(folder / 'CreativeVariantReferenceScene.java').write_text(wrapper, encoding='utf-8', newline='\r\n')
p = reference / 'build.gradle'
s = p.read_text(encoding='utf-8')
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", s)
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portCreativeVariantScene', 'true' }\n"
p.write_text(s, encoding='utf-8', newline='\r\n')
p = reference / 'run/mun-reference/config/anvilcraft-client.toml'
s = p.read_text(encoding='utf-8')
assert 'use_legacy_creative_tab' in s
s = re.sub(r'(use_legacy_creative_tab\s*=\s*)false', r'\1true', s)
p.write_text(s, encoding='utf-8')
