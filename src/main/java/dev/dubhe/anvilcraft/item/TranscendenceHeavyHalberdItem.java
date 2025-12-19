package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.entity.ThrownHeavyHalberdEntity;
import dev.dubhe.anvilcraft.entity.ThrownTranscendenceHeavyHalberdEntity;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.Eternal;
import dev.dubhe.anvilcraft.item.property.component.Merciless;
import dev.dubhe.anvilcraft.item.property.component.MultiphaseRef;
import dev.dubhe.anvilcraft.item.property.component.Providence;
import dev.dubhe.anvilcraft.saved.multiphase.Multiphase;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.Level;

public class TranscendenceHeavyHalberdItem extends HeavyHalberdItem {
    public static final Component NAME = Component.translatable("item.anvilcraft.transcendence_heavy_halberd");

    public TranscendenceHeavyHalberdItem(Properties properties) {
        super(
            ModTiers.TRANSCENDIUM,
            properties.fireResistant()
                .attributes(HeavyHalberdItem.createAttributes(ModTiers.TRANSCENDIUM, 17, -2.4f))
                .component(ModComponents.MULTIPHASE, new MultiphaseRef())
                .component(DataComponents.ITEM_NAME, Multiphase.firstPhaseName(NAME))
                .component(ModComponents.ETERNAL, Eternal.INSTANCE)
                .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
                .component(ModComponents.PROVIDENCE, Providence.INSTANCE)
                .component(ModComponents.MERCILESS, Merciless.DEFAULT)
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
}
