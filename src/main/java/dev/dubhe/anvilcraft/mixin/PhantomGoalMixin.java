package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.init.ModDataAttachments;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(Phantom.PhantomSweepAttackGoal.class)
public abstract class PhantomGoalMixin {

    @Shadow
    @Final
    Phantom this$0;

    @Redirect(
        method = "canContinueToUse",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/entity/monster/Phantom$PhantomSweepAttackGoal;isScaredOfCat:Z",
            opcode = Opcodes.PUTFIELD
        )
    )
    private void addAvoidPlayerGoal(Phantom.PhantomSweepAttackGoal instance, boolean value) {
        List<Player> players = this.this$0.level()
            .getEntitiesOfClass(Player.class, this.this$0.getBoundingBox().inflate(16.0), EntitySelector.NO_SPECTATORS.and(
                player -> player.getData(ModDataAttachments.SCARE_ENTITIES).getBoolean("phantoms")
            ));

        instance.isScaredOfCat = value || !players.isEmpty();
    }
}
