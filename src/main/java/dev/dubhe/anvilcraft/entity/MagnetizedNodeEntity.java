package dev.dubhe.anvilcraft.entity;

import dev.dubhe.anvilcraft.api.injection.entity.IItemEntityExtension;
import dev.dubhe.anvilcraft.init.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class MagnetizedNodeEntity extends Entity {
    private static final EntityDataAccessor<BlockPos> DATA_BLOCK_POS =
        SynchedEntityData.defineId(MagnetizedNodeEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<BlockState> DATA_BLOCK_STATE =
        SynchedEntityData.defineId(MagnetizedNodeEntity.class, EntityDataSerializers.BLOCK_STATE);

    public BlockPos blockPos = BlockPos.ZERO;
    private BlockState blockState = Blocks.AIR.defaultBlockState();

    public AnimationState rotatingState = new AnimationState();

    public MagnetizedNodeEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setInvulnerable(true);
    }

    public MagnetizedNodeEntity(Level level, Vec3 pos, BlockPos blockPos) {
        super(ModEntities.MAGNETIZED_NODE.get(), level);
        this.setPos(pos);
        this.xo = pos.x;
        this.yo = pos.y;
        this.zo = pos.z;
        this.noPhysics = true;
        this.setInvulnerable(true);
        this.blockPos = blockPos;
        this.blockState = level.getBlockState(blockPos);
    }

    @Override
    public void tick() {
        if (this.level().isClientSide()) {
            this.rotatingState.startIfStopped(this.tickCount);
        }
        super.tick();
        if (!this.level().isClientSide() && !this.level().getBlockState(this.blockPos).is(this.blockState.getBlock())) {
            BlockState currentState = this.level().getBlockState(this.blockPos);
            if (!currentState.is(this.blockState.getBlock())
                && (!currentState.is(BlockTags.CAULDRONS) || !this.blockState.is(BlockTags.CAULDRONS))) {
                this.discard();
            }
        }
        AABB aabb = new AABB(this.blockPos.getX() - 0.01,
                             this.blockPos.getY() - 0.01,
                             this.blockPos.getZ() - 0.01,
                             this.blockPos.getX() + 1.01,
                             this.blockPos.getY() + 1.01,
                             this.blockPos.getZ() + 1.01
        );
        this.level()
            .getEntities(EntityType.ITEM, aabb, IItemEntityExtension::anvilcraft$isAdsorbable)
            .forEach(entity -> {
                entity.teleportTo(this.position().x, this.position().y, this.position().z);
                entity.setDeltaMovement(Vec3.ZERO);
            });
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder
            .define(MagnetizedNodeEntity.DATA_BLOCK_POS, BlockPos.ZERO)
            .define(MagnetizedNodeEntity.DATA_BLOCK_STATE, Blocks.AIR.defaultBlockState());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput compoundTag) {
        compoundTag.read("block_pos", BlockPos.CODEC).ifPresent(it -> this.blockPos = it);
        compoundTag.read("block_state", BlockState.CODEC).ifPresent(it -> this.blockState = it);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput compoundTag) {
        compoundTag.store("block_state", BlockState.CODEC, this.blockState);
        compoundTag.store("block_pos", BlockPos.CODEC, this.blockPos);
    }

    @Override
    protected AABB makeBoundingBox(Vec3 pos) {
        return EntityDimensions.scalable(0.25F, 0.25F).makeBoundingBox(pos);
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }
}
