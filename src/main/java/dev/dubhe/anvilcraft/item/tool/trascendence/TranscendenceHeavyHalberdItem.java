package dev.dubhe.anvilcraft.item.tool.trascendence;

import dev.dubhe.anvilcraft.entity.ThrownHeavyHalberdEntity;
import dev.dubhe.anvilcraft.entity.ThrownTranscendenceHeavyHalberdEntity;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.ModTiers;
import dev.dubhe.anvilcraft.item.property.component.Eternal;
import dev.dubhe.anvilcraft.item.property.component.Ferocious;
import dev.dubhe.anvilcraft.item.property.component.MultiphaseRef;
import dev.dubhe.anvilcraft.item.property.component.Providence;
import dev.dubhe.anvilcraft.item.tool.HeavyHalberdItem;
import dev.dubhe.anvilcraft.saved.multiphase.Multiphase;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

public class TranscendenceHeavyHalberdItem extends HeavyHalberdItem {
    public static final Component NAME = Component.translatable("item.anvilcraft.transcendence_heavy_halberd");

    public TranscendenceHeavyHalberdItem(Properties properties) {
        super(
            ModTiers.TRANSCENDIUM,
            properties.fireResistant()
                .attributes(HeavyHalberdItem.createAttributes(ModTiers.TRANSCENDIUM, 17, -2.4F))
                .component(ModComponents.MULTIPHASE, new MultiphaseRef())
                .component(DataComponents.ITEM_NAME, Multiphase.firstPhaseName(NAME))
                .component(ModComponents.ETERNAL, Eternal.DEFAULT)
                .component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
                .component(ModComponents.PROVIDENCE, Unit.INSTANCE)
                .component(ModComponents.FEROCIOUS, Ferocious.DEFAULT)
        );
    }

    @Override
    protected double getBaseAttackDamage() {
        return 17;
    }

    @Override
    public ThrownHeavyHalberdEntity createThrown(Level level, LivingEntity shooter, ItemStack pickupItemStack) {
        return new ThrownTranscendenceHeavyHalberdEntity(level, shooter, pickupItemStack);
    }

    @Override
    public ThrownHeavyHalberdEntity createThrown(Level level, double x, double y, double z, ItemStack pickupItemStack) {
        return new ThrownTranscendenceHeavyHalberdEntity(level, x, y, z, pickupItemStack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        // TODO: 兼容性支持结束后将以下检测代码删除
        if (stack.has(ModComponents.MERCILESS)) {
            stack.set(ModComponents.MERCILESS, null);
        }
        if (stack.has(ModComponents.MERCILESS_ENCHANTMENTS)) {
            ItemEnchantments merciless = stack.get(ModComponents.MERCILESS_ENCHANTMENTS);
            ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(stack.get(DataComponents.ENCHANTMENTS));
            for (Holder<Enchantment> mercilessEnch : merciless.keySet()) {
                int mercilessLevel = merciless.getLevel(mercilessEnch);
                int enchLevel = enchantments.getLevel(mercilessEnch);
                if (enchLevel == mercilessLevel) {
                    enchLevel++;
                } else {
                    enchLevel = Math.max(mercilessLevel, enchLevel);
                }
                enchantments.set(mercilessEnch, enchLevel);
            }
            stack.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
        }
    }
}
