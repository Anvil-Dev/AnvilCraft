package dev.dubhe.anvilcraft.porting;

import com.google.gson.GsonBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.api.ui.TextElement;
import snownee.jade.impl.Tooltip;
import snownee.jade.impl.WailaClientRegistration;
import snownee.jade.overlay.OverlayRenderer;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class CursedGoldClientScene {
    private static final BlockPos POS = new BlockPos(0, 162, 0);
    private static final List<List<String>> RESULTS = new ArrayList<>();
    private static boolean installed;
    private static boolean capturing;
    private static boolean observed;
    private static volatile boolean prepared;
    private static volatile boolean reloaded = true;
    private static List<String> lines = List.of();
    private static long deadline;
    private static long next;
    private static long stable;

    public static void frame(Minecraft client) {
        if (RESULTS.size() >= 2) return;
        if (deadline == 0) deadline = System.currentTimeMillis() + 180000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Cursed gold Jade tooltip was not observed");
        IWailaConfig.get().overlay().setOverlayPosX(0.5F);
        IWailaConfig.get().overlay().setOverlayPosY(0.98F);
        IWailaConfig.get().overlay().setAnchorX(0.5F);
        IWailaConfig.get().overlay().setAnchorY(0.0F);
        IWailaConfig.get().overlay().setAnimation(false);
        if (capturing || !reloaded || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        if (!installed) {
            installed = true;
            WailaClientRegistration.instance().addTooltipCollectedCallback((box, accessor) -> {
                if (accessor instanceof BlockAccessor block && block.getPosition().equals(POS)
                    && block.getBlockState().is(ModBlocks.CURSED_GOLD_BLOCK)) {
                    lines = readLines(box.getTooltip());
                    observed = true;
                }
            });
            WailaClientRegistration.instance().tooltipCollectedCallback.sort();
            client.options.guiScale().set(2);
            client.resizeGui();
            client.setScreen(null);
            client.getSingleplayerServer().execute(() -> {
                var server = client.getSingleplayerServer();
                var level = server.overworld();
                for (BlockPos pos : BlockPos.betweenClosed(POS.offset(-3, -1, -3), POS.offset(3, 3, 6))) {
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
                level.setBlockAndUpdate(POS.below(), Blocks.GRAY_CONCRETE.defaultBlockState());
                level.setBlockAndUpdate(POS, ModBlocks.CURSED_GOLD_BLOCK.getDefaultState());
                server.getPlayerList().getPlayers().forEach(player -> {
                    player.setNoGravity(true);
                    player.getInventory().clearContent();
                    player.containerMenu.broadcastChanges();
                });
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set 6000");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0.5 162 4.5 180 16");
                prepared = true;
            });
            next = System.currentTimeMillis() + 4000;
            return;
        }
        if (!prepared || !observed || client.screen != null) return;
        if (!OverlayRenderer.shown) return;
        if (stable == 0) stable = System.currentTimeMillis() + 500;
        if (System.currentTimeMillis() < stable) return;
        var rect = OverlayRenderer.animation.rect;
        AnvilCraft.LOGGER.info("PORT_CURSED_GOLD_RECT: {},{} {}x{}", rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
        if (rect.getY() < 0 || rect.getY() + rect.getHeight() > client.getWindow().getGuiScaledHeight()) {
            throw new IllegalStateException("Jade tooltip is outside the visible viewport");
        }
        String expected = Component.translatable("jade.ench_power", "-1").getString();
        if (lines.stream().filter(expected::equals).count() != 1) {
            throw new IllegalStateException("Expected exactly one negative enchanting power line: " + lines);
        }
        RESULTS.add(List.copyOf(lines));
        capturing = true;
        Screenshot.grab(client.gameDirectory, "cursed-gold-26.1-" + RESULTS.size() + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                if (RESULTS.size() == 1) {
                    observed = false;
                    stable = 0;
                    reloaded = false;
                    client.reloadResourcePacks().thenRun(() -> reloaded = true);
                    next = System.currentTimeMillis() + 3000;
                } else {
                    save(client);
                }
            }));
    }

    private static List<String> readLines(Tooltip tooltip) {
        List<String> result = new ArrayList<>();
        for (var line : tooltip.lines) {
            for (var element : line.elements()) {
                if (element instanceof TextElement text) result.add(text.getString());
            }
        }
        return result;
    }

    private static void save(Minecraft client) {
        if (!RESULTS.getFirst().equals(RESULTS.getLast())) throw new IllegalStateException("Tooltip changed after resource reload");
        try {
            Files.writeString(client.gameDirectory.toPath().resolve("cursed-gold-26.1.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(RESULTS));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
        AnvilCraft.LOGGER.info("PORT_CURSED_GOLD_JADE_PASSED: {}", RESULTS);
        client.stop();
    }
}
