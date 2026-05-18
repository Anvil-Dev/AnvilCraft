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
        RandomSource random,
        float min,
        float maxExclusive,
        Operation<Float> original,
        @Local(argsOnly = true, name = "enchantmentLevel") int enchantmentLevel
    ) {
        float result = original.call(random, min, maxExclusive);
        if (!ProvidenceRef.shouldItTrigger()) return result;
        float randomValue = random.nextFloat();
        if (randomValue >= 0.25F) return result;
        result += original.call(random, this.minDamage.calculate(enchantmentLevel), this.maxDamage.calculate(enchantmentLevel));
        if (randomValue >= 0.05F) return result;
        result += original.call(random, this.minDamage.calculate(enchantmentLevel), this.maxDamage.calculate(enchantmentLevel));
        return result;
    }
}
