package dev.dubhe.anvilcraft.mixin.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.entity.AnvilFallOnLandEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin {
    @Shadow
    private BlockState blockState;
    @Shadow private boolean cancelDrop;
    @Unique
    private final FallingBlockEntity ths = (FallingBlockEntity) (Object) this;

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;", ordinal = 0), locals = LocalCapture.CAPTURE_FAILHARD)
    private void anvilFallOnGround(CallbackInfo ci, Block block, BlockPos blockPos, boolean bl, boolean bl2, double d) {
        if (ths.level().isClientSide()) return;
        if (!this.blockState.is(BlockTags.ANVIL)) return;
        AnvilFallOnLandEvent event = new AnvilFallOnLandEvent(ths.level(), blockPos, ths);
        AnvilCraft.EVENT_BUS.post(event);
        if (event.isAnvilDamage()) {
            BlockState state = AnvilBlock.damage(this.blockState);
            if (state != null) this.blockState = state;
            else this.cancelDrop = true;
        }
    }
}
