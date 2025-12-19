package dev.dubhe.anvilcraft.mixin.poachfixers;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mezz.jei.common.util.ServerCommandUtil;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerCommandUtil.class)
public class JeiServerCommandUtilMixin {
    @WrapOperation(
        method = "giveToInventory",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;"
                     + "drop(Lnet/minecraft/world/item/ItemStack;Z)"
                     + "Lnet/minecraft/world/entity/item/ItemEntity;",
            ordinal = 0
        )
    )
    @SuppressWarnings("checkstyle:MethodName")
    private static ItemEntity shouldNotPoach_PreVeNTDUpLICatioN(
        Player it, ItemStack stack, boolean b, Operation<ItemEntity> original
    ) {
        return original.call(it, stack, b);
    }
}
