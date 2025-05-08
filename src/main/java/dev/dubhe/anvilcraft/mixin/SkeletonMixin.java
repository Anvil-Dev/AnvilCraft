package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.init.ModDataAttachments;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractSkeleton.class, priority = 995)
public abstract class SkeletonMixin extends Monster {
    protected SkeletonMixin(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(
        method = "registerGoals",
        at = @At("HEAD")
    )
    private void addAvoidPlayerGoal(CallbackInfo ci) {
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(
            this, Player.class, 6.0F, 1.0, 1.2,
            EntitySelector.NO_SPECTATORS.and(
                player -> player.getData(ModDataAttachments.SCARE_ENTITIES).getBoolean("skeletons")
            )::test
        ));
    }
}
