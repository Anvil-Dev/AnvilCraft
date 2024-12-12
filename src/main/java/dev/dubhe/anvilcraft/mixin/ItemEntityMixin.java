package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.block.HollowMagnetBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.item.IFireReforging;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(ItemEntity.class)
abstract class ItemEntityMixin extends Entity {
    @Shadow
    public abstract ItemStack getItem();

    @Shadow
    @Nullable public abstract Entity getOwner();

    @Shadow
    public abstract void setItem(ItemStack stack);

    @Shadow
    @javax.annotation.Nullable private UUID target;

    @Shadow
    protected abstract void setUnderwaterMovement();

    public ItemEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Redirect(
            method = "tick",
            at = @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/item/ItemEntity;"
                                    + "getDeltaMovement()Lnet/minecraft/world/phys/Vec3;"
                    )
    )
    private @NotNull Vec3 slowDown(ItemEntity instance) {
        Vec3 vec3 = instance.getDeltaMovement();
        double dy = 1;
        if (this.getItem().is(ModItems.LEVITATION_POWDER.get())) dy *= -0.005;
        if (this.level().getBlockState(this.blockPosition()).is(ModBlocks.HOLLOW_MAGNET_BLOCK.get())) dy *= 0.2;
        if (this.getItem().is(ModItems.NEGATIVE_MATTER_NUGGET.get()) ||
                this.getItem().is(ModItems.NEGATIVE_MATTER.get()) ||
                this.getItem().is(ModBlocks.NEGATIVE_MATTER_BLOCK.asItem())){
            if (this.position().y <= this.level().getMaxBuildHeight())
                if (vec3.y < 0) dy *= -1;
        }
        return new Vec3(vec3.x, vec3.y * dy, vec3.z);
    }

    @Unique private boolean anvilcraft$needMagnetization = true;

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
            this.setItem(new ItemStack(ModItems.MAGNET_INGOT.get()));
        }
    }

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void voidResistant(CallbackInfo ci) {
//        if (this.getServer() == null) return;
        if (!this.getItem().is(ModItemTags.VOID_RESISTANT)) return;
        if(this.getY() < this.level().getMinBuildHeight() + 1) {
            double dy = (this.level().getMinBuildHeight() - this.getY()) * 0.01;
//            if(this.getDeltaMovement().y > 0) {
                dy += this.getDeltaMovement().y * -0.1;
//            }
            this.addDeltaMovement(new Vec3(0, 0.04 + dy, 0));
        }
//        else if(this.getY() < this.level().getMinBuildHeight() + 1){
//            double dy = -0.01 - this.getDeltaMovement().y;
//            this.addDeltaMovement(new Vec3(0, 0.04 + dy, 0));
//        }
    }

    @Unique private static final Map<Block, Integer> REPAIR_EFFICIENCY = new HashMap<>();

    static {
        REPAIR_EFFICIENCY.put(Blocks.FIRE, 2);
        REPAIR_EFFICIENCY.put(Blocks.SOUL_FIRE, 5);
        REPAIR_EFFICIENCY.put(Blocks.LAVA, 10);
        REPAIR_EFFICIENCY.put(Blocks.LAVA_CAULDRON, 10);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void fireReforging(CallbackInfo ci) {
        ItemStack item = this.getItem();
        if (!item.isEmpty() && item.getItem() instanceof IFireReforging) {
            if (!this.getItem().isDamaged()) return;
            Block block = this.level().getBlockState(this.blockPosition()).getBlock();
            if (REPAIR_EFFICIENCY.containsKey(block)) {
                this.getItem().setDamageValue(this.getItem().getDamageValue() - REPAIR_EFFICIENCY.get(block));
            }
        }
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void explosionProof(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!this.getItem().isEmpty()
                && this.getItem().is(ModItemTags.EXPLOSION_PROOF)
                && source.is(DamageTypeTags.IS_EXPLOSION)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getBlockPosBelowThatAffectsMyMovement", at = @At("HEAD"), cancellable = true)
    private void slidingRailProgress(CallbackInfoReturnable<BlockPos> cir) {
        BlockState blockState = this.level().getBlockState(this.getOnPos(0.1f));
        if (blockState.is(ModBlocks.SLIDING_RAIL) || blockState.is(ModBlocks.SLIDING_RAIL_STOP)) {
            cir.setReturnValue(this.getOnPos(0.1f));
        }
    }
}
