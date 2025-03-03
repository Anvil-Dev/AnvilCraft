package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RoyalAnvilHammerItem extends AnvilHammerItem implements IInherentEnchantment {
    /**
     * 初始化铁砧锤
     *
     * @param properties 物品属性
     */
    public RoyalAnvilHammerItem(Properties properties) {
        super(properties);
    }

    @Override
    protected float getAttackDamageModifierAmount() {
        return 7;
    }

    @Override
    public Block getAnvil() {
        return ModBlocks.ROYAL_ANVIL.get();
    }

    @Override
    protected float calculateFallDamageBonus(float fallDistance) {
        return Math.min(80, fallDistance * 2);
    }

    @Override
    public void appendHoverText(
        ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
        if (pContext.level() != null) {
            pTooltipComponents.addAll(this.getInherentEnchantmentsTooltip(pContext.level()));
        }
    }

    @Override
    public Map<ResourceKey<Enchantment>, Integer> getInherentEnchantments() {
        return Map.of(Enchantments.UNBREAKING, 3);
    }

    @Override
    public ItemEnchantments getAllEnchantments(ItemStack stack, HolderLookup.RegistryLookup<Enchantment> lookup) {
        ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(super.getAllEnchantments(stack, lookup));

        for (var entry : getInherentEnchantments().entrySet()) {
            Holder.Reference<Enchantment> holder = lookup.getOrThrow(entry.getKey());
            enchantments.set(holder, entry.getValue());
        }

        return enchantments.toImmutable();
    }
}
