package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.init.ModBlocks;

import dev.dubhe.anvilcraft.item.IInherentEnchantment;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.stream.Stream;

@Mixin(AnvilMenu.class)
public class AnvilMenuMixin {
    @Inject(method = "isValidBlock", at = @At("HEAD"), cancellable = true)
    private void voj(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(state.is(ModBlocks.GIANT_ANVIL.get()) || state.is(BlockTags.ANVIL));
    }

    @Inject(method = "createResult", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;setEnchantments(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/enchantment/ItemEnchantments;)V",
            shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void createResultInjected(CallbackInfo ci, ItemStack itemstack, int i, long j, int k, ItemStack itemstack1, ItemStack itemstack2, ItemEnchantments.Mutable itemenchantments$mutable, boolean flag, int k2, int i3) {
        if (itemstack1.getItem() instanceof IInherentEnchantment inherentEnchantment) {
            Stream<Holder<Enchantment>> holderStream = itemenchantments$mutable.keySet().stream().filter((enchantmentHolder) -> enchantmentHolder.is(Enchantments.UNBREAKING));
            ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(itemenchantments$mutable.toImmutable());
            holderStream.forEach((enchantmentHolder) -> {
                enchantments.set(enchantmentHolder, inherentEnchantment.getInherentEnchantments().get(Enchantments.UNBREAKING));
            });
            EnchantmentHelper.setEnchantments(itemstack1, enchantments.toImmutable());
        }
    }
}
