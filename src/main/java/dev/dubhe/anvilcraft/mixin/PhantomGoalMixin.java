package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.api.amulet.AmuletManager;
import dev.dubhe.anvilcraft.init.item.ModAmulets;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(Phantom.PhantomSweepAttackGoal.class)
public abstract class PhantomGoalMixin {

    // CHECKSTYLE:OFF
    @Shadow
    @Final
    Phantom this$0;
    // CHECKSTYLE:ON

    @WrapOperation(
        method = "canContinueToUse",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/entity/monster/Phantom$PhantomSweepAttackGoal;isScaredOfCat:Z",
            opcode = Opcodes.PUTFIELD
        )
    )
    private void addAvoidPlayerGoal(Phantom.PhantomSweepAttackGoal instance, boolean value, Operation<Void> original) {
        List<Player> players = this.this$0.level().getEntitiesOfClass(
            Player.class,
            this.this$0.getBoundingBox().inflate(16.0),
            EntitySelector.NO_SPECTATORS.and(
                entity -> entity instanceof Player player
                          && AmuletManager.get(player.registryAccess()).hasAmuletInInventory(player, ModAmulets.CAT)
            )
        );
        for (Player player : players) {
            player.makeSound(SoundEvents.CAT_HISS);
        }

        original.call(instance, value || !players.isEmpty());
    }
}
