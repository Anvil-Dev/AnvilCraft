package dev.dubhe.anvilcraft.mixin.providence;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.util.mixin.ProvidenceRef;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.DamageEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DamageEntity.class)
public class DamageEntityMixin {
    @Shadow
    @Final
    private LevelBasedValue minDamage;

    @Shadow
    @Final
    private LevelBasedValue maxDamage;

    @WrapOperation(
        method = "apply",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;randomBetween(Lnet/minecraft/util/RandomSource;FF)F")
    )
    private float randomMultipleForProvidence(
        RandomSource random1,
        float minInclusive,
        float maxExclusive,
        Operation<Float> original,
        @Local(argsOnly = true) int level
    ) {
        float result = original.call(random1, minInclusive, maxExclusive);
        if (!ProvidenceRef.shouldItTrigger()) return result;
        float random = random1.nextFloat();
        if (random >= 0.25f) return result;
        result += original.call(random1, this.minDamage.calculate(level), this.maxDamage.calculate(level));
        if (random >= 0.05f) return result;
        result += original.call(random1, this.minDamage.calculate(level), this.maxDamage.calculate(level));
        return result;
    }
}
