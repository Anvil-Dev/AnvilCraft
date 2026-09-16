package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.tooltip.StoragePortItemTooltip;
import dev.dubhe.anvilcraft.block.entity.StoragePortBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StoragePortItemTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_storage_item_native", StoragePortItemTests::nativeData,
        "port_storage_item_legacy", StoragePortItemTests::legacy,
        "port_storage_item_empty", StoragePortItemTests::empty
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_storage_item"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_empty"), 100, 0, true)
        )));
    }

    private static void nativeData(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ModBlocks.STORAGE_PORT.get());
        var port = helper.getBlockEntity(pos, StoragePortBlockEntity.class);
        port.setMarkedItem(new ItemStack(Items.IRON_INGOT));
        port.getBuffer().set(0, ItemResource.of(Items.IRON_INGOT), 64);
        port.getBuffer().set(1, ItemResource.of(Items.IRON_INGOT), 12);
        port.getBuffer().set(2, ItemResource.of(Items.GOLD_INGOT), 3);
        ItemStack packed = new ItemStack(ModBlocks.STORAGE_PORT.asItem());
        port.saveToDrop(packed, helper.getLevel().registryAccess());
        var data = StoragePortItemTooltip.contents(StoragePortItemTooltip.blockEntityTag(packed), helper.getLevel().registryAccess());
        helper.assertTrue(data.item().is(Items.IRON_INGOT) && data.count() == 76 && packed.getTooltipImage().isPresent(),
            "原生缓存应跨槽计数且只统计标记物品");
        port.setMarkedItem(ItemStack.EMPTY);
        packed = new ItemStack(ModBlocks.STORAGE_PORT.asItem());
        port.saveToDrop(packed, helper.getLevel().registryAccess());
        data = StoragePortItemTooltip.contents(StoragePortItemTooltip.blockEntityTag(packed), helper.getLevel().registryAccess());
        helper.assertTrue(data.item().is(Items.IRON_INGOT) && data.count() == 76 && packed.getTooltipImage().isPresent(),
            "未标记时必须回退到缓存第一个物品");
        helper.succeed();
    }

    private static CompoundTag itemTag(GameTestHelper helper, ItemStack stack) {
        return (CompoundTag) ItemStack.CODEC.encodeStart(helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE),
            stack).getOrThrow();
    }

    private static void legacy(GameTestHelper helper) {
        CompoundTag tag = new CompoundTag();
        tag.put("marked_item", itemTag(helper, new ItemStack(Items.DIAMOND)));
        final CompoundTag buffer = new CompoundTag();
        ListTag items = new ListTag();
        items.add(itemTag(helper, new ItemStack(Items.GOLD_INGOT, 7)));
        items.add(itemTag(helper, new ItemStack(Items.DIAMOND, 10)));
        items.add(itemTag(helper, new ItemStack(Items.DIAMOND, 2)));
        buffer.put("Items", items);
        tag.put("buffer", buffer);
        var data = StoragePortItemTooltip.contents(tag, helper.getLevel().registryAccess());
        helper.assertTrue(data.item().is(Items.DIAMOND) && data.count() == 12, "旧版缓存必须按标记匹配数量");
        tag.remove("marked_item");
        data = StoragePortItemTooltip.contents(tag, helper.getLevel().registryAccess());
        helper.assertTrue(data.item().is(Items.GOLD_INGOT) && data.count() == 7, "旧版无标记缓存必须保留首物品语义");
        helper.succeed();
    }

    private static void empty(GameTestHelper helper) {
        ItemStack item = new ItemStack(ModBlocks.STORAGE_PORT.asItem());
        helper.assertTrue(item.getTooltipImage().isEmpty(), "普通空端口不能出现空提示框");
        CompoundTag tag = new CompoundTag();
        tag.put("marked_item", itemTag(helper, new ItemStack(Items.DIAMOND)));
        var data = StoragePortItemTooltip.contents(tag, helper.getLevel().registryAccess());
        helper.assertTrue(data.item().is(Items.DIAMOND) && data.count() == 0, "空的已标记端口应显示零数量");
        helper.assertTrue(StoragePortItemTooltip.contents(tag, null).item().isEmpty(), "无注册表时不能错误解析物品");
        helper.succeed();
    }
}
