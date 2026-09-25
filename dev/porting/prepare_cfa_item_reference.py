"""Install the same visual fixture in the task-owned local 1.21 reference worktree."""
from pathlib import Path
import subprocess
import sys
import re

root = Path(__file__).resolve().parents[2]
reference = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 and not sys.argv[1].startswith("--") else root / "build/porting/reference-frost-1.21"
if not reference.is_relative_to((root / "build/porting").resolve()):
    raise SystemExit("Reference must be inside the task-owned build/porting directory")
source_ref = subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root, text=True).strip()
reference_ref = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=reference, text=True).strip()
if source_ref != reference_ref:
    raise SystemExit("Reference worktree must match the latest local source branch")
relative = "src/main/java/dev/dubhe/anvilcraft/porting/CelestialAnvilItemClientScene.java"
scene = (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/CelestialAnvilItemClientScene.java").read_text(encoding="utf-8")
scene = scene.replace("import net.minecraft.world.level.storage.TagValueOutput;\n", "")
scene = scene.replace("import net.minecraft.util.ProblemReporter;\n", "")
scene = scene.replace("client.getToastManager()", "client.getToasts()")
scene = scene.replace("        if (GATEWAY && stage == 1 && client.getOverlay() == null) GatewayItemCacheProbe.sample(client);\n", "")
scene = scene.replace("        if (GATEWAY && name.equals(\"gallery\")) GatewayItemCacheProbe.verify();\n", "")
scene = scene.replace("client.level.setTimeFromServer(500)", "client.level.setGameTime(500)")
scene = scene.replace("client.player.getGameProfile().name()", "client.player.getGameProfile().getName()")
scene = scene.replace('profile.store("id", UUIDUtil.CODEC, client.player.getUUID());', 'profile.putUUID("id", client.player.getUUID());')
scene = scene.replace('cfa-item-26.1-', 'cfa-item-1.21-').replace('stellar-26.1-', 'stellar-1.21-')
scene = scene.replace("client.getMainRenderTarget(), 1,", "client.getMainRenderTarget(),")
scene = scene.replace("GuiGraphicsExtractor", "GuiGraphics")
scene = scene.replace("extractRenderState(GuiGraphics", "render(GuiGraphics")
scene = scene.replace(".pushMatrix()", ".pushPose()").replace(".popMatrix()", ".popPose()")
scene = scene.replace(".translate(30 + (index % 7) * 84, (STELLAR ? 60 + (index / 7) * 150 : 90))", ".translate(30 + (index % 7) * 84, (STELLAR ? 60 + (index / 7) * 150 : 90), 0)")
scene = scene.replace(".scale(3)", ".scale(3, 3, 3)").replace("graphics.item(", "graphics.renderItem(").replace("graphics.text(", "graphics.drawString(")
scene = scene.replace("        var output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);\n        output.store(tag);\n        BlockItem.setBlockEntityData(stack, ModBlockEntities.CELESTIAL_FORGING_ANVIL.get(), output);", "        BlockItem.setBlockEntityData(stack, ModBlockEntities.CELESTIAL_FORGING_ANVIL.get(), tag);")
start = scene.index("    private static void verify() {")
end = scene.index("    private static void supply(", start)
scene = scene[:start] + scene[end:]
scene = scene.replace("                verify();\n", "").replace("        verify();\n", "")
scene = scene.replace("PORT_CFA_ITEM_RENDER_PASSED: body data, fitting, head parts, hands and reload", "PORT_CFA_ITEM_REFERENCE_CAPTURED: gallery, hands, head and reload")
(reference / relative).write_bytes(scene.replace("\n", "\r\n").encode("utf-8"))
wrapper = """package dev.dubhe.anvilcraft.porting;

@net.neoforged.fml.common.EventBusSubscriber(modid = "anvilcraft", value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class CfaItemReferenceScene {
    private static boolean creating;
    @net.neoforged.bus.api.SubscribeEvent
    public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) {
        if (!Boolean.getBoolean("anvilcraft.portCfaItemReference")) return;
        var client = net.minecraft.client.Minecraft.getInstance();
        client.options.pauseOnLostFocus = false;
        if (!creating) {
            if (client.screen == null || client.getOverlay() != null) return;
            creating = true;
            String name = "cfa-item-reference-" + System.currentTimeMillis();
            client.createWorldOpenFlows().createFreshLevel(name,
                new net.minecraft.world.level.LevelSettings(name, net.minecraft.world.level.GameType.CREATIVE, false,
                    net.minecraft.world.Difficulty.PEACEFUL, true, new net.minecraft.world.level.GameRules(),
                    net.minecraft.world.level.WorldDataConfiguration.DEFAULT),
                new net.minecraft.world.level.levelgen.WorldOptions(121261L, false, false),
                registries -> registries.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
                    .getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions(),
                client.screen);
            return;
        }
        if (client.player != null && client.level != null && client.getSingleplayerServer() != null) {
            CelestialAnvilItemClientScene.frame(client);
        }
    }
}
"""
(reference / "src/main/java/dev/dubhe/anvilcraft/porting/CfaItemReferenceScene.java").write_text(wrapper, encoding="utf-8")
build = reference / "build.gradle"
text = build.read_text(encoding="utf-8")
text = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", text)
text = re.sub(r"\nneoForge.runs.client \{ systemProperty 'anvilcraft\.portCfaItemReference', '[^']+' \}\n", "\n", text)
text += "\nneoForge.runs.client { systemProperty 'anvilcraft.portCfaItemReference', 'true' }\n"
if "--stellar" in sys.argv:
    text += "\nneoForge.runs.client { systemProperty 'anvilcraft.portStellarScene', 'true' }\n"
build.write_bytes(text.replace("\n", "\r\n").encode("utf-8"))
helper = "dev/dubhe/anvilcraft/porting/SpecialCelestialVisualFixture.java"
(reference / "src/main/java" / helper).write_bytes((root / "dev/porting/java" / helper).read_bytes())
print(source_ref)
