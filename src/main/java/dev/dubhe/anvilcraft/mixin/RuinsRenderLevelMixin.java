package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.block.RuinsBlock;
import dev.dubhe.anvilcraft.block.entity.RuinsBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.RuinsRenderContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.Nullable;

@Mixin(Level.class)
abstract class RuinsRenderLevelMixin {
    @ModifyReturnValue(method = "getBlockState", at = @At("RETURN"))
    private BlockState anvilcraft$displayState(BlockState original, BlockPos pos) {
        Level level = (Level) (Object) this;
        if (original.getBlock() instanceof RuinsBlock && RuinsRenderContext.isActive(level)
            && level.getChunkAt(pos).getBlockEntity(pos) instanceof RuinsBlockEntity ruins) {
            return ruins.getDisplayState();
        }
        return original;
    }

    @ModifyReturnValue(method = "getBlockEntity", at = @At("RETURN"))
    @Nullable
    private BlockEntity anvilcraft$displayEntity(@Nullable BlockEntity original) {
        return original instanceof RuinsBlockEntity ruins && RuinsRenderContext.isActive((Level) (Object) this)
            ? ruins.getDisplayEntity() : original;
    }

    @ModifyReturnValue(method = "getFluidState", at = @At("RETURN"))
    private FluidState anvilcraft$displayFluid(FluidState original, BlockPos pos) {
        Level level = (Level) (Object) this;
        if (RuinsRenderContext.isActive(level) && level.getChunkAt(pos).getBlockEntity(pos) instanceof RuinsBlockEntity ruins) {
            return ruins.getDisplayState().getFluidState();
        }
        return original;
    }
}
