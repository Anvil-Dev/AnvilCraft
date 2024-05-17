package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.TntDestroyListenerBlock;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Explosion.class)
abstract class ExplosionMixin {

    @Shadow @Final private Level level;

    @Inject(method = "finalizeExplosion",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;"
                + "getBlock()Lnet/minecraft/world/level/block/Block;"
        ),
        locals = LocalCapture.CAPTURE_FAILSOFT)
    private void finalizeExplosion(boolean spawnParticles, CallbackInfo ci, boolean bl,
        ObjectArrayList objectArrayList, boolean bl2, ObjectListIterator var5, BlockPos blockPos,
        BlockState blockState) {
        Block block = blockState.getBlock();
        if (block instanceof TntDestroyListenerBlock tntDestroyListenerBlock) {
            tntDestroyListenerBlock.tntWillDestroy(level, blockPos, blockState);
        }
    }
}
