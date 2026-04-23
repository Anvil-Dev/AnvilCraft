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

public record Ferocious() {
    public static final Ferocious DEFAULT = new Ferocious();
    public static final ResourceLocation FEROCIOUS_ID = AnvilCraft.of("ferocious");
    public static final MapCodec<Ferocious> CODEC = Codec.EMPTY.xmap(a -> Ferocious.DEFAULT, a -> Unit.INSTANCE);
    public static final StreamCodec<ByteBuf, Ferocious> STREAM_CODEC = StreamCodec.unit(Ferocious.DEFAULT);

    public static void tick(ServerPlayer player) {
        for (ItemStack stack : InventoryUtil.getItems(player.getInventory(), stack -> stack.has(ModComponents.FEROCIOUS))) {
            Ferocious.tick(stack);
        }
    }

    private static void tick(ItemStack stack) {
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) return;

        int levels = 0;

        // 将魔咒等级添至总等级
        for (Holder<Enchantment> enchantment : enchantments.keySet()) {
            int level = enchantments.getLevel(enchantment);
            levels += level;
        }

        // 初始化
        float attackDamage = Math.round(Math.sqrt(levels) * 2 + (double) levels / 3);
        float miningEfficiency = levels;

        // 修改属性修饰符
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        if (attackDamage != 0) {
            builder.add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(FEROCIOUS_ID, attackDamage, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND
            );
        }
        if (miningEfficiency != 0) {
            builder.add(
                Attributes.MINING_EFFICIENCY,
                new AttributeModifier(FEROCIOUS_ID, miningEfficiency, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND
            );
        }
        for (ItemAttributeModifiers.Entry entry : stack.getAttributeModifiers().modifiers()) {
            if (!entry.modifier().is(FEROCIOUS_ID)) {
                builder.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
    }
}
