"""Replay real handbook sidebar clicks and wheel input on the source client."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
assert subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root).strip() == subprocess.check_output(
    ["git", "rev-parse", "HEAD"], cwd=reference).strip()
folder = reference / "src/main/java/dev/dubhe/anvilcraft/porting"
scene = (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/HandbookSidebarScene.java").read_text(encoding="utf-8")
scene = scene.replace("Identifier", "ResourceLocation")
scene = scene.replace("import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;\n", "")
scene = scene.replace("import net.minecraft.client.input.MouseButtonEvent;\n", "")
scene = scene.replace("import net.minecraft.client.input.MouseButtonInfo;\n", "")
scene = scene.replace("if (RecipesRecord.CLIENTSIDE == null || RecipesRecord.CLIENTSIDE.values().isEmpty()) return;",
    "if (client.level.getRecipeManager().getRecipes().isEmpty()) return;")
scene = scene.replace("client.resizeGui()", "client.resizeDisplay()")
scene = scene.replace("screen.resize(client.getWindow()", "screen.resize(client, client.getWindow()")
scene = scene.replace("screen.mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)), false)",
    "screen.mouseClicked(x, y, 0)")
scene = scene.replace("client.getMainRenderTarget(), 1,", "client.getMainRenderTarget(),")
scene = scene.replace("handbook-sidebar-26.1", "handbook-sidebar-1.21")
(folder / "HandbookSidebarScene.java").write_text(scene, encoding="utf-8", newline="\r\n")
wrapper = (folder / "MonolithReferenceScene.java").read_text(encoding="utf-8")
wrapper = wrapper.replace("MonolithReferenceScene", "HandbookSidebarReferenceScene")
wrapper = wrapper.replace("MonolithClientScene", "HandbookSidebarScene").replace("portMonolithScene", "portHandbookSidebarScene")
(folder / "HandbookSidebarReferenceScene.java").write_text(wrapper, encoding="utf-8", newline="\r\n")
p = reference / "build.gradle"
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", p.read_text(encoding="utf-8"))
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portHandbookSidebarScene', 'true' }\n"
p.write_text(s, encoding="utf-8", newline="\r\n")
