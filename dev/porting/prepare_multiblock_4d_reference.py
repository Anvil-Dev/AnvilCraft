"""Prepare the source four-dimensional progress scene with the same development recipe."""
from pathlib import Path
import re
import shutil
import subprocess
root = Path(__file__).resolve().parents[2]
reference = root/'build/porting/reference-mun-1.21'
assert subprocess.check_output(['git','rev-parse','dev/1.21/1.6'],cwd=root).strip() == subprocess.check_output(
    ['git','rev-parse','HEAD'],cwd=reference).strip()
folder = reference/'src/main/java/dev/dubhe/anvilcraft/porting'
scene = (root/'dev/porting/java/dev/dubhe/anvilcraft/porting/Multiblock4DClientScene.java').read_text(encoding='utf-8')
scene = scene.replace('client.getMainRenderTarget(), 1,','client.getMainRenderTarget(),').replace('multiblock-4d-26.1-', 'multiblock-4d-1.21-')
(folder/'Multiblock4DClientScene.java').write_text(scene,encoding='utf-8',newline='\r\n')
wrapper = (folder/'MonolithReferenceScene.java').read_text(encoding='utf-8')
wrapper = wrapper.replace('MonolithReferenceScene','Multiblock4DReferenceScene').replace('MonolithClientScene','Multiblock4DClientScene')
wrapper = wrapper.replace('portMonolithScene','port4dScene')
(folder/'Multiblock4DReferenceScene.java').write_text(wrapper,encoding='utf-8',newline='\r\n')
recipe = Path('data/anvilcraft/recipe/port_4d/progress.json')
target = reference/'src/main/resources'/recipe
target.parent.mkdir(parents=True,exist_ok=True)
shutil.copyfile(root/'dev/porting/resources'/recipe,target)
p = reference/'build.gradle'
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", p.read_text(encoding='utf-8'))
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.port4dScene', 'true' }\n"
p.write_text(s,encoding='utf-8',newline='\r\n')
