package dev.dubhe.anvilcraft.mixin.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.entity.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.api.event.entity.AnvilHurtEntityEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin {
    @Shadow
    private BlockState blockState;
    @Shadow
    private boolean cancelDrop;
    @Unique
    private final FallingBlockEntity ths = (FallingBlockEntity) (Object) this;

    @Inject(method = "tick", at = @At(value = "INVOKE", ordinal = 10, target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;level()Lnet/minecraft/world/level/Level;"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void anvilFallOnGround(CallbackInfo ci, Block block, BlockPos blockPos) {
        if (ths.level().isClientSide()) return;
        if (!this.blockState.is(BlockTags.ANVIL)) return;
        AnvilFallOnLandEvent event = new AnvilFallOnLandEvent(ths.level(), blockPos, ths);
        AnvilCraft.EVENT_BUS.post(event);
        if (event.isAnvilDamage()) {
            BlockState state = AnvilBlock.damage(this.blockState);
            if (state != null) ths.level().setBlock(blockPos, state, 3);
            else this.cancelDrop = true;
        }
    }

    @Redirect(method = "method_32879", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private static boolean anvilHurtEntity(@NotNull Entity instance, DamageSource source, float amount) {
        boolean bl = instance.hurt(source, amount);
        Entity directEntity = source.getDirectEntity();
        if (!bl || !(directEntity instanceof FallingBlockEntity entity)) return bl;
        AnvilCraft.EVENT_BUS.post(new AnvilHurtEntityEvent(entity, entity.getOnPos(), entity.level(), instance, amount));
        return true;
    }
}
