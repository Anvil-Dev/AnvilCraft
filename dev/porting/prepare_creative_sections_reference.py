"""Prepare source creative catalog, banner and folding captures."""
from pathlib import Path
import re
import subprocess
root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
assert subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root).strip() == subprocess.check_output(
    ["git", "rev-parse", "HEAD"], cwd=reference).strip()
folder = reference / "src/main/java/dev/dubhe/anvilcraft/porting"
scene = (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/CreativeSectionsClientScene.java").read_text(encoding="utf-8")
scene = scene.replace("client.resizeGui()", "client.resizeDisplay()")
scene = scene.replace("BuiltInRegistries.CREATIVE_MODE_TAB.getValue(", "BuiltInRegistries.CREATIVE_MODE_TAB.get(")
scene = scene.replace("client.getMainRenderTarget(), 1,", "client.getMainRenderTarget(),")
scene = scene.replace("creative-sections-26.1", "creative-sections-1.21")
(folder / "CreativeSectionsClientScene.java").write_text(scene, encoding="utf-8", newline="\r\n")
wrapper = (folder / "MonolithReferenceScene.java").read_text(encoding="utf-8")
wrapper = wrapper.replace("MonolithReferenceScene", "CreativeSectionsReferenceScene").replace("MonolithClientScene", "CreativeSectionsClientScene")
wrapper = wrapper.replace("portMonolithScene", "portCreativeSectionsScene")
(folder / "CreativeSectionsReferenceScene.java").write_text(wrapper, encoding="utf-8", newline="\r\n")
p = reference / "build.gradle"
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", p.read_text(encoding="utf-8"))
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portCreativeSectionsScene', 'true' }\n"
p.write_text(s, encoding="utf-8", newline="\r\n")
