package dev.dubhe.anvilcraft.item.amulet;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SapphireAmuletItem extends AbstractAmuletItem {
    public SapphireAmuletItem(Properties properties) {
        super(properties);
    }

    @Override
    void UpdateAccessory(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (entity instanceof LivingEntity livingEntity) {
            if (!livingEntity.isInWater()) {
                MobEffectInstance effect = livingEntity.getEffect(MobEffects.CONDUIT_POWER);
                if (effect == null) {
                    livingEntity.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 2, 0, false, false));
                } else if (effect.getDuration() < 3600) {
                    livingEntity.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER,
                        effect.getDuration() + 2, effect.getAmplifier(),
                        effect.isAmbient(), effect.isVisible()));
                }
            }
        }
    }
}
