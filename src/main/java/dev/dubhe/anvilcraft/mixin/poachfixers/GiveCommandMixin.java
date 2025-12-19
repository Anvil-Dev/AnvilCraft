package dev.dubhe.anvilcraft.mixin.poachfixers;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.commands.GiveCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GiveCommand.class)
public class GiveCommandMixin {
    @WrapOperation(
        method = "giveItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;"
                     + "drop(Lnet/minecraft/world/item/ItemStack;Z)"
                     + "Lnet/minecraft/world/entity/item/ItemEntity;",
            ordinal = 0
        )
    )
    @SuppressWarnings("checkstyle:MethodName")
    private static ItemEntity shouldNotPoach_PreVeNTDUpLICatioN(
        ServerPlayer instance, ItemStack stack, boolean b, Operation<ItemEntity> original
    ) {
        return original.call(instance, stack, b);
    }
}
