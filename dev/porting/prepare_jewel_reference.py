"""Run identical jewel-menu hint/output cases in the local 1.21 reference client."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
assert subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root).strip() == subprocess.check_output(
    ["git", "rev-parse", "HEAD"], cwd=reference).strip()
folder = reference / "src/main/java/dev/dubhe/anvilcraft/porting"
scene = (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/JewelClientScene.java").read_text(encoding="utf-8")
scene = scene.replace("import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;", "import dev.dubhe.anvilcraft.recipe.anvil.cache.RecipeCaches;")
scene = scene.replace("if (RecipesRecord.CLIENTSIDE == null || RecipesRecord.CLIENTSIDE.values().isEmpty()) return;",
    "if (client.level.getRecipeManager().getRecipes().isEmpty()) return;")
scene = scene.replace("client.resizeGui()", "client.resizeDisplay()")
scene = scene.replace("menu.findRecipeBySource(sample).value()", "RecipeCaches.getJewelRecipeByResult(sample).value()")
scene = scene.replace("recipe.mergedIngredients()", "recipe.getMergedIngredients()")
scene = scene.replace("entry.getKey().getValues().get(0).value()", "entry.getKey().getItems()[0].getItem()")
scene = scene.replace("client.getMainRenderTarget(), 1,", "client.getMainRenderTarget(),")
scene = scene.replace("jewel-source-26.1", "jewel-source-1.21")
(folder / "JewelClientScene.java").write_text(scene, encoding="utf-8", newline="\r\n")
wrapper = (folder / "MonolithReferenceScene.java").read_text(encoding="utf-8")
wrapper = wrapper.replace("MonolithReferenceScene", "JewelReferenceScene").replace("MonolithClientScene", "JewelClientScene")
wrapper = wrapper.replace("portMonolithScene", "portJewelScene")
(folder / "JewelReferenceScene.java").write_text(wrapper, encoding="utf-8", newline="\r\n")
p = reference / "build.gradle"
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", p.read_text(encoding="utf-8"))
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portJewelScene', 'true' }\n"
p.write_text(s, encoding="utf-8", newline="\r\n")
