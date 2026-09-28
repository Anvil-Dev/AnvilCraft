"""Run the same live Jade fluid bar scenario on the local 1.21 branch."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
assert subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root).strip() == subprocess.check_output(
    ["git", "rev-parse", "HEAD"], cwd=reference).strip()
folder = reference / "src/main/java/dev/dubhe/anvilcraft/porting"
scene = (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/JadeFluidParityClientScene.java").read_text(encoding="utf-8")
scene = scene.replace("client.resizeGui()", "client.resizeDisplay()")
scene = scene.replace("client.getMainRenderTarget(), 1,", "client.getMainRenderTarget(),")
scene = scene.replace("crate-disposal-26.1-", "crate-disposal-1.21-")
scene = scene.replace("isSectionCompiledAndVisible", "isSectionCompiled")
scene = scene.replace("dev.dubhe.anvilcraft.block.container.LargeFluidTankBlock", "dev.dubhe.anvilcraft.block.LargeFluidTankBlock")
scene = scene.replace("jade-fluid-26.1-", "jade-fluid-1.21-").replace("jade-fluid-parity-26.1.json", "jade-fluid-parity-1.21.json")
scene = scene.replace("import net.neoforged.neoforge.transfer.ResourceHandler;", "import net.neoforged.neoforge.fluids.capability.IFluidHandler;")
scene = scene.replace("import net.neoforged.neoforge.transfer.fluid.FluidResource;\n", "")
scene = scene.replace("import net.neoforged.neoforge.transfer.transaction.Transaction;\n", "")
scene = scene.replace("import snownee.jade.api.view.ProgressView;\n", "")
scene = scene.replace("import snownee.jade.impl.Tooltip;", "import snownee.jade.api.ITooltip;")
scene = scene.replace('IDisplayHelper.get().humanReadableNumber(amount, "B", true)',
    'snownee.jade.util.FluidTextHelper.getUnicodeMillibuckets(amount, true)')
start = scene.index("    private static List<Bar> readBars(")
end = scene.index("    private static void prepare(", start)
scene = scene[:start] + """    private static List<Bar> readBars(ITooltip tooltip) {
        List<Bar> result = new ArrayList<>();
        for (int line = 0; line < tooltip.size(); line++) {
            for (var align : snownee.jade.api.ui.IElement.Align.values()) {
                for (var element : tooltip.get(line, align)) {
                    if (!(element instanceof ProgressElement progress)) continue;
                    try {
                        var field = ProgressElement.class.getDeclaredField("progress");
                        field.setAccessible(true);
                        result.add(new Bar(progress.getMessage(), field.getFloat(progress)));
                    } catch (ReflectiveOperationException exception) {
                        throw new IllegalStateException(exception);
                    }
                }
            }
        }
        return result;
    }

""" + scene[end:]
scene = scene.replace("ResourceHandler<FluidResource> handler;", "IFluidHandler handler;")
start = scene.index("        try (Transaction tx = Transaction.openRoot()) {")
end = scene.index("        if (enhanced && !TankUtil", start)
scene = scene[:start] + """        if (water > 0) handler.fill(new net.neoforged.neoforge.fluids.FluidStack(Fluids.WATER, water), IFluidHandler.FluidAction.EXECUTE);
        if (lava > 0) handler.fill(new net.neoforged.neoforge.fluids.FluidStack(Fluids.LAVA, lava), IFluidHandler.FluidAction.EXECUTE);
""" + scene[end:]
(folder / "JadeFluidParityClientScene.java").write_text(scene, encoding="utf-8", newline="\r\n")
wrapper = (folder / "MonolithReferenceScene.java").read_text(encoding="utf-8")
wrapper = wrapper.replace("MonolithReferenceScene", "JadeFluidParityReferenceScene").replace("MonolithClientScene", "JadeFluidParityClientScene")
wrapper = wrapper.replace("portMonolithScene", "portJadeFluidParityScene")
(folder / "JadeFluidParityReferenceScene.java").write_text(wrapper, encoding="utf-8", newline="\r\n")
p = reference / "build.gradle"
s = p.read_text(encoding="utf-8")
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", s)
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portJadeFluidParityScene', 'true' }\n"
p.write_text(s, encoding="utf-8", newline="\r\n")
