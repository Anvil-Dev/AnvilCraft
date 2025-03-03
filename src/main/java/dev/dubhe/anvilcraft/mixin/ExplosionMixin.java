package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.IHasMultiBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(Explosion.class)
abstract class ExplosionMixin {

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
            shift = At.Shift.AFTER),
        locals = LocalCapture.CAPTURE_FAILSOFT)
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
}
