"""Exercise the source export button, chunk transport and server-save output."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
assert subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root).strip() == subprocess.check_output(
    ["git", "rev-parse", "HEAD"], cwd=reference).strip()
folder = reference / "src/main/java/dev/dubhe/anvilcraft/porting"
scene = (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/HandbookExportClientScene.java").read_text(encoding="utf-8")
scene = scene.replace("import dev.dubhe.anvilcraft.util.HandbookStructureExporter;", "import dev.anvilcraft.resource.ageratum.structure.StructureExporter;")
scene = scene.replace("HandbookStructureExporter", "StructureExporter")
scene = scene.replace("Identifier", "ResourceLocation").replace("GuiGraphicsExtractor", "GuiGraphics")
scene = scene.replace("client.resizeGui()", "client.resizeDisplay()")
scene = scene.replace("client.getMainRenderTarget(), 1,", "client.getMainRenderTarget(),")
scene = scene.replace("extractRenderState", "render")
scene = scene.replace(".pushMatrix()", ".pushPose()").replace(".popMatrix()", ".popPose()")
scene = scene.replace("graphics.pose().translate(80, 80)", "graphics.pose().translate(80, 80, 0)")
scene = scene.replace('.getListOrEmpty("blocks")', '.getList("blocks", 10)').replace('.getListOrEmpty("entities")', '.getList("entities", 10)')
scene = scene.replace('.getCompoundOrEmpty(1)', '.getCompound(1)').replace('.getCompoundOrEmpty("nbt")', '.getCompound("nbt")')
scene = scene.replace("handbook-export-26.1", "handbook-export-1.21")
(folder / "HandbookExportClientScene.java").write_text(scene, encoding="utf-8", newline="\r\n")
relative = Path("assets/anvilcraft/ageratum/structures/port_export.snbt")
target = reference / "src/main/resources" / relative
target.parent.mkdir(parents=True, exist_ok=True)
target.write_bytes((root / "dev/porting/resources" / relative).read_bytes())
wrapper = (folder / "MonolithReferenceScene.java").read_text(encoding="utf-8")
wrapper = wrapper.replace("MonolithReferenceScene", "HandbookExportReferenceScene")
wrapper = wrapper.replace("MonolithClientScene", "HandbookExportClientScene").replace("portMonolithScene", "portHandbookExportScene")
(folder / "HandbookExportReferenceScene.java").write_text(wrapper, encoding="utf-8", newline="\r\n")
p = reference / "build.gradle"
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", p.read_text(encoding="utf-8"))
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portHandbookExportScene', 'true' }\n"
p.write_text(s, encoding="utf-8", newline="\r\n")
