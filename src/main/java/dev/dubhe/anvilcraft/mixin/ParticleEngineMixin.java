package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
abstract class ParticleEngineMixin {

    @Shadow
    protected ClientLevel level;

    /**
        由于为具有空碰撞箱的加速环、偏转环中间部分增加了玩家持有相应物品时可互动的功能，该方法会对具有空形状的方块调用
        但是后续的 {@code net.minecraft.world.phys.shapes.VoxelShape#bounds()} 方法会抛出异常
        因此为避免崩溃，必须在这之前取消方法调用
     */
    @Inject(method = "addBlockHitEffects", at = @At("HEAD"), cancellable = true)
    private void cancelHitEffectForEmptyBlock(BlockPos pos, BlockHitResult target, CallbackInfo ci) {
        BlockState state = this.level.getBlockState(pos);
        VoxelShape partShape = anvilcraft$partShape(state);
        if ((partShape == null ? state.getShape(this.level, pos) : partShape).isEmpty()) {
            ci.cancel();
        }
    }

    /**
        多方块的轮廓形状是整个结构的并集，若直接用于生成粒子，结构的每一格都会按整个结构的体积生成粒子，
        3x3x3 的结构就是 27 倍粒子量，足以造成卡顿。粒子只应使用当前部件自身的形状。
     */
    @WrapOperation(
        method = {"destroy", "crack"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;"
                     + "getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)"
                     + "Lnet/minecraft/world/phys/shapes/VoxelShape;"
        )
    )
    private VoxelShape useSinglePartShapeForParticles(
        BlockState state,
        BlockGetter getter,
        BlockPos pos,
        Operation<VoxelShape> original
    ) {
        VoxelShape partShape = anvilcraft$partShape(state);
        return partShape == null ? original.call(state, getter, pos) : partShape;
    }

    @Unique
    private static @Nullable VoxelShape anvilcraft$partShape(BlockState state) {
        if (state.getBlock() instanceof AbstractMultiPartBlock<?> multiPartBlock) {
            return multiPartBlock.getPartShape(state);
        }
        return null;
    }
}
