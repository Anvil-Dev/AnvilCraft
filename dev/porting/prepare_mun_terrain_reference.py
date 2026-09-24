"""Install the source/native terrain sampler on the current task-owned Mun reference checkout."""
from pathlib import Path
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
assert subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root) == subprocess.check_output(
    ["git", "rev-parse", "HEAD"], cwd=reference)
base = reference / "src/main/java/dev/dubhe/anvilcraft/porting"
base.mkdir(parents=True, exist_ok=True)
sample = (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/MunTerrainSamples.java").read_text(encoding="utf8")
sample = sample.replace("import net.minecraft.world.entity.EntitySpawnReason;\n", "")
sample = sample.replace("EntityType.PIG.create(level, EntitySpawnReason.COMMAND)", "EntityType.PIG.create(level)")
(base / "MunTerrainSamples.java").write_bytes(sample.replace("\n", "\r\n").encode())
wrapper = '''package dev.dubhe.anvilcraft.porting;

@net.neoforged.fml.common.EventBusSubscriber(modid = "anvilcraft", value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class MunTerrainReferenceScene {
    private static boolean creating;
    private static boolean started;
    private static volatile boolean finished;
    private static boolean stopping;
    private static volatile Throwable failure;

    @net.neoforged.bus.api.SubscribeEvent
    public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) {
        if (!Boolean.getBoolean("anvilcraft.portMunTerrain")) return;
        var client = net.minecraft.client.Minecraft.getInstance();
        client.options.pauseOnLostFocus = false;
        if (!creating) {
            if (client.screen == null || client.getOverlay() != null) return;
            creating = true;
            String name = "mun-terrain-reference-" + System.currentTimeMillis();
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
        if (failure != null) throw new IllegalStateException(failure);
        if (finished) {
            if (stopping) return;
            stopping = true;
            dev.dubhe.anvilcraft.AnvilCraft.LOGGER.info("PORT_MUN_TERRAIN_ORACLE_PASSED");
            client.stop();
            return;
        }
        if (started || client.player == null || client.getSingleplayerServer() == null) return;
        started = true;
        client.setScreen(null);
        client.getSingleplayerServer().execute(() -> {
            try {
                var level = client.getSingleplayerServer().getLevel(
                    dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager.MUN_LEVEL);
                if (level == null) throw new IllegalStateException("Builtin Mun dimension missing");
                var rows = MunTerrainSamples.sample(level);
                java.nio.file.Files.write(java.nio.file.Path.of(System.getProperty("anvilcraft.portMunOutput")), rows);
                finished = true;
            } catch (Throwable error) {
                failure = error;
            }
        });
    }
}
'''
(base / "MunTerrainReferenceScene.java").write_bytes(wrapper.replace("\n", "\r\n").encode())
p = reference / "build.gradle"
s = p.read_text(encoding="utf8")
if "anvilcraft.portMunTerrain" not in s:
    s += "\nneoForge.runs.client {\n    gameDirectory = file('run/mun-reference')\n"
    s += "    systemProperty 'anvilcraft.portMunTerrain', 'true'\n"
    output = (root / "build/porting/mun-terrain-source.csv").as_posix()
    s += f"    systemProperty 'anvilcraft.portMunOutput', '{output}'\n"
    s += "    programArguments.addAll '--width', '1280', '--height', '720'\n}\n"
p.write_bytes(s.replace("\n", "\r\n").encode())
