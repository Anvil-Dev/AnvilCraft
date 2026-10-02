package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.client.renderer.mun.MunSurfaceRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class MunShadowChunkMixin {
    @Shadow
    public abstract Level getLevel();

    @Shadow
    public abstract BlockState getBlockState(BlockPos pos);

    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void anvilcraft$shadowChange(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> cir) {
        if (!(this.getLevel() instanceof ClientLevel level)) return;
        BlockState previous = cir.getReturnValue();
        if (previous == null || previous == state) return;
        MunSurfaceRenderer.onBlockChanged(level, pos, previous, this.getBlockState(pos));
    }
}
