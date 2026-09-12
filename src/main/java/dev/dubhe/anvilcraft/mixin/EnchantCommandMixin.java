package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.EnchantCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnchantCommand.class)
abstract class EnchantCommandMixin {
    @ModifyExpressionValue(
        method = "enchant",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/Enchantment;getMaxLevel()I"
        )
    )
    private static int modifyMaxLevel(int original, @Local(argsOnly = true) CommandSourceStack source) {
        return anvilcraft$canBypassRestrictions(source) ? 255 : original;
    }

    @ModifyExpressionValue(
        method = "enchant",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;supportsEnchantment(Lnet/minecraft/core/Holder;)Z"
        )
    )
    private static boolean allowUnsupportedEnchantment(boolean original, @Local(argsOnly = true) CommandSourceStack source) {
        return original || anvilcraft$canBypassRestrictions(source);
    }

    @ModifyExpressionValue(
        method = "enchant",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;"
                     + "isEnchantmentCompatible(Ljava/util/Collection;Lnet/minecraft/core/Holder;)Z"
        )
    )
    private static boolean allowIncompatibleEnchantment(boolean original, @Local(argsOnly = true) CommandSourceStack source) {
        return original || anvilcraft$canBypassRestrictions(source);
    }

    @Unique
    private static boolean anvilcraft$canBypassRestrictions(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player) || !player.isCreative()) return false;
        ItemStack offhand = player.getOffhandItem();
        return offhand.is(ModBlocks.TRANSCENDENCE_ANVIL.asItem()) || offhand.is(ModItems.TRANSCENDENCE_ANVIL_HAMMER.get());
    }
}
