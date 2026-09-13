package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.dubhe.anvilcraft.block.RuinsBlock;
import dev.dubhe.anvilcraft.client.renderer.blockentity.RuinsParticles;
import dev.dubhe.anvilcraft.client.renderer.blockentity.RuinsRenderContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(ParticleEngine.class)
abstract class RuinsParticleEngineMixin {
    @Shadow
    @Nullable
    protected ClientLevel level;

    @WrapMethod(method = "destroy")
    private void anvilcraft$destroyDisguise(BlockPos pos, BlockState state, Operation<Void> original) {
        if (this.level != null && state.getBlock() instanceof RuinsBlock) {
            BlockState display = RuinsParticles.displayState(this.level, pos, state);
            try (RuinsRenderContext ignored = RuinsRenderContext.enter(this.level)) {
                original.call(pos, display);
            }
        } else {
            original.call(pos, state);
        }
    }

    @WrapMethod(method = "crack")
    private void anvilcraft$hitDisguise(BlockPos pos, Direction direction, Operation<Void> original) {
        if (this.level != null && this.level.getBlockState(pos).getBlock() instanceof RuinsBlock) {
            try (RuinsRenderContext ignored = RuinsRenderContext.enter(this.level)) {
                original.call(pos, direction);
            }
        } else {
            original.call(pos, direction);
        }
    }
}
