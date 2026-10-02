package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.dubhe.anvilcraft.block.RuinsBlock;
import dev.dubhe.anvilcraft.client.renderer.blockentity.RuinsParticles;
import dev.dubhe.anvilcraft.client.renderer.blockentity.RuinsRenderContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientLevel.class)
abstract class RuinsParticleEffectsMixin {
    @WrapMethod(method = "addDestroyBlockEffect")
    private void anvilcraft$destroyDisguise(BlockPos pos, BlockState state, Operation<Void> original) {
        ClientLevel level = (ClientLevel) (Object) this;
        if (state.getBlock() instanceof RuinsBlock) {
            BlockState display = RuinsParticles.displayState(level, pos, state);
            try (var ignored = RuinsRenderContext.enter(level)) {
                original.call(pos, display);
            }
        } else {
            original.call(pos, state);
        }
    }

    @WrapMethod(method = "addBreakingBlockEffect(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;"
        + "Lnet/minecraft/world/phys/HitResult;)V")
    private void anvilcraft$hitDisguise(BlockPos pos, Direction direction, @Nullable HitResult hit, Operation<Void> original) {
        ClientLevel level = (ClientLevel) (Object) this;
        if (level.getBlockState(pos).getBlock() instanceof RuinsBlock) {
            try (var ignored = RuinsRenderContext.enter(level)) {
                original.call(pos, direction, hit);
            }
        } else {
            original.call(pos, direction, hit);
        }
    }
}
