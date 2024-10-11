package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.chargecollector.HeatedBlockRecorder;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
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

    @Inject(method = "addPlayer", at = @At("HEAD"))
    void onPlayerJoinLevel(ServerPlayer player, CallbackInfo ci) {
        PowerGrid.MANAGER.onPlayerJoined((ServerLevel) (Object) this, player);
    }
}
