"""Run the real handbook parser, directory rendering and click audit on 1.21."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
assert subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root).strip() == subprocess.check_output(
    ["git", "rev-parse", "HEAD"], cwd=reference).strip()
folder = reference / "src/main/java/dev/dubhe/anvilcraft/porting"
scene = (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/HandbookPagesClientScene.java").read_text(encoding="utf-8")
scene = scene.replace("Identifier", "ResourceLocation")
scene = scene.replace("client.resizeGui()", "client.resizeDisplay()")
scene = scene.replace("<directory/>", "<directory />").replace("<ageratum:directory/>", "<ageratum:directory />")
scene = scene.replace("import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;\n", "")
scene = scene.replace("import net.minecraft.client.input.MouseButtonEvent;\n", "")
scene = scene.replace("import net.minecraft.client.input.MouseButtonInfo;\n", "")
scene = scene.replace("if (RecipesRecord.CLIENTSIDE == null || RecipesRecord.CLIENTSIDE.values().isEmpty()) return;",
    "if (client.level.getRecipeManager().getRecipes().isEmpty()) return;")
scene = scene.replace("RecipesRecord.CLIENTSIDE.byKey(ResourceKey.create(Registries.RECIPE, id)) == null",
    "client.level.getRecipeManager().byKey(id).isEmpty()")
scene = scene.replace("style.getClickEvent() instanceof ClickEvent.OpenUrl link && link.uri().toString().startsWith(\"#\")",
    "style.getClickEvent() != null && style.getClickEvent().getAction() == ClickEvent.Action.OPEN_URL && link.getValue().startsWith(\"#\")")
scene = scene.replace("style.getClickEvent() instanceof ClickEvent.OpenUrl any) observed.add(any.uri().toString())",
    "style.getClickEvent() != null) observed.add(style.getClickEvent().getValue())")
scene = scene.replace("style.getClickEvent() instanceof ClickEvent.OpenUrl link",
    "style.getClickEvent() != null")
scene = scene.replace("link.uri().toString()", "style.getClickEvent().getValue()")
scene = scene.replace("link.getValue()", "style.getClickEvent().getValue()")
scene = scene.replace("this.mouseClicked(new MouseButtonEvent(x / this.scale, y / this.scale, new MouseButtonInfo(0, 0)), false)",
    "this.mouseClicked(x / this.scale, y / this.scale, 0)")
scene = scene.replace("client.getMainRenderTarget(), 1,", "client.getMainRenderTarget(),")
scene = scene.replace("handbook-pages-26.1", "handbook-pages-1.21")
(folder / "HandbookPagesClientScene.java").write_text(scene, encoding="utf-8", newline="\r\n")
probe = (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/HandbookStyleProbe.java").read_text(encoding="utf-8")
probe = probe.replace("import net.minecraft.client.input.MouseButtonEvent;\n", "")
probe = probe.replace("import net.minecraft.client.input.MouseButtonInfo;\n", "")
probe = probe.replace("new MouseButtonEvent((this.leftPos + x) / this.scale, (this.topPos + y) / this.scale,\n                new MouseButtonInfo(0, 0)), false", "(this.leftPos + x) / this.scale, (this.topPos + y) / this.scale, 0")
(folder / "HandbookStyleProbe.java").write_text(probe, encoding="utf-8", newline="\r\n")
wrapper = (folder / "MonolithReferenceScene.java").read_text(encoding="utf-8")
wrapper = wrapper.replace("MonolithReferenceScene", "HandbookPagesReferenceScene").replace("MonolithClientScene", "HandbookPagesClientScene")
wrapper = wrapper.replace("portMonolithScene", "portHandbookPagesScene")
(folder / "HandbookPagesReferenceScene.java").write_text(wrapper, encoding="utf-8", newline="\r\n")
p = reference / "build.gradle"
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", p.read_text(encoding="utf-8"))
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portHandbookPagesScene', 'true' }\n"
p.write_text(s, encoding="utf-8", newline="\r\n")
