package dev.dubhe.anvilcraft.entity;

import dev.dubhe.anvilcraft.init.ModEntities;
import dev.dubhe.anvilcraft.util.AdsorbableItemEntity;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

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
            rotatingState.startIfStopped(this.tickCount);
        }
        super.tick();
        if (!this.level().isClientSide && !this.level().getBlockState(this.blockPos).is(this.blockState.getBlock())) {
            BlockState currentState = this.level().getBlockState(this.blockPos);
            if (!currentState.is(this.blockState.getBlock())
                && (!currentState.is(BlockTags.CAULDRONS) || !this.blockState.is(BlockTags.CAULDRONS))) {
                this.kill();
            }
        }
        AABB aabb = new AABB(blockPos.getX() - 0.01,
            blockPos.getY() - 0.01,
            blockPos.getZ() - 0.01,
            blockPos.getX() + 1.01,
            blockPos.getY() + 1.01,
            blockPos.getZ() + 1.01
        );
        level()
            .getEntities(EntityType.ITEM, aabb, it -> ((AdsorbableItemEntity) it).anvilcraft$isAdsorbable())
            .forEach(entity -> {
                entity.teleportTo(position().x, position().y, position().z);
                entity.setDeltaMovement(Vec3.ZERO);
            });
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_BLOCK_POS, BlockPos.ZERO).define(DATA_BLOCK_STATE, Blocks.AIR.defaultBlockState());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        this.blockPos = NbtUtils.readBlockPos(compoundTag, "BlockPos").orElse(BlockPos.ZERO);
        this.blockState = NbtUtils.readBlockState(this.level().holderLookup(Registries.BLOCK), compoundTag.getCompound("BlockState"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        compoundTag.put("BlockState", NbtUtils.writeBlockState(this.blockState));
        compoundTag.put("BlockPos", NbtUtils.writeBlockPos(this.blockPos));
    }

    @Override
    protected @NotNull AABB makeBoundingBox() {
        return EntityDimensions.scalable(0.25f, 0.25f).makeBoundingBox(this.position());
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }
}
