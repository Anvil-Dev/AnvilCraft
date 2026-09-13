package dev.dubhe.anvilcraft.porting;

import com.mojang.datafixers.util.Either;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.block.BlockPlacementRules;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
import dev.dubhe.anvilcraft.block.entity.StoragePortBlockEntity;
import dev.dubhe.anvilcraft.block.logistics.chute.OverflowChuteBlock;
import dev.dubhe.anvilcraft.block.logistics.storage.AbstractStoragePortBlock;
import dev.dubhe.anvilcraft.block.power.consumer.SmartBlockPlacerBlock;
import dev.dubhe.anvilcraft.block.state.StoragePortType;
import dev.dubhe.anvilcraft.block.utility.redstone.BigRedButtonBlock;
import dev.dubhe.anvilcraft.client.gui.screen.SmartBlockPlacerScreen;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.util.BlockStateAndEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

import java.util.List;

/** 仅由开发验证开关启用，在独立新世界中建立固定镜头的对照场景。 */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class PortVisualScene {
    private static final BlockPos ANCHOR = new BlockPos(-3, 81, 3);
    private static boolean creating;
    private static boolean preparing;
    private static volatile boolean prepared;
    private static boolean captured;
    private static boolean validatedPlacementRules;
    private static int readyFrames;
    private static int smartStage;
    private static final BlockPos SMART_POS = new BlockPos(0, 81, 0);

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) {
        if (!Boolean.getBoolean("anvilcraft.portVisualScene") || captured) return;
        Minecraft client = Minecraft.getInstance();
        client.options.pauseOnLostFocus = false;
        if (!creating) {
            if (client.screen == null || client.getOverlay() != null) return;
            creating = true;
            String name = "anvilcraft-port-visual-" + System.currentTimeMillis();
            client.createWorldOpenFlows().createFreshLevel(
                name,
                new LevelSettings(name, GameType.CREATIVE,
                    new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT),
                new WorldOptions(121261L, false, false), WorldPresets::createFlatWorldDimensions, client.screen
            );
            return;
        }
        if (client.level == null || client.player == null || client.getSingleplayerServer() == null) return;
        if (!preparing) {
            preparing = true;
            MinecraftServer server = client.getSingleplayerServer();
            server.execute(() -> prepare(server));
        }
        if (Boolean.getBoolean("anvilcraft.portSmartPlacerScene")) {
            smartFrame(client);
            return;
        }
        if (Boolean.getBoolean("anvilcraft.portStorageItemScene")) {
            if (prepared) StoragePortItemScene.frame(client);
            return;
        }
        if (Boolean.getBoolean("anvilcraft.portStorageFluidItemScene")) {
            if (prepared) StorageFluidPortItemScene.frame(client);
            return;
        }
        if (Boolean.getBoolean("anvilcraft.portPlacementPreviewScene")) {
            if (prepared) PlacementPreviewClientScene.frame(client);
            return;
        }
        if (!prepared || client.screen != null || !client.level.getBlockState(ANCHOR).is(ModBlocks.REDSTONE_DICE.get())) return;
        if (Math.abs(client.player.getY() - 86) > 0.1) return;
        if (!validatedPlacementRules) {
            BlockState candles = Blocks.CANDLE.defaultBlockState().setValue(BlockStateProperties.CANDLES, 3);
            int count = BlockPlacementRules.getPlacementItemCount(client.level.registryAccess(), candles, new ItemStack(Items.CANDLE));
            if (count != 3) throw new IllegalStateException("客户端未正确加载同步后的放置规则");
            validatedPlacementRules = true;
            AnvilCraft.LOGGER.info("PORT_PLACEMENT_CLIENT_SYNC_PASSED");
        }
        client.options.hideGui = true;
        if (++readyFrames < 120) return;
        captured = true;
        String imageName = Boolean.getBoolean("anvilcraft.portItemSplitterScene")
            ? "item-splitter-scene-26.1.png" : "dynamic-scene-26.1.png";
        if (Boolean.getBoolean("anvilcraft.portOverflowScene")) imageName = "overflow-scene-26.1.png";
        if (Boolean.getBoolean("anvilcraft.portStoragePortScene")) imageName = "storage-port-scene-26.1.png";
        if (Boolean.getBoolean("anvilcraft.portStorageFluidScene")) imageName = "storage-fluid-scene-26.1.png";
        Screenshot.grab(client.gameDirectory, imageName, client.getMainRenderTarget(), 1, message -> {
            AnvilCraft.LOGGER.info("PORT_VISUAL_SCENE_CAPTURED: {}", message.getString());
            client.execute(client::stop);
        });
    }

    private static void prepare(MinecraftServer server) {
        ServerLevel level = server.overworld();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) level.setChunkForced(x, z, true);
        }
        for (int x = -12; x <= 12; x++) {
            for (int z = -6; z <= 10; z++) {
                level.setBlock(new BlockPos(x, 80, z), Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        Direction[] directions = {Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        for (int index = 0; index < directions.length; index++) {
            level.setBlock(new BlockPos(-5 + index * 2, 81, 0), ModBlocks.BIG_RED_BUTTON.getDefaultState()
                .setValue(BigRedButtonBlock.FACING, directions[index]).setValue(BigRedButtonBlock.PRESSED, index % 2 == 0),
                Block.UPDATE_ALL);
        }
        int[] outcomes = {111, 246, 666};
        for (int index = 0; index < outcomes.length; index++) {
            BlockPos pos = ANCHOR.offset(index * 2, 0, 0);
            level.setBlock(pos, ModBlocks.REDSTONE_DICE.getDefaultState(), Block.UPDATE_ALL);
            BlockEntity dice = level.getBlockEntity(pos);
            if (dice == null) throw new IllegalStateException("对照场景缺少骰子实体");
            CompoundTag tag = new CompoundTag();
            tag.putInt("Faces", outcomes[index]);
            tag.putInt("PreviousFaces", outcomes[index]);
            tag.putLong("RollStart", -1);
            dice.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
            level.sendBlockUpdated(pos, dice.getBlockState(), dice.getBlockState(), Block.UPDATE_CLIENTS);
        }
        List<BlockState> machines = List.of(
            ModBlocks.CONTROL_VALVE.getDefaultState(), ModBlocks.ADVANCED_COMPARATOR.getDefaultState(),
            ModBlocks.PUMP.getDefaultState(), ModBlocks.CUT_EMBER_METAL_BLOCK.getDefaultState(),
            ModBlocks.CUT_FROST_METAL_BLOCK.getDefaultState(), ModBlocks.FE_COLLECTOR.getDefaultState()
        );
        for (int index = 0; index < machines.size(); index++) {
            level.setBlock(new BlockPos(-5 + index * 2, 81, 6), machines.get(index), Block.UPDATE_ALL);
        }
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set noon");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear");
        server.getPlayerList().getPlayers().forEach(player -> {
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        });
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 10 86 15 140 22");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick freeze");
        if (Boolean.getBoolean("anvilcraft.portSmartPlacerScene")) {
            level.setBlock(SMART_POS, ModBlocks.SMART_BLOCK_PLACER.getDefaultState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(SmartBlockPlacerBlock.OVERLOAD, false), Block.UPDATE_ALL);
            SmartBlockPlacerBlockEntity be = (SmartBlockPlacerBlockEntity) level.getBlockEntity(SMART_POS);
            if (be == null) throw new IllegalStateException("缺少测试放置器");
            be.onLoad();
            be.togglePosition(0, 12, true);
            be.togglePosition(2, 0, true);
            be.togglePosition(4, 24, true);
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 6 84 -7 45 20");
        }
        if (Boolean.getBoolean("anvilcraft.portItemSplitterScene")) {
            Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
            for (int index = 0; index < facings.length; index++) {
                level.setBlock(new BlockPos(-3 + index * 2, 81, 9), ModBlocks.ITEM_SPLITTER.getDefaultState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, facings[index]), Block.UPDATE_ALL);
            }
        }
        if (Boolean.getBoolean("anvilcraft.portOverflowScene")) {
            Direction[] facings = Direction.values();
            for (int index = 0; index < facings.length; index++) {
                BlockState state = ModBlocks.OVERFLOW_CHUTE.getDefaultState().setValue(OverflowChuteBlock.FACING, facings[index]);
                for (Direction port : Direction.values()) {
                    if (port.getAxis() != facings[index].getAxis() && index % 2 == 1) {
                        state = state.setValue(OverflowChuteBlock.overflowProperty(port), true);
                    }
                }
                level.setBlock(new BlockPos(-5 + index * 2, 81, 9), state, Block.UPDATE_ALL);
            }
        }
        if (Boolean.getBoolean("anvilcraft.portStoragePortScene")) {
            for (int index = 0; index < 2; index++) {
                BlockPos pos = new BlockPos(-2 + index * 4, 81, 9);
                level.setBlock(pos, ModBlocks.STORAGE_PORT.getDefaultState().setValue(AbstractStoragePortBlock.TYPE,
                    index == 0 ? StoragePortType.SHULKER_CONTAINER : StoragePortType.HYPERDIMENSION), Block.UPDATE_ALL);
                var port = (StoragePortBlockEntity) level.getBlockEntity(pos);
                port.setMarkedItem(new ItemStack(index == 0 ? Items.IRON_AXE : Items.DIAMOND));
            }
        }
        if (Boolean.getBoolean("anvilcraft.portStorageFluidScene")) {
            int[] amounts = {1, 32000, 64000, 128000};
            for (int index = 0; index < amounts.length; index++) {
                BlockPos pos = new BlockPos(-3 + index * 2, 81, 9);
                level.setBlock(pos, ModBlocks.STORAGE_FLUID_PORT.getDefaultState().setValue(AbstractStoragePortBlock.TYPE,
                    index % 2 == 0 ? StoragePortType.SHULKER_CONTAINER : StoragePortType.HYPERDIMENSION), Block.UPDATE_ALL);
                var port = (StorageFluidPortBlockEntity) level.getBlockEntity(pos);
                port.getTank().set(0, net.neoforged.neoforge.transfer.fluid.FluidResource.of(ModFluids.HONEY.get()), amounts[index]);
            }
        }
        prepared = true;
        if (Boolean.getBoolean("anvilcraft.portPlacementPreviewScene")) {
            level.setBlock(new BlockPos(4, 81, 8), ModBlocks.CELESTIAL_FORGING_ANVIL.getDefaultState(),
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    private static void smartFrame(Minecraft client) {
        if (!prepared || !(client.level.getBlockEntity(SMART_POS) instanceof SmartBlockPlacerBlockEntity be)) return;
        if (smartStage < 2) {
            if (client.screen != null || Math.abs(client.player.getY() - 84) > 0.1) return;
            be.setPhase(SmartBlockPlacerBlockEntity.ExecutionPhase.EXTEND);
            be.setPhaseProgress(0.5F);
            be.setClientAnimationTargetPos(SMART_POS.north(4));
            be.setCurrentHeldBlock(smartStage == 0 ? Either.left(new ItemStack(Items.STONE))
                : Either.right(new BlockStateAndEntity(Blocks.CHEST.defaultBlockState())));
            client.options.hideGui = true;
        } else if (!(client.screen instanceof SmartBlockPlacerScreen)) {
            return;
        }
        if (smartStage == 2) {
            if (Boolean.getBoolean("anvilcraft.portPreviewRaw")) AnvilCraft.CLIENT_CONFIG.renderScanPreviewEffect = false;
            try {
                var cache = SmartBlockPlacerScreen.class.getDeclaredField("cachedPreviewLevelLike");
                cache.setAccessible(true);
                var preview = (dev.dubhe.anvilcraft.util.LevelLike) cache.get(client.screen);
                if (preview != null) {
                    for (BlockPos pos : List.of(new BlockPos(2, 0, 2), new BlockPos(0, 2, 0), new BlockPos(4, 4, 4))) {
                        preview.setBlockState(pos, Blocks.LIME_CONCRETE.defaultBlockState());
                    }
                }
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(exception);
            }
        }
        if (++readyFrames < 120) return;
        captured = true;
        String name = "smart-placer-26.1-" + smartStage + (Boolean.getBoolean("anvilcraft.portPreviewRaw") ? "-raw" : "") + ".png";
        Screenshot.grab(client.gameDirectory, name, client.getMainRenderTarget(), 1, message -> client.execute(() -> {
            AnvilCraft.LOGGER.info("PORT_SMART_SCENE_CAPTURED: {}", message.getString());
            if (smartStage == 2) {
                client.stop();
                return;
            }
            smartStage++;
            readyFrames = 0;
            captured = false;
            if (smartStage == 2) {
                client.options.hideGui = false;
                MinecraftServer server = client.getSingleplayerServer();
                if (server != null) {
                    server.execute(() -> {
                        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0 83 2 180 25");
                        BlockEntity machine = server.overworld().getBlockEntity(SMART_POS);
                        if (machine instanceof SmartBlockPlacerBlockEntity placer) {
                            server.getPlayerList().getPlayers().forEach(player -> ModMenuTypes.open(player, placer, SMART_POS));
                        }
                    });
                }
            }
        }));
    }
}
