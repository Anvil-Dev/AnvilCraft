package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(EndPortalBlock.class)
abstract class EndPortalBlockMixin {
    @Inject(
            method = "entityInside",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;"
                            + "changeDimension(Lnet/minecraft/server/level/ServerLevel;)"
                            + "Lnet/minecraft/world/entity/Entity;"
            ),
            cancellable = true,
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private void fallBlockEntityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            CallbackInfo ci,
            ResourceKey<?> resourceKey,
            ServerLevel serverLevel
    ) {
        if (entity instanceof FallingBlockEntity fallingBlockEntity
                && !fallingBlockEntity.blockState.is(ModBlockTags.END_PORTAL_UNABLE_CHANGE)
        ) {
            if (fallingBlockEntity.blockState.is(BlockTags.ANVIL)) {
                fallingBlockEntity.blockState = ModBlocks.SPECTRAL_ANVIL.getDefaultState();
            } else {
                fallingBlockEntity.blockState = ModBlocks.END_DUST.getDefaultState();
            }
            fallingBlockEntity.changeDimension(serverLevel);
            ci.cancel();
        }
    }
}
