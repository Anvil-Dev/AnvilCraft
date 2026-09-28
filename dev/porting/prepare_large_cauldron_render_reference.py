"""Prepare matching large cauldron renderer captures on the source checkout."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
assert subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root).strip() == subprocess.check_output(
    ["git", "rev-parse", "HEAD"], cwd=reference).strip()
folder = reference / "src/main/java/dev/dubhe/anvilcraft/porting"
scene = (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/LargeCauldronRenderScene.java").read_text(encoding="utf-8")
scene = scene.replace("client.getMainRenderTarget(), 1,", "client.getMainRenderTarget(),")
scene = scene.replace("large-cauldron-26.1", "large-cauldron-1.21")
scene = scene.replace("import net.neoforged.neoforge.transfer.item.ItemResource;\n", "")
scene = scene.replace("getInputHandler().size()", "getInputHandler().getSlots()")
scene = scene.replace("getOutputHandler().size()", "getOutputHandler().getSlots()")
for handler, item in [("Input", "STONE"), ("Output", "GRANITE")]:
    scene = scene.replace(f"tank.get{handler}Handler().set(slot, index < 2 ? ItemResource.of(Items.{item}) : ItemResource.EMPTY, index < 2 ? slot + 1 : 0);",
        f"tank.get{handler}Handler().setStackInSlot(slot, index < 2 ? new net.minecraft.world.item.ItemStack(Items.{item}, slot + 1) : net.minecraft.world.item.ItemStack.EMPTY);")
(folder / "LargeCauldronRenderScene.java").write_text(scene, encoding="utf-8", newline="\r\n")
(folder / "LargeCauldronRenderProbe.java").write_text((root / "dev/porting/templates/LargeCauldronRenderProbe.java").read_text(encoding="utf-8"), encoding="utf-8", newline="\r\n")
(folder / "FluidGeometryRecorder.java").write_text((root / "dev/porting/java/dev/dubhe/anvilcraft/porting/FluidGeometryRecorder.java").read_text(encoding="utf-8"), encoding="utf-8", newline="\r\n")
wrapper = (folder / "MonolithReferenceScene.java").read_text(encoding="utf-8")
wrapper = wrapper.replace("MonolithReferenceScene", "LargeCauldronRenderReferenceScene").replace("MonolithClientScene", "LargeCauldronRenderScene")
wrapper = wrapper.replace("portMonolithScene", "portLargeCauldronRenderScene")
(folder / "LargeCauldronRenderReferenceScene.java").write_text(wrapper, encoding="utf-8", newline="\r\n")
p = reference / "build.gradle"
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", p.read_text(encoding="utf-8"))
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portLargeCauldronRenderScene', 'true' }\n"
p.write_text(s, encoding="utf-8", newline="\r\n")
