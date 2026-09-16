package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.BoxContents;
import dev.dubhe.anvilcraft.item.property.component.PillBoxContents;
import dev.dubhe.anvilcraft.item.utility.PillBoxItem;
import dev.dubhe.anvilcraft.rpc.BundleLikeServerStub;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class BundleActionTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_bundle_held_box", BundleActionTests::held,
        "port_bundle_slot_box", BundleActionTests::slotted,
        "port_bundle_inverted", BundleActionTests::inverted,
        "port_bundle_amulet_capacity", BundleActionTests::capacity,
        "port_bundle_slot_guards", BundleActionTests::guards,
        "port_bundle_snapshot_cooldown", BundleActionTests::snapshot,
        "port_bundle_owner_sync", BundleActionTests::owner
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_bundle_actions"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static int pills(ItemStack box) {
        return box.getOrDefault(ModComponents.PILL_BOX_CONTENTS, PillBoxContents.EMPTY).pills().stream()
            .mapToInt(ItemStack::getCount).sum();
    }

    private static void held(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var player = fixture.player();
            var menu = player.inventoryMenu;
            player.getInventory().setItem(9, new ItemStack(ModFoodItems.PILL.get(), 5));
            menu.setCarried(new ItemStack(ModItems.PILL_BOX.get()));
            menu.clicked(9, 1, ContainerInput.PICKUP, player);
            helper.assertTrue(player.getInventory().getItem(9).isEmpty() && pills(menu.getCarried()) == 5,
                "手持药盒右键应收纳全部药片");
            menu.clicked(10, 1, ContainerInput.PICKUP, player);
            helper.assertTrue(player.getInventory().getItem(10).is(ModFoodItems.PILL) && player.getInventory().getItem(10).getCount() == 5
                && pills(menu.getCarried()) == 0, "手持盒右键空槽应取出药片，保留空盒");
        }
        helper.succeed();
    }

    private static void slotted(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var player = fixture.player();
            var menu = player.inventoryMenu;
            player.getInventory().setItem(9, new ItemStack(ModItems.PILL_BOX.get()));
            menu.setCarried(new ItemStack(ModFoodItems.PILL.get(), 4));
            menu.clicked(9, 1, ContainerInput.PICKUP, player);
            helper.assertTrue(menu.getCarried().isEmpty() && pills(player.getInventory().getItem(9)) == 4,
                "药盒放在槽内时右键收纳也必须生效");
            menu.clicked(9, 1, ContainerInput.PICKUP, player);
            helper.assertTrue(menu.getCarried().is(ModFoodItems.PILL) && menu.getCarried().getCount() == 4
                && pills(player.getInventory().getItem(9)) == 0, "空指针右键应取出盒内物品");
            menu.clicked(9, 0, ContainerInput.PICKUP, player);
            helper.assertTrue(menu.getCarried().is(ModItems.PILL_BOX) && player.getInventory().getItem(9).is(ModFoodItems.PILL),
                "默认左键应保留原版交换语义");
        }
        helper.succeed();
    }

    private static void inverted(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var player = fixture.player();
            BundleLikeServerStub.updateInverted(player.getUUID(), true);
            try {
                var box = new ItemStack(ModItems.PILL_BOX.get());
                player.getInventory().setItem(9, new ItemStack(ModFoodItems.PILL.get(), 7));
                var slot = player.inventoryMenu.getSlot(9);
                helper.assertTrue(!box.getItem().overrideStackedOnOther(box, slot, ClickAction.SECONDARY, player)
                    && player.getInventory().getItem(9).getCount() == 7, "反转后右键非空槽不能收纳");
                helper.assertTrue(box.getItem().overrideStackedOnOther(box, slot, ClickAction.PRIMARY, player) && pills(box) == 7,
                    "反转后左键收纳必须生效");
                helper.assertTrue(box.getItem().overrideStackedOnOther(box, slot, ClickAction.SECONDARY, player)
                    && slot.getItem().getCount() == 7, "反转后取出仍应使用右键");
            } finally {
                BundleLikeServerStub.clear(player.getUUID());
            }
        }
        helper.succeed();
    }

    private static void capacity(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var player = fixture.player();
            var box = new ItemStack(ModItems.AMULET_BOX.get());
            var slot = player.inventoryMenu.getSlot(9);
            for (int i = 0; i < BoxContents.CAPACITY; i++) {
                slot.set(new ItemStack(Items.TOTEM_OF_UNDYING));
                helper.assertTrue(box.getItem().overrideStackedOnOther(box, slot, ClickAction.SECONDARY, player)
                    && slot.getItem().isEmpty(), "护符盒必须收纳一个图腾");
            }
            slot.set(new ItemStack(Items.TOTEM_OF_UNDYING));
            helper.assertTrue(!box.getItem().overrideStackedOnOther(box, slot, ClickAction.SECONDARY, player)
                && slot.getItem().is(Items.TOTEM_OF_UNDYING), "满盒不能吞掉剩余图腾");
            helper.assertTrue(box.get(ModComponents.BOX_CONTENTS).usage() == 16, "护符盒容量必须保持 16");
            slot.set(ItemStack.EMPTY);
            box.getItem().overrideStackedOnOther(box, slot, ClickAction.SECONDARY, player);
            helper.assertTrue(slot.getItem().is(Items.TOTEM_OF_UNDYING) && box.get(ModComponents.BOX_CONTENTS).usage() == 15,
                "护符盒取出必须只消耗一个格位");
        }
        helper.succeed();
    }

    private static void guards(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var box = new ItemStack(ModItems.PILL_BOX.get());
            var container = new SimpleContainer(new ItemStack(ModFoodItems.PILL.get(), 3));
            var inactive = new Slot(container, 0, 0, 0) {
                @Override
                public boolean isActive() {
                    return false;
                }
            };
            var locked = new Slot(container, 0, 0, 0) {
                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            };
            helper.assertTrue(!box.getItem().overrideStackedOnOther(box, inactive, ClickAction.SECONDARY, fixture.player())
                && !box.getItem().overrideStackedOnOther(box, locked, ClickAction.SECONDARY, fixture.player()), "隐藏或禁止修改槽不得收纳");
            helper.assertTrue(container.getItem(0).getCount() == 3 && pills(box) == 0, "拒绝交互不能修改任一方物品");
        }
        helper.succeed();
    }

    private static void snapshot(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var pill = new ItemStack(ModFoodItems.PILL.get(), 60);
            pill.set(DataComponents.CUSTOM_NAME, Component.literal("Snapshot"));
            var original = new PillBoxContents(List.of(pill));
            var mutable = original.mutable();
            helper.assertTrue(mutable.insert(pill.copyWithCount(7)), "药片应能分成 64 和 3 的两堆");
            helper.assertTrue(original.pills().getFirst().getCount() == 60 && mutable.immutable().pills().getFirst().getCount() == 64
                && mutable.immutable().pills().getLast().getCount() == 3, "组件编辑不能原地修改旧快照，数量必须守恒");
            var box = new ItemStack(ModItems.PILL_BOX.get());
            box.set(ModComponents.PILL_BOX_CONTENTS, new PillBoxContents(List.of(pill.copyWithCount(2))));
            PillBoxItem.use(box, fixture.player());
            helper.assertTrue(fixture.player().getCooldowns().isOnCooldown(box) && pills(box) == 1, "快捷键直接用药也应施加 40 tick 冷却");
            for (int i = 0; i < 39; i++) fixture.player().getCooldowns().tick();
            helper.assertTrue(fixture.player().getCooldowns().isOnCooldown(box), "第 39 tick 冷却不能提前结束");
            fixture.player().getCooldowns().tick();
            helper.assertTrue(!fixture.player().getCooldowns().isOnCooldown(box), "第 40 tick 冷却必须结束");
        }
        helper.succeed();
    }

    private static void owner(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            IPayloadContext context = (IPayloadContext) Proxy.newProxyInstance(IPayloadContext.class.getClassLoader(),
                new Class<?>[]{IPayloadContext.class}, (proxy, method, args) -> {
                    if (method.getName().equals("player")) return fixture.player();
                    throw new UnsupportedOperationException(method.getName());
                });
            var method = BundleLikeServerStub.class.getMethod("updateInverted", UUID.class, boolean.class);
            var validator = new BundleLikeServerStub.OwnActionValidator();
            helper.assertTrue(!validator.validate(context, method, new Object[]{UUID.randomUUID(), true}), "不能修改其他玩家的点击规则");
            helper.assertTrue(validator.validate(context, method, new Object[]{fixture.playerId(), true}), "本人配置应可同步");
            BundleLikeServerStub.updateInverted(fixture.playerId(), true);
            BundleLikeServerStub.setClientInverted(false);
            helper.assertTrue(BundleLikeServerStub.isInvertedAction(fixture.player()), "客户端预测状态不能覆盖服务端玩家配置");
            BundleLikeServerStub.clear(fixture.playerId());
            helper.assertTrue(!BundleLikeServerStub.isInvertedAction(fixture.player()), "退出清理后应恢复默认规则");
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
        helper.succeed();
    }
}
