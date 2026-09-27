"""Install the common glass visual fixture in the task's source reference checkout."""
from pathlib import Path
import re
import json
import zipfile
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
source_ref = subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root)
assert subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=reference) == source_ref
folder = reference / 'src/main/java/dev/dubhe/anvilcraft/porting'
text = (root / 'dev/porting/java/dev/dubhe/anvilcraft/porting/GlassVisualParityScene.java').read_text(encoding='utf-8')
text = text.replace('"glass-parity-26.1-"', '"glass-parity-1.21-"')
text = text.replace('client.getMainRenderTarget(), 1,', 'client.getMainRenderTarget(),')
(folder / 'GlassVisualParityScene.java').write_text(text, encoding='utf-8')
driver = folder / 'PortVisualScene.java'
text = driver.read_text(encoding='utf-8')
anchor = '        if (Boolean.getBoolean("anvilcraft.portSmartPlacerScene")) {'
if 'GlassVisualParityScene.frame' not in text:
    text = text.replace(anchor, '''        if (Boolean.getBoolean("anvilcraft.portGlassParityScene")) {
            if (prepared) GlassVisualParityScene.frame(client);
            return;
        }
''' + anchor, 1)
driver.write_text(text, encoding='utf-8')
build = reference / 'build.gradle'
text = build.read_text(encoding='utf-8')
text = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", text)
text += "\nneoForge.runs.client { systemProperty 'anvilcraft.portVisualScene', 'true'; systemProperty 'anvilcraft.portGlassParityScene', 'true' }\n"
dependency = "dependencies { compileOnly 'org.jspecify:jspecify:1.0.0' }"
if dependency not in text:
    text += '\n' + dependency + '\n'
asset_line = "sourceSets.main.resources.srcDir('../glass-parity-source-assets')"
if asset_line not in text:
    text += '\n' + asset_line + '\n'
build.write_text(text, encoding='utf-8')
for variant, jar in [
    ('target', root / 'build/moddev/artifacts/minecraft-patched-26.1.2.75.jar'),
    ('source', reference / 'build/moddev/artifacts/neoforge-21.1.238-client-extra-aka-minecraft-resources.jar'),
]:
    folder = root / 'build/porting' / f'glass-parity-{variant}-assets/assets/minecraft/textures/block'
    folder.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(jar) as archive:
        for name in ['water_still', 'lava_still']:
            (folder / f'{name}.png').write_bytes(archive.read(f'assets/minecraft/textures/block/{name}.png'))
            (folder / f'{name}.png.mcmeta').write_text(json.dumps({'animation': {'frames': [0], 'frametime': 1}}), encoding='utf-8')
print(source_ref.decode().strip())
