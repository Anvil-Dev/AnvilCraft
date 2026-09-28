package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.RuinsBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.item.block.RuinsBlockItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class RuinsGeometryClientScene {
    static final BlockPos CRATE = new BlockPos(0, 162, 0);
    static final BlockPos ARM = new BlockPos(4, 162, 0);
    static final BlockPos SLAB = new BlockPos(-4, 162, 0);
    static final BlockPos BEACON = new BlockPos(0, 162, -6);
    private static int stage;
    private static long deadline;
    private static long next;
    private static boolean capturing;
    private static float pitch = 20;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 120000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Ruins geometry stage " + stage);
        if (stage > 0) {
            client.player.setYRot(180);
            client.player.setXRot(pitch);
            client.player.yRotO = 180;
            client.player.xRotO = pitch;
        }
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> {
                    var server = client.getSingleplayerServer();
                    var level = server.overworld();
                    for (int x = -6; x <= 6; x++) {
                        for (int z = -7; z <= 8; z++) {
                            level.setBlockAndUpdate(new BlockPos(x, 161, z), Blocks.SMOOTH_STONE.defaultBlockState());
                        }
                    }
                    var block = ModBlocks.LARGE_CRATE.get();
                    var base = block.defaultBlockState();
                    for (var part : block.getParts()) {
                        level.setBlock(CRATE.offset(block.offsetFrom(base, part)), block.placedState(part, base),
                            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    }
                    level.setBlockAndUpdate(ARM, ModBlocks.SMART_BLOCK_PLACER.getDefaultState());
                    level.setBlockAndUpdate(SLAB, Blocks.OAK_SLAB.defaultBlockState());
                    level.setBlockAndUpdate(BEACON, Blocks.BEACON.defaultBlockState());
                    RuinsBlockItem.convert(level, CRATE);
                    RuinsBlockItem.convert(level, ARM);
                    RuinsBlockItem.convert(level, SLAB);
                    RuinsBlockItem.convert(level, BEACON);
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0.5 164 5 180 20");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set noon");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear");
                    server.getPlayerList().getPlayers().getFirst().setNoGravity(true);
                });
                client.options.guiScale().set(2);
                client.options.fov().set(70);
                client.options.hideGui = false;
                client.resizeGui();
                client.setScreen(null);
                advance(1, 4000);
            }
            case 1 -> {
                requireTarget(client);
                RuinsGeometryProbe.verify(client);
                capture(client, "crate", 2);
            }
            case 2 -> {
                teleport(client, "4.5 163 4.5", 28);
                advance(3, 2000);
            }
            case 3 -> {
                requireTarget(client);
                RuinsGeometryProbe.verifyOutline(client);
                capture(client, "arm", 4);
            }
            case 4 -> {
                teleport(client, "-3.5 163 4.5", 31);
                advance(5, 2000);
            }
            case 5 -> {
                requireTarget(client);
                RuinsGeometryProbe.verifyOutline(client);
                capture(client, "slab", 6);
            }
            case 6 -> {
                changeBeacon(client, false);
                advance(7, 3000);
            }
            case 7 -> {
                var ruins = (RuinsBlockEntity) client.level.getBlockEntity(BEACON);
                if (!ruins.getDisplayState().is(Blocks.STONE)) return;
                RuinsGeometryProbe.verifyBeacon(client, false);
                changeBeacon(client, true);
                advance(8, 3000);
            }
            case 8 -> {
                var ruins = (RuinsBlockEntity) client.level.getBlockEntity(BEACON);
                if (!ruins.getDisplayState().is(Blocks.BEACON)) return;
                RuinsGeometryProbe.verifyBeacon(client, true);
                AnvilCraft.LOGGER.info("PORT_RUINS_GEOMETRY_CLIENT_PASSED: multipart, arm, slab outline and disguise transitions");
                stage = 9;
                client.stop();
            }
            default -> {
            }
        }
    }

    private static void requireTarget(Minecraft client) {
        if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
            || !client.level.getBlockState(hit.getBlockPos()).is(ModBlocks.RUINS_BLOCK)) {
            throw new IllegalStateException("Camera does not target a ruins block at stage " + stage + ": " + client.hitResult);
        }
    }

    private static void teleport(Minecraft client, String location, float rotation) {
        pitch = rotation;
        client.getSingleplayerServer().execute(() -> {
            var server = client.getSingleplayerServer();
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a " + location + " 180 " + rotation);
        });
    }

    private static void changeBeacon(Minecraft client, boolean beacon) {
        client.getSingleplayerServer().execute(() -> {
            var ruins = (RuinsBlockEntity) client.getSingleplayerServer().overworld().getBlockEntity(BEACON);
            ruins.setDisplay((beacon ? Blocks.BEACON : Blocks.STONE).defaultBlockState(), new CompoundTag());
        });
    }

    private static void advance(int target, int delay) {
        stage = target;
        next = System.currentTimeMillis() + delay;
    }

    private static void capture(Minecraft client, String name, int target) {
        capturing = true;
        String prefix = Boolean.getBoolean("anvilcraft.portSodiumScene") ? "ruins-geometry-sodium-" : "ruins-geometry-26.1-";
        Screenshot.grab(client.gameDirectory, prefix + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(target, 300);
            }));
    }
}
