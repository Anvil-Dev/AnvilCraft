"""Prepare detail screenshots from the unchanged source handbook components."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
assert subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root).strip() == subprocess.check_output(
    ["git", "rev-parse", "HEAD"], cwd=reference).strip()
folder = reference / "src/main/java/dev/dubhe/anvilcraft/porting"
scene = (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/HandbookNewRecipeScene.java").read_text(encoding="utf-8")
scene = scene.replace("import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;\n", "")
scene = scene.replace("            if (RecipesRecord.CLIENTSIDE == null || RecipesRecord.CLIENTSIDE.values().isEmpty()) return;",
    "            if (client.level == null || client.level.getRecipeManager().getRecipes().isEmpty()) return;")
scene = scene.replace("RecipesRecord.CLIENTSIDE.byKey(ResourceKey.create(Registries.RECIPE, AnvilCraft.of(CASES.get(index).recipe())))",
    "client.level.getRecipeManager().byKey(AnvilCraft.of(CASES.get(index).recipe())).orElse(null)")
scene = scene.replace("            if (recipe instanceof ProceduralProcessRecipe process) HandbookWipProbe.verify(process);\n", "")
scene = scene.replace("Identifier", "ResourceLocation").replace("GuiGraphicsExtractor", "GuiGraphics")
scene = scene.replace("net.minecraft.util.Util", "net.minecraft.Util")
scene = scene.replace("client.resizeGui()", "client.resizeDisplay()")
scene = scene.replace("client.getMainRenderTarget(), 1,", "client.getMainRenderTarget(),")
scene = scene.replace("extractRenderState", "render").replace("graphics.text(", "graphics.drawString(")
scene = scene.replace(".pushMatrix()", ".pushPose()").replace(".popMatrix()", ".popPose()")
scene = scene.replace("graphics.pose().translate(120, 220)", "graphics.pose().translate(120, 220, 0)")
scene = scene.replace("graphics.pose().scale(2, 2)", "graphics.pose().scale(2, 2, 1)")
scene = scene.replace("graphics.fill(0, 0, this.width, this.height, 0xFFF1E6CD);",
    "graphics.pose().pushPose(); graphics.pose().translate(0, 0, -1000); "
    "graphics.fill(0, 0, this.width, this.height, 0xFFF1E6CD); graphics.pose().popPose();")
scene = scene.replace("process.steps()", "process.getSteps()").replace("process.multiLoopFirstStep()", "process.getMultiLoopFirstStep()")
scene = scene.replace("handbook-new-26.1", "handbook-new-1.21")
(folder / "HandbookNewRecipeScene.java").write_text(scene, encoding="utf-8", newline="\r\n")
recipe = reference / "src/main/resources/data/anvilcraft/recipe/port_handbook/block_processing.json"
recipe.parent.mkdir(parents=True, exist_ok=True)
recipe.write_bytes((root / "dev/porting/resources/data/anvilcraft/recipe/port_handbook/block_processing.json").read_bytes())
wrapper = (folder / "MonolithReferenceScene.java").read_text(encoding="utf-8")
wrapper = wrapper.replace("MonolithReferenceScene", "HandbookNewReferenceScene").replace("MonolithClientScene", "HandbookNewRecipeScene")
wrapper = wrapper.replace("portMonolithScene", "portHandbookNewScene")
(folder / "HandbookNewReferenceScene.java").write_text(wrapper, encoding="utf-8", newline="\r\n")
p = reference / "build.gradle"
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", p.read_text(encoding="utf-8"))
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portHandbookNewScene', 'true' }\n"
p.write_text(s, encoding="utf-8", newline="\r\n")
