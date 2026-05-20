package dev.dubhe.anvilcraft.mixin;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import dev.dubhe.anvilcraft.api.IHasMultiBlock;
import dev.dubhe.anvilcraft.api.injection.IExplosionExtension;
import dev.dubhe.anvilcraft.recipe.anvil.collision.BlockTransform;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@Mixin(ServerExplosion.class)
abstract class ServerExplosionMixin implements IExplosionExtension {

    @Unique
    // public HashMap<Block, ArrayList<BlockTransform>> anvilcraft$blockTransformMap = new HashMap<>();
    public Multimap<Block, BlockTransform> anvilcraft$blockTransformMap = MultimapBuilder.hashKeys().hashSetValues().build();

    @Unique
    @SuppressWarnings("FieldMayBeFinal")
    private HashMap<BlockTransform, Integer> anvilcraft$counterMap = new HashMap<>();

    @Unique
    @SuppressWarnings("FieldMayBeFinal")
    private HashSet<BlockPos> anvilcraft$processedPosSet = new HashSet<>();

    @Shadow
    @Final
    private ServerLevel level;

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

    @Inject(
        method = "calculateExplodedPositions",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;"
                     + "getBlockState(Lnet/minecraft/core/BlockPos;)"
                     + "Lnet/minecraft/world/level/block/state/BlockState;"
        )
    )
    private void anvilcraft$explosionBlockTransform0(
        CallbackInfoReturnable<List<BlockPos>> cir,
        @Share("isExplosionBlockTransformed") LocalBooleanRef isExplosionBlockTransformed,
        @Local(name = "pos") BlockPos pos
    ) {
        Block block = this.level.getBlockState(pos).getBlock();
        ArrayList<BlockTransform> blockTransforms = new ArrayList<>(this.anvilcraft$blockTransformMap.get(block));
        if (blockTransforms.isEmpty()) return;
        BlockTransform blockTransform = blockTransforms.get(this.level.getRandom().nextInt(blockTransforms.size()));
        if (this.anvilcraft$counterMap.getOrDefault(blockTransform, 0) >= blockTransform.maxCount()) return;
        if (this.anvilcraft$processedPosSet.contains(pos)) return;
        isExplosionBlockTransformed.set(blockTransform.progress(this.level, pos));
        if (isExplosionBlockTransformed.get() && !this.anvilcraft$processedPosSet.contains(pos)) {
            this.anvilcraft$processedPosSet.add(pos);
            if (this.anvilcraft$counterMap.containsKey(blockTransform)) {
                this.anvilcraft$counterMap.put(blockTransform, this.anvilcraft$counterMap.get(blockTransform) + 1);
            } else {
                this.anvilcraft$counterMap.put(blockTransform, 1);
            }
        }
    }

    @WrapOperation(
        method = "calculateExplodedPositions",
        at =
        @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/ExplosionDamageCalculator;"
                     + "shouldBlockExplode("
                     + "Lnet/minecraft/world/level/Explosion;"
                     + "Lnet/minecraft/world/level/BlockGetter;"
                     + "Lnet/minecraft/core/BlockPos;"
                     + "Lnet/minecraft/world/level/block/state/BlockState;F)Z"
        )
    )
    private boolean anvilcraft$explosionBlockTransform(
        ExplosionDamageCalculator instance,
        Explosion explosion,
        BlockGetter level,
        BlockPos pos,
        BlockState state,
        float power,
        Operation<Boolean> original,
        @Share("isExplosionBlockTransformed") LocalBooleanRef isExplosionBlockTransformed
    ) {
        return !isExplosionBlockTransformed.get() && original.call(instance, explosion, level, pos, state, power);
    }

    @Override
    public void anvilcraft$setBlockTransformExplosion(Collection<BlockTransform> blockTransformExplosions) {
        for (BlockTransform blockTransform : blockTransformExplosions) {
            for (BlockState state : blockTransform.inputBlock().getStatesCache()) {
                Block block = state.getBlock();
                this.anvilcraft$blockTransformMap.put(block, blockTransform);
            }
        }
    }
}
