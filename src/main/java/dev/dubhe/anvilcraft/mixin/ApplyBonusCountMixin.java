package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.init.ModComponents;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.common.Tags;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ApplyBonusCount.class)
public class ApplyBonusCountMixin {
    @Shadow @Final private Holder<Enchantment> enchantment;

    @ModifyVariable(method = "run", at = @At("STORE"), ordinal = 0)
    private int cancelWhenMercilessAndIncreaseDrop(
        int i, @Local(ordinal = 1) ItemStack itemStack, @Local(argsOnly = true) LootContext ctx
    ) {
        ItemStack itemstack = ctx.getParamOrNull(LootContextParams.TOOL);
        if (itemstack != null && itemstack.has(ModComponents.MERCILESS) && this.enchantment.is(Tags.Enchantments.INCREASE_BLOCK_DROPS))
            return 0;
        return i;
    }
}
