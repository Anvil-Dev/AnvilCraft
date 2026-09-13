package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.anvilcraft.lib.v2.cube.client.CubeSelection;
import dev.dubhe.anvilcraft.block.RuinsBlock;
import dev.dubhe.anvilcraft.block.entity.RuinsBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.RuinsRenderContext;
import dev.dubhe.anvilcraft.client.selection.RuinsSelection;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

import javax.annotation.Nullable;

@Mixin(value = CubeSelection.class, remap = false)
abstract class RuinsCubeSelectionMixin {
    @WrapMethod(method = "target")
    @Nullable
    private static CubeSelection.Target anvilcraft$disguisedTarget(
        ClientLevel level, BlockPos pos, BlockState state, float partialTick, Operation<CubeSelection.Target> original
    ) {
        if (state.getBlock() instanceof RuinsBlock && level.getBlockEntity(pos) instanceof RuinsBlockEntity ruins) {
            try (RuinsRenderContext ignored = RuinsRenderContext.enter(level)) {
                RuinsSelection.prepare(ruins);
                return original.call(level, pos, ruins.getDisplayState(), partialTick);
            }
        }
        return original.call(level, pos, state, partialTick);
    }
}
