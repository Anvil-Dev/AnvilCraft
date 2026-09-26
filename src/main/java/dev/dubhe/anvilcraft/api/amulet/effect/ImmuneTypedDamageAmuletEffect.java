package dev.dubhe.anvilcraft.api.amulet.effect;

import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import net.minecraft.advancements.critereon.TagPredicate;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/// 免疫指定类型伤害的护符效果
public record ImmuneTypedDamageAmuletEffect(List<TagPredicate<DamageType>> immune) implements IImmuneDamageAmuletEffect {
    @Override
    public boolean shouldImmune(LivingEntity entity, ItemStack amulet, DamageSource source, AmuletEffectContext ctx) {
        for (TagPredicate<DamageType> immune : this.immune) {
            if (immune.matches(source.typeHolder())) {
                return true;
            }
        }
        return false;
    }
}
