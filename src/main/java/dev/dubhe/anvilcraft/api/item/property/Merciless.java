package dev.dubhe.anvilcraft.api.item.property;

import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.init.ModEnchantmentTags;
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

import java.util.List;
import java.util.Objects;

public record Merciless(boolean enabled) {
    public static final Merciless DEFAULT = new Merciless(true);
    public static final Merciless DISABLED = new Merciless(false);
    public static final ResourceLocation MERCILESS_ID = AnvilCraft.of("merciless");
    public static final Codec<Merciless> CODEC = Codec.BOOL.xmap(Merciless::new, Merciless::enabled);
    public static final StreamCodec<ByteBuf, Merciless> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, Merciless::enabled,
        Merciless::new
    );

    public static void tick(ServerPlayer player) {
        List<ItemStack> mercilessItems = InventoryUtil.getItems(
            player.getInventory(), stack -> stack.has(ModComponents.MERCILESS));

        for (ItemStack stack : mercilessItems) {
            boolean isEnabled = Objects.requireNonNull(
                stack.get(ModComponents.MERCILESS),
                "InventoryUtil.getItems(Inventory, Predicate<ItemStack>) method has some problem. The predicate didn't work."
            ).enabled();
            int levelSum = 0;
            float attackDamage;
            float miningEfficiency = 0;

            ItemEnchantments.Mutable enchantmentsMutable = new ItemEnchantments.Mutable(
                stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
            ItemEnchantments.Mutable storedEnchantmentsMutable = new ItemEnchantments.Mutable(
                stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY));
            if (isEnabled) {
                for (Holder<Enchantment> enchantment : enchantmentsMutable.keySet()) {
                    if (!enchantment.is(ModEnchantmentTags.MERCILESS_PASSED)) {
                        storedEnchantmentsMutable.set(enchantment, enchantmentsMutable.getLevel(enchantment));
                        enchantmentsMutable.removeIf(enchantment1 -> enchantment1.equals(enchantment));
                    }
                }
            }
            for (Holder<Enchantment> enchantment : storedEnchantmentsMutable.keySet()) {
                if (isEnabled) {
                    int level = storedEnchantmentsMutable.getLevel(enchantment);
                    levelSum += level;
                    miningEfficiency += level;
                } else {
                    enchantmentsMutable.set(enchantment, storedEnchantmentsMutable.getLevel(enchantment));
                    storedEnchantmentsMutable.removeIf(enchantment1 -> enchantment1.equals(enchantment));
                }
            }
            attackDamage = Math.round(Math.sqrt(levelSum) * 2 + (double) levelSum / 3);
            stack.set(DataComponents.ENCHANTMENTS, enchantmentsMutable.toImmutable());
            stack.set(DataComponents.STORED_ENCHANTMENTS, storedEnchantmentsMutable.toImmutable());

            //noinspection ConstantValue
            if ((attackDamage != 0 || miningEfficiency != 0) && isEnabled) {
                ItemAttributeModifiers attributeModifiers = stack.getAttributeModifiers()
                    .withModifierAdded(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(MERCILESS_ID, attackDamage, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.HAND
                    )
                    .withModifierAdded(
                        Attributes.MINING_EFFICIENCY,
                        new AttributeModifier(MERCILESS_ID, miningEfficiency, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.HAND
                    );
                stack.set(DataComponents.ATTRIBUTE_MODIFIERS, attributeModifiers);
            } else {
                ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                for (ItemAttributeModifiers.Entry entry : stack.getAttributeModifiers().modifiers()) {
                    if (!entry.matches(Attributes.ATTACK_DAMAGE, MERCILESS_ID)
                        && !entry.matches(Attributes.MINING_EFFICIENCY, MERCILESS_ID)) {
                        builder.add(entry.attribute(), entry.modifier(), entry.slot());
                    }
                }
                stack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
            }
        }
    }
}
