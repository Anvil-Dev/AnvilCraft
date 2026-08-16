package dev.dubhe.anvilcraft.mixin.piglin;

import dev.dubhe.anvilcraft.item.abnormal.IEnchantedGold;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.PiglinBruteAi;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(PiglinBruteAi.class)
public abstract class PiglinBruteAiMixin {

    @Inject(method = "findNearestValidAttackTarget", at = @At("RETURN"), cancellable = true)
    private static void calmDownForEnchantedGoldHolder(
        AbstractPiglin piglinBrute,
        CallbackInfoReturnable<Optional<? extends LivingEntity>> cir
    ) {
        Optional<? extends LivingEntity> target = cir.getReturnValue();
        if (target.isEmpty()) return;
        if (!(target.get() instanceof Player player)) return;
        if (!IEnchantedGold.isHoldingEnchantedGold(player)) return;
        Optional<LivingEntity> angryAt = BehaviorUtils.getLivingEntityFromUUIDMemory(piglinBrute, MemoryModuleType.ANGRY_AT);
        if (angryAt.filter(entity -> entity == player).isPresent()) return;
        cir.setReturnValue(Optional.empty());
    }
}
