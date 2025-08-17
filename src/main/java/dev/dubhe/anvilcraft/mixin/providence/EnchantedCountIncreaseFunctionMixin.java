package dev.dubhe.anvilcraft.mixin.providence;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.init.ModEnchantmentTags;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnchantedCountIncreaseFunction.class)
public class EnchantedCountIncreaseFunctionMixin {
    @Shadow
    @Final
    private Holder<Enchantment> enchantment;

    @WrapOperation(
        method = "run",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/loot/providers/number/NumberProvider;"
                + "getFloat(Lnet/minecraft/world/level/storage/loot/LootContext;)F"))
    private float getMultipleForProvidence(NumberProvider instance, LootContext context, Operation<Float> original) {
        float result = original.call(instance, context);
        if (!(context.getParamOrNull(LootContextParams.TOOL) instanceof ItemStack stack)
            || !stack.has(ModComponents.PROVIDENCE)
            || !this.enchantment.is(ModEnchantmentTags.PROVIDENCE_BONUS)
        ) return result;
        float random = context.getRandom().nextFloat();
        if (random >= 0.25f) return result;
        result += original.call(instance, context);
        if (random >= 0.05f) return result;
        result += original.call(instance, context);
        return result;
    }
}
