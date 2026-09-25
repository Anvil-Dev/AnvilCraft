package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.EmberSmithingMenu;
import dev.dubhe.anvilcraft.inventory.RoyalSmithingMenu;
import dev.dubhe.anvilcraft.inventory.TranscendenceSmithingMenu;
import dev.dubhe.anvilcraft.rpc.SmithingServerStub;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class SmithingTransferTests {
    private static final BlockPos TABLE = new BlockPos(2, 2, 2);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_smithing_select_inventory", SmithingTransferTests::inventory,
        "port_smithing_select_adjacent", SmithingTransferTests::adjacent,
        "port_smithing_select_full", SmithingTransferTests::full,
        "port_smithing_batch", SmithingTransferTests::batch,
        "port_smithing_select_builtin", SmithingTransferTests::builtin,
        "port_smithing_select_authority", SmithingTransferTests::authority
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_smithing_transfer"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static RoyalSmithingMenu royal(GameTestHelper helper, StorageFluidRpcTests.Fixture fixture) {
        helper.getLevel().setBlock(helper.absolutePos(TABLE), ModBlocks.ROYAL_SMITHING_TABLE.getDefaultState(), Block.UPDATE_ALL);
        var menu = new RoyalSmithingMenu(7, fixture.player().getInventory(),
            ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(TABLE)));
        fixture.player().containerMenu = menu;
        return menu;
    }

    private static boolean select(StorageFluidRpcTests.Fixture fixture, ItemStack template) {
        return SmithingServerStub.selectTemplate(fixture.playerId(), fixture.player().containerMenu.containerId, template);
    }

    private static void inventory(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final var menu = royal(helper, fixture);
            fixture.player().getInventory().setItem(9, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 2));
            helper.assertTrue(select(fixture, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE))
                && menu.getSlot(0).getItem().getCount() == 1 && fixture.player().getInventory().getItem(9).getCount() == 1,
                "配方选择应从背包取一个实体模板");
            helper.assertTrue(select(fixture, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE))
                && menu.getSlot(0).hasItem() && fixture.player().getInventory().getItem(9).getCount() == 1,
                "重复选择不能取消模板或再次扣除");
        }
        helper.succeed();
    }

    private static void adjacent(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final var menu = royal(helper, fixture);
            BlockPos chestPos = helper.absolutePos(TABLE.east());
            helper.getLevel().setBlock(chestPos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
            var chest = (ChestBlockEntity) helper.getLevel().getBlockEntity(chestPos);
            chest.setItem(0, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
            fixture.player().getInventory().setItem(9, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
            fixture.player().getInventory().setItem(10, new ItemStack(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE));
            helper.assertTrue(select(fixture, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE))
                && chest.getItem(0).isEmpty() && !fixture.player().getInventory().getItem(9).isEmpty()
                && menu.isBorrowedTemplate(menu.getSlot(0).getItem()), "应优先借用相邻模板并保留背包同类模板");
            helper.assertTrue(select(fixture, new ItemStack(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE))
                && chest.getItem(0).is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)
                && fixture.player().getInventory().getItem(10).isEmpty()
                && menu.getSlot(0).getItem().is(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE), "切换应先归还借用模板再取背包模板");
        }
        helper.succeed();
    }

    private static void full(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final var menu = royal(helper, fixture);
            menu.getSlot(0).set(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
            for (int slot = 0; slot < 36; slot++) fixture.player().getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
            fixture.player().getInventory().setItem(10, new ItemStack(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE));
            helper.assertTrue(!select(fixture, new ItemStack(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE))
                && menu.getSlot(0).getItem().is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)
                && fixture.player().getInventory().getItem(10).getCount() == 1, "无法归还旧实体模板时不得覆盖或吞物");
        }
        helper.succeed();
    }

    private static void batch(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            helper.getLevel().setBlock(helper.absolutePos(TABLE), ModBlocks.EMBER_SMITHING_TABLE.getDefaultState(), Block.UPDATE_ALL);
            var menu = new EmberSmithingMenu(7, fixture.player().getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(TABLE)));
            fixture.player().containerMenu = menu;
            menu.getSlot(0).set(new ItemStack(ModItems.TWO_TO_ONE_SMITHING_TEMPLATE.get()));
            var ember = new ItemStack(ModBlocks.EMBER_GRINDSTONE.asItem());
            var frost = new ItemStack(ModBlocks.FROST_GRINDSTONE.asItem());
            helper.assertTrue(!menu.getSlot(2).mayPlace(ember), "普通交互必须先放中心材料");
            menu.runRecipeTransfer(() -> {
                helper.assertTrue(menu.getSlot(2).mayPlace(ember), "批量转移应允许中心材料尚未写入的过渡状态");
                menu.getSlot(2).set(ember);
                menu.runRecipeTransfer(() -> menu.getSlot(3).set(frost));
                helper.assertTrue(menu.isRecipeTransferInProgress(), "嵌套转移不能提前结束外层批次");
                menu.getSlot(1).set(new ItemStack(ModItems.MULTIPHASE_TRANSCENDIUM.get()));
            });
            helper.assertTrue(!menu.isRecipeTransferInProgress()
                && menu.getSlot(10).getItem().is(ModBlocks.TRANSCENDENCE_GRINDSTONE.asItem()),
                "批量填料不能被槽位联动提前退回，应在完成时形成正确产物");
            menu.clicked(10, 0, ContainerInput.PICKUP, fixture.player());
            helper.assertTrue(menu.getCarried().is(ModBlocks.TRANSCENDENCE_GRINDSTONE.asItem())
                && menu.getSlot(0).hasItem() && !menu.getSlot(1).hasItem() && !menu.getSlot(2).hasItem() && !menu.getSlot(3).hasItem(),
                "实际锻造应只消耗中心和周围材料，保留模板");
            try {
                menu.runRecipeTransfer(() -> {
                    throw new IllegalStateException("test interruption");
                });
            } catch (IllegalStateException expected) {
                helper.assertTrue(!menu.isRecipeTransferInProgress(), "异常退出也必须清理批处理状态");
            }
        }
        helper.succeed();
    }

    private static void builtin(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            helper.getLevel().setBlock(helper.absolutePos(TABLE),
                ModBlocks.TRANSCENDENCE_SMITHING_TABLE.getDefaultState(), Block.UPDATE_ALL);
            var menu = new TranscendenceSmithingMenu(7, fixture.player().getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(TABLE)));
            fixture.player().containerMenu = menu;
            var template = new ItemStack(ModItems.TWO_TO_ONE_SMITHING_TEMPLATE.get());
            helper.assertTrue(select(fixture, template) && select(fixture, template)
                && menu.getSelectedTemplate().is(ModItems.TWO_TO_ONE_SMITHING_TEMPLATE), "内置模板重复选择必须保留当前选项");
        }
        helper.succeed();
    }

    private static void authority(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            royal(helper, fixture);
            try {
                var method = SmithingServerStub.class.getMethod("selectTemplate", UUID.class, int.class, ItemStack.class);
                var validator = new SmithingServerStub.Validator();
                var template = new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
                helper.assertTrue(validator.validate(TerminalAccessTests.context(fixture), method,
                    new Object[]{fixture.playerId(), 7, template}), "当前菜单请求应通过身份校验");
                helper.assertTrue(!validator.validate(TerminalAccessTests.context(fixture), method,
                    new Object[]{UUID.randomUUID(), 7, template}), "不能冒用其他玩家身份");
                helper.assertTrue(!validator.validate(TerminalAccessTests.context(fixture), method,
                    new Object[]{fixture.playerId(), 8, template}), "旧菜单请求应拒绝");
                helper.assertTrue(!select(fixture, template), "没有实际可用模板时不能凭请求生成模板");
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException(error);
            }
        }
        helper.succeed();
    }
}
