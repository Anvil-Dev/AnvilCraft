package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.BoxContents;
import dev.dubhe.anvilcraft.item.property.component.PillBoxContents;
import dev.dubhe.anvilcraft.network.BoxSelectionSyncPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class BoxSelectionTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_box_pill_selection", BoxSelectionTests::pill,
        "port_box_amulet_selection", BoxSelectionTests::amulet,
        "port_box_stale_menu", BoxSelectionTests::stale,
        "port_box_selection_bounds", BoxSelectionTests::bounds,
        "port_box_selection_codec", BoxSelectionTests::codec
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_box_selection"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    static ItemStack named(ItemStack stack, String name) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    static ItemStack pillBox() {
        var box = new ItemStack(ModItems.PILL_BOX.get());
        box.set(ModComponents.PILL_BOX_CONTENTS, new PillBoxContents(List.of(
            named(new ItemStack(ModFoodItems.PILL.get(), 3), "First pill"),
            named(new ItemStack(ModFoodItems.PILL.get(), 7), "Second pill"))));
        return box;
    }

    static ItemStack amuletBox() {
        var box = new ItemStack(ModItems.AMULET_BOX.get());
        box.set(ModComponents.BOX_CONTENTS, new BoxContents(List.of(), List.of(
            named(new ItemStack(Items.TOTEM_OF_UNDYING), "First totem"),
            named(new ItemStack(Items.TOTEM_OF_UNDYING), "Second totem")), 0, 2));
        return box;
    }

    private static void pill(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var player = fixture.player();
            var box = pillBox();
            player.getInventory().setItem(9, box);
            new BoxSelectionSyncPacket(0, 9, 1).handleOnServer(player);
            helper.assertTrue(box.get(ModComponents.PILL_BOX_CONTENTS).index() == 1, "必须同步第二堆药片索引");
            player.inventoryMenu.clicked(9, 1, ContainerInput.PICKUP, player);
            helper.assertTrue(player.inventoryMenu.getCarried().getHoverName().getString().equals("Second pill")
                && player.inventoryMenu.getCarried().getCount() == 7, "右键取出必须匹配客户端高亮的第二堆");
            helper.assertTrue(box.get(ModComponents.PILL_BOX_CONTENTS).pills().getFirst().getCount() == 3, "第一堆药片必须保持完整");
            player.inventoryMenu.setCarried(ItemStack.EMPTY);
            new BoxSelectionSyncPacket(0, 9, -1).handleOnServer(player);
            player.inventoryMenu.clicked(9, 1, ContainerInput.PICKUP, player);
            helper.assertTrue(player.inventoryMenu.getCarried().getHoverName().getString().equals("First pill"), "移开重置后应取第一堆");
        }
        helper.succeed();
    }

    private static void amulet(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var player = fixture.player();
            var box = amuletBox();
            player.getInventory().setItem(9, box);
            new BoxSelectionSyncPacket(0, 9, 1).handleOnServer(player);
            player.inventoryMenu.clicked(9, 1, ContainerInput.PICKUP, player);
            helper.assertTrue(player.inventoryMenu.getCarried().getHoverName().getString().equals("Second totem")
                && box.get(ModComponents.BOX_CONTENTS).usage() == 1, "护符盒必须按已同步索引取出并正确扣减容量");
        }
        helper.succeed();
    }

    private static void stale(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var player = fixture.player();
            var menu = ChestMenu.threeRows(17, player.getInventory());
            player.containerMenu = menu;
            var box = pillBox();
            menu.getSlot(0).set(box);
            new BoxSelectionSyncPacket(0, 0, 1).handleOnServer(player);
            helper.assertTrue(box.get(ModComponents.PILL_BOX_CONTENTS).index() == -1, "旧菜单的同号槽包必须拒绝");
            new BoxSelectionSyncPacket(17, 0, 1).handleOnServer(player);
            helper.assertTrue(box.get(ModComponents.PILL_BOX_CONTENTS).index() == 1, "当前菜单内的收纳盒可以选择");
        }
        helper.succeed();
    }

    private static void bounds(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var player = fixture.player();
            var box = amuletBox();
            player.getInventory().setItem(9, box);
            for (int slot : new int[]{-999, -1, 999}) new BoxSelectionSyncPacket(0, slot, 1).handleOnServer(player);
            helper.assertTrue(box.get(ModComponents.BOX_CONTENTS).selection() == 0, "非法槽位不能改动选择");
            new BoxSelectionSyncPacket(0, 9, Integer.MAX_VALUE).handleOnServer(player);
            helper.assertTrue(box.get(ModComponents.BOX_CONTENTS).selection() == 1, "护符索引上界必须夹取");
            new BoxSelectionSyncPacket(0, 9, Integer.MIN_VALUE).handleOnServer(player);
            helper.assertTrue(box.get(ModComponents.BOX_CONTENTS).selection() == 0, "护符负索引不得进入 pop 的负下标路径");
            player.getInventory().setItem(9, pillBox());
            new BoxSelectionSyncPacket(0, 9, Integer.MAX_VALUE).handleOnServer(player);
            helper.assertTrue(player.getInventory().getItem(9).get(ModComponents.PILL_BOX_CONTENTS).index() == 0,
                "药片越界选择应使用源版环绕规则");
            player.getInventory().setItem(9, new ItemStack(Items.STONE, 5));
            new BoxSelectionSyncPacket(0, 9, 1).handleOnServer(player);
            helper.assertTrue(player.getInventory().getItem(9).getCount() == 5, "普通物品不得接受盒子选择组件");
        }
        helper.succeed();
    }

    private static void codec(GameTestHelper helper) {
        var buffer = Unpooled.buffer();
        try {
            var packet = new BoxSelectionSyncPacket(17, 9, -1);
            BoxSelectionSyncPacket.STREAM_CODEC.encode(buffer, packet);
            helper.assertTrue(packet.equals(BoxSelectionSyncPacket.STREAM_CODEC.decode(buffer)), "菜单、槽位及未选中索引必须完整传输");
            helper.assertTrue(!buffer.isReadable(), "选择包不能附带物品内容");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }
}
