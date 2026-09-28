"""Prepare matching live 4D JEI layouts and interactions on the local source checkout."""
from pathlib import Path
import re
import shutil
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
assert subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root).strip() == subprocess.check_output(
    ["git", "rev-parse", "HEAD"], cwd=reference).strip()
folder = reference / "src/main/java/dev/dubhe/anvilcraft/porting"
scene = (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/Multiblock4DJeiScene.java").read_text(encoding="utf-8")
scene = scene.replace("client.resizeGui()", "client.resizeDisplay()")
scene = scene.replace("MultiblockPreviewProbe.verify(level);", "")
scene = scene.replace("Identifier", "ResourceLocation").replace(".id().identifier()", ".id()")
scene = scene.replace("createCraftingStationLookup", "createRecipeCatalystLookup")
scene = scene.replace("client.getMainRenderTarget(), 1,", "client.getMainRenderTarget(),")
scene = scene.replace("GuiGraphicsExtractor", "GuiGraphics").replace("extractRenderState(", "render(")
scene = scene.replace("4d-jei-26.1-", "4d-jei-1.21-")
(folder / "Multiblock4DJeiScene.java").write_text(scene, encoding="utf-8", newline="\r\n")
wrapper = (folder / "MonolithReferenceScene.java").read_text(encoding="utf-8")
wrapper = wrapper.replace("MonolithReferenceScene", "Multiblock4DJeiReferenceScene").replace("MonolithClientScene", "Multiblock4DJeiScene")
wrapper = wrapper.replace("portMonolithScene", "port4dJeiScene")
(folder / "Multiblock4DJeiReferenceScene.java").write_text(wrapper, encoding="utf-8", newline="\r\n")
for name in ["progress", "jei_tags"]:
    recipe = Path("data/anvilcraft/recipe/port_4d") / (name + ".json")
    target = reference / "src/main/resources" / recipe
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(root / "dev/porting/resources" / recipe, target)
p = reference / "build.gradle"
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", p.read_text(encoding="utf-8"))
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.port4dJeiScene', 'true' }\n"
p.write_text(s, encoding="utf-8", newline="\r\n")
