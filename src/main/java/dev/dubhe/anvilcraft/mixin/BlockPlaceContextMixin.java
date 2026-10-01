package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.util.BlockPlacementPicking;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockPlaceContext.class)
abstract class BlockPlaceContextMixin extends UseOnContext implements BlockPlacementPicking.PlacementRejection {
    @Unique
    private boolean anvilcraft$placementRejected;

    private BlockPlaceContextMixin(Player player, InteractionHand hand, BlockHitResult hit) {
        super(player, hand, hit);
    }

    @ModifyVariable(method = "<init>(Lnet/minecraft/world/item/context/UseOnContext;)V", at = @At("HEAD"), argsOnly = true)
    private static UseOnContext useOriginalPlacementShape(UseOnContext context) {
        return BlockPlacementPicking.forPlacement(context);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/item/context/UseOnContext;)V", at = @At("RETURN"))
    private void copyPlacementRejection(UseOnContext context, CallbackInfo ci) {
        this.anvilcraft$placementRejected = context instanceof BlockPlacementPicking.PlacementRejection rejection
            && rejection.anvilcraft$isPlacementRejected();
    }

    @Override
    public boolean anvilcraft$isPlacementRejected() {
        return this.anvilcraft$placementRejected;
    }

    @Inject(method = "canPlace", at = @At("HEAD"), cancellable = true)
    private void rejectMissedPlacement(CallbackInfoReturnable<Boolean> cir) {
        if (this.anvilcraft$placementRejected) cir.setReturnValue(false);
    }
}
