package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.IHasMultiBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

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
                            target = "Lnet/minecraft/world/level/block/state/BlockState;"
                                    + "getBlock()Lnet/minecraft/world/level/block/Block;"),
            locals = LocalCapture.CAPTURE_FAILSOFT)
    private void finalizeExplosion(
            boolean spawnParticles,
            CallbackInfo ci,
            boolean bl,
            ObjectArrayList<Pair<ItemStack, BlockPos>> objectArrayList,
            boolean bl2,
            ObjectListIterator<BlockPos> var5,
            BlockPos blockPos,
            @NotNull BlockState blockState) {
        Block block = blockState.getBlock();
        if (block instanceof IHasMultiBlock multiBlock) {
            multiBlock.onRemove(level, blockPos, blockState);
        }
    }
}
