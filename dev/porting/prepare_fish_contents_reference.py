"""Prepare matching fish tank contents render captures on the source checkout."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
assert subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root).strip() == subprocess.check_output(
    ["git", "rev-parse", "HEAD"], cwd=reference).strip()
folder = reference / "src/main/java/dev/dubhe/anvilcraft/porting"
scene = (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/FishTankContentsClientScene.java").read_text(encoding="utf-8")
scene = scene.replace("client.resizeGui()", "client.resizeDisplay()").replace("isSectionCompiledAndVisible", "isSectionCompiled")
scene = scene.replace("client.getMainRenderTarget(), 1,", "client.getMainRenderTarget(),")
scene = scene.replace("fish-contents-26.1-", "fish-contents-1.21-").replace("fish-contents-26.1.json", "fish-contents-1.21.json")
scene = scene.replace("import net.minecraft.world.entity.animal.fish.TropicalFish;", "import net.minecraft.world.entity.animal.TropicalFish;")
scene = scene.replace("import net.neoforged.neoforge.transfer.fluid.FluidResource;\n", "")
scene = scene.replace("import net.neoforged.neoforge.transfer.item.ItemResource;\n", "")
scene = scene.replace('tank.getFluidHandler().set(wet ? FluidResource.of(Fluids.WATER) : FluidResource.EMPTY, wet ? 1000 : 0);',
    'tank.getFluidHandler().setFluid(wet ? new net.neoforged.neoforge.fluids.FluidStack(Fluids.WATER, 1000) : net.neoforged.neoforge.fluids.FluidStack.EMPTY);')
scene = scene.replace('for (int slot = 0; slot < tank.getItemHandler().size(); slot++) tank.getItemHandler().set(slot, ItemResource.EMPTY, 0);',
    'for (int slot = 0; slot < tank.getItemHandler().getSlots(); slot++) tank.getItemHandler().setStackInSlot(slot, net.minecraft.world.item.ItemStack.EMPTY);')
scene = scene.replace('tank.getItemHandler().set(0, ItemResource.of(Items.STONE), 9);', 'tank.getItemHandler().setStackInSlot(0, new net.minecraft.world.item.ItemStack(Items.STONE, 9));')
scene = scene.replace('tank.getItemHandler().set(1, ItemResource.of(Items.GRANITE), 17);', 'tank.getItemHandler().setStackInSlot(1, new net.minecraft.world.item.ItemStack(Items.GRANITE, 17));')
scene = scene.replace('tank.getFishes().clear();', 'tank.getTropicalFishData().clear();')
scene = scene.replace('tank.getFishes().add(new FishTankBlockEntity.TropicalFishData(TropicalFish.Pattern.KOB, DyeColor.WHITE, DyeColor.WHITE));',
    'tank.getTropicalFishData().add(new net.minecraft.nbt.CompoundTag());')
(folder / "FishTankContentsClientScene.java").write_text(scene, encoding="utf-8", newline="\r\n")
(folder / "FishTankContentsProbe.java").write_text((root / "dev/porting/templates/FishTankContentsProbe.java").read_text(encoding="utf-8"), encoding="utf-8", newline="\r\n")
(folder / "FluidGeometryRecorder.java").write_text((root / "dev/porting/java/dev/dubhe/anvilcraft/porting/FluidGeometryRecorder.java").read_text(encoding="utf-8"), encoding="utf-8", newline="\r\n")
wrapper = (folder / "MonolithReferenceScene.java").read_text(encoding="utf-8")
wrapper = wrapper.replace("MonolithReferenceScene", "FishTankContentsReferenceScene").replace("MonolithClientScene", "FishTankContentsClientScene")
wrapper = wrapper.replace("portMonolithScene", "portFishTankContentsScene")
(folder / "FishTankContentsReferenceScene.java").write_text(wrapper, encoding="utf-8", newline="\r\n")
p = reference / "build.gradle"
s = p.read_text(encoding="utf-8")
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", s)
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portFishTankContentsScene', 'true' }\n"
p.write_text(s, encoding="utf-8", newline="\r\n")
