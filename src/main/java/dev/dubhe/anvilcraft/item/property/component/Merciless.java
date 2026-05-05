package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.util.InventoryUtil;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
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

public record Merciless() {
    public static final Merciless DEFAULT = new Merciless();
    public static final ResourceLocation MERCILESS_ID = AnvilCraft.of("merciless");
    public static final MapCodec<Merciless> CODEC = Codec.EMPTY.xmap(a -> Merciless.DEFAULT, a -> Unit.INSTANCE);
    public static final StreamCodec<ByteBuf, Merciless> STREAM_CODEC = StreamCodec.unit(Merciless.DEFAULT);

    public static void tick(ServerPlayer player) {
        for (ItemStack stack : InventoryUtil.getItems(player.getInventory(), stack -> stack.has(ModComponents.MERCILESS))) {
            if (stack.has(ModComponents.MERCILESS)) Merciless.tick(stack);
        }
    }

    private static void tick(ItemStack stack) {
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) return;

        int levels = 0;

        // 将已无情的魔咒的等级添至总等级
        ItemEnchantments mercilessEnchs = stack.getOrDefault(ModComponents.MERCILESS_ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Holder<Enchantment> enchantment : mercilessEnchs.keySet()) {
            levels += mercilessEnchs.getLevel(enchantment);
        }
        // 将未无情的魔咒的等级添至总等级并无效化
        ItemEnchantments.Mutable enchsMut = new ItemEnchantments.Mutable(enchantments);
        ItemEnchantments.Mutable mercilessEnchsMut = new ItemEnchantments.Mutable(mercilessEnchs);
        for (Iterator<Holder<Enchantment>> it = enchsMut.keySet().iterator(); it.hasNext(); ) {
            Holder<Enchantment> enchantment = it.next();

            // 若已无情的魔咒拥有相同的等级，则将等级+1，否则将等级设为两者中高的那个，并修正levels
            int level = enchsMut.getLevel(enchantment);
            int mercilessLevel = mercilessEnchs.getLevel(enchantment);
            if (mercilessLevel == level) {
                level++;
            } else {
                level = Math.max(level, mercilessLevel);
                levels += level - mercilessLevel;
            }

            // 将魔咒添至无情魔咒中并从源魔咒中删除
            mercilessEnchsMut.set(enchantment, level);
            it.remove();
        }
        stack.set(DataComponents.ENCHANTMENTS, enchsMut.toImmutable());
        stack.set(ModComponents.MERCILESS_ENCHANTMENTS, mercilessEnchsMut.toImmutable());

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
            if (!entry.modifier().is(MERCILESS_ID)) {
                builder.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
    }
}
