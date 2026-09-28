"""Run the same live tank title and Jade scenario on the local 1.21 branch."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
assert subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root).strip() == subprocess.check_output(
    ["git", "rev-parse", "HEAD"], cwd=reference).strip()
folder = reference / "src/main/java/dev/dubhe/anvilcraft/porting"
scene = (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/TankDisposalClientScene.java").read_text(encoding="utf-8")
scene = scene.replace("client.resizeGui()", "client.resizeDisplay()")
scene = scene.replace("isSectionCompiledAndVisible", "isSectionCompiled")
scene = scene.replace("client.getMainRenderTarget(), 1,", "client.getMainRenderTarget(),")
scene = scene.replace("tank-disposal-26.1-", "tank-disposal-1.21-")
scene = scene.replace("box.getTooltip().getString(JadeIds.CORE_OBJECT_NAME)",
    'box.getTooltip().get(JadeIds.CORE_OBJECT_NAME).stream().map(snownee.jade.api.ui.IElement::getMessage)'
    '.collect(java.util.stream.Collectors.joining())')
scene = scene.replace("import net.neoforged.neoforge.transfer.fluid.FluidResource;\n", "")
scene = scene.replace("import net.neoforged.neoforge.transfer.transaction.Transaction;\n", "")
scene = re.sub(r"    private static void fill\(FluidTankBlockEntity tank\) \{.*?\n    }", """    private static void fill(FluidTankBlockEntity tank) {
        tank.getFluidHandler().fill(new net.neoforged.neoforge.fluids.FluidStack(Fluids.WATER, 16000),
            net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
    }""", scene, flags=re.S)
scene = scene.replace("getAmountAsInt(0)", "getFluidInTank(0).getAmount()")
(folder / "TankDisposalClientScene.java").write_text(scene, encoding="utf-8", newline="\r\n")
wrapper = (folder / "MonolithReferenceScene.java").read_text(encoding="utf-8")
wrapper = wrapper.replace("MonolithReferenceScene", "TankDisposalReferenceScene").replace("MonolithClientScene", "TankDisposalClientScene")
wrapper = wrapper.replace("portMonolithScene", "portTankDisposalScene")
(folder / "TankDisposalReferenceScene.java").write_text(wrapper, encoding="utf-8", newline="\r\n")
p = reference / "build.gradle"
s = p.read_text(encoding="utf-8")
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", s)
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portTankDisposalScene', 'true' }\n"
p.write_text(s, encoding="utf-8", newline="\r\n")
