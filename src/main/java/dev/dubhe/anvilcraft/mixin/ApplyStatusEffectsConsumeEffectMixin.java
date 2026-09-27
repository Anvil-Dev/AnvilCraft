package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.api.amulet.AmuletManager;
import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ApplyStatusEffectsConsumeEffect.class)
abstract class ApplyStatusEffectsConsumeEffectMixin {
    @WrapOperation(method = "apply", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private boolean anvilcraft$foodEffect(
        LivingEntity entity, MobEffectInstance effect, Operation<Boolean> original, @Local(argsOnly = true) ItemStack stack
    ) {
        if (!stack.has(DataComponents.FOOD) || !AmuletManager.shouldEvaluate(entity)) return original.call(entity, effect);
        var ctx = new AmuletEffectContext();
        ctx.set(ModAmuletEffectContextKeys.MOB_EFFECT, effect);
        ctx.set(ModAmuletEffectContextKeys.CONSUMING_FOOD, true);
        AmuletManager.get(entity.registryAccess()).trigger(entity, ctx);
        return !ctx.getOrDefault(ModAmuletEffectContextKeys.IMMUNE_MOB_EFFECT, false) && original.call(entity, effect);
    }
}
