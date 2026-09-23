package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ItemCollectorBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class BuildingBlockMaterialTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_block_material_components", BuildingBlockMaterialTests::components,
        "port_block_material_supplied", BuildingBlockMaterialTests::supplied,
        "port_block_material_configuration", BuildingBlockMaterialTests::configuration,
        "port_block_material_permissions", BuildingBlockMaterialTests::permissions,
        "port_block_material_filters", BuildingBlockMaterialTests::filters,
        "port_block_material_fluid", BuildingBlockMaterialTests::fluid
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_block_material"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static void components(GameTestHelper helper) {
        var original = new ChestBlockEntity(BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
        var namedChest = new ItemStack(Items.CHEST);
        namedChest.set(DataComponents.CUSTOM_NAME, Component.literal("blueprint chest"));
        original.applyComponentsFromItemStack(namedChest);
        var diamond = new ItemStack(Items.DIAMOND, 7);
        diamond.set(DataComponents.CUSTOM_NAME, Component.literal("stored component"));
        original.setItem(5, diamond);
        var tag = original.saveWithFullMetadata(helper.getLevel().registryAccess());
        var before = tag.copy();
        var material = BuildingBlockMaterial.extract(original.getBlockState(), tag, helper.getLevel());
        helper.assertTrue(material.stack().is(Items.CHEST)
            && Component.literal("blueprint chest").equals(material.stack().get(DataComponents.CUSTOM_NAME)),
            "容器名称必须作为所需物品组件保留");
        helper.assertTrue(material.contents().size() == 1 && material.contents().getFirst().slot() == 5
            && ItemStack.matches(material.contents().getFirst().stack(), diamond), "库存数量和内部物品组件必须独立保留");
        helper.assertTrue(material.stack().getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
            .equals(ItemContainerContents.EMPTY)
            && !material.config().contains("Items") && !material.config().contains("CustomName") && before.equals(tag),
            "库存与物品组件不能留在免费恢复配置中，也不能改变输入快照");
        helper.succeed();
    }

    private static void supplied(GameTestHelper helper) {
        var stock = new ItemStack(Items.CHEST);
        stock.set(DataComponents.CUSTOM_NAME, Component.literal("actual supply"));
        var emerald = new ItemStack(Items.EMERALD, 5);
        emerald.set(DataComponents.CUSTOM_NAME, Component.literal("actual content"));
        var contents = ItemContainerContents.fromItems(List.of(ItemStack.EMPTY, emerald));
        stock.set(DataComponents.CONTAINER, contents);
        var before = stock.copy();
        var material = BuildingBlockMaterial.separateContents(stock, helper.getLevel());
        helper.assertTrue(ItemStack.matches(stock, before) && Component.literal("actual supply").equals(
            material.stack().get(DataComponents.CUSTOM_NAME)), "实际供料组件必须保留且不能在预检时改变原物品");
        helper.assertTrue(material.contents().size() == 1 && material.contents().getFirst().slot() == 1
            && ItemStack.matches(material.contents().getFirst().stack(), emerald)
            && material.stack().getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).equals(ItemContainerContents.EMPTY),
            "供料容器的内容应拆出一份，不能重复携带");
        var wrongData = new CompoundTag();
        wrongData.putString("CustomName", "foreign data");
        stock.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(BlockEntityType.FURNACE, wrongData));
        var mismatch = BuildingBlockMaterial.separateContents(stock, helper.getLevel());
        helper.assertTrue(mismatch.contents().isEmpty() && ItemStack.matches(stock, mismatch.stack()),
            "不匹配的原生类型不能被默默改写或解析为可供料库存");
        helper.succeed();
    }

    private static void configuration(GameTestHelper helper) {
        var state = ModBlocks.ITEM_DETECTOR.getDefaultState();
        var base = BuildingBlockMaterial.extract(state, null, helper.getLevel());
        var settings = new CompoundTag();
        settings.putInt("Range", 6);
        settings.putInt("OpaquePayload", 9);
        settings.putInt("x", 120);
        settings.putInt("y", 80);
        settings.putInt("z", 300);
        var material = base.withConfiguration(state, settings, helper.getLevel().registryAccess());
        var data = material.stack().get(DataComponents.BLOCK_ENTITY_DATA);
        helper.assertTrue(material.config().getIntOr("Range", 0) == 6 && !material.config().contains("OpaquePayload")
            && data != null && data.type() == ModBlockEntities.ITEM_DETECTOR.get(), "安全设置与所需物品的额外数据必须分离");
        var raw = data.copyTagWithoutId();
        helper.assertTrue(raw.getIntOr("OpaquePayload", 0) == 9 && !raw.contains("Range") && !raw.contains("x")
            && settings.contains("x"), "特殊数据必须依附材料，旧世界坐标必须剥离且不改输入");
        helper.succeed();
    }

    private static void permissions(GameTestHelper helper) {
        helper.assertTrue(BuildingBlockMaterial.extract(Blocks.COMMAND_BLOCK.defaultBlockState(), null, helper.getLevel())
            .requiresOperator()
            && !BuildingBlockMaterial.extract(Blocks.OAK_SIGN.defaultBlockState(), null, helper.getLevel()).requiresOperator()
            && !BuildingBlockMaterial.extract(Blocks.LECTERN.defaultBlockState(), null, helper.getLevel()).requiresOperator(),
            "原生方块实体权限必须保留，告示牌与讲台沿用独立内容处理");
        var wrong = new CompoundTag();
        wrong.putString("id", "minecraft:furnace");
        boolean rejected = false;
        try {
            BuildingBlockMaterial.extract(Blocks.CHEST.defaultBlockState(), wrong, helper.getLevel());
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        helper.assertTrue(rejected, "蓝图不能给箱子加载不匹配的实体类型");
        helper.assertTrue(BuildingBlockMaterial.extract(ModBlocks.CRUSHING_TABLE.getDefaultState(), null, helper.getLevel())
            .stack().is(ModBlocks.STAMPING_PLATFORM.asItem()), "加工台必须沿用已移植的基础材料映射");
        helper.succeed();
    }

    private static void filters(GameTestHelper helper) {
        var state = ModBlocks.ITEM_COLLECTOR.getDefaultState();
        var collector = new ItemCollectorBlockEntity(ModBlockEntities.ITEM_COLLECTOR.get(), BlockPos.ZERO, state);
        var inventory = collector.getFilteredItemStackHandler();
        inventory.set(0, ItemResource.of(Items.DIAMOND), 3);
        inventory.setFilterEnabled(true);
        inventory.setFilter(2, new ItemStack(Items.EMERALD));
        inventory.setSlotLimit(2, 16);
        inventory.setSlotDisabled(1, true);
        var material = BuildingBlockMaterial.extract(state, collector.saveWithFullMetadata(helper.getLevel().registryAccess()),
            helper.getLevel());
        helper.assertTrue(material.contents().size() == 1 && material.contents().getFirst().stack().getCount() == 3,
            "过滤样本不应变成建造材料中的真实库存");
        var restored = new ItemCollectorBlockEntity(ModBlockEntities.ITEM_COLLECTOR.get(), BlockPos.ZERO, state);
        restored.loadWithComponents(TagValueInput.create(
            ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), material.config()));
        BlockEntityContentAdapter.insert(restored, material.contents(), helper.getLevel().registryAccess());
        var result = restored.getFilteredItemStackHandler();
        helper.assertTrue(result.getAmountAsLong(0) == 3 && result.getFilter(2).is(Items.EMERALD)
            && result.getSlotLimit(2) == 16 && result.isSlotDisabled(1), "材料规划后恢复必须保留过滤、数量和禁用槽");
        helper.succeed();
    }

    private static void fluid(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var state = ModBlocks.FLUID_TANK.getDefaultState();
        var tank = new FluidTankBlockEntity(ModBlockEntities.FLUID_TANK.get(), BlockPos.ZERO, state);
        try (Transaction transaction = Transaction.openRoot()) {
            tank.getFluidHandler().insert(FluidResource.of(Fluids.WATER), 1234, transaction);
            transaction.commit();
        }
        var material = BuildingBlockMaterial.extract(state, tank.saveWithFullMetadata(registries), helper.getLevel());
        helper.assertTrue(material.contents().isEmpty() && !material.config().contains("Tank"),
            "储罐流体不能作为免费恢复配置或物品库存");
        var data = material.stack().get(DataComponents.BLOCK_ENTITY_DATA);
        helper.assertTrue(data != null && data.type() == ModBlockEntities.FLUID_TANK.get(), "储罐材料必须携带原生有类型数据");
        var restored = new FluidTankBlockEntity(ModBlockEntities.FLUID_TANK.get(), BlockPos.ZERO, state);
        restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, data.copyTagWithoutId()));
        helper.assertTrue(restored.getFluidHandler().getAmountAsLong(0) == 1234
            && restored.getFluidHandler().getResource(0).getFluid() == Fluids.WATER, "所需储罐必须保留精确流体内容");
        var supplied = BuildingBlockMaterial.separateContents(material.stack(), helper.getLevel());
        helper.assertTrue(supplied.contents().isEmpty() && ItemStack.matches(supplied.stack(), material.stack()),
            "供料拆库存不能剥离储罐真实携带的流体");
        helper.succeed();
    }
}
