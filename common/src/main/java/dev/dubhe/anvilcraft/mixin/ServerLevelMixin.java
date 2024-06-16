package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.chargecollector.HeatedBlockRecorder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    @Inject(method = "onBlockStateChange", at = @At("RETURN"))
    void onBlockChange(BlockPos pos, BlockState blockState, BlockState newState, CallbackInfo ci) {
        HeatedBlockRecorder.getInstance((ServerLevel) (Object) this).onBlockStateChange(pos, blockState, newState);
    }
}
