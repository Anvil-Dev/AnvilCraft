package dev.dubhe.anvilcraft.porting;

import com.mojang.serialization.JsonOps;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.DevourRange;
import dev.dubhe.anvilcraft.item.tool.DragonRodItem;
import dev.dubhe.anvilcraft.network.SwitchDragonRodProtectContainersPacket;
import dev.dubhe.anvilcraft.util.DevourUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class DragonRodTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_devour_planes", DragonRodTests::planes,
        "port_devour_support_order", DragonRodTests::support,
        "port_devour_chain", DragonRodTests::chain,
        "port_dragon_costs", DragonRodTests::costs,
        "port_dragon_container_protection", DragonRodTests::protection,
        "port_devour_multipart", DragonRodTests::multipart,
        "port_devour_machine_order", DragonRodTests::machine,
        "port_dragon_continuous", DragonRodTests::continuous
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_dragon_rod"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static BlockPos center(GameTestHelper helper) {
        return helper.absolutePos(new BlockPos(8, 12, 8));
    }

    private static void place(GameTestHelper helper, BlockPos pos, Block block) {
        helper.getLevel().setBlock(pos, block.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    private static void planes(GameTestHelper helper) {
        var center = center(helper);
        for (var face : Direction.values()) {
            for (int radius = 1; radius <= 4; radius++) {
                var expected = new HashSet<BlockPos>();
                for (int a = -radius; a <= radius; a++) {
                    for (int b = -radius; b <= radius; b++) {
                        var pos = switch (face.getAxis()) {
                            case X -> center.offset(0, a, b);
                            case Y -> center.offset(a, 0, b);
                            case Z -> center.offset(a, b, 0);
                        };
                        expected.add(pos);
                        place(helper, pos, Blocks.GLASS);
                    }
                }
                var actual = DevourUtil.getDevourPosList(helper.getLevel(), center, face, radius, 0);
                helper.assertTrue(actual.size() == expected.size() && new HashSet<>(actual).equals(expected),
                    "六面和四种范围必须形成准确、无重复的平面：" + face + "/" + radius);
                expected.forEach(pos -> place(helper, pos, Blocks.AIR));
            }
        }
        helper.succeed();
    }

    private static void support(GameTestHelper helper) {
        var center = center(helper);
        place(helper, center, Blocks.OAK_PLANKS);
        place(helper, center.above(), Blocks.TORCH);
        var targets = DevourUtil.getDevourPosList(helper.getLevel(), center, Direction.NORTH, 1, 0);
        helper.assertTrue(targets.indexOf(center.above()) < targets.indexOf(center), "依赖支撑的火把必须先于支撑方块吞噬");
        helper.succeed();
    }

    private static void chain(GameTestHelper helper) {
        var center = center(helper);
        place(helper, center, Blocks.OAK_PLANKS);
        place(helper, center.above(), Blocks.SAND);
        place(helper, center.above(2), Blocks.GRAVEL);
        place(helper, center.above(3), Blocks.STONE);
        place(helper, center.above(4), Blocks.SAND);
        var targets = DevourUtil.getDevourPosList(helper.getLevel(), center, Direction.UP, 0, 8);
        helper.assertTrue(targets.size() == 3 && targets.contains(center.above(2)) && !targets.contains(center.above(4)),
            "向上连锁必须在首个不可连锁方块处停止");
        helper.succeed();
    }

    private static void costs(GameTestHelper helper) {
        helper.assertTrue(!DevourUtil.canDevour(ModBlocks.MINERAL_FOUNTAIN.getDefaultState())
            && !DevourUtil.canDevour(ModBlocks.STURDY_DEEPSLATE.getDefaultState())
            && DevourUtil.canDevour(Blocks.CHEST.defaultBlockState()), "新禁用名单必须保护源指定方块并允许关闭容器保护");
        var stack = ModItems.DRAGON_ROD.asStack();
        int[] damage = {0, 1, 2, 4};
        int i = 0;
        for (var range : DevourRange.values()) {
            stack.set(ModComponents.DEVOUR_RANGE, range);
            helper.assertTrue(DragonRodItem.calculateDamage(stack) == damage[i++], "3/5/7/9 范围费用必须为 0/1/2/4");
        }
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            stack.set(ModComponents.DEVOUR_RANGE, DevourRange.THREE);
            var center = center(helper);
            place(helper, center, Blocks.OAK_PLANKS);
            DragonRodItem.devourBlock(helper.getLevel(), player, InteractionHand.MAIN_HAND, center,
                helper.getLevel().getBlockState(center), Direction.UP);
            helper.assertTrue(helper.getLevel().getBlockState(center).isAir() && stack.getDamageValue() == 0,
                "3x3 必须真实完成吞噬且无耐久消耗");
            helper.assertTrue(player.getCooldowns().isOnCooldown(ModItems.FROST_DRAGON_ROD.asStack())
                && !player.getCooldowns().isOnCooldown(ModItems.TRANSCENDENCE_DRAGON_ROD.asStack()),
                "普通龙杖共用冷却但不能给超限龙杖追加普通冷却");
            var unrelated = new ItemStack(Items.IRON_PICKAXE);
            player.setItemInHand(InteractionHand.MAIN_HAND, unrelated);
            place(helper, center, Blocks.OAK_PLANKS);
            DragonRodItem.devourBlock(helper.getLevel(), player, InteractionHand.MAIN_HAND, center,
                helper.getLevel().getBlockState(center), Direction.UP);
            helper.assertTrue(helper.getLevel().getBlockState(center).is(Blocks.OAK_PLANKS), "没有吞噬组件的物品不能通过吞噬入口破坏方块");
            player.getAbilities().instabuild = true;
            helper.assertTrue(!stack.canDestroyBlock(Blocks.STONE.defaultBlockState(), helper.getLevel(), center, player),
                "创造模式不能额外执行原版直接挖掘");
        }
        helper.succeed();
    }

    private static void protection(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            var rod = ModItems.EMBER_DRAGON_ROD.asStack();
            player.setItemInHand(InteractionHand.MAIN_HAND, rod);
            var center = center(helper);
            place(helper, center, Blocks.CHEST);
            place(helper, center.east(), Blocks.OAK_PLANKS);
            var chest = (ChestBlockEntity) helper.getLevel().getBlockEntity(center);
            chest.setItem(0, new ItemStack(Items.DIAMOND, 3));
            new SwitchDragonRodProtectContainersPacket(InteractionHand.MAIN_HAND, true).handleOnServer(player);
            var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
            var restored = ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, rod).getOrThrow()).getOrThrow();
            helper.assertTrue(DragonRodItem.protectsContainers(restored), "容器保护必须持久化");
            DragonRodItem.devourBlock(helper.getLevel(), player, InteractionHand.MAIN_HAND, center,
                helper.getLevel().getBlockState(center), Direction.UP);
            helper.assertTrue(helper.getLevel().getBlockState(center).is(Blocks.CHEST) && chest.getItem(0).getCount() == 3
                && helper.getLevel().getBlockState(center.east()).isAir(), "保护模式必须保留容器及内容、正常吞噬旁边方块");
            player.getCooldowns().addCooldown(DragonRodItem.COOLDOWN_GROUP, 0);
            new SwitchDragonRodProtectContainersPacket(InteractionHand.MAIN_HAND, false).handleOnServer(player);
            DragonRodItem.devourBlock(helper.getLevel(), player, InteractionHand.MAIN_HAND, center,
                helper.getLevel().getBlockState(center), Direction.UP);
            helper.assertTrue(helper.getLevel().getBlockState(center).isAir(), "关闭保护后应恢复容器吞噬");
            var invalid = new ItemStack(Items.STICK);
            player.setItemInHand(InteractionHand.OFF_HAND, invalid);
            new SwitchDragonRodProtectContainersPacket(InteractionHand.OFF_HAND, true).handleOnServer(player);
            helper.assertTrue(!invalid.has(ModComponents.DEVOUR_PROTECT_CONTAINERS), "保护包不能修改非龙杖物品");
            var offhand = ModItems.FROST_DRAGON_ROD.asStack();
            player.setItemInHand(InteractionHand.OFF_HAND, offhand);
            new SwitchDragonRodProtectContainersPacket(InteractionHand.OFF_HAND, true).handleOnServer(player);
            helper.assertTrue(DragonRodItem.protectsContainers(offhand) && !DragonRodItem.protectsContainers(rod),
                "副手保护包必须只修改指定使用手");
        }
        helper.succeed();
    }

    private static void multipart(GameTestHelper helper) {
        var center = center(helper);
        place(helper, center.below(), Blocks.OAK_PLANKS);
        helper.getLevel().setBlock(center, Blocks.OAK_DOOR.defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.getLevel().setBlock(center.above(), Blocks.OAK_DOOR.defaultBlockState()
            .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), Block.UPDATE_CLIENTS);
        var targets = DevourUtil.getDevourPosList(helper.getLevel(), center, Direction.NORTH, 1, 0);
        helper.assertTrue(targets.contains(center) && !targets.contains(center.above()), "门上下半必须合并到唯一主部件");
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            player.setItemInHand(InteractionHand.MAIN_HAND, ModItems.DRAGON_ROD.asStack());
            DragonRodItem.devourBlock(helper.getLevel(), player, InteractionHand.MAIN_HAND, center,
                helper.getLevel().getBlockState(center), Direction.NORTH);
            helper.assertTrue(player.getInventory().countItem(Items.OAK_DOOR) == 1, "多部件不能重复掉落");
        }
        helper.succeed();
    }

    private static void machine(GameTestHelper helper) {
        var machine = center(helper);
        place(helper, machine, ModBlocks.BLOCK_DEVOURER.get());
        var center = machine.north();
        place(helper, center, Blocks.OAK_PLANKS);
        place(helper, center.above(), Blocks.TORCH);
        place(helper, machine.south(), Blocks.CHEST);
        ModBlocks.BLOCK_DEVOURER.get().devourBlock(helper.getLevel(), machine, Direction.NORTH, 1);
        var output = (ChestBlockEntity) helper.getLevel().getBlockEntity(machine.south());
        helper.assertTrue(output.countItem(Items.TORCH) == 1 && output.countItem(Items.OAK_PLANKS) == 1,
            "方块吞噬器也必须共用依赖顺序，将火把和支撑的掉落都送进输出容器");
        helper.succeed();
    }

    private static void continuous(GameTestHelper helper) {
        var fixture = new StorageFluidRpcTests.Fixture(helper, true);
        var player = fixture.player();
        var center = helper.absolutePos(new BlockPos(2, 15, 4));
        player.setPos(center.getX() + 0.5, center.getY() - 1, center.getZ() - 2.5);
        player.setYRot(0);
        player.setYHeadRot(0);
        player.yHeadRotO = 0;
        player.setXRot(0);
        player.setOldPosAndRot();
        var rod = ModItems.TRANSCENDENCE_DRAGON_ROD.asStack();
        player.setItemInHand(InteractionHand.MAIN_HAND, rod);
        place(helper, center, Blocks.OAK_PLANKS);
        DragonRodItem.devourBlock(helper.getLevel(), player, InteractionHand.MAIN_HAND, center,
            helper.getLevel().getBlockState(center), Direction.NORTH);
        helper.assertTrue(player.getCooldowns().isOnCooldown(rod), "超限首次吞噬后必须有启动冷却");
        helper.runAfterDelay(10, () -> {
            try {
                for (int i = 0; i < 10; i++) player.getCooldowns().tick();
                place(helper, center, Blocks.OAK_PLANKS);
                DragonRodItem.devourBlock(helper.getLevel(), player, InteractionHand.MAIN_HAND, center,
                    helper.getLevel().getBlockState(center), Direction.NORTH);
                helper.assertTrue(!player.getCooldowns().isOnCooldown(rod), "预热后的超限吞噬不能被普通组冷却阻挡");
                place(helper, center, Blocks.OAK_PLANKS);
                var hit = (net.minecraft.world.phys.BlockHitResult) player.pick(player.blockInteractionRange(), 0, false);
                helper.assertTrue(hit.getBlockPos().equals(center), "连续吞噬射线必须命中目标：" + hit.getType() + "/" + hit.getBlockPos() + "/" + center
                    + "/" + helper.getLevel().getBlockState(hit.getBlockPos()) + "/" + player.getViewVector(0));
                helper.assertTrue(DragonRodItem.canDevour(player, player.getMainHandItem()), "实际主手仍须可吞噬");
                DragonRodItem.tickContinuousDevour(player);
                helper.assertTrue(helper.getLevel().getBlockState(center).isAir(), "预热完成后必须通过服务器射线继续吞噬");
                DragonRodItem.stopContinuousMode(player);
                place(helper, center, Blocks.OAK_PLANKS);
                DragonRodItem.tickContinuousDevour(player);
                helper.assertTrue(helper.getLevel().getBlockState(center).is(Blocks.OAK_PLANKS), "释放攻击后必须停止连续吞噬");
                helper.succeed();
            } finally {
                DragonRodItem.stopContinuousMode(player);
                fixture.close();
            }
        });
    }
}
