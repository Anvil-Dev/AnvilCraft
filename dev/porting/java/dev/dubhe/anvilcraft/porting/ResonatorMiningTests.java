package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.tool.ResonateMode;
import dev.dubhe.anvilcraft.item.tool.ResonatorItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class ResonatorMiningTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_resonator_standard_mining", ResonatorMiningTests::standard,
        "port_resonator_transcendence_mining", ResonatorMiningTests::transcendence,
        "port_resonator_durability_gate", ResonatorMiningTests::durability,
        "port_resonator_cancel", ResonatorMiningTests::cancel,
        "port_resonator_main_hand", ResonatorMiningTests::hand,
        "port_resonator_mode_tags", ResonatorMiningTests::tags
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_resonator_mining"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static BlockPos prepare(GameTestHelper helper, StorageFluidRpcTests.Fixture fixture, ItemStack stack, InteractionHand hand) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 15, 4));
        helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        var player = fixture.player();
        player.setPos(pos.getX() + 0.5, pos.getY() - 1, pos.getZ() - 2.5);
        player.setYRot(0);
        player.setXRot(0);
        player.setOldPosAndRot();
        player.getInventory().setSelectedSlot(0);
        player.setItemInHand(hand, stack);
        stack.set(ModComponents.RESONATE_MODE, ResonateMode.AUTO);
        return pos;
    }

    private static InteractionResult start(StorageFluidRpcTests.Fixture fixture, ItemStack stack, BlockPos pos, InteractionHand hand) {
        return ((ResonatorItem) stack.getItem()).onItemUseFirst(stack,
            new UseOnContext(fixture.player(), hand, new BlockHitResult(pos.getCenter(), Direction.NORTH, pos, false)));
    }

    private static void tick(GameTestHelper helper, StorageFluidRpcTests.Fixture fixture, ItemStack stack, int elapsed) {
        stack.getItem().onUseTick(helper.getLevel(), fixture.player(), stack, stack.getUseDuration(fixture.player()) - elapsed);
    }

    private static void standard(GameTestHelper helper) {
        for (var item : new ResonatorItem[]{ModItems.FROST_METAL_RESONATOR.get(), ModItems.EMBER_METAL_RESONATOR.get()}) {
            try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
                var stack = new ItemStack(item);
                stack.setDamageValue(10);
                stack.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.UNBREAKING), 3);
                BlockPos pos = prepare(helper, fixture, stack, InteractionHand.MAIN_HAND);
                helper.assertTrue(start(fixture, stack, pos, InteractionHand.MAIN_HAND) == InteractionResult.CONSUME,
                    "普通共振器 AUTO 主手应进入采掘");
                tick(helper, fixture, stack, 19);
                helper.assertTrue(helper.getLevel().getBlockState(pos).is(Blocks.STONE) && stack.getDamageValue() == 10,
                    "20 tick 之前不能破坏目标或消耗耐久");
                tick(helper, fixture, stack, 20);
                helper.assertTrue(helper.getLevel().getBlockState(pos).isAir() && stack.getDamageValue() == 138,
                    "成功采掘必须固定消耗 128 耐久，不叠加原版损耗且不受耐久附魔影响");
                helper.assertTrue(!fixture.player().isUsingItem()
                    && !ResonatorItem.isResonanceMining(helper.getLevel(), fixture.player(), pos),
                    "完成后应清理使用状态和采掘目标");
            }
        }
        helper.succeed();
    }

    private static void transcendence(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var stack = ModItems.TRANSCENDENCE_RESONATOR.asStack();
            BlockPos pos = prepare(helper, fixture, stack, InteractionHand.MAIN_HAND);
            start(fixture, stack, pos, InteractionHand.MAIN_HAND);
            tick(helper, fixture, stack, 9);
            helper.assertTrue(!helper.getLevel().getBlockState(pos).isAir(), "超限共振器仍需等待 10 tick");
            tick(helper, fixture, stack, 10);
            helper.assertTrue(helper.getLevel().getBlockState(pos).isAir() && stack.getDamageValue() == 0,
                "超限共振器共用采掘路径但不收取额外耐久");
        }
        helper.succeed();
    }

    private static void durability(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var stack = ModItems.FROST_METAL_RESONATOR.asStack();
            BlockPos pos = prepare(helper, fixture, stack, InteractionHand.MAIN_HAND);
            stack.setDamageValue(stack.getMaxDamage() - 128);
            helper.assertTrue(start(fixture, stack, pos, InteractionHand.MAIN_HAND) == InteractionResult.PASS,
                "仅剩 128 耐久时必须拒绝，保留至少一点耐久");
            stack.setDamageValue(stack.getMaxDamage() - 129);
            helper.assertTrue(start(fixture, stack, pos, InteractionHand.MAIN_HAND) == InteractionResult.CONSUME, "129 耐久应恰好足够");
            tick(helper, fixture, stack, 20);
            helper.assertTrue(stack.getDamageValue() == stack.getMaxDamage() - 1 && !stack.isEmpty(), "临界采掘后必须保留工具");
        }
        helper.succeed();
    }

    private static void cancel(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var stack = ModItems.EMBER_METAL_RESONATOR.asStack();
            BlockPos pos = prepare(helper, fixture, stack, InteractionHand.MAIN_HAND);
            start(fixture, stack, pos, InteractionHand.MAIN_HAND);
            fixture.player().setYRot(90);
            fixture.player().setYHeadRot(90);
            fixture.player().yHeadRotO = 90;
            fixture.player().setOldPosAndRot();
            tick(helper, fixture, stack, 20);
            helper.assertTrue(helper.getLevel().getBlockState(pos).is(Blocks.STONE) && stack.getDamageValue() == 0,
                "改变目标应取消且不消耗耐久：" + helper.getLevel().getBlockState(pos) + "/" + stack.getDamageValue());
            fixture.player().setYRot(0);
            fixture.player().setYHeadRot(0);
            fixture.player().yHeadRotO = 0;
            fixture.player().setOldPosAndRot();
            helper.getLevel().setBlockAndUpdate(pos, Blocks.BEDROCK.defaultBlockState());
            helper.assertTrue(start(fixture, stack, pos, InteractionHand.MAIN_HAND) == InteractionResult.PASS, "不可破坏方块不能启动采掘");
        }
        helper.succeed();
    }

    private static void hand(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var stack = ModItems.FROST_METAL_RESONATOR.asStack();
            BlockPos pos = prepare(helper, fixture, stack, InteractionHand.OFF_HAND);
            helper.assertTrue(start(fixture, stack, pos, InteractionHand.OFF_HAND) == InteractionResult.PASS, "副手不能启动共振采掘");
            fixture.player().setItemInHand(InteractionHand.MAIN_HAND, stack);
            fixture.player().setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            ResonatorItem.setMode(fixture.player(), InteractionHand.MAIN_HAND, ResonateMode.PICKAXE);
            helper.assertTrue(start(fixture, stack, pos, InteractionHand.MAIN_HAND) == InteractionResult.PASS,
                "专用工具模式不能误触发 AUTO 采掘");
            helper.assertTrue(stack.getDestroySpeed(Blocks.STONE.defaultBlockState()) > 1, "镐模式应通过原版工具组件提供采矿能力");
        }
        helper.succeed();
    }

    private static void tags(GameTestHelper helper) {
        var stack = ModItems.FROST_METAL_RESONATOR.asStack();
        var pickaxes = helper.getLevel().registryAccess().lookupOrThrow(Registries.ITEM).getOrThrow(ItemTags.PICKAXES);
        stack.set(ModComponents.RESONATE_MODE, ResonateMode.AXE);
        helper.assertTrue(stack.is(ItemTags.AXES) && !stack.is(ItemTags.PICKAXES) && !stack.is(pickaxes),
            "普通标签和命名 HolderSet 都必须服从当前工具模式");
        helper.assertTrue(stack.is(ModItemTags.RESONATOR), "专用模式不能丢失共振器身份标签");
        stack.set(ModComponents.RESONATE_MODE, ResonateMode.AUTO);
        helper.assertTrue(stack.is(ItemTags.AXES) && stack.is(ItemTags.PICKAXES) && stack.is(pickaxes), "AUTO 应恢复所有声明的工具类型");
        stack.remove(ModComponents.RESONATE_MODE);
        helper.assertTrue(stack.is(ItemTags.AXES), "缺少显式模式组件时默认 AUTO");
        helper.succeed();
    }
}
