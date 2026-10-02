package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class StoragePickMixin {
    @Inject(method = "addBlockDataToItem", at = @At("RETURN"))
    private static void anvilcraft$restoreStorageReference(
        BlockState state, ServerLevel level, BlockPos pos, ItemStack stack, CallbackInfo ci
    ) {
        StorageBlockEntity.applyPickStorageId(stack, level, pos, state, true);
    }
}
