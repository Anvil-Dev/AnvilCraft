package dev.dubhe.anvilcraft.mixin;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.mojang.datafixers.util.Pair;
import dev.dubhe.anvilcraft.api.IHasMultiBlock;
import dev.dubhe.anvilcraft.recipe.anvil.collision.BlockTransform;
import dev.dubhe.anvilcraft.util.BlockTransformExplosion;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@Mixin(Explosion.class)
abstract class ExplosionMixin implements BlockTransformExplosion {

    @Unique
//    public HashMap<Block, ArrayList<BlockTransform>> anvilcraft$blockTransformMap = new HashMap<>();
    public Multimap<Block, BlockTransform> anvilcraft$blockTransformMap = MultimapBuilder.hashKeys().hashSetValues().build();

    @Unique
    @SuppressWarnings("FieldMayBeFinal")
    private HashMap<BlockTransform, Integer> anvilcraft$counterMap = new HashMap<>();

    @Unique
    @SuppressWarnings("FieldMayBeFinal")
    private HashSet<BlockPos> anvilcraft$processedPosSet = new HashSet<>();

    @Shadow
    @Final
    private Level level;

    @Inject(
        method = "finalizeExplosion",
        at =
        @At(
            value = "INVOKE",
            target =
                "Lnet/minecraft/world/level/block/state/BlockState;onExplosionHit(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Explosion;Ljava/util/function/BiConsumer;)V",
            shift = At.Shift.AFTER
        ),
        locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private void finalizeExplosion(
        boolean pSpawnParticles,
        CallbackInfo ci,
        boolean flag,
        List<Pair<ItemStack, BlockPos>> list,
        ObjectListIterator<BlockPos> var4,
        BlockPos blockpos) {
        BlockState state = this.level.getBlockState(blockpos);
        Block block = state.getBlock();
        if (block instanceof IHasMultiBlock multiBlock) {
            multiBlock.onRemove(level, blockpos, state);
        }
    }

    @Inject(
        method = "explode()V", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
    )
    )
    private void anvilCraft$explosionBlockTransform(
        CallbackInfo ci,
        @Share("isExplosionBlockTransformed") LocalBooleanRef isExplosionBlockTransformed,
        @Local(ordinal = 0) BlockPos pos
    ) {
        Block block = level.getBlockState(pos).getBlock();
        ArrayList<BlockTransform> blockTransforms = new ArrayList<>(this.anvilcraft$blockTransformMap.get(block));
        BlockTransform blockTransform = blockTransforms.get(level.random.nextInt(blockTransforms.size()));
        if (anvilcraft$counterMap.getOrDefault(blockTransform, 0) >= blockTransform.maxCount()) return;
        if (anvilcraft$processedPosSet.contains(pos)) return;
        isExplosionBlockTransformed.set(blockTransform.progress(level, pos));
        if (isExplosionBlockTransformed.get() && !anvilcraft$processedPosSet.contains(pos)) {
            anvilcraft$processedPosSet.add(pos);
            if (anvilcraft$counterMap.containsKey(blockTransform)) {
                anvilcraft$counterMap.put(blockTransform, anvilcraft$counterMap.get(blockTransform) + 1);
            } else {
                anvilcraft$counterMap.put(blockTransform, 1);
            }
        }
    }

    @WrapOperation(
        method = "explode",
        at =
        @At(
            value = "INVOKE",
            target =
                "Lnet/minecraft/world/level/ExplosionDamageCalculator;shouldBlockExplode(Lnet/minecraft/world/level/Explosion;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;F)Z"
        )
    )
    private boolean anvilCraft$explosionBlockTransform(
        ExplosionDamageCalculator instance,
        Explosion explosion,
        BlockGetter reader,
        BlockPos pos,
        BlockState state,
        float power,
        Operation<Boolean> original,
        @Share("isExplosionBlockTransformed") @NotNull LocalBooleanRef isExplosionBlockTransformed
    ) {
        return !isExplosionBlockTransformed.get() && original.call(instance, explosion, reader, pos, state, power);
    }

    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Override
    public void setBlockTransformExplosion(@NotNull Collection<BlockTransform> blockTransformExplosions) {
        for (BlockTransform blockTransform : blockTransformExplosions) {
            for (BlockState state : blockTransform.inputBlock().getStatesCache()) {
                Block block = state.getBlock();
                this.anvilcraft$blockTransformMap.put(block, blockTransform);
            }
        }
    }
}
