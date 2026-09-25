"""Reuse the task-owned reference launcher with the fixed world evolution fixture."""
from pathlib import Path
import runpy
import sys

root = Path(__file__).resolve().parents[2]
runpy.run_path(str(root / "dev/porting/prepare_cfa_item_reference.py"), run_name="__main__")
reference = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else root / "build/porting/reference-frost-1.21"
relative = "dev/dubhe/anvilcraft/porting/StellarEvolutionClientScene.java"
scene = (root / "dev/porting/java" / relative).read_text(encoding="utf-8")
scene = scene.replace("            StellarEvolutionRendererChecks.verify();\n", "")
scene = scene.replace("        StellarEvolutionRendererChecks.verifyBounds(be);\n", "")
scene = scene.replace("import net.minecraft.world.level.storage.TagValueInput;\n", "")
scene = scene.replace("import net.minecraft.util.ProblemReporter;\n", "")
scene = scene.replace("client.level.setTimeFromServer(500)", "client.level.setGameTime(500)")
scene = scene.replace("be.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, client.level.registryAccess(), tag))",
                      "be.loadCustomOnly(tag, client.level.registryAccess())")
scene = scene.replace("stellar-evolution-26.1-", "stellar-evolution-1.21-")
scene = scene.replace("client.getMainRenderTarget(), 1,", "client.getMainRenderTarget(),")
(reference / "src/main/java" / relative).write_bytes(scene.replace("\n", "\r\n").encode("utf-8"))
wrapper = reference / "src/main/java/dev/dubhe/anvilcraft/porting/CfaItemReferenceScene.java"
text = wrapper.read_text(encoding="utf-8").replace("CelestialAnvilItemClientScene.frame(client)",
                                               "StellarEvolutionClientScene.frame(client)")
wrapper.write_bytes(text.replace("\n", "\r\n").encode("utf-8"))
