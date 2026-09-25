package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.integration.StorageRecipeTransferPlan;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StorageRecipeTransferPlanTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_jei_plan_variants", StorageRecipeTransferPlanTests::variants,
        "port_jei_plan_long", StorageRecipeTransferPlanTests::large,
        "port_jei_plan_fluids", StorageRecipeTransferPlanTests::fluids,
        "port_jei_plan_fluid_shortage", StorageRecipeTransferPlanTests::shortage,
        "port_jei_plan_components", StorageRecipeTransferPlanTests::components
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_jei_plan"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static void variants(GameTestHelper helper) {
        var variants = List.of(new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.BIRCH_PLANKS));
        var stock = new HashMap<>(Map.of(ItemResource.of(Items.OAK_PLANKS), 1L, ItemResource.of(Items.BIRCH_PLANKS), 1L));
        var plan = StorageRecipeTransferPlan.create(List.of(variants, List.of(), variants, variants), stock, List.of());
        helper.assertTrue(plan.inputs().get(0).is(Items.OAK_PLANKS) && plan.inputs().get(2).is(Items.BIRCH_PLANKS),
            "标签输入必须按余量选择不同变体");
        helper.assertTrue(plan.inputs().get(1).isEmpty() && plan.counts().getInt(1) == 0
            && plan.missing().size() == 1 && plan.missing().getInt(0) == 3, "空位不得计入缺料，重复输入不得重复认领");
        helper.assertTrue(stock.get(ItemResource.of(Items.OAK_PLANKS)) == 1 && variants.getFirst().getCount() == 1,
            "预检不得修改客户端缓存或 JEI 变体");
        helper.succeed();
    }

    private static void large(GameTestHelper helper) {
        var plan = StorageRecipeTransferPlan.create(Collections.nCopies(9, List.of(new ItemStack(Items.STICK))),
            Map.of(ItemResource.of(Items.STICK), Long.MAX_VALUE), List.of());
        helper.assertTrue(plan.inputs().size() == 9 && plan.missing().isEmpty()
            && plan.counts().intStream().sum() == 9, "超大存储只能按配方槽位规模分配，不能按数量展开");
        helper.succeed();
    }

    private static void fluids(GameTestHelper helper) {
        var variants = List.of(List.of(new ItemStack(Items.WATER_BUCKET)), List.of(new ItemStack(Items.LAVA_BUCKET)),
            List.of(new ItemStack(Items.BUCKET)));
        var fluids = List.of(new StorageServerStub.FluidEntry(new FluidStack(Fluids.WATER, 1), 1000),
            new StorageServerStub.FluidEntry(new FluidStack(Fluids.LAVA, 1), 1000));
        var plan = StorageRecipeTransferPlan.create(variants, Map.of(ItemResource.of(Items.BUCKET), 2L), fluids);
        helper.assertTrue(plan.inputs().get(0).is(Items.WATER_BUCKET) && plan.inputs().get(1).is(Items.LAVA_BUCKET)
            && plan.missing().size() == 1 && plan.missing().getInt(0) == 2, "两种流体与空桶输入必须共用容器预算");
        plan = StorageRecipeTransferPlan.create(variants,
            Map.of(ItemResource.of(Items.WATER_BUCKET), 1L, ItemResource.of(Items.BUCKET), 2L), fluids);
        helper.assertTrue(plan.missing().isEmpty(), "优先使用成品桶才能保留空桶供其余输入使用");
        helper.succeed();
    }

    private static void shortage(GameTestHelper helper) {
        var water = Collections.nCopies(2, List.of(new ItemStack(Items.WATER_BUCKET)));
        var plan = StorageRecipeTransferPlan.create(water, Map.of(ItemResource.of(Items.BUCKET), 5L),
            List.of(new StorageServerStub.FluidEntry(new FluidStack(Fluids.WATER, 1), 1500)));
        helper.assertTrue(plan.missing().size() == 1 && plan.missing().getInt(0) == 1, "同种流体不能被多槽重复预占");
        plan = StorageRecipeTransferPlan.create(Collections.nCopies(3, List.of(new ItemStack(Items.HONEY_BOTTLE))),
            Map.of(ItemResource.of(Items.GLASS_BOTTLE), 3L),
            List.of(new StorageServerStub.FluidEntry(new FluidStack(ModFluids.HONEY.get(), 1), 500)));
        helper.assertTrue(plan.missing().size() == 1 && plan.missing().getInt(0) == 2, "蜂蜜瓶必须按 250 mB 计算可填充数量");
        helper.succeed();
    }

    private static void components(GameTestHelper helper) {
        var named = new ItemStack(Items.STICK);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Named"));
        var plan = StorageRecipeTransferPlan.create(List.of(List.of(new ItemStack(Items.STICK))),
            Map.of(ItemResource.of(named), 100L), List.of());
        helper.assertTrue(plan.missing().size() == 1, "同物品异组件不能伪装为可用材料");
        plan = StorageRecipeTransferPlan.create(List.of(List.of(named)), Map.of(ItemResource.of(named), 1L), List.of());
        helper.assertTrue(plan.missing().isEmpty() && ItemStack.isSameItemSameComponents(plan.inputs().getFirst(), named),
            "已选变体必须保留组件传给服务端");
        helper.succeed();
    }
}
