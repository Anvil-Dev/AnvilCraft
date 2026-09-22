package dev.dubhe.anvilcraft.porting;

import com.mojang.serialization.JsonOps;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.network.multiple.MultiphasePackets;
import io.netty.buffer.Unpooled;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class MultiphaseTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_multiphase_defaults", MultiphaseTests::defaults,
        "port_multiphase_switch", MultiphaseTests::switching,
        "port_multiphase_preview", MultiphaseTests::preview,
        "port_multiphase_serialization", MultiphaseTests::serialization,
        "port_multiphase_packets", MultiphaseTests::packets,
        "port_multiphase_bounds", MultiphaseTests::bounds
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_multiphase"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static void assertName(GameTestHelper helper, ItemStack stack, String suffix) {
        String base = Component.translatable(stack.getItem().getDescriptionId()).getString();
        helper.assertTrue(stack.getItemName().getString().equals(base + "-" + suffix),
            "默认物品名必须只含当前相位后缀：" + stack.getItemName().getString());
    }

    private static void defaults(GameTestHelper helper) {
        for (ItemLike item : List.of(ModItems.TRANSCENDENCE_RESONATOR, ModItems.TRANSCENDENCE_ANVIL_HAMMER,
            ModItems.TRANSCENDENCE_HEAVY_HALBERD, ModItems.TRANSCENDENCE_DRAGON_ROD)) {
            var stack = new ItemStack(item);
            assertName(helper, stack, "α");
            helper.assertTrue(stack.getRarity() == Rarity.EPIC, "所有超限多相工具应使用史诗名称颜色：" + stack + "/" + stack.getRarity());
        }
        for (ItemLike item : List.of(ModItems.FROST_METAL_RESONATOR, ModItems.EMBER_METAL_RESONATOR)) {
            helper.assertTrue(new ItemStack(item).getRarity() == Rarity.EPIC, "普通共振器应按源使用史诗稀有度");
        }
        for (ItemLike item : List.of(ModItems.DRAGON_ROD, ModItems.ROYAL_DRAGON_ROD,
            ModItems.EMBER_DRAGON_ROD, ModItems.FROST_DRAGON_ROD)) {
            helper.assertTrue(new ItemStack(item).getRarity() == Rarity.UNCOMMON, "普通龙杖应按源使用罕见稀有度");
        }
        var ordinary = ModItems.FROST_METAL_HEAVY_HALBERD.asStack();
        helper.assertTrue(ordinary.getItemName().getString()
            .equals(Component.translatable(ordinary.getItem().getDescriptionId()).getString()),
            "非多相物品不能被追加后缀");
        helper.succeed();
    }

    private static void switching(GameTestHelper helper) {
        var stack = ModItems.TRANSCENDENCE_RESONATOR.asStack();
        var unbreaking = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("First phase"));
        stack.set(DataComponents.REPAIR_COST, 7);
        stack.enchant(unbreaking, 3);
        stack.get(ModComponents.MULTIPHASE).cycle(stack);
        assertName(helper, stack, "β");
        helper.assertTrue(!stack.has(DataComponents.CUSTOM_NAME) && stack.getOrDefault(DataComponents.REPAIR_COST, 0) == 0
            && stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty(), "新相位应恢复自身的空属性");
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Second phase"));
        stack.set(DataComponents.REPAIR_COST, 12);
        stack.enchant(unbreaking, 1);
        for (int i = 0; i < 3; i++) {
            stack.get(ModComponents.MULTIPHASE).select(stack, 0);
            assertName(helper, stack, "α");
            helper.assertTrue(stack.getHoverName().getString().equals("First phase")
                && stack.get(DataComponents.REPAIR_COST) == 7
                && stack.get(DataComponents.ENCHANTMENTS).getLevel(unbreaking) == 3, "第一相位改名、修复费用和附魔必须恢复");
            stack.get(ModComponents.MULTIPHASE).select(stack, 1);
            assertName(helper, stack, "β");
            helper.assertTrue(stack.getHoverName().getString().equals("Second phase")
                && stack.get(DataComponents.REPAIR_COST) == 12
                && stack.get(DataComponents.ENCHANTMENTS).getLevel(unbreaking) == 1, "反复切换不能串相位或累积名称后缀");
        }
        helper.succeed();
    }

    private static void preview(GameTestHelper helper) {
        var stack = ModItems.TRANSCENDENCE_ANVIL_HAMMER.asStack();
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Saved alpha"));
        stack.get(ModComponents.MULTIPHASE).cycle(stack);
        var before = stack.copy();
        var preview = stack.copy();
        preview.get(ModComponents.MULTIPHASE).applySelectionPreview(preview, 0);
        assertName(helper, preview, "α");
        helper.assertTrue(preview.getHoverName().getString().equals("Saved alpha") && ItemStack.matches(stack, before),
            "预览应呈现对应相位且不修改持有物品");
        helper.succeed();
    }

    private static void serialization(GameTestHelper helper) {
        var stack = ModItems.TRANSCENDENCE_DRAGON_ROD.asStack();
        stack.get(ModComponents.MULTIPHASE).addPhase(stack);
        stack.get(ModComponents.MULTIPHASE).select(stack, 2);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Stored gamma"));
        stack.get(ModComponents.MULTIPHASE).select(stack, 0);
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var saved = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
        var restored = ItemStack.CODEC.parse(ops, saved).getOrThrow();
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            ItemStack.STREAM_CODEC.encode(buffer, restored);
            var synced = ItemStack.STREAM_CODEC.decode(buffer);
            synced.get(ModComponents.MULTIPHASE).select(synced, 2);
            assertName(helper, synced, "γ");
            helper.assertTrue(synced.getHoverName().getString().equals("Stored gamma"), "存档与网络往返不能丢失未激活相位");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    private static void packets(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            var main = ModItems.TRANSCENDENCE_RESONATOR.asStack();
            var off = ModItems.TRANSCENDENCE_HEAVY_HALBERD.asStack();
            player.setItemInHand(InteractionHand.MAIN_HAND, main);
            player.setItemInHand(InteractionHand.OFF_HAND, off);
            new MultiphasePackets.SwitchPhase().handleOnServer(player);
            assertName(helper, main, "β");
            assertName(helper, off, "α");
            new MultiphasePackets.ChangePhase(InteractionHand.OFF_HAND, 1).handleOnServer(player);
            assertName(helper, off, "β");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            new MultiphasePackets.SwitchPhase().handleOnServer(player);
            assertName(helper, off, "α");
        }
        helper.succeed();
    }

    private static void bounds(GameTestHelper helper) {
        var stack = ModItems.TRANSCENDENCE_HEAVY_HALBERD.asStack();
        helper.assertTrue(stack.get(ModComponents.MULTIPHASE).addPhase(stack), "应允许第三相位");
        helper.assertTrue(stack.get(ModComponents.MULTIPHASE).addPhase(stack), "应允许第四相位");
        helper.assertTrue(!stack.get(ModComponents.MULTIPHASE).addPhase(stack), "不能超过四相位");
        var before = stack.copy();
        stack.get(ModComponents.MULTIPHASE).select(stack, -1);
        stack.get(ModComponents.MULTIPHASE).select(stack, 4);
        helper.assertTrue(ItemStack.matches(stack, before), "非法相位索引不能更改物品");
        stack.get(ModComponents.MULTIPHASE).select(stack, 3);
        assertName(helper, stack, "δ");
        stack.get(ModComponents.MULTIPHASE).cycle(stack);
        assertName(helper, stack, "α");
        helper.succeed();
    }
}
