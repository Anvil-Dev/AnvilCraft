package dev.dubhe.anvilcraft.porting;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.ProcessingTableBlockEntity;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.client.renderer.blockentity.ProcessingItemStackRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.ProcessingTableRenderState;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class ProcessingTableScene {
    private static boolean creating;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    private static boolean capturing;
    private static int stage;
    private static long next;
    private static long deadline;

    private static BlockPos pos(int index) {
        return new BlockPos(198 + index * 2, 101, 200);
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) {
        if (!Boolean.getBoolean("anvilcraft.portProcessingTableScene")) return;
        var client = Minecraft.getInstance();
        client.options.pauseOnLostFocus = false;
        if (!creating) {
            if (client.screen == null || client.getOverlay() != null) return;
            creating = true;
            String name = "processing-visual-" + System.currentTimeMillis();
            client.createWorldOpenFlows().createFreshLevel(name,
                new LevelSettings(name, GameType.CREATIVE,
                    new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT),
                new WorldOptions(121261L, false, false), WorldPresets::createFlatWorldDimensions, client.screen);
            return;
        }
        if (client.level == null || client.player == null || client.getSingleplayerServer() == null) return;
        if (deadline == 0) deadline = System.currentTimeMillis() + 120000;
        if (failure != null || System.currentTimeMillis() > deadline) {
            throw new IllegalStateException("Processing visual scene failed at " + stage, failure);
        }
        client.options.hideGui = true;
        client.options.fov().set(70);
        client.options.bobView().set(false);
        if (client.screen != null) client.setScreen(null);
        if (capturing || System.currentTimeMillis() < next) return;
        switch (stage) {
            case 0 -> {
                server(client, () -> {
                    var server = client.getSingleplayerServer();
                    var level = server.overworld();
                    for (int x = 195; x <= 207; x++) {
                        for (int z = 197; z <= 204; z++) {
                            level.setBlock(new BlockPos(x, 100, z), Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_ALL);
                        }
                    }
                    var states = List.of(ModBlocks.STAMPING_PLATFORM.getDefaultState(), ModBlocks.CRUSHING_TABLE.getDefaultState(),
                        ModBlocks.SIFTING_TABLE.getDefaultState(), ModBlocks.UNPACKING_TABLE.getDefaultState());
                    for (int index = 0; index < 4; index++) level.setBlock(pos(index), states.get(index), Block.UPDATE_ALL);
                    fill((ProcessingTableBlockEntity) level.getBlockEntity(pos(0)), 0, new ItemStack(Items.DIAMOND, 16));
                    fill((ProcessingTableBlockEntity) level.getBlockEntity(pos(0)), 1, new ItemStack(Items.IRON_INGOT, 8));
                    fill((ProcessingTableBlockEntity) level.getBlockEntity(pos(1)), 0, new ItemStack(Items.DIAMOND_BLOCK, 64));
                    fill((ProcessingTableBlockEntity) level.getBlockEntity(pos(2)), 0, new ItemStack(Items.STONE, 32));
                    fill((ProcessingTableBlockEntity) level.getBlockEntity(pos(3)), 0, new ItemStack(Items.OAK_PLANKS, 32));
                    for (var player : server.getPlayerList().getPlayers()) {
                        player.getInventory().clearContent();
                        player.setNoGravity(true);
                        player.getAbilities().flying = true;
                        player.onUpdateAbilities();
                    }
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 201.5 103 207 180 18");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set 6000");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick freeze");
                    prepared = true;
                });
                advance(1, 5000);
            }
            case 1 -> {
                if (!prepared || client.player.getX() < 195) return;
                for (int index = 0; index < 4; index++) {
                    if (!(client.level.getBlockEntity(pos(index)) instanceof ProcessingTableBlockEntity table)
                        || table.getItemHandler().getResource(0).isEmpty()) return;
                }
                for (int index = 0; index < 4; index++) {
                    var table = (ProcessingTableBlockEntity) client.level.getBlockEntity(pos(index));
                    AnvilCraft.LOGGER.info("PROCESSING_LAYOUT_SEED {}: {}", index,
                        dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil.hash(table.getItemHandler()));
                }
                AnvilCraftClient.CONFIG.siftingUnpackingBlockRenderEnabled = true;
                check(client, true, false);
                advance(2, 1500);
            }
            case 2 -> capture(client, "idle", 3);
            case 3 -> {
                for (int index = 0; index < 2; index++) {
                    var table = (ProcessingTableBlockEntity) client.level.getBlockEntity(pos(index));
                    var tag = table.saveWithFullMetadata(client.level.registryAccess());
                    tag.putLong("DoorStartTick", client.level.getGameTime() - (index == 0 ? 400000 : 500000));
                    tag.putInt("DoorDurationTick", index == 0 ? 800000 : 2000000);
                    table.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, client.level.registryAccess(), tag));
                }
                check(client, true, false);
                advance(4, 1500);
            }
            case 4 -> capture(client, "active", 5);
            case 5 -> {
                AnvilCraftClient.CONFIG.siftingUnpackingBlockRenderEnabled = false;
                check(client, false, false);
                advance(6, 1500);
            }
            case 6 -> capture(client, "scattered", 7);
            case 7 -> {
                AnvilCraftClient.CONFIG.siftingUnpackingBlockRenderEnabled = true;
                server(client, () -> client.getSingleplayerServer().overworld()
                    .setBlock(pos(2).above(), Blocks.GLASS.defaultBlockState(), Block.UPDATE_ALL));
                advance(8, 2000);
            }
            case 8 -> {
                if (!client.level.getBlockState(pos(2).above()).is(Blocks.GLASS)) return;
                check(client, true, true);
                advance(9, 1000);
            }
            case 9 -> capture(client, "blocked", 10);
            case 10 -> {
                server(client, () -> client.getSingleplayerServer().getCommands().performPrefixedCommand(
                    client.getSingleplayerServer().createCommandSourceStack(), "tp @a 198.5 99.6 204 180 -8"));
                advance(11, 2000);
            }
            case 11 -> capture(client, "door-active", 12);
            case 12 -> {
                clearAnimation(client, 0);
                advance(13, 1000);
            }
            case 13 -> capture(client, "door-idle", 14);
            case 14 -> {
                fill((ProcessingTableBlockEntity) client.level.getBlockEntity(pos(1)), 0, ItemStack.EMPTY);
                server(client, () -> client.getSingleplayerServer().getCommands().performPrefixedCommand(
                    client.getSingleplayerServer().createCommandSourceStack(), "tp @a 200.5 101 203 180 25"));
                advance(15, 2000);
            }
            case 15 -> {
                logWheelPose(client);
                capture(client, "wheel-active", 16);
            }
            case 16 -> {
                clearAnimation(client, 1);
                advance(17, 1000);
            }
            case 17 -> capture(client, "wheel-idle", 18);
            case 18 -> {
                AnvilCraft.LOGGER.info("PORT_PROCESSING_VISUAL_PASSED");
                client.stop();
            }
            default -> throw new IllegalStateException("Unknown processing scene stage");
        }
    }

    private static void fill(ProcessingTableBlockEntity table, int slot, ItemStack stack) {
        ((ItemStacksResourceHandler) table.getInput()).set(slot, ItemResource.of(stack), stack.getCount());
    }

    @SuppressWarnings("unchecked")
    private static void check(Minecraft client, boolean enabled, boolean blocked) {
        var fixture = new dev.dubhe.anvilcraft.block.entity.SiftingTableBlockEntity(
            dev.dubhe.anvilcraft.init.block.ModBlockEntities.SIFTING_TABLE.get(), pos(3), ModBlocks.SIFTING_TABLE.getDefaultState());
        fixture.setLevel(client.level);
        var fixtureRenderer = (ProcessingItemStackRenderer<ProcessingTableBlockEntity>)
            (Object) client.getBlockEntityRenderDispatcher().getRenderer(fixture);
        var fixtureState = fixtureRenderer.createRenderState();
        var frontLit = ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK.asStack(32);
        fill(fixture, 0, frontLit);
        fixtureRenderer.extractRenderState(fixture, fixtureState, 0, client.gameRenderer.getMainCamera().position(), null);
        if (!fixtureState.items.getFirst().blockModel || fixtureState.items.getFirst().poseCount != (enabled ? 1 : 5)) {
            throw new IllegalStateException("Front-lit cuboid must remain eligible for block enlargement");
        }
        fill(fixture, 0, new ItemStack(Items.OAK_SAPLING, 32));
        fixtureRenderer.extractRenderState(fixture, fixtureState, 0, client.gameRenderer.getMainCamera().position(), null);
        if (fixtureState.items.getFirst().blockModel || fixtureState.items.getFirst().poseCount != 5) {
            throw new IllegalStateException("Generated flat block items must remain scattered");
        }
        for (int index = 0; index < 4; index++) {
            var table = (ProcessingTableBlockEntity) client.level.getBlockEntity(pos(index));
            var renderer = (ProcessingItemStackRenderer<ProcessingTableBlockEntity>)
                (Object) client.getBlockEntityRenderDispatcher().getRenderer(table);
            if (renderer == null) throw new IllegalStateException("Missing processing renderer");
            ProcessingTableRenderState state = renderer.createRenderState();
            renderer.extractRenderState(table, state, 0, client.gameRenderer.getMainCamera().position(), null);
            int count = state.items.getFirst().poseCount;
            int expected = index == 0 ? 3 : index == 1 ? 9 : enabled && !(blocked && index == 2) ? 1 : 5;
            if (count != expected) throw new IllegalStateException("Wrong display count: " + index + "/" + count + "/" + expected);
            var firstPose = new org.joml.Matrix4f(state.items.getFirst().poses.getFirst());
            var reusedItem = state.items.getFirst().item;
            renderer.extractRenderState(table, state, 0, client.gameRenderer.getMainCamera().position(), null);
            if (reusedItem != state.items.getFirst().item || !firstPose.equals(state.items.getFirst().poses.getFirst())) {
                throw new IllegalStateException("Item extraction is unstable or discarded its reusable state");
            }
            List<org.joml.Matrix4f> transforms = new ArrayList<>();
            renderer.collectSelectionModels(table, 0, new PoseStack(),
                (model, pose) -> transforms.add(new org.joml.Matrix4f(pose.last().pose())));
            if (transforms.size() != (index < 2 ? 2 : 0) || state.models.size() != transforms.size()) {
                throw new IllegalStateException("Mechanical rendering and picking disagree");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void logWheelPose(Minecraft client) {
        var table = (ProcessingTableBlockEntity) client.level.getBlockEntity(pos(1));
        AnvilCraft.LOGGER.info("PROCESSING_WHEEL_PROGRESS: {} / {}", table.getSpinProgress(0), table.getDoorDurationTick());
        var renderer = (dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer<ProcessingTableBlockEntity>)
            (Object) client.getBlockEntityRenderDispatcher().getRenderer(table);
        List<org.joml.Matrix4f> matrices = new ArrayList<>();
        renderer.collectSelectionModels(table, 0, new PoseStack(), (model, pose) -> {
            matrices.add(new org.joml.Matrix4f(pose.last().pose()));
            AnvilCraft.LOGGER.info("PROCESSING_WHEEL_MATRIX: {}", java.util.Arrays.toString(pose.last().pose().get(new float[16])));
        });
        // Snapshot from the actual 1.21 renderer at 25% progress, independent of the misleading easeOutQuint method name.
        float[][] expected = {
            {1, 0, 0, 0, 0, -0.8819213F, -0.47139668F, 0, 0, 0.47139668F, -0.8819213F, 0, 0, 1.2788607F, 0.8828379F, 1},
            {1, 0, 0, 0, 0, -0.8819213F, 0.47139668F, 0, 0, -0.47139668F, -0.8819213F, 0, 0, 1.7502574F, 0.99908334F, 1}
        };
        if (matrices.size() != expected.length) throw new IllegalStateException("Incomplete wheel pair");
        for (int index = 0; index < expected.length; index++) {
            float[] actual = matrices.get(index).get(new float[16]);
            for (int component = 0; component < 16; component++) {
                if (Math.abs(actual[component] - expected[index][component]) > 0.00001F) {
                    throw new IllegalStateException("Wheel transform differs from the source runtime snapshot");
                }
            }
        }
    }

    private static void clearAnimation(Minecraft client, int index) {
        var table = (ProcessingTableBlockEntity) client.level.getBlockEntity(pos(index));
        var tag = table.saveWithFullMetadata(client.level.registryAccess());
        tag.putInt("DoorDurationTick", 0);
        table.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, client.level.registryAccess(), tag));
    }

    private static void server(Minecraft client, Runnable action) {
        client.getSingleplayerServer().execute(() -> {
            try {
                action.run();
            } catch (Throwable error) {
                failure = error;
            }
        });
    }

    private static void advance(int target, long delay) {
        AnvilCraft.LOGGER.info("PROCESSING_SCENE_STAGE: {} -> {}", stage, target);
        stage = target;
        next = System.currentTimeMillis() + delay;
    }

    private static void capture(Minecraft client, String name, int target) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "processing-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(target, 0);
            }));
    }
}
