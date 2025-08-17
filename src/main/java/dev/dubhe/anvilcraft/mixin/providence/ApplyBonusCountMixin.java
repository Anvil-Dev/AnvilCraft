package dev.dubhe.anvilcraft.mixin.providence;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.init.ModEnchantmentTags;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ApplyBonusCount.class)
public class ApplyBonusCountMixin {
    @Shadow
    @Final
    private Holder<Enchantment> enchantment;

    @WrapOperation(
        method = "run",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/loot/functions/ApplyBonusCount$Formula;"
                + "calculateNewCount(Lnet/minecraft/util/RandomSource;II)I"))
    private int calculateMultipleForProvidence(
        ApplyBonusCount.Formula instance, RandomSource random1, int count, int level, Operation<Integer> original,
        @Local(ordinal = 0, argsOnly = true) LootContext context
    ) {
        int result = original.call(instance, random1, count, level);
        if (level == 0
            || !(context.getParamOrNull(LootContextParams.TOOL) instanceof ItemStack stack)
            || !stack.has(ModComponents.PROVIDENCE)
            || !this.enchantment.is(ModEnchantmentTags.PROVIDENCE_BONUS)
        ) return result;
        float random = random1.nextFloat();
        if (random >= 0.25f) return result;
        result += original.call(instance, random1, count, level);
        if (random >= 0.05f) return result;
        result += original.call(instance, random1, count, level);
        return result;
    }
}
