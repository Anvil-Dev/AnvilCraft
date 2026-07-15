package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.injection.entity.IExperienceOrbExtension;
import dev.dubhe.anvilcraft.block.entity.ExpCollectorBlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ExperienceOrb.class)
abstract class ExperienceOrbMixin extends Entity implements IExperienceOrbExtension {
    @Unique
    private boolean anvilcraft$discarded;

    protected ExperienceOrbMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void anvilcraft$poach() {
        if (this.level().isClientSide()) return;
        this.anvilcraft$discarded = ExpCollectorBlockEntity.poachExperienceOrb((ExperienceOrb) (Object) this);
    }

    @Override
    public boolean anvilcraft$getDiscarded() {
        return this.anvilcraft$discarded;
    }
}
