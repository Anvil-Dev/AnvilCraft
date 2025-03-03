package dev.dubhe.anvilcraft.item.amulet;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class RubyAmuletItem extends AbstractAmuletItem {
    public RubyAmuletItem(Properties properties) {
        super(properties);
    }

    @Override
    void UpdateAccessory(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (entity instanceof LivingEntity livingEntity) {
            if (!livingEntity.isInLava()) {
                MobEffectInstance effect = livingEntity.getEffect(MobEffects.FIRE_RESISTANCE);
                if (effect == null) {
                    livingEntity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2, 0, false, false));
                } else if (effect.getDuration() < 3600) {
                    livingEntity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,
                        effect.getDuration() + 2, effect.getAmplifier(),
                        effect.isAmbient(), effect.isVisible()));
                }
            }
        }
    }
}
