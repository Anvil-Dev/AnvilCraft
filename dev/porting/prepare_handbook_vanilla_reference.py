"""Replay the standard recipe display fixtures with unmodified 1.21 components."""
from pathlib import Path
import json
import re
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
assert subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root).strip() == subprocess.check_output(
    ["git", "rev-parse", "HEAD"], cwd=reference).strip()
folder = reference / "src/main/java/dev/dubhe/anvilcraft/porting"
scene = (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/HandbookVanillaScene.java").read_text(encoding="utf-8")
scene = scene.replace("import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;\n", "")
scene = scene.replace("if (RecipesRecord.CLIENTSIDE == null || RecipesRecord.CLIENTSIDE.values().isEmpty()) return;",
    "if (client.level.getRecipeManager().getRecipes().isEmpty()) return;")
scene = scene.replace("Identifier", "ResourceLocation").replace("GuiGraphicsExtractor", "GuiGraphics")
scene = scene.replace("client.resizeGui()", "client.resizeDisplay()")
scene = scene.replace("client.getMainRenderTarget(), 1,", "client.getMainRenderTarget(),")
scene = scene.replace("        proxy.getHeight(client, 128, 128);", """        try {
            var setter = proxy.getClass().getDeclaredMethod("setComponent", net.minecraft.world.item.crafting.RecipeHolder.class);
            setter.setAccessible(true);
            setter.invoke(proxy, client.level.getRecipeManager().byKey(AnvilCraft.of(recipe)).orElseThrow());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
        proxy.getHeight(client, 128, 128);""")
scene = scene.replace("ingredient.getValues().stream().map(holder -> BuiltInRegistries.ITEM.getKey(holder.value()).toString())",
    "java.util.Arrays.stream(ingredient.getItems()).map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())")
scene = scene.replace("extractRenderState", "render").replace("graphics.text(", "graphics.drawString(")
scene = scene.replace(".pushMatrix()", ".pushPose()").replace(".popMatrix()", ".popPose()")
scene = scene.replace("graphics.pose().translate(80, 80)", "graphics.pose().translate(80, 80, 0)")
scene = scene.replace("graphics.pose().scale(3, 3)", "graphics.pose().scale(3, 3, 1)")
scene = scene.replace("handbook-vanilla-26.1", "handbook-vanilla-1.21")
(folder / "HandbookVanillaScene.java").write_text(scene, encoding="utf-8", newline="\r\n")
for path in (root / "dev/porting/resources/data/anvilcraft/recipe/port_handbook").glob("vanilla_*.json"):
    data = json.loads(path.read_bytes())
    def ingredient(value):
        return {"tag": value[1:]} if value.startswith("#") else {"item": value}
    if "key" in data:
        data["key"] = {key: ingredient(value) for key, value in data["key"].items()}
    if "ingredients" in data:
        data["ingredients"] = [ingredient(value) for value in data["ingredients"]]
    if "ingredient" in data:
        data["ingredient"] = ingredient(data["ingredient"])
    target = reference / "src/main/resources/data/anvilcraft/recipe/port_handbook" / path.name
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
wrapper = (folder / "MonolithReferenceScene.java").read_text(encoding="utf-8")
wrapper = wrapper.replace("MonolithReferenceScene", "HandbookVanillaReferenceScene")
wrapper = wrapper.replace("MonolithClientScene", "HandbookVanillaScene").replace("portMonolithScene", "portHandbookVanillaScene")
(folder / "HandbookVanillaReferenceScene.java").write_text(wrapper, encoding="utf-8", newline="\r\n")
p = reference / "build.gradle"
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", p.read_text(encoding="utf-8"))
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portHandbookVanillaScene', 'true' }\n"
p.write_text(s, encoding="utf-8", newline="\r\n")
