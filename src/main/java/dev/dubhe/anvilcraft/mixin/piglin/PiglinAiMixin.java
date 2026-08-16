package dev.dubhe.anvilcraft.mixin.piglin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.init.ModDataAttachments;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.abnormal.IEnchantedGold;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

@Mixin(PiglinAi.class)
public abstract class PiglinAiMixin {

    @Inject(method = "angerNearbyPiglins", at = @At("HEAD"), cancellable = true)
    private static void ignoreEnchantedGoldHolder(Player player, boolean angerOnlyIfCanSee, CallbackInfo ci) {
        if (IEnchantedGold.isHoldingEnchantedGold(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "findNearestValidAttackTarget", at = @At("RETURN"), cancellable = true)
    private static void calmDownForEnchantedGoldHolder(
        Piglin piglin,
        CallbackInfoReturnable<Optional<? extends LivingEntity>> cir
    ) {
        Optional<? extends LivingEntity> target = cir.getReturnValue();
        if (target.isEmpty()) return;
        if (!(target.get() instanceof Player player)) return;
        if (!IEnchantedGold.isHoldingEnchantedGold(player)) return;
        Optional<LivingEntity> angryAt = BehaviorUtils.getLivingEntityFromUUIDMemory(piglin, MemoryModuleType.ANGRY_AT);
        if (angryAt.filter(entity -> entity == player).isPresent()) return;
        cir.setReturnValue(Optional.empty());
    }

    @Inject(method = "holdInOffhand", at = @At("HEAD"))
    private static void markEnchantedGoldBarter(Piglin piglin, ItemStack stack, CallbackInfo ci) {
        piglin.setData(ModDataAttachments.ENCHANTED_GOLD_BARTER, stack.is(ModItems.ENCHANTED_GOLD_INGOT.get()));
    }

    @WrapOperation(
        method = "stopHoldingOffHandItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/monster/piglin/PiglinAi;getBarterResponseItems("
                     + "Lnet/minecraft/world/entity/monster/piglin/Piglin;"
                     + ")Ljava/util/List;"
        )
    )
    private static List<ItemStack> amplifyBarterResponse(Piglin piglin, Operation<List<ItemStack>> original) {
        List<ItemStack> items = original.call(piglin);
        if (piglin.getData(ModDataAttachments.ENCHANTED_GOLD_BARTER)) {
            piglin.setData(ModDataAttachments.ENCHANTED_GOLD_BARTER, false);
            return items.stream().map(stack -> stack.copyWithCount(stack.getCount() * 4)).toList();
        }
        return items;
    }
}
