"""Prepare matching ordinary/creative tank and minecart render captures on the source checkout."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
assert subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root).strip() == subprocess.check_output(
    ["git", "rev-parse", "HEAD"], cwd=reference).strip()
folder = reference / "src/main/java/dev/dubhe/anvilcraft/porting"
scene = (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/SingleTankRenderClientScene.java").read_text(encoding="utf-8")
scene = scene.replace("client.resizeGui()", "client.resizeDisplay()").replace("isSectionCompiledAndVisible", "isSectionCompiled")
scene = scene.replace("client.getMainRenderTarget(), 1,", "client.getMainRenderTarget(),")
scene = scene.replace("single-tank-26.1-", "single-tank-1.21-").replace("single-tank-render-26.1.json", "single-tank-render-1.21.json")
scene = scene.replace("import net.neoforged.neoforge.transfer.fluid.FluidResource;\n", "")
scene = scene.replace("import net.neoforged.neoforge.transfer.transaction.Transaction;\n", "")
scene = scene.replace('((CreativeFluidTankBlockEntity) level.getBlockEntity(POS)).getFluidHandler().replaceStacks(List.of(contents));',
    '((net.neoforged.neoforge.fluids.capability.templates.FluidTank) ((CreativeFluidTankBlockEntity) level.getBlockEntity(POS)).getFluidHandler()).setFluid(contents);')
start = scene.index("        try (Transaction tx = Transaction.openRoot()) {")
end = scene.index("        level.addFreshEntity(cart);", start)
scene = scene[:start] + """        if (amount > 0) {
            if (!creative) ((FluidTankBlockEntity) level.getBlockEntity(POS)).getFluidHandler()
                .fill(contents, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            cart.getFluidHandler().fill(contents, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        }
""" + scene[end:]
(folder / "SingleTankRenderClientScene.java").write_text(scene, encoding="utf-8", newline="\r\n")
(folder / "SingleTankRenderProbe.java").write_text((root / "dev/porting/templates/SingleTankRenderProbe.java").read_text(encoding="utf-8"), encoding="utf-8", newline="\r\n")
(folder / "FluidGeometryRecorder.java").write_text((root / "dev/porting/java/dev/dubhe/anvilcraft/porting/FluidGeometryRecorder.java").read_text(encoding="utf-8"), encoding="utf-8", newline="\r\n")
wrapper = (folder / "MonolithReferenceScene.java").read_text(encoding="utf-8")
wrapper = wrapper.replace("MonolithReferenceScene", "SingleTankRenderReferenceScene").replace("MonolithClientScene", "SingleTankRenderClientScene")
wrapper = wrapper.replace("portMonolithScene", "portSingleTankRenderScene")
(folder / "SingleTankRenderReferenceScene.java").write_text(wrapper, encoding="utf-8", newline="\r\n")
p = reference / "build.gradle"
s = p.read_text(encoding="utf-8")
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", s)
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portSingleTankRenderScene', 'true' }\n"
p.write_text(s, encoding="utf-8", newline="\r\n")
