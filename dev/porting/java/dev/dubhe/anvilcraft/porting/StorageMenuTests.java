package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.StorageMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StorageMenuTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_storage_menu_mapping", StorageMenuTests::mapping,
        "port_storage_menu_cursor", StorageMenuTests::cursor,
        "port_storage_menu_inert", StorageMenuTests::inert
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_storage_menu"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static void mapping(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var menu = new StorageMenu(fixture.player(), fixture.core());
            helper.assertTrue(menu.slots.size() == fixture.player().inventoryMenu.slots.size(), "视图必须完整对应原版菜单索引");
            for (boolean flipped : new boolean[]{false, true, false}) {
                menu.setFlipped(flipped);
                for (int i = 0; i < menu.slots.size(); i++) {
                    var slot = menu.getSlot(i);
                    var original = fixture.player().inventoryMenu.getSlot(i);
                    helper.assertTrue(slot.index == i && slot.container == original.container
                        && slot.getContainerSlot() == original.getContainerSlot(), "包括装备、副手及合成格在内的槽不能错位");
                    boolean visible = i >= 9 && i < 45;
                    helper.assertTrue(slot.isActive() == visible && menu.canDragTo(slot) == visible
                        && menu.canTakeItemForPickAll(ItemStack.EMPTY, slot) == visible, "隐藏槽不可被扩展当作可操作输入");
                    if (visible) {
                        int inventorySlot = i >= 36 ? i - 36 : i;
                        helper.assertTrue(slot.x == (flipped ? 8 : 114) + inventorySlot % 9 * 18
                            && slot.y == (inventorySlot < 9 ? 198 : 140 + (inventorySlot - 9) / 9 * 18), "翻转后的菜单槽坐标必须与画面一致");
                    }
                }
            }
        }
        helper.succeed();
    }

    private static void cursor(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var menu = new StorageMenu(fixture.player(), fixture.core());
            fixture.player().inventoryMenu.setCarried(new ItemStack(Items.DIAMOND, 7));
            helper.assertTrue(menu.getCarried().getCount() == 7, "菜单指针必须实时委托原版背包");
            menu.setCarried(new ItemStack(Items.STICK, 5));
            helper.assertTrue(fixture.player().inventoryMenu.getCarried().is(Items.STICK)
                && fixture.player().inventoryMenu.getCarried().getCount() == 5, "写入菜单指针必须更新同一份背包状态");
            fixture.player().getInventory().setItem(9, new ItemStack(Items.STONE, 3));
            menu.setFlipped(true);
            helper.assertTrue(menu.getSlot(9).getItem().getCount() == 3, "翻转不能复制槽位快照");
            fixture.player().getInventory().setItem(9, new ItemStack(Items.STONE, 9));
            helper.assertTrue(menu.getSlot(9).getItem().getCount() == 9, "服务端背包更新后菜单视图必须立即可见");
        }
        helper.succeed();
    }

    private static void inert(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var menu = new StorageMenu(fixture.player(), fixture.core());
            fixture.player().getInventory().setItem(9, new ItemStack(Items.STONE, 3));
            fixture.player().inventoryMenu.setCarried(new ItemStack(Items.DIAMOND, 7));
            menu.clicked(9, 0, ContainerInput.PICKUP, fixture.player());
            menu.quickMoveStack(fixture.player(), 9);
            menu.removed(fixture.player());
            helper.assertTrue(fixture.player().getInventory().getItem(9).getCount() == 3
                && menu.getCarried().is(Items.DIAMOND) && menu.getCarried().getCount() == 7, "扩展菜单不能触发第二套物品交互或关闭归还");
            helper.assertTrue(fixture.player().containerMenu == fixture.player().inventoryMenu, "客户端视图不能替代服务器活动容器");
        }
        helper.succeed();
    }
}
