"""Run the station interaction and title scenario against the local 1.21 source checkout."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
assert subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root).strip() == subprocess.check_output(
    ['git', 'rev-parse', 'HEAD'], cwd=reference).strip()
folder = reference / 'src/main/java/dev/dubhe/anvilcraft/porting'
scene = (root / 'dev/porting/java/dev/dubhe/anvilcraft/porting/StorageInteractionScene.java').read_text(encoding='utf-8')
scene = scene.replace('TooltipDedupProbe.verifyNoJade(client);', '')
scene = scene.replace('client.resizeGui()', 'client.resizeDisplay()')
scene = scene.replace('import net.minecraft.client.input.MouseButtonEvent;\n', '')
scene = scene.replace('import net.minecraft.client.input.MouseButtonInfo;\n', '')
scene = scene.replace('var event = new MouseButtonEvent(button.getX() + 5, button.getY() + 4, new MouseButtonInfo(0, 0));',
    'double x = button.getX() + 5; double y = button.getY() + 4;')
scene = scene.replace('client.screen.mouseClicked(event, false);', 'client.screen.mouseClicked(x, y, 0);')
scene = scene.replace('client.screen.mouseReleased(event);', 'client.screen.mouseReleased(x, y, 0);')
scene = scene.replace('client.getMainRenderTarget(), 1,', 'client.getMainRenderTarget(),')
scene = scene.replace('storage-interaction-26.1-', 'storage-interaction-1.21-')
(folder / 'StorageInteractionScene.java').write_text(scene, encoding='utf-8', newline='\r\n')
wrapper = (folder / 'MonolithReferenceScene.java').read_text(encoding='utf-8')
wrapper = wrapper.replace('MonolithReferenceScene', 'StorageInteractionReferenceScene').replace('MonolithClientScene', 'StorageInteractionScene')
wrapper = wrapper.replace('portMonolithScene', 'portStorageInteractionScene')
(folder / 'StorageInteractionReferenceScene.java').write_text(wrapper, encoding='utf-8', newline='\r\n')
p = reference / 'build.gradle'
s = p.read_text(encoding='utf-8')
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", s)
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portStorageInteractionScene', 'true' }\n"
p.write_text(s, encoding='utf-8', newline='\r\n')
