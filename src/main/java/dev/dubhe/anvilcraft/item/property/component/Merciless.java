package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.enchantment.ModEnchantmentTags;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.util.InventoryUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Iterator;

public record Merciless(boolean enabled) {
    public static final Merciless DEFAULT = new Merciless(true);
    public static final Merciless DISABLED = new Merciless(false);
    public static final ResourceLocation MERCILESS_ID = AnvilCraft.of("merciless");
    public static final Codec<Merciless> CODEC = Codec.BOOL.xmap(Merciless::new, Merciless::enabled);
    public static final StreamCodec<ByteBuf, Merciless> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        Merciless::enabled,
        Merciless::new
    );

    public static void tick(ServerPlayer player) {
        for (ItemStack stack : InventoryUtil.getItems(player.getInventory(), stack -> stack.has(ModComponents.MERCILESS))) {
            if (stack.getOrDefault(ModComponents.MERCILESS, Merciless.DISABLED).enabled()) {
                Merciless.tickEnabled(stack);
            } else {
                Merciless.tickDisabled(stack);
            }
        }
    }

    private static void tickEnabled(ItemStack stack) {
        int levels = 0;

        // 将已无情的魔咒的等级添至总等级
        ItemEnchantments mercilessEnchs = stack.getOrDefault(ModComponents.MERCILESS_ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Holder<Enchantment> enchantment : mercilessEnchs.keySet()) {
            levels += mercilessEnchs.getLevel(enchantment);
        }
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (!enchantments.isEmpty()) {
            ItemEnchantments.Mutable enchsMut = new ItemEnchantments.Mutable(enchantments);
            ItemEnchantments.Mutable mercilessEnchsMut = new ItemEnchantments.Mutable(mercilessEnchs);
            for (Iterator<Holder<Enchantment>> it = enchsMut.keySet().iterator(); it.hasNext(); ) {
                Holder<Enchantment> enchantment = it.next();

                // 若魔咒在无情可忽略的标签中，则跳过后续处理
                if (enchantment.is(ModEnchantmentTags.MERCILESS_PASSED)) continue;

                // 将魔咒的等级添至总等级
                int level = enchsMut.getLevel(enchantment);
                levels += level;

                // 将魔咒添至无情魔咒中并从源魔咒中删除
                mercilessEnchsMut.set(enchantment, level);
                it.remove();
            }
            stack.set(DataComponents.ENCHANTMENTS, enchsMut.toImmutable());
            stack.set(ModComponents.MERCILESS_ENCHANTMENTS, mercilessEnchsMut.toImmutable());
        }

        // 初始化
        float attackDamage = Math.round(Math.sqrt(levels) * 2 + (double) levels / 3);
        float miningEfficiency = levels;

        // 修改属性修饰符
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        if (attackDamage != 0) {
            builder.add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(MERCILESS_ID, attackDamage, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND
            );
        }
        if (miningEfficiency != 0) {
            builder.add(
                Attributes.MINING_EFFICIENCY,
                new AttributeModifier(MERCILESS_ID, miningEfficiency, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND
            );
        }
        for (ItemAttributeModifiers.Entry entry : stack.getAttributeModifiers().modifiers()) {
            if (
                !entry.matches(Attributes.ATTACK_DAMAGE, MERCILESS_ID)
                && !entry.matches(Attributes.MINING_EFFICIENCY, MERCILESS_ID)
            ) {
                builder.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
    }

    private static void tickDisabled(ItemStack stack) {
        ItemEnchantments mercilessEnchs = stack.getOrDefault(ModComponents.MERCILESS_ENCHANTMENTS, ItemEnchantments.EMPTY);
        // 若无情魔咒为空，则说明已进行或无需进行禁用处理
        if (mercilessEnchs.isEmpty()) return;

        // 魔咒处理
        ItemEnchantments.Mutable enchsMut = new ItemEnchantments.Mutable(
            stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
        );
        ItemEnchantments.Mutable mercilessEnchsMut = new ItemEnchantments.Mutable(mercilessEnchs);
        for (Holder<Enchantment> enchantment : mercilessEnchsMut.keySet()) {
            enchsMut.set(enchantment, mercilessEnchsMut.getLevel(enchantment));
        }
        stack.set(DataComponents.ENCHANTMENTS, enchsMut.toImmutable());
        stack.set(ModComponents.MERCILESS_ENCHANTMENTS, ItemEnchantments.EMPTY);

        // 移除属性修饰符
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        for (ItemAttributeModifiers.Entry entry : stack.getAttributeModifiers().modifiers()) {
            if (
                !entry.matches(Attributes.ATTACK_DAMAGE, MERCILESS_ID)
                && !entry.matches(Attributes.MINING_EFFICIENCY, MERCILESS_ID)
            ) {
                builder.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
    }
}
