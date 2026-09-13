package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.util.BlockPlacementPicking;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(UseOnContext.class)
abstract class UseOnContextMixin implements BlockPlacementPicking.PlayerClick {
    @Shadow
    @Final
    private BlockHitResult hitResult;

    @Unique
    private boolean anvilcraft$playerClick;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;"
        + "Lnet/minecraft/world/phys/BlockHitResult;)V", at = @At("RETURN"))
    private void markPlayerClick(Player player, InteractionHand hand, BlockHitResult hit, CallbackInfo ci) {
        this.anvilcraft$playerClick = true;
    }

    @Override
    public boolean anvilcraft$isPlayerClick() {
        return this.anvilcraft$playerClick;
    }

    @Override
    public boolean anvilcraft$hasBlockHit() {
        return this.hitResult.getType() == HitResult.Type.BLOCK;
    }
}
