"""Prepare the source client's handbook gallery using the native gallery's recipe IDs."""
from pathlib import Path
import re
import subprocess
import sys

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
assert subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root).strip() == subprocess.check_output(
    ["git", "rev-parse", "HEAD"], cwd=reference).strip()
subprocess.run([sys.executable, str(root / "dev/porting/prepare_handbook_new_reference.py")], check=True)
folder = reference / "src/main/java/dev/dubhe/anvilcraft/porting"
recipe = reference / "src/main/resources/data/anvilcraft/recipe/port_handbook/block_processing.json"
recipe.parent.mkdir(parents=True, exist_ok=True)
recipe.write_bytes((root / "dev/porting/resources/data/anvilcraft/recipe/port_handbook/block_processing.json").read_bytes())
scene = (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/HandbookRecipeClientScene.java").read_text(encoding="utf-8")
start = scene.index("    private static void verify(")
end = scene.index("    private static MDComponent parse(", start)
scene = scene[:start] + '''    private static void verify(Minecraft client) {
        try {
            var path = client.gameDirectory.toPath().resolve("handbook-recipes-input.json");
            var input = com.google.gson.JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            for (var entry : input.entrySet()) {
                if (!entry.getKey().startsWith("anvilcraft:")) continue;
                String id = entry.getValue().getAsJsonObject().get("recipe").getAsString().replace("iron_chain", "chain");
                var location = ResourceLocation.parse(id);
                if (client.level.getRecipeManager().byKey(location).isEmpty()) {
                    throw new IllegalStateException("Missing source recipe " + id);
                }
                COMPONENTS.add(parse(location));
                LABELS.add(entry.getKey());
                RESULT.put(entry.getKey(), entry.getValue());
            }
            COMPONENTS.add(parse(AnvilCraft.of("port_missing_recipe")));
            RESULT.put("helpers", HandbookHelperPreview.report());
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

''' + scene[end:]
scene = scene.replace("import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;\n", "")
scene = scene.replace("import net.minecraft.world.item.crafting.RecipeMap;\n", "")
scene = scene.replace("if (RecipesRecord.CLIENTSIDE == null || RecipesRecord.CLIENTSIDE.values().isEmpty()) return;",
    "if (client.level.getRecipeManager().getRecipes().isEmpty()) return;")
scene = scene.replace("Identifier", "ResourceLocation").replace("GuiGraphicsExtractor", "GuiGraphics")
scene = scene.replace("client.resizeGui()", "client.resizeDisplay()")
scene = scene.replace("client.getMainRenderTarget(), 1,", "client.getMainRenderTarget(),")
scene = scene.replace("extractRenderState", "render").replace("graphics.text(", "graphics.drawString(")
scene = scene.replace(".pushMatrix()", ".pushPose()").replace(".popMatrix()", ".popPose()")
scene = scene.replace("graphics.pose().translate(x, y)", "graphics.pose().translate(x, y, 0)")
scene = scene.replace("handbook-recipes-26.1", "handbook-recipes-1.21")
(folder / "HandbookRecipeClientScene.java").write_text(scene, encoding="utf-8", newline="\r\n")
helper = (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/HandbookHelperPreview.java").read_text(encoding="utf-8")
helper = helper.replace("GuiGraphicsExtractor", "GuiGraphics").replace("import net.minecraft.client.renderer.RenderPipelines;\n", "")
helper = helper.replace(".pushMatrix()", ".pushPose()").replace(".popMatrix()", ".popPose()")
helper = helper.replace("graphics.pose().scale(2, 2)", "graphics.pose().scale(2, 2, 1)")
helper = helper.replace("graphics.blit(RenderPipelines.GUI_TEXTURED, ", "graphics.blit(")
# Keep the flat test background behind the complete 3D model, including negative depth.
helper = helper.replace("graphics.fill(0, 0, width, height, 0xFFF1E6CD);",
    "graphics.pose().pushPose(); graphics.pose().translate(0, 0, -1000); "
    "graphics.fill(0, 0, width, height, 0xFFF1E6CD); graphics.pose().popPose();")
helper = helper.replace("new ItemStack(Blocks.ANVIL), graphics,", "new ItemStack(Blocks.ANVIL), graphics.pose(),")
helper = helper.replace("AgeratumUtil.renderBlock(context, state, -1, -1, x, y);",
    "AgeratumUtil.renderBlock(context, state, -1, -1, x, y, x == 280 ? (274 - y) / 14 * 10 : 0);")
(folder / "HandbookHelperPreview.java").write_text(helper, encoding="utf-8", newline="\r\n")
wrapper = (folder / "MonolithReferenceScene.java").read_text(encoding="utf-8")
wrapper = wrapper.replace("MonolithReferenceScene", "HandbookReferenceScene").replace("MonolithClientScene", "HandbookRecipeClientScene")
wrapper = wrapper.replace("portMonolithScene", "portHandbookRecipeScene")
(folder / "HandbookReferenceScene.java").write_text(wrapper, encoding="utf-8", newline="\r\n")
input_path = reference / "run/mun-reference/handbook-recipes-input.json"
input_path.parent.mkdir(parents=True, exist_ok=True)
input_path.write_bytes((root / "run/port-validation/client/handbook-recipes-26.1.json").read_bytes())
p = reference / "build.gradle"
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", p.read_text(encoding="utf-8"))
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portHandbookRecipeScene', 'true'; systemProperty 'anvilcraft.portHandbookNewAfterGallery', 'true' }\n"
p.write_text(s, encoding="utf-8", newline="\r\n")
