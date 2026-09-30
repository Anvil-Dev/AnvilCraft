"""Prepare the same gas-container cases in the task-owned source reference checkout."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
assert subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root).strip() == subprocess.check_output(
    ["git", "rev-parse", "HEAD"], cwd=reference).strip()
folder = reference / "src/main/java/dev/dubhe/anvilcraft/porting"
scene = (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/GasContainerRenderScene.java").read_text(encoding="utf-8")
scene = scene.replace("dev.dubhe.anvilcraft.block.container.LargeFluidTankBlock", "dev.dubhe.anvilcraft.block.LargeFluidTankBlock")
scene = scene.replace("client.getMainRenderTarget(), 1,", "client.getMainRenderTarget(),")
scene = scene.replace("gas-container-26.1", "gas-container-1.21")
(folder / "GasContainerRenderScene.java").write_text(scene, encoding="utf-8", newline="\r\n")
for name in ["GasContainerRenderProbe.java", "LargeTankRenderProbe.java"]:
    (folder / name).write_text((root / "dev/porting/templates" / name).read_text(encoding="utf-8"), encoding="utf-8", newline="\r\n")
(folder / "FluidGeometryRecorder.java").write_text(
    (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/FluidGeometryRecorder.java").read_text(encoding="utf-8"),
    encoding="utf-8", newline="\r\n")
wrapper = (folder / "MonolithReferenceScene.java").read_text(encoding="utf-8")
wrapper = wrapper.replace("MonolithReferenceScene", "GasContainerReferenceScene").replace("MonolithClientScene", "GasContainerRenderScene")
wrapper = wrapper.replace("portMonolithScene", "portGasContainerScene")
(folder / "GasContainerReferenceScene.java").write_text(wrapper, encoding="utf-8", newline="\r\n")
p = reference / "build.gradle"
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", p.read_text(encoding="utf-8"))
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portGasContainerScene', 'true' }\n"
p.write_text(s, encoding="utf-8", newline="\r\n")
