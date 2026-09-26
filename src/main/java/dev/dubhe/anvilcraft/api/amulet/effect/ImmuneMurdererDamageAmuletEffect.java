package dev.dubhe.anvilcraft.api.amulet.effect;

import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import net.minecraft.advancements.critereon.TagPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/// 免疫指定实体造成的伤害的护符效果
public record ImmuneMurdererDamageAmuletEffect(
    List<TagPredicate<EntityType<?>>> source,
    List<TagPredicate<EntityType<?>>> direct
) implements IImmuneDamageAmuletEffect {
    @Override
    public boolean shouldImmune(LivingEntity entity, ItemStack amulet, DamageSource source, AmuletEffectContext ctx) {
        Entity indirect = source.getEntity();
        if (indirect != null) {
            boolean passed = this.source.isEmpty();
            for (TagPredicate<EntityType<?>> immune : this.source) {
                if (immune.matches(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(indirect.getType()))) {
                    passed = true;
                }
            }
            if (!passed) {
                return false;
            }
        }
        Entity direct = source.getDirectEntity();
        if (direct != null) {
            boolean passed = this.direct.isEmpty();
            for (TagPredicate<EntityType<?>> immune : this.direct) {
                if (immune.matches(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(direct.getType()))) {
                    passed = true;
                }
            }
            return passed;
        }
        return true;
    }
}
