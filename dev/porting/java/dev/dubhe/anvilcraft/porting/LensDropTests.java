package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.laser.LensBlock;
import dev.dubhe.anvilcraft.block.state.LensType;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.tool.DragonRodItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class LensDropTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_lens_normal_drops", LensDropTests::normal,
        "port_lens_rod_drops", LensDropTests::rod,
        "port_lens_devourer_drops", LensDropTests::machine
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_lens_drops"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static Item glass(LensType type) {
        return switch (type) {
            case NONE -> Items.AIR;
            case ROYAL -> ModBlocks.TEMPERING_GLASS.asItem();
            case FROST -> ModBlocks.FROST_GLASS.asItem();
            case EMBER -> ModBlocks.EMBER_GLASS.asItem();
        };
    }

    private static BlockPos place(GameTestHelper helper, LensType type) {
        var pos = helper.absolutePos(new BlockPos(8, 14, 8));
        helper.getLevel().setBlock(pos, ModBlocks.LENS.getDefaultState().setValue(LensBlock.TYPE, type), Block.UPDATE_ALL);
        return pos;
    }

    private static int dropped(GameTestHelper helper, BlockPos pos, Item item) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream()
            .filter(entity -> entity.getItem().is(item)).mapToInt(entity -> entity.getItem().getCount()).sum();
    }

    private static void normal(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_PICKAXE));
            for (var type : LensType.values()) {
                var pos = place(helper, type);
                helper.assertTrue(player.gameMode.destroyBlock(pos), "普通挖掘必须成功：" + type);
                helper.assertTrue(dropped(helper, pos, ModBlocks.LENS.asItem()) == 1
                    && dropped(helper, pos, glass(type)) == (type == LensType.NONE ? 0 : 1),
                    "普通挖掘必须掉落一个透镜和至多一块对应玻璃：" + type);
                helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).forEach(Entity::discard);
            }
        }
        helper.succeed();
    }

    private static void rod(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            player.setItemInHand(InteractionHand.MAIN_HAND, ModItems.DRAGON_ROD.asStack());
            for (var type : LensType.values()) {
                for (int slot = 1; slot < 36; slot++) player.getInventory().setItem(slot, ItemStack.EMPTY);
                player.getCooldowns().addCooldown(DragonRodItem.COOLDOWN_GROUP, 0);
                var pos = place(helper, type);
                DragonRodItem.devourBlock(helper.getLevel(), player, InteractionHand.MAIN_HAND, pos,
                    helper.getLevel().getBlockState(pos), Direction.UP);
                helper.assertTrue(helper.getLevel().getBlockState(pos).isAir(), "龙杖必须吞噬透镜：" + type);
                helper.assertTrue(player.getInventory().countItem(ModBlocks.LENS.asItem()) == 1
                    && player.getInventory().countItem(glass(type)) == (type == LensType.NONE ? 0 : 1),
                    "龙杖收集的掉落必须包括且仅包括一次加载玻璃：" + type);
            }
        }
        helper.succeed();
    }

    private static void machine(GameTestHelper helper) {
        for (var type : LensType.values()) {
            var pos = place(helper, type);
            var machine = pos.south();
            helper.getLevel().setBlock(machine, ModBlocks.BLOCK_DEVOURER.getDefaultState(), Block.UPDATE_ALL);
            helper.getLevel().setBlock(machine.south(), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
            var output = (ChestBlockEntity) helper.getLevel().getBlockEntity(machine.south());
            output.clearContent();
            ModBlocks.BLOCK_DEVOURER.get().devourBlock(helper.getLevel(), machine, Direction.NORTH, 0);
            helper.assertTrue(output.countItem(ModBlocks.LENS.asItem()) == 1
                && output.countItem(glass(type)) == (type == LensType.NONE ? 0 : 1),
                "吞噬器输出必须包含加载玻璃且不能重复：" + type);
        }
        helper.succeed();
    }
}
