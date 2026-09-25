package dev.dubhe.anvilcraft.building;

import com.mojang.authlib.GameProfile;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.IntStream;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class BuildingMaterialBookTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_building_book_pages", BuildingMaterialBookTests::pages,
        "port_building_book_noop", BuildingMaterialBookTests::noop
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_building_books"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static void pages(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "PortBookPages"));
        player.setItemSlot(EquipmentSlot.LEGS, ModItems.POCKETS_LEGGINGS.asStack());
        var pockets = PocketInventory.get(player);
        pockets.setItem(0, new ItemStack(Items.BOOK, 2));
        player.getInventory().setItem(10, new ItemStack(Items.BOOK, 3));
        List<Component> missing = IntStream.range(0, 13).mapToObj(index -> (Component) Component.literal("material " + index)).toList();
        BuildingRodMaterialBook.give(player, missing);
        helper.assertTrue(pockets.getItem(0).getCount() == 1 && player.getInventory().countItem(Items.BOOK) == 3,
            "缺料清单应按随身物品顺序实际消耗一本口袋书");
        ItemStack book = ItemStack.EMPTY;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(Items.WRITTEN_BOOK)) book = player.getInventory().getItem(slot);
        }
        helper.assertTrue(!book.isEmpty() && book.getCount() == 1, "应把唯一一本签名清单书放回玩家背包");
        var content = book.get(DataComponents.WRITTEN_BOOK_CONTENT);
        helper.assertTrue(content != null && content.pages().size() == 3
            && content.pages().get(0).raw().getString().equals("material 0\nmaterial 1\nmaterial 2\nmaterial 3\nmaterial 4\nmaterial 5\n")
            && content.pages().get(2).raw().getString().equals("material 12\n"), "清单必须按六行分页且不遗漏最后一页");
        helper.assertTrue(Component.translatable("book.anvilcraft.material_list.title").equals(book.get(DataComponents.ITEM_NAME)),
            "清单显示名应使用源版本地化标题");
        var ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var encoded = ItemStack.CODEC.encodeStart(ops, book).getOrThrow();
        helper.assertTrue(ItemStack.matches(book, ItemStack.CODEC.parse(ops, encoded).getOrThrow()), "清单页与标题必须通过原生存档往返");
        helper.succeed();
    }

    private static void noop(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "PortBookNoop"));
        player.getInventory().setItem(0, new ItemStack(Items.BOOK, 2));
        BuildingRodMaterialBook.give(player, List.of());
        helper.assertTrue(player.getInventory().countItem(Items.BOOK) == 2
            && player.getInventory().countItem(Items.WRITTEN_BOOK) == 0, "没有缺料时不能消耗或生成书");
        player.getInventory().clearContent();
        BuildingRodMaterialBook.give(player, List.of(Component.literal("missing")));
        helper.assertTrue(player.getInventory().countItem(Items.WRITTEN_BOOK) == 0, "没有空白书时不得免费生成缺料清单");
        helper.succeed();
    }
}
