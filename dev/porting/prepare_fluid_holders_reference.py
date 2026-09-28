"""Prepare matching ordinary/creative tank and minecart render captures on the source checkout."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
assert subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root).strip() == subprocess.check_output(
    ["git", "rev-parse", "HEAD"], cwd=reference).strip()
folder = reference / "src/main/java/dev/dubhe/anvilcraft/porting"
scene = (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/FluidHolderRenderClientScene.java").read_text(encoding="utf-8")
scene = scene.replace("client.resizeGui()", "client.resizeDisplay()").replace("isSectionCompiledAndVisible", "isSectionCompiled")
scene = scene.replace("client.getMainRenderTarget(), 1,", "client.getMainRenderTarget(),")
scene = scene.replace("fluid-holders-26.1-", "fluid-holders-1.21-").replace("fluid-holders-26.1.json", "fluid-holders-1.21.json")
(folder / "FluidHolderRenderClientScene.java").write_text(scene, encoding="utf-8", newline="\r\n")
(folder / "FluidHolderRenderProbe.java").write_text((root / "dev/porting/templates/FluidHolderRenderProbe.java").read_text(encoding="utf-8"), encoding="utf-8", newline="\r\n")
(folder / "FluidGeometryRecorder.java").write_text((root / "dev/porting/java/dev/dubhe/anvilcraft/porting/FluidGeometryRecorder.java").read_text(encoding="utf-8"), encoding="utf-8", newline="\r\n")
wrapper = (folder / "MonolithReferenceScene.java").read_text(encoding="utf-8")
wrapper = wrapper.replace("MonolithReferenceScene", "FluidHolderRenderReferenceScene").replace("MonolithClientScene", "FluidHolderRenderClientScene")
wrapper = wrapper.replace("portMonolithScene", "portFluidHolderRenderScene")
(folder / "FluidHolderRenderReferenceScene.java").write_text(wrapper, encoding="utf-8", newline="\r\n")
p = reference / "build.gradle"
s = p.read_text(encoding="utf-8")
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", s)
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portFluidHolderRenderScene', 'true' }\n"
p.write_text(s, encoding="utf-8", newline="\r\n")
