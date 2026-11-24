package dev.dubhe.anvilcraft.entity;

import dev.dubhe.anvilcraft.api.injection.entity.IItemEntityExtension;
import dev.dubhe.anvilcraft.init.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
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
        this.setSupport(blockPos, level.getBlockState(blockPos));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.rotatingState.startIfStopped(this.tickCount);
            return;
        }
        BlockState currentState = this.level().getBlockState(this.blockPos);
        if (currentState.is(Blocks.MOVING_PISTON)
            && this.level().getBlockEntity(this.blockPos) instanceof PistonMovingBlockEntity piston
            && !piston.isSourcePiston()) {
            currentState = piston.getMovedState();
        }
        if (!currentState.is(this.blockState.getBlock())
            && (!currentState.is(BlockTags.CAULDRONS) || !this.blockState.is(BlockTags.CAULDRONS))) {
            this.discard();
            return;
        }
        this.level()
            .getEntities(EntityType.ITEM, this.getAttractionArea(), IItemEntityExtension::anvilcraft$isAdsorbable)
            .forEach(item -> item.anvilcraft$attachToMagnetizedNode(this));
    }

    public AABB getAttractionArea() {
        return new AABB(this.blockPos).inflate(0.01);
    }

    /** 保留节点相对附着方块的偏移，并同步活塞移动后的附着位置。 */
    public void moveWithPiston(Direction direction) {
        this.setSupport(this.blockPos.relative(direction), this.blockState);
        this.setPos(this.position().relative(direction, 1));
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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_BLOCK_POS, BlockPos.ZERO).define(DATA_BLOCK_STATE, Blocks.AIR.defaultBlockState());
    }

    private void setSupport(BlockPos pos, BlockState state) {
        this.entityData.set(DATA_BLOCK_POS, pos);
        this.entityData.set(DATA_BLOCK_STATE, state);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_BLOCK_POS.equals(key)) this.blockPos = this.entityData.get(DATA_BLOCK_POS);
        if (DATA_BLOCK_STATE.equals(key)) this.blockState = this.entityData.get(DATA_BLOCK_STATE);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        this.setSupport(NbtUtils.readBlockPos(compoundTag, "BlockPos").orElse(BlockPos.ZERO),
            NbtUtils.readBlockState(this.level().holderLookup(Registries.BLOCK), compoundTag.getCompound("BlockState")));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        compoundTag.put("BlockState", NbtUtils.writeBlockState(this.blockState));
        compoundTag.put("BlockPos", NbtUtils.writeBlockPos(this.blockPos));
    }

    @Override
    protected AABB makeBoundingBox() {
        return EntityDimensions.scalable(0.25f, 0.25f).makeBoundingBox(this.position());
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }
}
