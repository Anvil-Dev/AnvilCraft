package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity.ExecutionPhase;
import dev.dubhe.anvilcraft.block.power.consumer.SmartBlockPlacerBlock;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.SmartBlockPlacerMenu;
import dev.dubhe.anvilcraft.item.property.component.StructureDiskData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class SmartPlacerPortGameTests {
    private static final BlockPos MACHINE = new BlockPos(5, 2, 6);
    private static final BlockPos SOURCE = MACHINE.south();
    private static final BlockPos TARGET = MACHINE.north(4);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_sbp_phases", SmartPlacerPortGameTests::phases,
        "port_sbp_redstone_pause", SmartPlacerPortGameTests::redstonePause,
        "port_sbp_move", SmartPlacerPortGameTests::move,
        "port_sbp_blueprint", SmartPlacerPortGameTests::blueprint,
        "port_sbp_persistence", SmartPlacerPortGameTests::persistence,
        "port_sbp_menu", SmartPlacerPortGameTests::menu
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_sbp"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_empty"), 100, 0, true)
        )));
    }

    private static SmartBlockPlacerBlockEntity machine(GameTestHelper helper) {
        helper.setBlock(MACHINE, ModBlocks.SMART_BLOCK_PLACER.getDefaultState()
            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        helper.setBlock(SOURCE.below(), Blocks.STONE);
        helper.setBlock(TARGET.below(), Blocks.STONE);
        SmartBlockPlacerBlockEntity be = helper.getBlockEntity(MACHINE, SmartBlockPlacerBlockEntity.class);
        // 隔离电网调度，直接验证实际机器状态机在有电条件下的时序。
        be.setGrid(new PowerGrid(helper.getLevel()));
        be.togglePosition(0, 12, true);
        return be;
    }

    private static ChestBlockEntity supply(GameTestHelper helper, ItemStack stack) {
        helper.setBlock(SOURCE, Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(SOURCE, ChestBlockEntity.class);
        chest.setItem(0, stack);
        return chest;
    }

    private static void tick(GameTestHelper helper, SmartBlockPlacerBlockEntity be, int ticks) {
        for (int tick = 0; tick < ticks; tick++) be.tickServer(helper.getLevel(), helper.absolutePos(MACHINE));
    }

    private static void phases(GameTestHelper helper) {
        var be = machine(helper);
        final var chest = supply(helper, new ItemStack(Items.STONE, 2));
        tick(helper, be, 1);
        helper.assertTrue(be.getPhase() == ExecutionPhase.PREPARE, "找到材料后应进入准备阶段");
        tick(helper, be, 6);
        helper.assertTrue(be.getPhase() == ExecutionPhase.EXTEND, "准备阶段应持续 6 tick");
        tick(helper, be, 7);
        helper.assertTrue(helper.getBlockState(TARGET).isAir() && chest.getItem(0).getCount() == 2, "伸出完成前不能消耗或放置");
        tick(helper, be, 1);
        helper.assertTrue(be.getPhase() == ExecutionPhase.RESET && helper.getBlockState(TARGET).is(Blocks.STONE), "第 8 tick 应完成放置");
        helper.assertTrue(chest.getItem(0).getCount() == 1, "一次工作只消耗一个方块");
        tick(helper, be, 6);
        helper.assertTrue(be.getPhase() == ExecutionPhase.IDLE, "收回阶段应持续 6 tick");
        helper.succeed();
    }

    private static void redstonePause(GameTestHelper helper) {
        var be = machine(helper);
        supply(helper, new ItemStack(Items.STONE));
        tick(helper, be, 4);
        float progress = be.getPhaseProgress();
        helper.getLevel().setBlock(helper.absolutePos(MACHINE), be.getBlockState().setValue(SmartBlockPlacerBlock.POWERED, true),
            Block.UPDATE_CLIENTS);
        tick(helper, be, 20);
        helper.assertTrue(be.getPhase() == ExecutionPhase.PREPARE && be.getPhaseProgress() == progress, "红石锁定应暂停阶段进度");
        helper.getLevel().setBlock(helper.absolutePos(MACHINE), be.getBlockState().setValue(SmartBlockPlacerBlock.POWERED, false),
            Block.UPDATE_CLIENTS);
        tick(helper, be, 11);
        helper.assertTrue(helper.getBlockState(TARGET).is(Blocks.STONE), "解除锁定后应继续原工作周期");
        helper.succeed();
    }

    private static void move(GameTestHelper helper) {
        var be = machine(helper);
        be.setPickupMode(false);
        helper.setBlock(SOURCE, Blocks.STONE);
        tick(helper, be, 15);
        helper.assertTrue(helper.getBlockState(SOURCE).isAir() && helper.getBlockState(TARGET).is(Blocks.STONE), "移动模式必须搬运来源方块");
        helper.succeed();
    }

    private static ItemStack disk(GameTestHelper helper, BlockState state) {
        try {
            UUID id = UUID.randomUUID();
            final String file = "port_" + id + ".nbt";
            var directory = helper.getLevel().getServer().getWorldPath(LevelResource.ROOT).resolve("anvilcraft/structures");
            Files.createDirectories(directory);
            CompoundTag nbt = new CompoundTag();
            ListTag size = new ListTag();
            for (int axis = 0; axis < 3; axis++) size.add(IntTag.valueOf(1));
            nbt.put("size", size);
            ListTag palette = new ListTag();
            palette.add(NbtUtils.writeBlockState(state));
            nbt.put("palette", palette);
            CompoundTag block = new CompoundTag();
            ListTag pos = new ListTag();
            for (int axis = 0; axis < 3; axis++) pos.add(IntTag.valueOf(0));
            block.put("pos", pos);
            block.putInt("state", 0);
            ListTag blocks = new ListTag();
            blocks.add(block);
            nbt.put("blocks", blocks);
            NbtIo.writeCompressed(nbt, directory.resolve(file));
            ItemStack disk = new ItemStack(ModItems.STRUCTURE_DISK.get());
            disk.set(ModComponents.STRUCTURE_DISK_DATA, new StructureDiskData(file, "port blueprint", id, Direction.NORTH, 1, 1, 1));
            return disk;
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void blueprint(GameTestHelper helper) {
        var be = machine(helper);
        ItemStack disk = disk(helper, Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X));
        be.getBlueprintItemHandler().set(0, ItemResource.of(disk), 1);
        helper.assertTrue(be.hasBlueprint() && be.getTarget() == SmartBlockPlacerBlockEntity.TargetMode.BLUEPRINT, "插入磁盘应切换蓝图模式");
        be.setSkipMissingMode(false);
        tick(helper, be, 1);
        helper.assertTrue(be.getMissingBlock() != null && be.getPhase() == ExecutionPhase.IDLE, "等待模式缺料应停止并同步缺失材料");
        supply(helper, new ItemStack(Items.OAK_LOG));
        tick(helper, be, 15);
        helper.assertTrue(helper.getBlockState(TARGET) == be.getBlueprintStateForPlacement(12), "蓝图放置必须恢复原木轴向");
        helper.succeed();
    }

    private static void persistence(GameTestHelper helper) {
        var be = machine(helper);
        ItemStack disk = disk(helper, Blocks.STONE.defaultBlockState());
        be.getBlueprintItemHandler().set(0, ItemResource.of(disk), 1);
        be.togglePosition(4, 24, true);
        be.setPickupMode(false);
        CompoundTag saved = be.saveWithoutMetadata(helper.getLevel().registryAccess());
        SmartBlockPlacerBlockEntity restored = new SmartBlockPlacerBlockEntity(be.getBlockPos(), be.getBlockState());
        restored.setLevel(helper.getLevel());
        restored.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
        restored.onLoad();
        helper.assertTrue(ItemStack.matches(restored.getBlueprintItem(), disk) && restored.hasBlueprint(), "重载应保留磁盘并恢复蓝图");
        helper.assertTrue(restored.getLayerPositions()[124] && !restored.isPickupMode(), "重载应保留所有层与模式");
        CompoundTag legacy = new CompoundTag();
        legacy.putBoolean("isPickupMode", false);
        CompoundTag positions = new CompoundTag();
        positions.putIntArray("layer_4", new int[]{0, 24});
        legacy.put("layerPositions", positions);
        legacy.put("diskInventory", ItemStack.CODEC.encodeStart(
            helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE), disk).getOrThrow());
        restored.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), legacy));
        helper.assertTrue(restored.getLayerPositions()[100] && restored.getLayerPositions()[124] && !restored.isPickupMode(),
            "旧分层选择和模式应兼容读取");
        helper.assertTrue(ItemStack.matches(restored.getBlueprintItem(), disk), "旧磁盘 NBT 应兼容读取");
        helper.succeed();
    }

    private static void menu(GameTestHelper helper) {
        var be = machine(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack disk = disk(helper, Blocks.STONE.defaultBlockState());
        ItemStack expected = disk.copy();
        player.getInventory().setItem(0, disk);
        SmartBlockPlacerMenu menu = new SmartBlockPlacerMenu(ModMenuTypes.SMART_BLOCK_PLACER.get(), 1, player.getInventory(), be);
        menu.quickMoveStack(player, 30);
        helper.assertTrue(ItemStack.matches(be.getBlueprintItem(), expected) && player.getInventory().getItem(0).isEmpty(),
            "快捷移动必须把磁盘存入新的资源槽位");
        menu.getBookInventory().setItem(0, new ItemStack(Items.BOOK));
        helper.assertTrue(menu.slots.get(2).hasItem(), "材料清单书必须由菜单生成");
        helper.assertTrue(menu.quickMoveStack(player, 0).isEmpty() && !be.getBlueprintItem().isEmpty(), "有书时不能取出磁盘");
        menu.getBookInventory().setItem(0, ItemStack.EMPTY);
        menu.quickMoveStack(player, 0);
        helper.assertTrue(be.getBlueprintItem().isEmpty(), "清空书槽后必须可以取出磁盘");
        helper.succeed();
    }
}
