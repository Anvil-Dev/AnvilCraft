package dev.dubhe.anvilcraft.porting;

import com.mojang.serialization.JsonOps;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.amulet.AmuletManager;
import dev.dubhe.anvilcraft.init.item.ModAmulets;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import dev.dubhe.anvilcraft.inventory.PocketSlot;
import dev.dubhe.anvilcraft.item.TerminalItem;
import dev.dubhe.anvilcraft.item.property.component.BoxContents;
import dev.dubhe.anvilcraft.network.SwapPocketPacket;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class PocketTests {
    private static Item ticker;
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_pocket_slots_lock", PocketTests::slots,
        "port_pocket_codec", PocketTests::codec,
        "port_pocket_player_save", PocketTests::save,
        "port_pocket_swap_guards", PocketTests::swap,
        "port_pocket_forced_replacement", PocketTests::replacement,
        "port_pocket_tick_clone_drop", PocketTests::lifecycle,
        "port_pocket_creative_packet", PocketTests::creative,
        "port_pocket_terminal_amulet", PocketTests::carried,
        "port_pocket_box_totem", PocketTests::totem,
        "port_pocket_box_totem_canceled_hand", helper -> totem(helper, true)
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
        event.register(Registries.ITEM, registry -> {
            var key = ResourceKey.create(Registries.ITEM, net.minecraft.resources.Identifier.parse("anvilcraft_porting:tick"));
            ticker = new Item(new Item.Properties().setId(key).durability(100)) {
                @Override
                public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
                    if (slot != null) throw new IllegalStateException("口袋物品不能被误判为装备或选中物品");
                    stack.setDamageValue(stack.getDamageValue() + 1);
                }
            };
            registry.register(key.identifier(), ticker);
        });
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_pockets"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static void slots(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            var menu = player.inventoryMenu;
            helper.assertTrue(menu.slots.size() == 58 && menu.getSlot(46) instanceof PocketSlot && !menu.getSlot(46).isActive(),
                "口袋槽位必须固定追加在原版 46 槽之后，未穿护腿时禁用");
            player.setItemSlot(EquipmentSlot.LEGS, ModItems.POCKETS_LEGGINGS.asStack());
            helper.assertTrue(PocketInventory.capacity(player) == 6 && menu.getSlot(51).isActive() && !menu.getSlot(52).isActive(),
                "普通护腿仅启用六个口袋");
            menu.getSlot(46).set(new ItemStack(Items.DIAMOND, 3));
            helper.assertTrue(!menu.getSlot(7).mayPickup(player) && !menu.getSlot(7).mayPlace(new ItemStack(Items.IRON_LEGGINGS)),
                "非空口袋必须锁定护腿放入和拿取");
            menu.clicked(7, 0, ContainerInput.PICKUP, player);
            helper.assertTrue(menu.getCarried().isEmpty() && menu.quickMoveStack(player, 7).isEmpty(),
                "普通点击和快捷移动均不能绕过锁定");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_LEGGINGS));
            var equippable = player.getMainHandItem().get(DataComponents.EQUIPPABLE);
            helper.assertTrue(equippable.swapWithEquipmentSlot(player.getMainHandItem(), player) == InteractionResult.FAIL,
                "手持其它护腿右键也必须拒绝替换");
            menu.getSlot(46).set(ItemStack.EMPTY);
            helper.assertTrue(menu.getSlot(7).mayPickup(player), "清空口袋后必须解锁");
            player.setItemSlot(EquipmentSlot.LEGS, ModItems.WEATHERPROOF_SPACESUIT_LEGGINGS.asStack());
            helper.assertTrue(PocketInventory.capacity(player) == 12 && menu.getSlot(57).isActive()
                && menu.getSlot(46).isActive() && menu.getSlot(46).x == -41 && menu.getSlot(49).isActive() && menu.getSlot(49).x == -23,
                "高级护腿应启用十二槽并更新为双列位置");
        }
        helper.succeed();
    }

    private static void codec(GameTestHelper helper) {
        var pockets = new PocketInventory();
        var stack = new ItemStack(Items.DIAMOND, 7);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("pocket-codec"));
        pockets.setItem(11, stack);
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var restored = PocketInventory.CODEC.parse(ops, PocketInventory.CODEC.encodeStart(ops, pockets).getOrThrow()).getOrThrow();
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            PocketInventory.STREAM_CODEC.encode(buffer, restored);
            var synced = PocketInventory.STREAM_CODEC.decode(buffer);
            helper.assertTrue(synced.getContainerSize() == 12
                && ItemStack.matches(stack, synced.getItem(11)) && synced.getItem(0).isEmpty(),
                "持久化和网络编解码必须保留空槽位置、数量和组件");
            var copy = synced.snapshot();
            copy.get(11).shrink(2);
            helper.assertTrue(synced.getItem(11).getCount() == 7, "同步快照不能与活跃库存共享可变物品");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    private static void save(GameTestHelper helper) {
        try (var first = new StorageFluidRpcTests.Fixture(helper, true); var second = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = first.player();
            player.setItemSlot(EquipmentSlot.LEGS, ModItems.WEATHERPROOF_SPACESUIT_LEGGINGS.asStack());
            player.inventoryMenu.getSlot(57).set(new ItemStack(Items.DIAMOND, 7));
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
            player.saveWithoutId(output);
            second.player().load(TagValueInput.create(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
            PocketInventory.get(second.player()).tick(second.player());
            helper.assertTrue(PocketInventory.capacity(second.player()) == 12
                && PocketInventory.get(second.player()).getItem(11).getCount() == 7,
                "完整玩家存档读写并执行首个 tick 后，护腿和十二号口袋必须仍然存在");
        }
        helper.succeed();
    }

    private static void swap(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            player.setItemSlot(EquipmentSlot.LEGS, ModItems.POCKETS_LEGGINGS.asStack());
            var pockets = PocketInventory.get(player);
            pockets.setItem(0, new ItemStack(Items.DIAMOND, 3));
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.EMERALD, 5));
            new SwapPocketPacket(0).handleOnServer(player);
            helper.assertTrue(player.getOffhandItem().is(Items.DIAMOND) && player.getOffhandItem().getCount() == 3
                && pockets.getItem(0).is(Items.EMERALD) && pockets.getItem(0).getCount() == 5, "口袋与副手必须完整交换");
            player.inventoryMenu.setCarried(new ItemStack(Items.STONE));
            new SwapPocketPacket(0).handleOnServer(player);
            player.inventoryMenu.setCarried(ItemStack.EMPTY);
            for (int index : new int[]{-1, 6, 12, Integer.MAX_VALUE}) new SwapPocketPacket(index).handleOnServer(player);
            player.setGameMode(GameType.SPECTATOR);
            new SwapPocketPacket(0).handleOnServer(player);
            helper.assertTrue(player.getOffhandItem().is(Items.DIAMOND) && pockets.getItem(0).is(Items.EMERALD),
                "鼠标持物、非法索引及旁观状态不能交换或复制物品");
        }
        helper.succeed();
    }

    private static void replacement(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            player.setItemSlot(EquipmentSlot.LEGS, ModItems.WEATHERPROOF_SPACESUIT_LEGGINGS.asStack());
            var pockets = PocketInventory.get(player);
            player.inventoryMenu.getSlot(57).set(new ItemStack(Items.DIAMOND, 3));
            pockets.tick(player);
            player.setItemSlot(EquipmentSlot.LEGS, player.getItemBySlot(EquipmentSlot.LEGS).copy());
            pockets.tick(player);
            int diamonds = player.getInventory().countItem(Items.DIAMOND);
            helper.assertTrue(pockets.isEmpty() && diamonds == 3, "同类型护腿被强制替换时也要完整退回旧口袋物品");
            for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
            player.inventoryMenu.getSlot(57).set(new ItemStack(Items.DIAMOND, 4));
            player.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
            pockets.tick(player);
            int dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(4)).stream()
                .filter(entity -> entity.getItem().is(Items.DIAMOND)).mapToInt(entity -> entity.getItem().getCount()).sum();
            helper.assertTrue(pockets.isEmpty() && dropped == 4, "背包满时应掉落退回物品，不能丢失或留在不可见槽位");
        }
        helper.succeed();
    }

    private static void lifecycle(GameTestHelper helper) {
        try (var first = new StorageFluidRpcTests.Fixture(helper, true); var second = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = first.player();
            player.setItemSlot(EquipmentSlot.LEGS, ModItems.POCKETS_LEGGINGS.asStack());
            player.inventoryMenu.getSlot(46).set(new ItemStack(ticker));
            PocketInventory.get(player).tick(player);
            helper.assertTrue(PocketInventory.get(player).getItem(0).getDamageValue() == 1, "口袋物品必须执行原版服务器物品 tick");
            second.player().restoreFrom(player, true);
            var copied = PocketInventory.get(second.player());
            helper.assertTrue(copied.getItem(0).getDamageValue() == 1 && copied != PocketInventory.get(player),
                "重生/克隆应复制附件内容而不共享库存对象");
            player.getInventory().dropAll();
            helper.assertTrue(PocketInventory.get(player).isEmpty() && !copied.isEmpty(), "原版背包掉落应清空口袋，且不影响保留的副本");
        }
        helper.succeed();
    }

    private static void creative(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            player.setGameMode(GameType.CREATIVE);
            player.setItemSlot(EquipmentSlot.LEGS, ModItems.POCKETS_LEGGINGS.asStack());
            var oldConnection = player.connection;
            var connection = new Connection(PacketFlow.SERVERBOUND);
            var channel = new EmbeddedChannel(connection);
            try {
                var listener = new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false));
                listener.handleSetCreativeModeSlot(new ServerboundSetCreativeModeSlotPacket(46, new ItemStack(Items.DIAMOND, 3)));
                helper.assertTrue(PocketInventory.get(player).getItem(0).getCount() == 3, "真实创造模式包处理器必须接纳已启用的扩展槽");
                listener.handleSetCreativeModeSlot(new ServerboundSetCreativeModeSlotPacket(52, new ItemStack(Items.EMERALD)));
                listener.handleSetCreativeModeSlot(new ServerboundSetCreativeModeSlotPacket(7, ItemStack.EMPTY));
                listener.handleSetCreativeModeSlot(new ServerboundSetCreativeModeSlotPacket(500, new ItemStack(Items.EMERALD)));
                helper.assertTrue(PocketInventory.get(player).getItem(6).isEmpty()
                    && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.POCKETS_LEGGINGS), "创造包不能写禁用槽、越界槽或卸下锁定护腿");
            } finally {
                player.connection = oldConnection;
                channel.finishAndReleaseAll();
            }
        }
        helper.succeed();
    }

    private static void carried(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            player.setItemSlot(EquipmentSlot.LEGS, ModItems.POCKETS_LEGGINGS.asStack());
            var pockets = PocketInventory.get(player);
            pockets.setItem(0, ModItems.HYPERDIMENSION_TERMINAL.asStack());
            pockets.setItem(1, ModItems.EMERALD_AMULET.asStack());
            helper.assertTrue(TerminalItem.getAll(player).size() == 1
                && AmuletManager.get(player.registryAccess()).hasAmuletInInventory(player, ModAmulets.EMERALD.getKey()),
                "口袋终端和护符必须进入各自现有功能查找入口");
        }
        helper.succeed();
    }

    private static void totem(GameTestHelper helper) {
        totem(helper, false);
    }

    private static void totem(GameTestHelper helper, boolean emptyOffhandBox) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            player.setGameMode(GameType.SURVIVAL);
            player.connection.markClientLoaded();
            player.setInvulnerable(false);
            player.getAbilities().invulnerable = false;
            player.setItemSlot(EquipmentSlot.LEGS, ModItems.POCKETS_LEGGINGS.asStack());
            var contents = BoxContents.EMPTY.mutable();
            contents.tryInsert(new ItemStack(Items.TOTEM_OF_UNDYING));
            var box = ModItems.AMULET_BOX.asStack();
            box.set(ModComponents.BOX_CONTENTS, contents.immutable());
            player.inventoryMenu.getSlot(46).set(box);
            ItemStack offhand = emptyOffhandBox ? ModItems.AMULET_BOX.asStack() : new ItemStack(Items.STONE, 3);
            player.setItemInHand(InteractionHand.OFF_HAND, offhand);
            player.hurtServer(helper.getLevel(), player.damageSources().generic(), 1000);
            helper.assertTrue(player.isAlive() && player.getHealth() > 0
                && ItemStack.matches(player.getOffhandItem(), offhand), "致命伤害应消耗口袋盒内图腾，不能替换或消耗副手");
            var remaining = PocketInventory.get(player).getItem(0);
            helper.assertTrue(remaining.is(ModItems.AMULET_BOX) && remaining.getCount() == 1
                && remaining.get(ModComponents.BOX_CONTENTS).totems().isEmpty(), "必须保留护符盒并只消耗一枚图腾");
        }
        helper.succeed();
    }
}
