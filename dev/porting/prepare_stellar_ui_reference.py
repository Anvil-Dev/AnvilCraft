"""Install the same evolution-panel fixture in the local 1.21 reference worktree."""
from pathlib import Path
import runpy
import sys

root = Path(__file__).resolve().parents[2]
runpy.run_path(str(root / "dev/porting/prepare_stellar_evolution_reference.py"), run_name="__main__")
reference = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else root / "build/porting/reference-frost-1.21"
relative = "dev/dubhe/anvilcraft/porting/StellarEvolutionUiClientScene.java"
scene = (root / "dev/porting/java" / relative).read_text(encoding="utf-8")
scene = scene.replace("import net.minecraft.world.level.storage.TagValueInput;\n", "")
scene = scene.replace("import net.minecraft.util.ProblemReporter;\n", "")
scene = scene.replace(".setTimeFromServer(", ".setGameTime(")
scene = scene.replace("be.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, client.level.registryAccess(), tag))",
                      "be.loadCustomOnly(tag, client.level.registryAccess())")
scene = scene.replace("GuiGraphicsExtractor", "GuiGraphics").replace("extractRenderState(", "render(")
scene = scene.replace("ModMenuTypes.CELESTIAL_FORGING_ANVIL", "ModMenuTypes.CFA")
scene = scene.replace("animatedTexturesStates", "animatedTextures")
scene = scene.replace("stellar-ui-26.1-", "stellar-ui-1.21-").replace("cfa-preview-26.1-", "cfa-preview-1.21-")
scene = scene.replace("client.getMainRenderTarget(), 1,", "client.getMainRenderTarget(),")
(reference / "src/main/java" / relative).write_bytes(scene.replace("\n", "\r\n").encode("utf-8"))
wrapper = reference / "src/main/java/dev/dubhe/anvilcraft/porting/CfaItemReferenceScene.java"
text = wrapper.read_text(encoding="utf-8").replace("StellarEvolutionClientScene.frame(client)",
                                               "StellarEvolutionUiClientScene.frame(client)")
wrapper.write_bytes(text.replace("\n", "\r\n").encode("utf-8"))
