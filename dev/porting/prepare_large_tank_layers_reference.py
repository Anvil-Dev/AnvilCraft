"""Run the same live Jade fluid bar scenario on the local 1.21 branch."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
assert subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root).strip() == subprocess.check_output(
    ["git", "rev-parse", "HEAD"], cwd=reference).strip()
folder = reference / "src/main/java/dev/dubhe/anvilcraft/porting"
scene = (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/LargeTankLayersClientScene.java").read_text(encoding="utf-8")
scene = scene.replace("client.resizeGui()", "client.resizeDisplay()")
scene = scene.replace("client.getMainRenderTarget(), 1,", "client.getMainRenderTarget(),")
scene = scene.replace("crate-disposal-26.1-", "crate-disposal-1.21-")
scene = scene.replace("isSectionCompiledAndVisible", "isSectionCompiled")
scene = scene.replace("dev.dubhe.anvilcraft.block.container.LargeFluidTankBlock", "dev.dubhe.anvilcraft.block.LargeFluidTankBlock")
scene = scene.replace("large-tank-layers-26.1-", "large-tank-layers-1.21-").replace("large-tank-layers-26.1.json", "large-tank-layers-1.21.json")
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
        if (index == 10 || index == 11) handler.fill(new net.neoforged.neoforge.fluids.FluidStack(ModFluids.HYDROGEN.get(), 128000), IFluidHandler.FluidAction.EXECUTE);
        if (index == 12) handler.fill(new net.neoforged.neoforge.fluids.FluidStack(net.neoforged.neoforge.common.NeoForgeMod.MILK.get(), 256000), IFluidHandler.FluidAction.EXECUTE);
""" + scene[end:]
(folder / "LargeTankLayersClientScene.java").write_text(scene, encoding="utf-8", newline="\r\n")
wrapper = (folder / "MonolithReferenceScene.java").read_text(encoding="utf-8")
wrapper = wrapper.replace("MonolithReferenceScene", "LargeTankLayersReferenceScene").replace("MonolithClientScene", "LargeTankLayersClientScene")
wrapper = wrapper.replace("portMonolithScene", "portLargeTankLayersScene")
(folder / "LargeTankLayersReferenceScene.java").write_text(wrapper, encoding="utf-8", newline="\r\n")
p = reference / "build.gradle"
s = p.read_text(encoding="utf-8")
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", s)
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portLargeTankLayersScene', 'true' }\n"
p.write_text(s, encoding="utf-8", newline="\r\n")

(folder / "FluidGeometryRecorder.java").write_text(
    (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/FluidGeometryRecorder.java").read_text(encoding="utf-8"), encoding="utf-8", newline="\r\n")
(folder / "LargeTankRenderProbe.java").write_text((root / "dev/porting/templates/LargeTankRenderProbe.java").read_text(encoding="utf-8"), encoding="utf-8", newline="\r\n")

gallery = (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/LargeTankItemGallery.java").read_text(encoding="utf-8")
gallery = gallery.replace("GuiGraphicsExtractor", "GuiGraphics").replace("extractRenderState(", "render(")
gallery = gallery.replace("graphics.text(", "graphics.drawString(").replace("graphics.item(", "graphics.renderItem(")
gallery = gallery.replace("pushMatrix()", "pushPose()").replace("popMatrix()", "popPose()")
gallery = gallery.replace("translate(x, y)", "translate(x, y, 0)").replace("scale(4, 4)", "scale(4, 4, 4)")
(folder / "LargeTankItemGallery.java").write_text(gallery, encoding="utf-8", newline="\r\n")
