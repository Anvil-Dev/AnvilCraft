package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.block.HollowMagnetBlock;
import dev.dubhe.anvilcraft.block.ItemCollectorBlock;
import dev.dubhe.anvilcraft.block.entity.ItemCollectorBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.util.AdsorbableItemEntity;
import dev.dubhe.anvilcraft.util.IDiscardableItemEntity;
import dev.dubhe.anvilcraft.util.MergeCooldownItemEntity;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.EventHooks;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static dev.dubhe.anvilcraft.block.entity.ItemCollectorBlockEntity.PoachingCollectorMap;

@Mixin(ItemEntity.class)
abstract class ItemEntityMixin extends Entity implements MergeCooldownItemEntity, IDiscardableItemEntity, AdsorbableItemEntity {
    @Shadow
    public abstract ItemStack getItem();

    @Shadow
    @Nullable
    public abstract Entity getOwner();

    @Shadow
    public abstract void setItem(ItemStack stack);

    @Shadow
    protected abstract boolean isMergable();

    @Shadow
    protected abstract void mergeWithNeighbours();

    @Shadow
    private int pickupDelay;

    @Shadow
    private int age;

    @Shadow
    public int lifespan;

    @Unique
    public int anvilCraft$mergeCooldown = 0;

    @Unique
    public boolean anvilCraft$isAdsorbable = true;

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
        if (this.getItem().is(ModItemTags.LEVITATIONALS)) dy *= -0.005;
        if (this.level().getBlockState(this.blockPosition()).is(ModBlocks.HOLLOW_MAGNET_BLOCK)) dy *= 0.2;
        if (this.getItem().is(ModItems.NEGATIVE_MATTER_NUGGET)
            || this.getItem().is(ModItems.NEGATIVE_MATTER)
            || this.getItem().is(ModBlocks.NEGATIVE_MATTER_BLOCK.asItem())) {
            if (this.position().y <= this.level().getMaxBuildHeight())
                if (vec3.y < 0) dy *= -1;
        }
        return new Vec3(vec3.x, vec3.y * dy, vec3.z);
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
            this.setItem(new ItemStack(ModItems.MAGNET_INGOT.get()));
        }
    }

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void voidResistant(CallbackInfo ci) {
        if (!this.getItem().is(ModItemTags.VOID_RESISTANT) && !this.getItem().has(ModComponents.ETERNAL)) return;
        if (this.getY() < this.level().getMinBuildHeight() + 5) {
            double dy = (this.level().getMinBuildHeight() + 4 - this.getY()) * 0.01;
            dy += this.getDeltaMovement().y * -0.1;
            this.addDeltaMovement(new Vec3(0, 0.04 + dy, 0));
        }
    }

    @Unique
    private static final Map<Block, Integer> REPAIR_EFFICIENCY = new HashMap<>();

    static {
        REPAIR_EFFICIENCY.put(Blocks.FIRE, 2);
        REPAIR_EFFICIENCY.put(Blocks.SOUL_FIRE, 5);
        REPAIR_EFFICIENCY.put(Blocks.LAVA, 10);
        REPAIR_EFFICIENCY.put(Blocks.LAVA_CAULDRON, 10);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void fireReforging(CallbackInfo ci) {
        ItemStack item = this.getItem();
        if (!item.isEmpty() && item.get(ModComponents.FIRE_REFORGING) != null) {
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

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void eternalProof(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (this.getItem().has(ModComponents.ETERNAL)
            && (source.is(DamageTypeTags.IS_EXPLOSION)
            || source.is(DamageTypeTags.IS_FIRE)
            || source.is(DamageTypes.CACTUS)
            || source.is(DamageTypes.FELL_OUT_OF_WORLD))) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getBlockPosBelowThatAffectsMyMovement", at = @At("HEAD"), cancellable = true)
    private void slidingRailProgress(CallbackInfoReturnable<BlockPos> cir) {
        BlockState blockState = this.level().getBlockState(this.getOnPos(0.1f));
        if (blockState.is(ModBlockTags.SLIDING_RAILS)) {
            cir.setReturnValue(this.getOnPos(0.1f));
        }
    }

    // 以下是中子锭运动相关mixin

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$neutroniumTick(CallbackInfo ci) {
        ItemEntity thiz = Util.cast(this);
        ItemStack item = this.getItem();
        if (!item.is(ModItems.NEUTRONIUM_INGOT)) return;
        if (item.onEntityItemUpdate(thiz)) {
            ci.cancel();
            return;
        }

        this.level().getProfiler().push("entityBaseTick");

        this.inBlockState = null;
        if (this.isPassenger() && Objects.requireNonNull(this.getVehicle()).isRemoved()) {
            this.stopRiding();
        }
        if (this.boardingCooldown > 0) {
            this.boardingCooldown--;
        }
        this.walkDistO = this.walkDist;
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
        this.handlePortal();
        this.wasInPowderSnow = this.isInPowderSnow;
        this.isInPowderSnow = false;
        this.checkBelowWorld();

        this.level().getProfiler().pop();

        if (this.pickupDelay > 0 && this.pickupDelay != 32767) {
            --this.pickupDelay;
        }

        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        Vec3 vec3 = this.getDeltaMovement();
        this.applyGravity();
        this.noPhysics = false;
        if (!this.onGround() || this.getDeltaMovement().horizontalDistanceSqr() > (double) 1.0E-5F || (this.tickCount + this.getId()) % 4 == 0) {
            this.anvilCraft$neutroniumMove(MoverType.SELF, this.getDeltaMovement());
            float f = 0.98F;
            if (this.onGround()) {
                BlockPos groundPos = this.getBlockPosBelowThatAffectsMyMovement();
                f = this.level().getBlockState(groundPos).getFriction(this.level(), groundPos, this) * 0.98F;
            }
            this.setDeltaMovement(this.getDeltaMovement().multiply(f, 0.98, f));
            if (this.onGround()) {
                Vec3 vec31 = this.getDeltaMovement();
                if (vec31.y < (double) 0.0F) {
                    this.setDeltaMovement(vec31.multiply(1.0, -0.5, 1.0));
                }
            }
        }
        boolean flag = Mth.floor(this.xo) != Mth.floor(this.getX()) || Mth.floor(this.yo) != Mth.floor(this.getY()) || Mth.floor(this.zo) != Mth.floor(this.getZ());
        int i = flag ? 2 : 40;
        if (this.tickCount % i == 0 && !this.level().isClientSide && this.isMergable()) {
            this.mergeWithNeighbours();
        }
        if (this.age != -32768) {
            ++this.age;
        }
        if (!this.level().isClientSide) {
            double d0 = this.getDeltaMovement().subtract(vec3).lengthSqr();
            if (d0 > 0.01) {
                this.hasImpulse = true;
            }
        }
        item = this.getItem();
        if (!this.level().isClientSide && this.age >= this.lifespan) {
            this.lifespan = Mth.clamp(this.lifespan + EventHooks.onItemExpire(thiz), 0, 32766);
            if (this.age >= this.lifespan) {
                this.discard();
            }
        }
        if (item.isEmpty() && !this.isRemoved()) {
            this.discard();
        }
        ci.cancel();
    }

    @Override
    @NotNull
    public PushReaction getPistonPushReaction() {
        if (this.getItem().is(ModItems.NEUTRONIUM_INGOT)) return PushReaction.IGNORE;
        return super.getPistonPushReaction();
    }

    @SuppressWarnings({"unused", "SameParameterValue", "SuspiciousNameCombination", "deprecation"})
    @Unique
    private void anvilCraft$neutroniumMove(MoverType moverType, Vec3 motion) {

        this.level().getProfiler().push("move");
        //代替原版move方法中的collide调用
        AABB box = this.getBoundingBox().expandTowards(motion);
        int x1 = Mth.floor(box.minX - 1.0E-7) - 1;
        int x2 = Mth.floor(box.maxX + 1.0E-7) + 1;
        int y1 = Mth.floor(box.minY - 1.0E-7) - 1;
        int y2 = Mth.floor(box.maxY + 1.0E-7) + 1;
        int z1 = Mth.floor(box.minZ - 1.0E-7) - 1;
        int z2 = Mth.floor(box.maxZ + 1.0E-7) + 1;
        List<VoxelShape> shapes = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = x1; x < x2; x++) {
            for (int y = y1; y < y2; y++) {
                for (int z = z1; z < z2; z++) {
                    pos.set(x, y, z);
                    BlockState blockState = this.level().getBlockState(pos);
                    //只检测带有特定标签的方块的碰撞
                    if (blockState.is(ModBlockTags.NEUTRONIUM_CANNOT_PASS_THROUGH)) {
                        shapes.add(blockState.getCollisionShape(this.level(), pos).move(x, y, z));
                    }
                }
            }
        }
        Vec3 motion2 = Entity.collideWithShapes(motion, this.getBoundingBox(), shapes);
        if (motion2.lengthSqr() > 1.0E-7) {
            this.setPos(this.getX() + motion2.x, this.getY() + motion2.y, this.getZ() + motion2.z);
        }

        this.level().getProfiler().popPush("rest");
        // 处理一些原版move方法中，对ItemEntity有必要的后续操作
        boolean collisionX = !Mth.equal(motion2.x, motion.x);
        boolean collisionZ = !Mth.equal(motion2.z, motion.z);
        this.horizontalCollision = collisionX || collisionZ;
        this.verticalCollision = motion2.y != motion.y;
        this.verticalCollisionBelow = this.verticalCollision && motion.y < (double) 0.0F;
        this.minorHorizontalCollision = false;
        this.setOnGroundWithMovement(this.verticalCollisionBelow, motion2);
        BlockPos blockpos = this.getOnPosLegacy();
        BlockState blockState = this.level().getBlockState(blockpos);
        if (this.horizontalCollision) {
            Vec3 vec31 = this.getDeltaMovement();
            this.setDeltaMovement(collisionX ? 0.0 : vec31.x, vec31.y, collisionZ ? 0.0 : vec31.z);
        }
        Block block = blockState.getBlock();
        if (motion2.y != motion.y) {
            block.updateEntityAfterFallOn(this.level(), this);
        }
        if (this.onGround()) {
            block.stepOn(this.level(), blockpos, blockState, this);
        }
        this.tryCheckInsideBlocks();

        this.level().getProfiler().pop();
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;isMergable()Z"))
    public boolean preventMerge(ItemEntity instance, Operation<Boolean> original) {
        if (!original.call(instance)) return false;
        if (anvilCraft$mergeCooldown <= 0) return true;
        anvilCraft$mergeCooldown--;
        return false;
    }

    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Override
    public void setMergeCooldown(int cooldown) {
        anvilCraft$mergeCooldown = cooldown;
    }

    @Inject(
        method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V",
        at = @At("TAIL")
    )
    public void itemCollectorPoach0(CallbackInfo ci) {
        this.anvilcraft$poach(ci);
    }

    @Inject(
        method = "<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;DDD)V",
        at = @At("TAIL")
    )
    public void itemCollectorPoach1(CallbackInfo ci) {
        this.anvilcraft$poach(ci);
    }

    @Inject(
        method = "<init>(Lnet/minecraft/world/entity/item/ItemEntity;)V",
        at = @At("TAIL")
    )
    public void itemCollectorPoach2(CallbackInfo ci) {
        this.anvilcraft$poach(ci);
    }

    @Unique
    public boolean anvilcraft$discarded = false;

    @Unique
    private void anvilcraft$poach(CallbackInfo ci) {
        Level level = this.level();
        if (level.isClientSide) return;
        Map<ChunkPos, List<ItemCollectorBlockEntity>> map = PoachingCollectorMap.get(level);
        if (map == null) return;
        ChunkPos chunkPos = this.chunkPosition();
        List<ItemCollectorBlockEntity> list = map.get(chunkPos);
        if (list == null || list.isEmpty()) return;
        ItemStack itemStack = this.getItem().copy();
        boolean flag = false;
        for (ItemCollectorBlockEntity collector : list) {
            if (collector.isGridWorking()
                && !collector.getBlockState().getValue(ItemCollectorBlock.POWERED)
                && collector.shape().contains(this.position())
                && !collector.isRemoved()) {
                int slotIndex = 0;
                while (!itemStack.isEmpty() && slotIndex < 9) {
                    itemStack = collector.getItemHandler().insertItem(slotIndex++, itemStack, false);
                }
                flag = true;
                if (itemStack.isEmpty()) break;
            }
        }
        if (!itemStack.isEmpty()) {
            this.setItem(itemStack);
        } else if (flag) {
            this.remove(Entity.RemovalReason.DISCARDED);
            this.discard();
            anvilcraft$discarded = true;
        }
    }

    @Override
    public boolean anvilcraft$getDiscarded() {
        return anvilcraft$discarded;
    }

    @Override
    public void anvilcraft$setIsAdsorbable(boolean value) {
        this.anvilCraft$isAdsorbable = value;
    }

    @Override
    public boolean anvilcraft$isAdsorbable() {
        return this.anvilCraft$isAdsorbable;
    }
}
