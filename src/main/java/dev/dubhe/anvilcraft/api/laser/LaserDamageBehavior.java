package dev.dubhe.anvilcraft.api.laser;

import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

public record LaserDamageBehavior() implements ILaserComponent {
    @Override
    public boolean onHitBlock(ILaserComponentOwner owner, Level level, BlockPos blockPos) {
        if (!(level instanceof ServerLevel)) return true;
        int strength = LaserStrengthComponent.getStrength(owner);
        if (LaserTypeComponent.isGamma(owner)) {
            GammaLaserEffects.damageEntities(level, owner.getLaserSourcePos(), blockPos, owner.getLaserDirection(), strength);
            return true;
        }
        int damage = Math.min(16, strength - 4);
        if (damage <= 0) return true;
        Direction direction = owner.getLaserDirection();
        AABB bounds = new AABB(
            owner.getLaserOrigin().relative(direction).getCenter().add(-0.0625, -0.0625, -0.0625),
            blockPos.relative(direction.getOpposite()).getCenter().add(0.0625, 0.0625, 0.0625)
        );
        level.getEntities(EntityTypeTest.forClass(LivingEntity.class), bounds, Entity::isAlive)
            .forEach(entity -> entity.hurt(ModDamageTypes.laser(level), damage));
        return true;
    }
}
