package dev.dubhe.anvilcraft.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SolidBucketItem.class)
abstract class SolidBucketItemMixin {
    @Shadow
    @Final
    private SoundEvent placeSound;
    @Unique
    private final SolidBucketItem anvilCraft$ths = (SolidBucketItem) (Object) this;

    @Inject(method = "emptyContents", at = @At("HEAD"), cancellable = true)
    private void emptyContents(
        Player player, @NotNull Level level, BlockPos pos, BlockHitResult result, CallbackInfoReturnable<Boolean> cir
    ) {
        if (!anvilCraft$ths.equals(Items.POWDER_SNOW_BUCKET)) return;
        if (level.isInWorldBounds(pos) && level.getBlockState(pos).is(Blocks.CAULDRON)) {
            if (!level.isClientSide) {
                level.setBlockAndUpdate(pos, Blocks.POWDER_SNOW_CAULDRON.defaultBlockState()
                    .setValue(LayeredCauldronBlock.LEVEL, 3));
            }
            level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
            level.playSound(player, pos, this.placeSound, SoundSource.BLOCKS, 1.0f, 1.0f);
            cir.setReturnValue(true);
        }
    }
}
