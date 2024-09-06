package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.block.HollowMagnetBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(ItemEntity.class)
abstract class ItemEntityMixin extends Entity {
    @Shadow
    public abstract ItemStack getItem();

    @Shadow
    @Nullable
    public abstract Entity getOwner();

    @Shadow
    public abstract void setItem(ItemStack stack);

    @Shadow @javax.annotation.Nullable private UUID target;

    @Shadow protected abstract void setUnderwaterMovement();

    public ItemEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private @NotNull Vec3 slowDown(Vec3 instance, double dx, double dy, double dz) {
        if (this.getItem().is(ModItems.LEVITATION_POWDER.get()))
            dy *= -0.005;
        if (this.level().getBlockState(this.blockPosition()).is(ModBlocks.HOLLOW_MAGNET_BLOCK.get()))
            dy *= 0.2;
        return instance.add(dx, dy, dz);
    }

    @Unique
    private boolean anvilcraft$needMagnetization = true;

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void magnetization(CallbackInfo ci) {
        if (this.getServer() == null) return;
        ItemStack itemStack = this.getItem();
        if (!itemStack.is(Items.IRON_INGOT)) return;
        BlockState blockState = this.level().getBlockState(this.blockPosition());
        if (!blockState.is(ModBlocks.HOLLOW_MAGNET_BLOCK.get()) || blockState.getValue(HollowMagnetBlock.LIT)) return;
        if (this.getOwner() == null || !(this.getOwner() instanceof ServerPlayer)) return;
        if (itemStack.getCount() != 1) return;
        if (!this.anvilcraft$needMagnetization) return;
        this.anvilcraft$needMagnetization = false;
        if (this.level().random.nextInt(100) <= 10) {
            this.setItem(new ItemStack(ModItems.MAGNET_INGOT));
        }
    }

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void voidResistant(CallbackInfo ci) {
        if (this.getServer() == null) return;
        if (this.getItem().is(ModItemTags.VOID_RESISTANT) && this.getY() < this.level().getMinBuildHeight())
                this.setDeltaMovement(0, 0.6f, 0);
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void explosionProof(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (
            !this.getItem().isEmpty()
                    && this.getItem().is(ModItemTags.EXPLOSION_PROOF)
                    && source.is(DamageTypeTags.IS_EXPLOSION)
        ) {
            cir.setReturnValue(false);
        }
    }
}
