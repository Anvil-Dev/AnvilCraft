package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.util.SpectralAnvilConversionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(EndPortalBlock.class)
abstract class EndPortalBlockMixin {
    @Inject(
        method = "entityInside",
        at =
        @At(
            value = "INVOKE",
            target =
                "Lnet/minecraft/world/entity/Entity;setAsInsidePortal(Lnet/minecraft/world/level/block/Portal;Lnet/minecraft/core/BlockPos;)V"),
        cancellable = true)
    private void fallBlockEntityInside(
        BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity, CallbackInfo ci
    ) {
        if (
            pEntity instanceof FallingBlockEntity fallingBlockEntity
                && !fallingBlockEntity.blockState.is(ModBlockTags.END_PORTAL_UNABLE_CHANGE)
        ) {
            BlockState newState = ModBlocks.END_DUST.getDefaultState();
            if (fallingBlockEntity.blockState.is(BlockTags.ANVIL)) {
                double rand = pLevel.random.nextDouble();
                if (rand < SpectralAnvilConversionUtil.chance(fallingBlockEntity.blockState.getBlock())) {
                    newState = ModBlocks.SPECTRAL_ANVIL.getDefaultState();
                }
            }
            fallingBlockEntity.blockState = newState;
            fallingBlockEntity.setAsInsidePortal((Portal) this, pPos);
            ci.cancel();
        }
    }
}
