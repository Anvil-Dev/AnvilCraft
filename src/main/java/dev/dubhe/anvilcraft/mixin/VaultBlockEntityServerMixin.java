package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VaultBlockEntity.Server.class)
public abstract class VaultBlockEntityServerMixin {
    @WrapOperation(
        method = "isValidToInsert",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;"
                     + "isSameItemSameComponents("
                     + "Lnet/minecraft/world/item/ItemStack;"
                     + "Lnet/minecraft/world/item/ItemStack;)Z"
        )
    )
    private static boolean validKeyItemOnly(ItemStack a, ItemStack b, Operation<Boolean> original) {
        if (b.getComponentsPatch().isEmpty()) return a.is(b.getItem());
        return original.call(a, b);
    }
}
