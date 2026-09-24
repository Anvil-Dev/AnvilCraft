package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.GiantMonolithCoreBlock;
import dev.dubhe.anvilcraft.block.MonolithBlock;
import dev.dubhe.anvilcraft.block.entity.MonolithCoreBlockEntity;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

public final class MonolithOfferingTests {
    @EventBusSubscriber(modid = AnvilCraft.MOD_ID)
    public static final class Registration {
        @SubscribeEvent
        public static void functions(RegisterEvent event) {
            event.register(Registries.TEST_FUNCTION, registry -> registry.register(AnvilCraft.of("port_monolith_offering"),
                MonolithOfferingTests::offering));
        }

        @SubscribeEvent
        public static void register(RegisterGameTestsEvent event) {
            var environment = event.registerEnvironment(AnvilCraft.of("port_monolith_offering"));
            event.registerTest(AnvilCraft.of("port_monolith_offering"), new FunctionGameTestInstance(
                ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of("port_monolith_offering")),
                new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)));
        }
    }

    private static void offering(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(pos, ModBlocks.MONOLITH_CORE.getDefaultState().setValue(MonolithBlock.AXIS, Direction.Axis.X), 3);
        for (int y = 1; y <= 3; y++) {
            level.setBlock(pos.above(y), ModBlocks.MONOLITH_LINE.getDefaultState().setValue(MonolithBlock.AXIS, Direction.Axis.X), 3);
        }
        level.setBlock(pos.above(4), ModBlocks.MONOLITH_LINE.getDefaultState().setValue(MonolithBlock.AXIS, Direction.Axis.Z), 3);
        var core = (MonolithCoreBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(core != null, "Small monolith has its core block entity");
        if (core == null) return;
        var player = FakePlayerFactory.getMinecraft(level);
        var tool = new ItemStack(Items.DIAMOND_PICKAXE);
        var silk = tool.copy();
        silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), 1);
        for (var block : new net.minecraft.world.level.block.Block[]{ModBlocks.MONOLITH.get(), ModBlocks.MONOLITH_CORE.get(),
            ModBlocks.MONOLITH_LINE.get(), ModBlocks.GIANT_MONOLITH_LINE.get()}) {
            var plain = net.minecraft.world.level.block.Block.getDrops(block.defaultBlockState(), level, pos, null, player, tool);
            var precise = net.minecraft.world.level.block.Block.getDrops(block.defaultBlockState(), level, pos, null, player, silk);
            helper.assertTrue(plain.size() == 1 && plain.getFirst().is(ModBlocks.MONOLITH.asItem()) && plain.getFirst().count() == 1,
                "Ordinary monolith drop changed");
            helper.assertTrue(precise.size() == 1 && precise.getFirst().is(block.asItem()) && precise.getFirst().count() == 1,
                "Silk-touch monolith drop changed");
        }
        var mainState = ModBlocks.GIANT_MONOLITH_CORE.getDefaultState().setValue(GiantMonolithCoreBlock.HALF, Cube3x3PartHalf.MID_CENTER);
        var giantDrops = net.minecraft.world.level.block.Block.getDrops(mainState, level, pos, null, player, tool);
        helper.assertTrue(giantDrops.size() == 1 && giantDrops.getFirst().is(ModBlocks.MONOLITH.asItem())
            && giantDrops.getFirst().count() == 27, "Giant core did not yield 27 monolith blocks");
        var giantSilk = net.minecraft.world.level.block.Block.getDrops(mainState, level, pos, null, player, silk);
        helper.assertTrue(giantSilk.size() == 1 && giantSilk.getFirst().is(ModBlocks.GIANT_MONOLITH_CORE.asItem()),
            "Giant core silk-touch drop changed");
        helper.assertTrue(net.minecraft.world.level.block.Block.getDrops(ModBlocks.GIANT_MONOLITH_CORE.getDefaultState(),
            level, pos, null, player, tool).isEmpty(), "Non-primary giant part duplicated loot");
        helper.assertTrue(!core.beginOffering(new ItemStack(Items.STONE), player, new ItemStack(Items.DIAMOND)),
            "Invalid offering accepted");
        helper.assertTrue(core.beginOffering(new ItemStack(Items.CHIPPED_ANVIL), player, new ItemStack(Items.DIAMOND, 2)),
            "Valid offering rejected");
        helper.assertTrue(core.getLineHeight() == 3 && core.getAxis() == Direction.Axis.X && core.isCoolingDown(),
            "Line scan or offering state changed");
        helper.assertTrue(!core.beginOffering(new ItemStack(Items.ANVIL), player, new ItemStack(Items.EMERALD)),
            "Concurrent offering replaced pending reward");
        var tag = core.saveWithoutMetadata(level.registryAccess());
        helper.assertTrue(tag.contains("Offering") && tag.contains("OfferingPlayer") && tag.contains("PendingReward"),
            "Offering save omitted legacy keys");
        tag.putLong("OfferingStart", level.getGameTime() - 99);
        core.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
        core.tick();
        helper.assertTrue(core.isCoolingDown() && core.saveWithoutMetadata(level.registryAccess()).contains("PendingReward"),
            "Reward escaped before tick 100");
        tag.putLong("OfferingStart", level.getGameTime() - 100);
        core.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
        core.tick();
        core.tick();
        int diamonds = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3), item -> item.getItem().is(Items.DIAMOND))
            .stream().mapToInt(item -> item.getItem().count()).sum();
        helper.assertTrue(diamonds == 2, "Offline reward was lost or duplicated");
        helper.assertTrue(!core.saveWithoutMetadata(level.registryAccess()).contains("PendingReward"),
            "Reward remained pending after delivery");
        var giant = ModBlocks.GIANT_MONOLITH_CORE.get();
        var bottom = giant.defaultBlockState();
        int main = 0;
        for (Cube3x3PartHalf part : Cube3x3PartHalf.values()) {
            var state = giant.placedState(part, bottom);
            if (giant.isMainPart(state)) main++;
            helper.assertTrue(state.getCollisionShape(level, pos).isEmpty(), "Giant core placeholder gained collision");
            helper.assertTrue(giant.getMainPartPos(pos.offset(part.getOffset()), state).equals(pos.above()), "Giant core anchor changed");
        }
        helper.assertTrue(main == 1, "Giant core must have exactly one primary block entity");
        helper.assertTrue(bottom.rotate(Rotation.CLOCKWISE_90).getValue(GiantMonolithCoreBlock.AXIS) == Direction.Axis.X,
            "Giant core did not rotate with the template");
        helper.assertTrue(MonolithCoreBlockEntity.acceptsOffering(ModBlocks.GIANT_ANVIL.asStack(), true)
            && !MonolithCoreBlockEntity.acceptsOffering(new ItemStack(Items.ANVIL), true), "Giant offering contract changed");
        AnvilCraft.LOGGER.info("PORT_MONOLITH_OFFERING_PASSED: timing, persistence, offline reward, axes and giant parts");
        helper.succeed();
    }
}
