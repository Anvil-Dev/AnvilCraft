package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionBlacklist;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.item.block.RuinsBlockItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.HashSet;
import java.util.Set;

public final class RuinsFallbackClientScene {
    private static int stage;
    private static long next;
    private static long deadline;
    private static boolean capturing;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 180000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Ruins fallback stage " + stage);
        if (stage > 0) {
            client.player.setYRot(180);
            client.player.setXRot(20);
            client.player.yRotO = 180;
            client.player.xRotO = 20;
        }
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> {
                    var server = client.getSingleplayerServer();
                    var level = server.overworld();
                    for (int x = -6; x <= 6; x++) {
                        for (int z = -4; z <= 6; z++) {
                            level.setBlockAndUpdate(new BlockPos(x, 161, z), Blocks.SMOOTH_STONE.defaultBlockState());
                        }
                    }
                    var block = ModBlocks.LARGE_CRATE.get();
                    var state = block.defaultBlockState();
                    for (int x : new int[]{-3, 3}) {
                        var pos = new BlockPos(x, 162, 0);
                        for (var part : block.getParts()) {
                            level.setBlock(pos.offset(block.offsetFrom(state, part)), block.placedState(part, state),
                                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                        }
                        if (x == 3) RuinsBlockItem.convert(level, pos);
                    }
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a -2.5 164 5.5 180 20");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set noon");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear");
                    server.getPlayerList().getPlayers().getFirst().setNoGravity(true);
                });
                client.options.guiScale().set(2);
                client.options.fov().set(70);
                client.options.hideGui = false;
                client.resizeGui();
                client.setScreen(null);
                advance(1);
            }
            case 1 -> {
                MultipartShapeSnapshot.capture(client);
                blacklistCrate();
                advance(2);
            }
            case 2 -> {
                requireTarget(client, false);
                capture(client, "ordinary", 3);
            }
            case 3 -> {
                client.getSingleplayerServer().execute(() -> {
                    var server = client.getSingleplayerServer();
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 3.5 164 5.5 180 20");
                });
                advance(4);
            }
            case 4 -> {
                requireTarget(client, true);
                capture(client, "ruins", 5);
            }
            case 5 -> {
                ModelSelectionBlacklist.reload(client.getResourceManager());
                AnvilCraft.LOGGER.info(
                    "PORT_RUINS_FALLBACK_CLIENT_PASSED: ordinary and disguised whole outlines with model outlines disabled");
                stage = 6;
                client.stop();
            }
            default -> {
            }
        }
    }

    private static void blacklistCrate() {
        try {
            var field = ModelSelectionBlacklist.class.getDeclaredField("rules");
            field.setAccessible(true);
            Object previous = field.get(null);
            var type = previous.getClass();
            var picking = type.getDeclaredMethod("picking");
            var outline = type.getDeclaredMethod("outline");
            var entities = type.getDeclaredMethod("blockEntity");
            picking.setAccessible(true);
            outline.setAccessible(true);
            entities.setAccessible(true);
            Set<Block> excluded = new HashSet<>();
            for (Object value : (Set<?>) outline.invoke(previous)) excluded.add((Block) value);
            excluded.add(ModBlocks.LARGE_CRATE.get());
            var constructor = type.getDeclaredConstructor(Set.class, Set.class, Set.class);
            constructor.setAccessible(true);
            field.set(null, constructor.newInstance(picking.invoke(previous), Set.copyOf(excluded), entities.invoke(previous)));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void requireTarget(Minecraft client, boolean ruins) {
        if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
            || !client.level.getBlockState(hit.getBlockPos()).is(ruins ? ModBlocks.RUINS_BLOCK : ModBlocks.LARGE_CRATE)) {
            throw new IllegalStateException("Fallback target is not the expected crate");
        }
    }

    private static void advance(int target) {
        stage = target;
        next = System.currentTimeMillis() + 2000;
    }

    private static void capture(Minecraft client, String name, int target) {
        capturing = true;
        String prefix = Boolean.getBoolean("anvilcraft.portSodiumScene") ? "ruins-fallback-sodium-" : "ruins-fallback-26.1-";
        Screenshot.grab(client.gameDirectory, prefix + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(target);
            }));
    }
}
