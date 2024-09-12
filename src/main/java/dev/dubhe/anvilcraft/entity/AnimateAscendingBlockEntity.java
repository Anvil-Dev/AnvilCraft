package dev.dubhe.anvilcraft.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class AnimateAscendingBlockEntity extends Entity {

    private BlockState blockState;

    private static final EntityDataAccessor<BlockPos> DATA_START_POS = SynchedEntityData.defineId(
            AnimateAscendingBlockEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<BlockPos> DATA_END_POS = SynchedEntityData.defineId(
            AnimateAscendingBlockEntity.class, EntityDataSerializers.BLOCK_POS);

    /**
     *
     */
    public AnimateAscendingBlockEntity(
            EntityType<? extends AnimateAscendingBlockEntity> entityType, Level level) {
        super(entityType, level);
        this.blockState = Blocks.SAND.defaultBlockState();
        this.noPhysics = true;
    }

    private AnimateAscendingBlockEntity(
            Level level, double x, double y, double z, BlockState state, BlockPos endPos) {
        this(ModEntities.ASCENDING_BLOCK_ENTITY.get(), level);
        this.blockState = state;
        this.blocksBuilding = true;
        this.setPos(x, y, z);
        this.setDeltaMovement(Vec3.ZERO);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.setStartPos(this.blockPosition());
        this.setEndPos(endPos);
    }

    public void setStartPos(BlockPos startPos) {
        this.entityData.set(DATA_START_POS, startPos);
    }

    public BlockPos getStartPos() {
        return this.entityData.get(DATA_START_POS);
    }

    public void setEndPos(BlockPos startPos) {
        this.entityData.set(DATA_END_POS, startPos);
    }

    public BlockPos getEndPos() {
        return this.entityData.get(DATA_END_POS);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(DATA_START_POS, BlockPos.ZERO).define(DATA_END_POS, BlockPos.ZERO);
    }

    @Override
    public void tick() {
        if (this.blockState.isAir()) {
            this.discard();
            return;
        }
        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.4, 0.0));
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.level().isClientSide) return;
        BlockPos current = this.blockPosition();
        BlockPos eyePos = BlockPos.containing(this.getEyePosition());
        BlockPos up = current.above();
        if (!this.level().getBlockState(up).isAir()
                || current.getY() >= getEndPos().getY()
                || eyePos.getY() >= getEndPos().getY()) {
            this.discard();
        }
        this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag compound) {}

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag compound) {}

    /**
     * 动画
     */
    public static void animate(
            Level level, @NotNull BlockPos startPos, @NotNull BlockState blockState, BlockPos endPos) {
        if (!AnvilCraft.config.displayAnvilAnimation) return;
        AnimateAscendingBlockEntity entity = new AnimateAscendingBlockEntity(
                level,
                startPos.getX() + 0.5,
                startPos.getY(),
                startPos.getZ() + 0.5,
                blockState.hasProperty(BlockStateProperties.WATERLOGGED)
                        ? blockState.setValue(BlockStateProperties.WATERLOGGED, false)
                        : blockState,
                endPos);
        level.addFreshEntity(entity);
    }

    @Override
    public void recreateFromPacket(@NotNull ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        this.blockState = Block.stateById(packet.getData());
        this.blocksBuilding = true;
        double d = packet.getX();
        double e = packet.getY();
        double f = packet.getZ();
        this.setPos(d, e, f);
        this.setStartPos(this.blockPosition());
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        return EntityDimensions.fixed(1, 1);
    }
}
