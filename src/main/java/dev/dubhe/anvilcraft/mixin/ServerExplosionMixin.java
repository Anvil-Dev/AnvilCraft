package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.dubhe.anvilcraft.api.IHasMultiBlock;
import dev.dubhe.anvilcraft.api.injection.IExplosionExtension;
import dev.dubhe.anvilcraft.block.logistics.chute.ChutePlateExplosionRay;
import dev.dubhe.anvilcraft.recipe.anvil.collision.BlockTransform;
import dev.dubhe.anvilcraft.recipe.anvil.collision.BlockTransformDamageCalculator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.List;

@Mixin(ServerExplosion.class)
abstract class ServerExplosionMixin implements IExplosionExtension {

    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    @Final
    @Mutable
    private ExplosionDamageCalculator damageCalculator;

    @Shadow
    @Final
    private Vec3 center;

    @ModifyExpressionValue(
        method = "calculateExplodedPositions",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;isInWorldBounds(Lnet/minecraft/core/BlockPos;)Z")
    )
    private boolean stopAtChutePlate(
        boolean inBounds,
        @Local(name = "pos") BlockPos pos,
        @Local(name = "xp") double x,
        @Local(name = "yp") double y,
        @Local(name = "zp") double z,
        @Share("plateRay") LocalRef<ChutePlateExplosionRay> plateRay
    ) {
        if (!inBounds) {
            return false;
        }
        if (plateRay.get() == null) {
            plateRay.set(new ChutePlateExplosionRay(this.level, this.center));
        }
        return !plateRay.get().isBlocked(pos, x, y, z);
    }

    @Inject(
        method = "interactWithBlocks",
        at =
        @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;"
                     + "onExplosionHit("
                     + "Lnet/minecraft/server/level/ServerLevel;"
                     + "Lnet/minecraft/core/BlockPos;"
                     + "Lnet/minecraft/world/level/Explosion;"
                     + "Ljava/util/function/BiConsumer;)V",
            shift = At.Shift.AFTER
        )
    )
    private void finalizeExplosion(
        List<BlockPos> targetBlocks,
        CallbackInfo ci,
        @Local(name = "pos") BlockPos pos
    ) {
        BlockState state = this.level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof IHasMultiBlock multiBlock) {
            multiBlock.onRemove(this.level, pos, state);
        }
    }

    @Override
    public void anvilcraft$setBlockTransformExplosion(Collection<BlockTransform> blockTransformExplosions) {
        if (this.damageCalculator instanceof BlockTransformDamageCalculator calculator) {
            calculator.addTransforms(blockTransformExplosions);
        } else {
            this.damageCalculator = new BlockTransformDamageCalculator(this.damageCalculator, blockTransformExplosions);
        }
    }
}
