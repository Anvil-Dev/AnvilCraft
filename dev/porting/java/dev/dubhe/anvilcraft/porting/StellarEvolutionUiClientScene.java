package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarEvolutionState;
import dev.dubhe.anvilcraft.client.gui.screen.CelestialForgingAnvilScreen;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.CelestialForgingAnvilMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class StellarEvolutionUiClientScene {
    private static final BlockPos POS = new BlockPos(8, 80, 8);
    private static final List<Case> CASES = List.of(
        new Case("running-top", 49, "rgb", false, false, "en_us"),
        new Case("paused-top", 49, "rgb", true, false, "en_us"),
        new Case("paused-bottom", 49, "rgb", true, true, "en_us"),
        new Case("long-phase", 42, "fully_convective_main_sequence", true, false, "en_us"),
        new Case("supernova-bottom", 56, "supernova", true, true, "en_us"),
        new Case("collapse-bottom", 62, "direct_collapse", true, true, "en_us"),
        new Case("chinese-top", 49, "rgb", true, false, "zh_cn"),
        new Case("chinese-bottom", 49, "rgb", true, true, "zh_cn")
    );
    private static boolean requested;
    private static volatile boolean ready;
    private static volatile boolean reloading;
    private static boolean loaded;
    private static boolean capturing;
    private static boolean scrolled;
    private static int index;
    private static long next;
    private static long deadline;

    private record Case(String name, int mass, String phase, boolean paused, boolean bottom, String language) {
    }

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 240000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Stellar UI timed out at " + index);
        client.options.pauseOnLostFocus = false;
        client.options.guiScale().set(2);
        client.options.hideGui = false;
        client.level.setTimeFromServer(500);
        client.player.setNoGravity(true);
        client.player.setDeltaMovement(Vec3.ZERO);
        client.player.setPos(8.5, 83, 12.5);
        if (!requested) {
            requested = true;
            client.getSingleplayerServer().execute(() -> {
                var server = client.getSingleplayerServer();
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick freeze");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 8.5 83 12.5 180 15");
                server.getPlayerList().getPlayers().getFirst().setNoGravity(true);
                server.overworld().setBlock(POS, ModBlocks.CELESTIAL_FORGING_ANVIL.getDefaultState(), Block.UPDATE_CLIENTS);
                ready = true;
            });
            return;
        }
        if (!ready || reloading || capturing || System.currentTimeMillis() < next) return;
        if (index == CASES.size()) {
            AnvilCraft.LOGGER.info("PORT_STELLAR_UI_CAPTURED: eight layouts and countdown controls");
            client.stop();
            return;
        }
        var sample = CASES.get(index);
        if (!client.getLanguageManager().getSelected().equals(sample.language)) {
            client.getLanguageManager().setSelected(sample.language);
            client.options.languageCode = sample.language;
            reloading = true;
            client.reloadResourcePacks().whenComplete((ignored, error) -> client.execute(() -> {
                if (error != null) throw new IllegalStateException(error);
                reloading = false;
                next = System.currentTimeMillis() + 1500;
            }));
            return;
        }
        if (!(client.level.getBlockEntity(POS) instanceof CelestialForgingAnvilBlockEntity be)) return;
        if (!loaded) {
            var tag = StellarEvolutionClientScene.snapshot(sample.mass, sample.phase, 0.5F, 0);
            if (!sample.paused) {
                var state = StellarEvolutionState.fromTag(tag);
                state.shiftTimeline(500);
                state.save(tag);
                tag.putLong("acceleratorPausedSinceGameTime", -1);
            }
            be.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, client.level.registryAccess(), tag));
            var menu = new CelestialForgingAnvilMenu(ModMenuTypes.CELESTIAL_FORGING_ANVIL.get(), 77, client.player.getInventory(), be);
            var screen = new FixedScreen(menu, client.player.getInventory(), ModBlocks.CELESTIAL_FORGING_ANVIL.get().getName());
            client.setScreen(screen);
            verifyCountdown(screen, sample.paused);
            scrolled = false;
            loaded = true;
            next = System.currentTimeMillis() + 1800;
            return;
        }
        if (client.getOverlay() != null) {
            next = System.currentTimeMillis() + 500;
            return;
        }
        var screen = (CelestialForgingAnvilScreen) client.screen;
        if (sample.bottom && !scrolled) {
            double x = (screen.width - 344) / 2.0 + 180;
            double y = (screen.height - 207) / 2.0 + 35;
            screen.mouseScrolled(x, y, 0, -100);
            scrolled = true;
            next = System.currentTimeMillis() + 700;
            return;
        }
        int scroll = field(screen, "scrollOffset");
        if (sample.bottom && scroll == 0) throw new IllegalStateException("Evolution panel did not scroll");
        AnvilCraft.LOGGER.info("PORT_STELLAR_UI {}: scroll={}, remaining={}, phase={}, total={}", sample.name, scroll,
            field(screen, "localAcceleratorTicksRemaining"), be.getStellarPhaseProgress(), be.getStellarTotalProgress(0));
        capturing = true;
        Screenshot.grab(client.gameDirectory, "stellar-ui-26.1-" + sample.name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                loaded = false;
                index++;
                next = System.currentTimeMillis() + 500;
            }));
    }

    private static void verifyCountdown(CelestialForgingAnvilScreen screen, boolean paused) {
        screen.tick();
        int before = field(screen, "localAcceleratorTicksRemaining");
        for (int tick = 0; tick < 8; tick++) screen.tick();
        int after = field(screen, "localAcceleratorTicksRemaining");
        if (before <= 8 || after != before - (paused ? 0 : 8)) throw new IllegalStateException("Incorrect paused/running countdown");
        AnvilCraft.LOGGER.info("PORT_STELLAR_UI_COUNTDOWN paused={}: before={}, after={}", paused, before, after);
    }

    private static int field(CelestialForgingAnvilScreen screen, String name) {
        try {
            var field = CelestialForgingAnvilScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.getInt(screen);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static final class FixedScreen extends CelestialForgingAnvilScreen {
        private FixedScreen(CelestialForgingAnvilMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            Minecraft.getInstance().level.setTimeFromServer(500);
            try {
                var field = CelestialForgingAnvilScreen.class.getDeclaredField("localAcceleratorTicksRemaining");
                field.setAccessible(true);
                field.setInt(this, this.getMenu().getBlockEntity().getAcceleratorTicksRemaining());
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException(error);
            }
            super.extractRenderState(graphics, 0, 0, 0);
        }
    }
}
