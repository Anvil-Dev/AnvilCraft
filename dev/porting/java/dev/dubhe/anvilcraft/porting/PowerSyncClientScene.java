package dev.dubhe.anvilcraft.porting;

import com.google.gson.GsonBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.power.PowerSyncFixture;
import dev.dubhe.anvilcraft.api.tooltip.impl.ChargerTooltipProvider;
import dev.dubhe.anvilcraft.api.tooltip.impl.DischargerTooltipProvider;
import dev.dubhe.anvilcraft.api.tooltip.impl.HeatCollectorTooltipProvider;
import dev.dubhe.anvilcraft.api.tooltip.impl.PowerComponentTooltipProvider;
import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.client.hud.PowerGridHUD;
import dev.dubhe.anvilcraft.client.support.PowerGridSupport;
import dev.dubhe.anvilcraft.init.ModDataAttachments;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.network.PowerGridRemovePacket;
import dev.dubhe.anvilcraft.network.PowerGridSyncChunkPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.EntityBlock;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PowerSyncClientScene {
    private static final String[] CASES = {"normal", "overloaded", "outside", "hidden", "creative", "spectator"};
    private static final Map<String, Object> RESULT = new LinkedHashMap<>();
    private static volatile PowerGrid serverGrid;
    private static volatile PowerGridSyncChunkPacket[] pending;
    private static volatile int id;
    private static int stage;
    private static int hudCase;
    private static long next;
    private static long deadline;
    private static boolean capturing;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 180000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Power sync stage " + stage);
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        if (stage == 0) {
            client.options.guiScale().set(2);
            client.resizeGui();
            client.setScreen(null);
            client.getSingleplayerServer().execute(() -> {
                var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.SURVIVAL);
                player.getAbilities().mayfly = true;
                player.getAbilities().flying = true;
                player.getAbilities().invulnerable = true;
                player.onUpdateAbilities();
                player.teleportTo(player.level(), 0.5, 163, 0.5, Set.of(), 180, 0, true);
                serverGrid = PowerSyncFixture.create(player.level(), 1025);
                id = serverGrid.hashCode();
                PowerGridSyncChunkPacket.sendToPlayer(serverGrid, player);
            });
            stage = 1;
        } else if (stage == 1) {
            var grid = PowerGridSupport.getGridMap().get(id);
            if (grid == null || grid.getPowerComponentInfoList().size() != 1025) return;
            if (grid.getGenerate() != 12345 || grid.getConsume() != 2345 || !grid.isInfinitePower()) {
                throw new IllegalStateException("Remote summary changed");
            }
            for (var info : grid.getPowerComponentInfoList()) {
                int index = info.pos().getX() - 100000;
                if (info.produces() != 100 + index || info.infinitePower() != (index == 0)) {
                    throw new IllegalStateException("Remote component field changed");
                }
            }
            RESULT.put("remote_components", grid.getPowerComponentInfoList().size());
            tooltips(client);
            client.getSingleplayerServer().execute(() -> {
                PowerSyncFixture.reset(serverGrid, 257);
                pending = PowerSyncFixture.chunks(serverGrid);
                PacketDistributor.sendToPlayer(client.getSingleplayerServer().getPlayerList().getPlayers().getFirst(), pending[1]);
            });
            stage = 2;
            next = System.currentTimeMillis() + 1000;
        } else if (stage == 2) {
            if (PowerGridSupport.getGridMap().get(id).getPowerComponentInfoList().size() != 1025) {
                throw new IllegalStateException("Partial live packet replaced the old grid");
            }
            client.getSingleplayerServer().execute(() -> PacketDistributor.sendToPlayer(
                client.getSingleplayerServer().getPlayerList().getPlayers().getFirst(), pending[0]));
            stage = 3;
        } else if (stage == 3) {
            if (PowerGridSupport.getGridMap().get(id).getPowerComponentInfoList().size() != 257) return;
            RESULT.put("replacement_components", 257);
            client.getSingleplayerServer().execute(() -> PacketDistributor.sendToPlayer(
                client.getSingleplayerServer().getPlayerList().getPlayers().getFirst(), new PowerGridRemovePacket(id)));
            stage = 4;
        } else if (stage == 4) {
            if (PowerGridSupport.getGridMap().containsKey(id)) return;
            transitions(client);
            configure(client);
        } else if (stage == 5) {
            client.gui.getChat().clearMessages(true);
            capturing = true;
            Screenshot.grab(client.gameDirectory, "power-hud-26.1-" + CASES[hudCase] + ".png", client.getMainRenderTarget(), 1,
                message -> client.execute(() -> {
                    capturing = false;
                    AnvilCraft.LOGGER.info("PORT_POWER_HUD_CAPTURED: {}", CASES[hudCase]);
                    if (++hudCase == CASES.length) save(client);
                    else configure(client);
                }));
        }
    }

    private static void tooltips(Minecraft client) {
        var blocks = List.of(ModBlocks.CREATIVE_GENERATOR.get(), ModBlocks.HEAT_COLLECTOR.get(),
            ModBlocks.CHARGER.get(), ModBlocks.DISCHARGER.get());
        List<ITooltipProvider.BlockEntityTooltipProvider> providers = List.of(new PowerComponentTooltipProvider(),
            new HeatCollectorTooltipProvider(), new ChargerTooltipProvider(), new DischargerTooltipProvider());
        var all = new LinkedHashMap<String, List<String>>();
        for (int i = 0; i < blocks.size(); i++) {
            var block = blocks.get(i);
            var entity = ((EntityBlock) block).newBlockEntity(new BlockPos(100000, 160, 100000), block.defaultBlockState());
            entity.setLevel(client.level);
            List<String> lines = providers.get(i).tooltip(entity).stream().map(Component::getString).toList();
            if (lines.stream().noneMatch(text -> text.contains("∞"))) throw new IllegalStateException("Missing remote infinity " + i);
            all.put(Integer.toString(i), lines);
        }
        RESULT.put("tooltips", all);
    }

    private static void transitions(Minecraft client) {
        try {
            var value = PowerGridHUD.class.getDeclaredField("visibility");
            var old = PowerGridHUD.class.getDeclaredField("previousVisibility");
            value.setAccessible(true);
            old.setAccessible(true);
            value.setFloat(null, 0);
            old.setFloat(null, 0);
            var samples = new ArrayList<Float>();
            for (boolean inside : new boolean[]{true, false}) {
                client.player.setData(ModDataAttachments.IN_POWER_GRID, inside);
                for (int step = 1; step <= 4; step++) {
                    PowerGridHUD.onClientTick(new ClientTickEvent.Post());
                    float actual = value.getFloat(null);
                    float expected = inside ? step / 4F : 1 - step / 4F;
                    if (actual != expected) throw new IllegalStateException("Visibility transition " + actual);
                    samples.add(actual);
                }
            }
            RESULT.put("visibility_steps", samples);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void configure(Minecraft client) {
        client.options.hideGui = hudCase == 3;
        client.player.setData(ModDataAttachments.IN_POWER_GRID, hudCase != 2);
        client.player.setData(ModDataAttachments.POWER_GRID_OVERLOADED, hudCase == 1);
        if (hudCase >= 4) {
            client.getSingleplayerServer().execute(() -> client.getSingleplayerServer().getPlayerList().getPlayers().getFirst()
                .setGameMode(hudCase == 4 ? GameType.CREATIVE : GameType.SPECTATOR));
        }
        stage = 5;
        next = System.currentTimeMillis() + 1500;
    }

    private static void save(Minecraft client) {
        stage = 6;
        try {
            Files.writeString(client.gameDirectory.toPath().resolve("power-sync-26.1.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(RESULT));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
        AnvilCraft.LOGGER.info("PORT_POWER_SYNC_CLIENT_PASSED: 1025 remote components, atomic replacement/removal, tooltips and HUD");
        client.stop();
    }
}
