package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.RuinsBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.client.renderer.blockentity.RuinsBlockEntityRenderer;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.item.block.RuinsBlockItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;
import java.util.Queue;

public final class RuinsEffectsClientScene {
    private static final BlockPos BOOK = new BlockPos(1, 162, 0);
    private static final BlockPos GRASS = new BlockPos(0, 162, -2);
    private static final BlockPos SLAB = new BlockPos(2, 162, -2);
    private static int stage;
    private static long next;
    private static long deadline;
    private static int bookTime;
    private static long gameTime;
    private static boolean capturing;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 120000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Ruins effects stage " + stage);
        if (stage > 0) {
            client.player.setYRot(180);
            client.player.setXRot(15);
            client.player.yRotO = 180;
            client.player.xRotO = 15;
        }
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> {
                    var server = client.getSingleplayerServer();
                    var level = server.overworld();
                    for (int x = -4; x <= 4; x++) {
                        for (int z = -4; z <= 5; z++) {
                            level.setBlockAndUpdate(new BlockPos(x, 161, z), Blocks.SMOOTH_STONE.defaultBlockState());
                        }
                    }
                    level.setBlockAndUpdate(BOOK.west(2), Blocks.ENCHANTING_TABLE.defaultBlockState());
                    level.setBlockAndUpdate(BOOK, Blocks.ENCHANTING_TABLE.defaultBlockState());
                    level.setBlockAndUpdate(GRASS, Blocks.GRASS_BLOCK.defaultBlockState());
                    level.setBlockAndUpdate(SLAB, Blocks.OAK_SLAB.defaultBlockState());
                    for (var pos : List.of(BOOK, GRASS, SLAB)) RuinsBlockItem.convert(level, pos);
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0.5 162 2.5 180 15");
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
                if (!(client.level.getBlockEntity(BOOK) instanceof RuinsBlockEntity ruins)) return;
                Object rendererHandle = client.getBlockEntityRenderDispatcher().getRenderer(ruins);
                var renderer = (RuinsBlockEntityRenderer) rendererHandle;
                var book = (EnchantingTableBlockEntity) renderer.prepareDisplay(ruins);
                checkCelestialVisuals(client, renderer);
                bookTime = book.time;
                gameTime = client.level.getGameTime();
                for (int index = 0; index < 20; index++) renderer.prepareDisplay(ruins);
                require(book.time == bookTime, "Render frames cannot tick an entity twice in one game tick");
                require(bookTime > 0 && book.open > 0.5F, "Disguised book animates and responds to nearby player");
                advance(2, 1200);
            }
            case 2 -> {
                var ruins = (RuinsBlockEntity) client.level.getBlockEntity(BOOK);
                var book = (EnchantingTableBlockEntity) ruins.getDisplayEntity();
                long elapsed = client.level.getGameTime() - gameTime;
                require(book.time > bookTime && book.time - bookTime <= elapsed, "Animation advances at most once per game tick");
                particles(client, GRASS, Blocks.GRASS_BLOCK.defaultBlockState(), 64, false);
                particles(client, GRASS, Blocks.GRASS_BLOCK.defaultBlockState(), 1, true);
                particles(client, SLAB, Blocks.OAK_SLAB.defaultBlockState(), 32, false);
                particles(client, new BlockPos(-3, 161, 0), Blocks.SMOOTH_STONE.defaultBlockState(), 64, false);
                capture(client, "books", 3);
            }
            case 3 -> {
                client.getSingleplayerServer().execute(() -> client.getSingleplayerServer().overworld().destroyBlock(GRASS, false));
                advance(4, 500);
            }
            case 4 -> {
                if (!client.level.getBlockState(GRASS).isAir()) return;
                particles(client, GRASS, Blocks.GRASS_BLOCK.defaultBlockState(), 64, false);
                require(client.level.getBlockState(GRASS).isAir(), "Particle context does not change the real world");
                AnvilCraft.LOGGER.info(
                    "PORT_RUINS_EFFECTS_PASSED: visual tick rate, animated book, hit/destroy sprites, tint and removal cache");
                advance(5, 100);
            }
            case 5 -> {
                stage = 6;
                client.stop();
            }
            default -> {
            }
        }
    }

    private static void checkCelestialVisuals(Minecraft client, RuinsBlockEntityRenderer renderer) {
        var ruins = new RuinsBlockEntity(ModBlockEntities.RUINS_BLOCK.get(), BOOK.above(12), ModBlocks.RUINS_BLOCK.getDefaultState());
        ruins.setDisplay(ModBlocks.CELESTIAL_FORGING_ANVIL.getDefaultState(), new CompoundTag());
        ruins.setLevel(client.level);
        var anvil = (CelestialForgingAnvilBlockEntity) ruins.getDisplayEntity();
        anvil.setCelestialBodyData(new StarData(CelestialBodyClass.G_MAIN, 32, 255, 220, 160, 10, 2, 0, 64, null));
        anvil.setAmplifierPresent(false);
        float rotation = anvil.getRotation();
        renderer.prepareDisplay(ruins);
        require(anvil.getRotation() > rotation, "Celestial ring rotates in ruins");
        require(Boolean.FALSE.equals(field(anvil, "clientMissingAmplifier")), "Ruins cannot register missing-amplifier previews");
        rotation = anvil.getRotation();
        renderer.prepareDisplay(ruins);
        require(anvil.getRotation() == rotation, "Celestial visuals tick only once per game tick");
    }

    private static void particles(Minecraft client, BlockPos pos, BlockState display, int count, boolean hit) {
        ((Queue<?>) field(client.particleEngine, "particlesToAdd")).clear();
        if (hit) {
            client.level.addBreakingBlockEffect(pos, Direction.UP, new BlockHitResult(pos.getCenter(), Direction.UP, pos, false));
        } else {
            client.level.addDestroyBlockEffect(pos,
                pos.equals(GRASS) || pos.equals(SLAB) ? ModBlocks.RUINS_BLOCK.getDefaultState() : display);
        }
        var queue = (Queue<?>) field(client.particleEngine, "particlesToAdd");
        require(queue.size() == count, "Particle count follows original shape: " + count + " vs " + queue.size());
        Particle expected = new TerrainParticle(client.level, pos.getX(), pos.getY(), pos.getZ(), 0, 0, 0, display, pos)
            .updateSprite(display, pos);
        for (Object particle : queue) {
            require(particle instanceof TerrainParticle, "Terrain particle type");
            for (String name : List.of("sprite", "rCol", "gCol", "bCol")) {
                require(field(particle, name).equals(field(expected, name)), "Original particle " + name);
            }
        }
        ((Queue<?>) field(client.particleEngine, "particlesToAdd")).clear();
    }

    private static Object field(Object instance, String name) {
        for (Class<?> type = instance.getClass(); type != null; type = type.getSuperclass()) {
            try {
                var field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(instance);
            } catch (NoSuchFieldException ignored) {
                continue;
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException(exception);
            }
        }
        throw new IllegalStateException(name);
    }

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }

    private static void advance(int target, int delay) {
        stage = target;
        next = System.currentTimeMillis() + delay;
    }

    private static void capture(Minecraft client, String name, int target) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "ruins-effects-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(target, 300);
            }));
    }
}
