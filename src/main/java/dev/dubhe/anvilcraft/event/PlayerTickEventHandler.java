package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.IDynamicPowerComponentHolder;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.init.ModEnchantmentTags;
import dev.dubhe.anvilcraft.item.CrabClawItem;
import dev.dubhe.anvilcraft.item.IonoCraftBackpackItem;
import dev.dubhe.anvilcraft.util.InventoryUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;

@EventBusSubscriber
public class PlayerTickEventHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        CrabClawItem.holdingCrabClawIncreasesRange(event.getEntity());
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            applyPowerGrid(serverPlayer);
            IonoCraftBackpackItem.flightTick(serverPlayer);
            processMerciless(serverPlayer);
        }
    }

    private static void applyPowerGrid(ServerPlayer player) {
        if (player instanceof IDynamicPowerComponentHolder holder) {
            PowerGrid powerGrid = PowerGrid.findPowerGridContains(
                player.level(),
                holder.anvilCraft$getPowerSupplyingBoundingBox()
            ).orElse(null);
            holder.anvilCraft$getPowerComponent().switchTo(powerGrid);
        }
    }

    private static final ResourceLocation MERCILESS_ID = AnvilCraft.of("merciless");

    private static void processMerciless(ServerPlayer player) {
        List<ItemStack> mercilessItems = InventoryUtil.getItems(player.getInventory(), stack -> stack.has(ModComponents.MERCILESS));

        for (ItemStack stack : mercilessItems) {
            float attackDamage = 0;
            int miningEfficiency = 0;

            ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (Holder<Enchantment> enchantment : enchantments.keySet()) {
                if (!enchantment.is(ModEnchantmentTags.MERCILESS_PASSED)) {
                    if (enchantment.is(Tags.Enchantments.WEAPON_DAMAGE_ENHANCEMENTS)
                        || enchantment.is(ModEnchantmentTags.MODIFY_ENTITY_DROPS)
                        || enchantment.is(EnchantmentTags.SMELTS_LOOT)
                    ) {
                        attackDamage += stack.getEnchantmentLevel(enchantment);
                    } else if (enchantment.is(ModEnchantmentTags.MODIFY_BLOCK_DROPS)) {
                        miningEfficiency += stack.getEnchantmentLevel(enchantment);
                    }
                }
            }

            if (attackDamage != 0 || miningEfficiency != 0) {
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
