package dev.dubhe.anvilcraft.mixin.forge;

import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.api.event.anvil.AnvilEvent;
import dev.dubhe.anvilcraft.init.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

@Mixin(FallingBlockEntity.class)
abstract class FallingBlockEntityMixin extends Entity {
    @Shadow
    public BlockState blockState;

    @Shadow
    public boolean cancelDrop;

    @Unique private float anvilcraft$fallDistance;

    public FallingBlockEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(
        method = "tick",
        at =
        @At(
            value = "INVOKE",
            ordinal = 0,
            target =
                "Lnet/minecraft/world/entity/item/FallingBlockEntity;level()Lnet/minecraft/world/level/Level;")
    )
    private void anvilPerFallOnGround(CallbackInfo ci) {
        if (this.level().isClientSide()) return;
        if (this.onGround()) return;
        this.anvilcraft$fallDistance = this.fallDistance;
    }

    @SuppressWarnings("UnreachableCode")
    @Inject(
        method = "tick",
        at =
        @At(
            value = "INVOKE",
            target =
                "Lnet/minecraft/world/level/block/Fallable;onLand(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/item/FallingBlockEntity;)V")
    )
    private void anvilFallOnGround(CallbackInfo ci, @Local BlockPos blockPos) {
        if (this.level().isClientSide()) return;
        if (!this.blockState.is(BlockTags.ANVIL)) return;
        FallingBlockEntity entity = (FallingBlockEntity) (Object) this;
        AnvilEvent.OnLand event = new AnvilEvent.OnLand(this.level(), blockPos, entity, this.anvilcraft$fallDistance);
        NeoForge.EVENT_BUS.post(event);
        if (event.isAnvilDamage()) {
            BlockState state = this.blockState.is(ModBlocks.ROYAL_ANVIL.get())
                    ? this.blockState
                    : AnvilBlock.damage(this.blockState);
            if (state != null) this.level().setBlockAndUpdate(blockPos, state);
            else {
                this.level().setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState());
                if (!this.isSilent()) this.level().levelEvent(1029, this.getOnPos(), 0);
                this.cancelDrop = true;
            }
        }
    }

    @SuppressWarnings("UnreachableCode")
    @Inject(
        method = "causeFallDamage",
        at =
        @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;"
                + "Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;")
    )
    private void anvilHurtEntity(
            float pFallDistance,
            float pMultiplier,
            DamageSource pSource,
            CallbackInfoReturnable<Boolean> cir,
            @Local Predicate<Entity> predicate,
            @Local(ordinal = 2) float f) {
        FallingBlockEntity anvil = (FallingBlockEntity) (Object) this;
        Level level = this.level();
        List<Entity> entities = level.getEntities(this, this.getBoundingBox(), predicate);
        for (Entity entity : entities) {
            NeoForge.EVENT_BUS.post(new AnvilEvent.HurtEntity(anvil, this.getOnPos(), level, entity, f));
        }
    }
}
