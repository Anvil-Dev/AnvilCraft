package dev.dubhe.anvilcraft.building;

import com.mojang.authlib.GameProfile;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import dev.dubhe.anvilcraft.item.BuildingRodItem;
import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;
import dev.dubhe.anvilcraft.item.property.component.StructureDiskData;
import dev.dubhe.anvilcraft.network.BuildingRodPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class BuildingServiceTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_service_box_undo", BuildingServiceTests::box,
        "port_service_selection", BuildingServiceTests::selection,
        "port_service_confirmation", BuildingServiceTests::confirmation,
        "port_service_confirmation_expiry", BuildingServiceTests::expiry,
        "port_service_partial", BuildingServiceTests::partial,
        "port_service_water", BuildingServiceTests::water,
        "port_service_lava", BuildingServiceTests::lava,
        "port_service_canceled", BuildingServiceTests::canceled,
        "port_service_evaporation", BuildingServiceTests::evaporation,
        "port_service_blueprint_file", BuildingServiceTests::blueprint
    );

    private static UUID deniedPlayer;

    @SubscribeEvent
    public static void denyPlacement(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() != null && event.getEntity().getUUID().equals(deniedPlayer)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_building_service"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 120, 0, true)
        )));
    }

    private static BlockPos origin(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(3, 16, 3));
        for (var cursor : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(7, 5, 8))) {
            helper.getLevel().setBlock(cursor, cursor.getY() < pos.getY() ? Blocks.STONE.defaultBlockState()
                : Blocks.AIR.defaultBlockState(), Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        }
        return pos;
    }

    private static ServerPlayer player(ServerLevel level, BlockPos pos, ItemStack material) {
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "PortService"));
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() - 2);
        var rod = ModItems.BUILDING_ROD.asStack();
        rod.set(ModComponents.STORED_ENERGY, new StoredEnergy(10000));
        player.setItemInHand(InteractionHand.MAIN_HAND, rod);
        player.setItemInHand(InteractionHand.OFF_HAND, material);
        BuildingRodItem.updateReach(player);
        return player;
    }

    private static int energy(ServerPlayer player) {
        return BuildingRodItem.heldRod(player).getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value();
    }

    private static int count(ServerPlayer player, Item item) {
        return PocketInventory.carriedItems(player).stream()
            .filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static BuildingRodPacket packet(BlockPos first, BlockPos last, BuildingRodPacket.Action action) {
        return new BuildingRodPacket(first, last, Direction.UP, false, Rotation.NONE, Mirror.NONE, false, null, action, 42);
    }

    private static void box(GameTestHelper helper) {
        var pos = origin(helper);
        var player = player(helper.getLevel(), pos, new ItemStack(Items.STONE, 4));
        var request = packet(pos, pos.east(3), BuildingRodPacket.Action.PLACE);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            BuildingRodPacket.STREAM_CODEC.encode(buffer, request);
            var decoded = BuildingRodPacket.STREAM_CODEC.decode(buffer);
            helper.assertTrue(decoded.equals(request), "数据包保留动作、种子及坐标");
            decoded.handleOnServer(player);
        } finally {
            buffer.release();
        }
        for (int x = 0; x < 4; x++) helper.assertTrue(helper.getLevel().getBlockState(pos.east(x)).is(Blocks.STONE), "实际区域放置");
        helper.assertTrue(count(player, Items.STONE) == 0 && energy(player) == 9600, "真实扣除四个方块和 400 FE");
        packet(pos, pos, BuildingRodPacket.Action.UNDO).handleOnServer(player);
        helper.assertTrue(count(player, Items.STONE) == 4 && energy(player) == 9600, "撤销退款且不额外恢复电量");
        for (int x = 0; x < 4; x++) helper.assertTrue(helper.getLevel().getBlockState(pos.east(x)).isAir(), "撤销恢复区域");
        helper.succeed();
    }

    private static void selection(GameTestHelper helper) {
        var pos = origin(helper);
        var player = player(helper.getLevel(), pos, new ItemStack(Items.STONE, 4));
        packet(pos, pos, BuildingRodPacket.Action.START).handleOnServer(player);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.DIRT, 4));
        packet(pos, pos.east(), BuildingRodPacket.Action.PLACE).handleOnServer(player);
        helper.assertTrue(helper.getLevel().getBlockState(pos).isAir() && energy(player) == 10000, "开始后换材料使选区失效");
        var hit = new BlockHitResult(new Vec3(Double.NaN, 0, 0), Direction.UP, pos, false);
        BuildingRodService.box(player, pos, pos.east(), Direction.UP, hit);
        helper.assertTrue(helper.getLevel().getBlockState(pos).isAir(), "非有限点击位置必须拒绝");
        packet(pos.east(100), pos.east(101), BuildingRodPacket.Action.PLACE).handleOnServer(player);
        helper.assertTrue(count(player, Items.DIRT) == 4, "超距请求不能扣料");
        BuildingRodItem.heldRod(player).set(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY);
        packet(pos, pos.east(), BuildingRodPacket.Action.PLACE).handleOnServer(player);
        helper.assertTrue(helper.getLevel().getBlockState(pos).isAir() && count(player, Items.DIRT) == 4,
            "无电量且无电容时不能放置或扣料");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        packet(pos, pos.east(), BuildingRodPacket.Action.PLACE).handleOnServer(player);
        helper.assertTrue(helper.getLevel().getBlockState(pos).isAir(), "未持杖请求必须拒绝");
        helper.succeed();
    }

    private static ItemStack named(Item item, String name) {
        var stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static BuildingPlan.Group group(BlockPos pos, BlockState state, ItemStack material) {
        var group = new BuildingPlan.Group();
        group.cells.add(new BuildingPlan.Cell(pos, state, new CompoundTag(), List.of()));
        group.blockMaterials.put(pos, material);
        group.separateContents = true;
        return group;
    }

    private static void confirmation(GameTestHelper helper) {
        var pos = origin(helper);
        var player = player(helper.getLevel(), pos, ModItems.STRUCTURE_DISK.asStack());
        var groups = List.of(group(pos, Blocks.CHEST.defaultBlockState(), named(Items.CHEST, "Blueprint name")));
        player.getInventory().setItem(1, named(Items.CHEST, "Actual A"));
        helper.assertTrue(!BuildingRodService.commit(player, groups, false, true) && energy(player) == 10000,
            "组件不匹配第一次请求只能提示确认");
        player.getInventory().setItem(1, named(Items.CHEST, "Actual B"));
        helper.assertTrue(!BuildingRodService.commit(player, groups, false, true), "实际供料组件变化必须重新确认");
        helper.assertTrue(BuildingRodService.commit(player, groups, false, true), "相同供料第二次请求确认提交");
        var chest = (ChestBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(chest != null && chest.getName().getString().equals("Actual B"), "最终方块使用实际消耗物品的组件");
        helper.assertTrue(count(player, Items.CHEST) == 0 && energy(player) == 9900, "只在最终提交扣料及电量");
        BuildingRodUndo.undo(player);
        helper.assertTrue(PocketInventory.carriedItems(player).stream().anyMatch(stack -> stack.is(Items.CHEST)
            && stack.getHoverName().getString().equals("Actual B")), "撤销返还实际组件");
        helper.succeed();
    }

    private static void expiry(GameTestHelper helper) {
        var pos = origin(helper);
        var player = player(helper.getLevel(), pos, ModItems.STRUCTURE_DISK.asStack());
        player.getInventory().setItem(1, new ItemStack(Items.STONE));
        var groups = List.of(group(pos, Blocks.STONE.defaultBlockState(), named(Items.STONE, "Expected")));
        helper.assertTrue(!BuildingRodService.commit(player, groups, false, true), "第一次请求等待确认");
        helper.runAtTickTime(62, () -> {
            helper.assertTrue(!BuildingRodService.commit(player, groups, false, true), "超过 60 tick 的确认过期");
            helper.assertTrue(helper.getLevel().getBlockState(pos).isAir(), "过期确认不能提前放置");
            helper.assertTrue(BuildingRodService.commit(player, groups, false, true), "重新确认后允许提交");
            helper.succeed();
        });
    }

    private static void partial(GameTestHelper helper) {
        var pos = origin(helper);
        var player = player(helper.getLevel(), pos, ModItems.STRUCTURE_DISK.asStack());
        player.getInventory().setItem(1, new ItemStack(Items.STONE));
        var groups = List.of(group(pos, Blocks.STONE.defaultBlockState(), new ItemStack(Items.STONE)),
            group(pos.east(), Blocks.DIRT.defaultBlockState(), new ItemStack(Items.DIRT)));
        helper.assertTrue(!BuildingRodService.commit(player, groups, false, true)
            && helper.getLevel().getBlockState(pos).isAir() && count(player, Items.STONE) == 1, "完整模式缺料时不放置不扣料");
        helper.assertTrue(!BuildingRodService.commit(player, groups, true, true), "部分模式不宣称整份蓝图完成");
        helper.assertTrue(helper.getLevel().getBlockState(pos).is(Blocks.STONE)
            && helper.getLevel().getBlockState(pos.east()).isAir() && energy(player) == 9900, "仅提交材料充足的组");
        BuildingRodUndo.undo(player);
        helper.assertTrue(helper.getLevel().getBlockState(pos).isAir() && count(player, Items.STONE) == 1, "部分提交可撤销");
        helper.succeed();
    }

    private static void water(GameTestHelper helper) {
        var pos = origin(helper);
        var player = player(helper.getLevel(), pos, new ItemStack(Items.WATER_BUCKET));
        player.getInventory().setItem(1, new ItemStack(Items.WATER_BUCKET));
        var slab = Blocks.OAK_SLAB.defaultBlockState();
        helper.getLevel().setBlockAndUpdate(pos, slab);
        BuildingRodService.box(player, pos, pos.east(2), Direction.UP);
        helper.assertTrue(helper.getLevel().getBlockState(pos).getValue(BlockStateProperties.WATERLOGGED), "已有半砖可含水");
        helper.assertTrue(helper.getLevel().getFluidState(pos.east()).isSource()
            && helper.getLevel().getFluidState(pos.east(2)).isSource(), "空格放置水源");
        helper.assertTrue(count(player, Items.WATER_BUCKET) == 0 && count(player, Items.BUCKET) == 2 && energy(player) == 9700,
            "无限水区域只收两桶，能量仍按实际三格收取");
        BuildingRodUndo.undo(player);
        helper.assertTrue(helper.getLevel().getBlockState(pos) == slab && helper.getLevel().getBlockState(pos.east()).isAir(),
            "撤销保留原半砖并清除新水源");
        helper.assertTrue(count(player, Items.WATER_BUCKET) == 2 && count(player, Items.BUCKET) == 0, "撤销流体容器账单");
        helper.succeed();
    }

    private static void lava(GameTestHelper helper) {
        var pos = origin(helper);
        var player = player(helper.getLevel(), pos, new ItemStack(Items.LAVA_BUCKET));
        player.getInventory().setItem(1, new ItemStack(Items.LAVA_BUCKET));
        BuildingRodService.box(player, pos, pos.east(2), Direction.UP);
        helper.assertTrue(helper.getLevel().getBlockState(pos).isAir() && count(player, Items.LAVA_BUCKET) == 2,
            "不可再生熔岩逐格计费，两桶不足三格时不提前扣料");
        player.getInventory().setItem(2, new ItemStack(Items.LAVA_BUCKET));
        BuildingRodService.box(player, pos, pos.east(2), Direction.UP);
        for (int x = 0; x < 3; x++) {
            helper.assertTrue(helper.getLevel().getBlockState(pos.east(x)).is(Blocks.LAVA), "材料充足后放置全部熔岩格");
        }
        helper.assertTrue(count(player, Items.LAVA_BUCKET) == 0 && count(player, Items.BUCKET) == 3 && energy(player) == 9700,
            "实际扣除三桶并返还三只空桶");
        BuildingRodUndo.undo(player);
        helper.assertTrue(count(player, Items.LAVA_BUCKET) == 3 && count(player, Items.BUCKET) == 0, "熔岩账单可精确撤销");
        helper.succeed();
    }

    private static void canceled(GameTestHelper helper) {
        var pos = origin(helper);
        var player = player(helper.getLevel(), pos, new ItemStack(Items.STONE, 2));
        deniedPlayer = player.getUUID();
        try {
            BuildingRodService.box(player, pos, pos.east(), Direction.UP);
            helper.assertTrue(helper.getLevel().getBlockState(pos).isAir() && count(player, Items.STONE) == 2
                && energy(player) == 10000, "取消放置事件不得改变世界、材料或电量");
        } finally {
            deniedPlayer = null;
        }
        BuildingRodService.box(player, pos, pos.east(), Direction.UP);
        helper.assertTrue(helper.getLevel().getBlockState(pos).is(Blocks.STONE), "取消监听解除后真实放置可继续");
        helper.succeed();
    }

    private static void evaporation(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(level != null, "验证需要实际下界");
        var pos = new BlockPos(-480, 120, -480);
        level.getChunkAt(pos);
        var before = level.getBlockState(pos);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        try {
            var player = player(level, pos, new ItemStack(Items.WATER_BUCKET));
            player.getInventory().setItem(1, new ItemStack(Items.WATER_BUCKET));
            BuildingRodService.box(player, pos, pos, Direction.UP);
            helper.assertTrue(level.getBlockState(pos).isAir() && count(player, Items.WATER_BUCKET) == 0
                && count(player, Items.BUCKET) == 2 && energy(player) == 9900, "下界蒸发消耗流体与电量但不生成水源");
        } finally {
            level.setBlock(pos, before, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        }
        helper.succeed();
    }

    private static void blueprint(GameTestHelper helper) {
        var pos = origin(helper);
        var source = pos.south(6);
        var level = helper.getLevel();
        level.setBlockAndUpdate(source, Blocks.CHEST.defaultBlockState());
        ((ChestBlockEntity) level.getBlockEntity(source)).setItem(0, new ItemStack(Items.DIAMOND, 2));
        var stand = EntityType.ARMOR_STAND.create(level, EntitySpawnReason.LOAD);
        stand.setPos(Vec3.atBottomCenterOf(source.east()));
        stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        level.addFreshEntity(stand);
        final var snapshot = BlueprintCapture.capture(level, new AABB(Vec3.atLowerCornerOf(source),
            Vec3.atLowerCornerOf(source.offset(2, 3, 1)))).snapshot();
        var uuid = UUID.randomUUID();
        var file = "service_" + uuid + ".nbt";
        final var path = level.getServer().getWorldPath(LevelResource.ROOT).resolve("anvilcraft/structures").resolve(file);
        var disk = ModItems.STRUCTURE_DISK.asStack();
        disk.set(ModComponents.STRUCTURE_DISK_DATA, new StructureDiskData(file, "Service test", uuid, Direction.NORTH, 2, 3, 1));
        var player = player(level, pos, disk);
        player.getInventory().setItem(1, new ItemStack(Items.CHEST));
        player.getInventory().setItem(2, new ItemStack(Items.DIAMOND, 2));
        player.getInventory().setItem(3, new ItemStack(Items.ARMOR_STAND));
        player.getInventory().setItem(4, new ItemStack(Items.IRON_HELMET));
        try {
            Files.createDirectories(path.getParent());
            NbtIo.writeCompressed(StructureSnapshotCodec.write(snapshot), path);
            BuildingRodService.blueprint(player, pos, Rotation.NONE, Mirror.NONE, false);
            helper.assertTrue(level.getBlockEntity(pos) instanceof ChestBlockEntity, "实际磁盘文件入口放置箱子");
            var chest = (ChestBlockEntity) level.getBlockEntity(pos);
            helper.assertTrue(chest.getItem(0).is(Items.DIAMOND) && chest.getItem(0).getCount() == 2, "真实容器内容独立供料并恢复");
            var copies = level.getEntitiesOfClass(ArmorStand.class, new AABB(pos).inflate(2));
            helper.assertTrue(copies.size() == 1 && copies.getFirst().getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET),
                "实体和装备从蓝图链路一起生成");
            helper.assertTrue(count(player, Items.ARMOR_STAND) == 0 && count(player, Items.IRON_HELMET) == 0, "实体装备实际扣料");
            BuildingRodUndo.undo(player);
            helper.assertTrue(level.getBlockState(pos).isAir() && copies.getFirst().isRemoved(), "蓝图撤销清理方块和生成实体");
            helper.assertTrue(count(player, Items.CHEST) == 1 && count(player, Items.DIAMOND) == 2
                && count(player, Items.ARMOR_STAND) == 1 && count(player, Items.IRON_HELMET) == 1, "撤销精确返还整份蓝图账单");
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        } finally {
            stand.discard();
            try {
                Files.deleteIfExists(path);
            } catch (IOException error) {
                throw new UncheckedIOException(error);
            }
        }
        helper.succeed();
    }
}
