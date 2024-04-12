package dev.dubhe.anvilcraft.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
abstract class BucketItemMixin {
    @Shadow
    @Final
    private Fluid content;
    @Unique
    private final BucketItem anvilCraft$ths = (BucketItem) (Object) this;

    @Inject(method = "emptyContents", at = @At("HEAD"), cancellable = true)
    private void putLiquidToCauldron(
        Player player, @NotNull Level level, BlockPos pos, BlockHitResult result, CallbackInfoReturnable<Boolean> cir) {
        if (level.isInWorldBounds(pos)) {
            if (level.getBlockState(pos).is(Blocks.CAULDRON)) {
                if (anvilCraft$ths.equals(Items.WATER_BUCKET)) {
                    if (!level.isClientSide) {
                        level.setBlockAndUpdate(
                            pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3)
                        );
                    }
                } else if (anvilCraft$ths.equals(Items.LAVA_BUCKET)) {
                    if (!level.isClientSide) {
                        level.setBlockAndUpdate(pos, Blocks.LAVA_CAULDRON.defaultBlockState());
                    }
                } else return;
                if (this.content.getPickupSound().isPresent()) {
                    level.playSound(player, pos, this.content.getPickupSound().get(), SoundSource.BLOCKS, 1.0f, 1.0f);
                }
                level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
                cir.setReturnValue(true);
            }
        }
    }
}
