package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.item.block.RuinsBlockItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

import java.util.List;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class RuinsFinishClientScene {
    private static int stage;
    private static long deadline;
    private static long next;
    private static long phase = 500;
    private static int offset;
    private static boolean capturing;
    private static volatile boolean reloaded;

    @SubscribeEvent
    public static void clock(RenderFrameEvent.Pre event) {
        if (!Boolean.getBoolean("anvilcraft.portRuinsFinishScene")) return;
        var client = Minecraft.getInstance();
        if (client.level == null || !(client.screen instanceof Gallery) || !client.isPaused()) return;
        client.level.setTimeFromServer(phase);
        try {
            var timer = client.getDeltaTracker();
            var residual = timer.getClass().getDeclaredField("pausedDeltaTickResidual");
            residual.setAccessible(true);
            residual.setFloat(timer, 0);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    static List<Pair> pairs() {
        var slab = Blocks.OAK_SLAB.defaultBlockState();
        var stone = Blocks.STONE.defaultBlockState();
        var glass = Blocks.GLASS.defaultBlockState();
        return List.of(new Pair(glass, glass, false), new Pair(stone, stone, false), new Pair(slab, slab, false),
            new Pair(stone, slab, true), new Pair(slab, slab.setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP), true),
            new Pair(glass, stone, false), new Pair(stone, Blocks.AIR.defaultBlockState(), true),
            new Pair(ModBlocks.TRANSCENDIUM_BLOCK.getDefaultState(), ModBlocks.TRANSCENDIUM_BLOCK.getDefaultState(), false));
    }

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 150000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Ruins finish stage " + stage);
        client.level.setTimeFromServer(phase);
        if (stage > 0) {
            client.player.setYRot(180);
            client.player.setXRot(25);
            client.player.yRotO = 180;
            client.player.xRotO = 25;
        }
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> {
                    var server = client.getSingleplayerServer();
                    var level = server.overworld();
                    var pairs = pairs();
                    for (int row = 0; row < pairs.size(); row++) {
                        var pair = pairs.get(row);
                        for (int x : new int[]{-3, 2}) {
                            var pos = new BlockPos(x, 162, row * 3);
                            level.setBlockAndUpdate(pos.below(), Blocks.SMOOTH_STONE.defaultBlockState());
                            level.setBlockAndUpdate(pos.east().below(), Blocks.SMOOTH_STONE.defaultBlockState());
                            level.setBlockAndUpdate(pos, pair.self());
                            level.setBlockAndUpdate(pos.east(), pair.neighbor());
                            if (x == 2) RuinsBlockItem.convert(level, pos.east());
                        }
                    }
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0.5 166 11.5 180 25");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set noon");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear");
                    server.getPlayerList().getPlayers().getFirst().setNoGravity(true);
                });
                client.options.guiScale().set(2);
                client.options.fov().set(70);
                client.options.hideGui = true;
                client.resizeGui();
                client.setScreen(null);
                advance(1, 4000);
            }
            case 1 -> {
                RuinsFaceProbe.verify(client);
                capture(client, "faces", 2);
            }
            case 2 -> {
                client.setScreen(new Gallery());
                advance(3, 1200);
            }
            case 3 -> capture(client, "items", 4);
            case 4 -> {
                phase = 6000;
                advance(5, 1200);
            }
            case 5 -> capture(client, "items-later", 6);
            case 6 -> {
                phase = 500;
                offset = 40;
                advance(7, 1200);
            }
            case 7 -> capture(client, "items-offset", 8);
            case 8 -> {
                stage = 9;
                client.reloadResourcePacks().thenRun(() -> reloaded = true);
                next = System.currentTimeMillis() + 2000;
            }
            case 9 -> {
                if (reloaded) capture(client, "items-reloaded", 10);
            }
            case 10 -> {
                client.setScreen(null);
                client.options.hideGui = false;
                client.gui.getChat().clearMessages(false);
                hand(client, false);
                advance(11, 2000);
            }
            case 11 -> capture(client, "main-hand", 12);
            case 12 -> {
                hand(client, true);
                advance(13, 2000);
            }
            case 13 -> capture(client, "off-hand", 14);
            case 14 -> {
                AnvilCraft.LOGGER.info(
                    "PORT_RUINS_FINISH_CLIENT_PASSED: culling pairs, timed item animation, location and resource reload");
                stage = 15;
                client.stop();
            }
            default -> {
            }
        }
    }

    private static void hand(Minecraft client, boolean offhand) {
        client.getSingleplayerServer().execute(() -> {
            var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
            player.setItemInHand(InteractionHand.MAIN_HAND, offhand ? ItemStack.EMPTY : ModBlocks.RUINS_BLOCK.asStack());
            player.setItemInHand(InteractionHand.OFF_HAND, offhand ? ModBlocks.RUINS_BLOCK.asStack() : ItemStack.EMPTY);
            player.containerMenu.broadcastChanges();
        });
    }

    private static void advance(int target, int delay) {
        stage = target;
        next = System.currentTimeMillis() + delay;
    }

    private static void capture(Minecraft client, String name, int target) {
        if (name.startsWith("items")) {
            float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            if (partialTick != 0) throw new IllegalStateException("Unfixed shader time: " + partialTick);
            AnvilCraft.LOGGER.info("PORT_RUINS_ITEM_CLOCK: {} phase={} fraction={}", name, phase, partialTick);
        }
        capturing = true;
        String prefix = Boolean.getBoolean("anvilcraft.portSodiumScene") ? "ruins-finish-sodium-" : "ruins-finish-26.1-";
        Screenshot.grab(client.gameDirectory, prefix + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(target, 300);
            }));
    }

    record Pair(BlockState self, BlockState neighbor, boolean visible) {
    }

    private static final class Gallery extends Screen {
        private Gallery() {
            super(Component.literal("Ruins item projection"));
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, this.width, this.height, 0xFF303030);
            int[] positions = {90, 270, 460};
            int[] scales = {1, 3, 5};
            for (int index = 0; index < positions.length; index++) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(positions[index] + offset, 120);
                graphics.pose().scale(scales[index], scales[index]);
                graphics.item(ModBlocks.RUINS_BLOCK.asStack(), 0, 0);
                graphics.pose().popMatrix();
            }
        }
    }
}
