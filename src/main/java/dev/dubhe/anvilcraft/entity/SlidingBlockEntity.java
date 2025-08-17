package dev.dubhe.anvilcraft.entity;

import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.contraptions.mounted.MountedContraption;
import dev.dubhe.anvilcraft.api.sliding.SlidingBlockSection;
import dev.dubhe.anvilcraft.block.sliding.ISlidingRail;
import dev.dubhe.anvilcraft.init.ModEntities;
import dev.dubhe.anvilcraft.network.SlidingEntitySyncPacket;
import dev.dubhe.anvilcraft.util.Util;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.CrashReportCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@ParametersAreNonnullByDefault
public class SlidingBlockEntity extends Entity {
    @Getter
    @Setter
    private SlidingBlockSection section;
    @Getter
    private Direction moveDirection;
    public static final double DEFAULT_MOVEMENT = 0.35;
    protected static final EntityDataAccessor<BlockPos> DATA_START_POS = SynchedEntityData.defineId(
        SlidingBlockEntity.class, EntityDataSerializers.BLOCK_POS);
    private int time = 0;

    public SlidingBlockEntity(EntityType<? extends SlidingBlockEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.refreshDimensions();
        this.section = SlidingBlockSection.EMPTY;
    }

    public SlidingBlockEntity(
        Level level, BlockPos pos, Direction moveDirection,
        Iterable<Triple<BlockPos, BlockState, Optional<CompoundTag>>> infos
    ) {
        this(ModEntities.SLIDING_BLOCK.get(), level);
        this.blocksBuilding = true;
        this.section = SlidingBlockSection.create(pos, infos);
        Vec3 bottomCenter = pos.getBottomCenter();
        this.setPos(bottomCenter);
        this.setDeltaMovement(Vec3.ZERO);
        this.xo = bottomCenter.x;
        this.yo = bottomCenter.y;
        this.zo = bottomCenter.z;
        this.setStartPos(pos);
        this.moveDirection = moveDirection;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static SlidingBlockEntity slid(
        Level level, BlockPos pos, Direction moveDirection,
        Iterable<Triple<BlockPos, BlockState, Optional<CompoundTag>>> infos
    ) {
        SlidingBlockEntity entity = new SlidingBlockEntity(level, pos, moveDirection, infos);
        for (var entry : infos) {
            level.setBlock(pos, level.getFluidState(entry.getLeft()).createLegacyBlock(), 3);
        }
        level.addFreshEntity(entity);
        Util.castSafely(level, ServerLevel.class)
            .ifPresent(it -> PacketDistributor.sendToPlayersTrackingChunk(
                it, new ChunkPos(pos),
                new SlidingEntitySyncPacket(entity.getId(), entity.section.blocks(), entity.moveDirection)
            ));
        return entity;
    }

    @Override
    public void tick() {
        this.setNoGravity(true);
        if (this.section.isEmpty()) {
            this.discard();
            return;
        }
        this.time++;
        BlockPos pos = this.blockPosition();
        BlockPos belowPos = pos.below();
        BlockState belowState = this.level().getBlockState(belowPos);
        if (belowState.getBlock() instanceof ISlidingRail slidingRail && !this.level().isClientSide) {
            slidingRail.onSlidingAbove(this.level(), belowPos, belowState, this);
        }
        Direction.Axis horizontalAnother = this.moveDirection.getClockWise().getAxis();
        this.setPos(this.position().with(horizontalAnother, Math.ceil(this.position().get(horizontalAnother)) - 0.5));

        if (this.level().isOutsideBuildHeight(pos)) {
            this.stop();
        } else if (this.checkCanMove()) {
            this.setDeltaMovement(Vec3.ZERO.relative(this.moveDirection, DEFAULT_MOVEMENT));
        } else if (!this.level().isClientSide && !this.isRemoved()) {
            this.setDeltaMovement(Vec3.ZERO);
            this.stop();
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    @Override
    public void move(MoverType type, Vec3 motion) {
        super.move(type, motion);
//        if (motion.x == 0 && motion.y == 0 && motion.z == 0) return;
//        List<Entity> list = this.level().getEntities(
//            this,
//            this.section.getBoundsOnSide(Direction.UP)
//                .expandTowards(0, 1, 0)
//                .move(this.blockPosition()),
//            EntitySelector.pushableBy(this)
//        );
//        if (list.isEmpty()) return;
//        for (Entity entity : list) {
//            if (entity instanceof SlidingBlockEntity) continue;
//            Vec3 collide = this.section.findCollide(this.position(), entity.getBoundingBox());
//            entity.setDeltaMovement(
//                entity.getDeltaMovement().x + collide.x() + DEFAULT_MOVEMENT * 2.8,
//                entity.getDeltaMovement().y < 0 ? 0 : entity.getDeltaMovement().y,
//                entity.getDeltaMovement().z + collide.z() + DEFAULT_MOVEMENT * 2.8
//            );
//        }
    }

    protected boolean checkCanMove() {
        if (!(this.level().getBlockState(this.blockPosition().below()).getBlock() instanceof ISlidingRail)) return false;
        if (this.time > 1 && this.getDeltaMovement().equals(Vec3.ZERO)) return false;
        for (Vec3i pos : this.section.getWallsOnSide(this.moveDirection)) {
            BlockPos checking = this.blockPosition().offset(pos);
            if (!this.level().getBlockState(checking).isAir()) return false;
            if (!this.level().getBlockState(checking.relative(this.moveDirection)).isAir()) return false;
        }
        return true;
    }

    public int getBlockCount() {
        return this.section.size();
    }

    public void setMoveDirection(Direction moveDirection) {
        this.moveDirection = moveDirection;
        if (this.level().isClientSide) return;
        PacketDistributor.sendToPlayersTrackingChunk(
            (ServerLevel) this.level(), new ChunkPos(this.blockPosition()),
            new SlidingEntitySyncPacket(this.getId(), this.section.blocks(), moveDirection)
        );
    }

    public void stop() {
        if (this.level().isClientSide) return;
        this.section.setBlock(this.level(), this.blockPosition(), this);
        this.discard();
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    public void setStartPos(BlockPos startPos) {
        this.entityData.set(DATA_START_POS, startPos);
    }

    public BlockPos getStartPos() {
        return this.entityData.get(DATA_START_POS);
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_START_POS, BlockPos.ZERO);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        SlidingBlockSection.CODEC.encode(this.section, NbtOps.INSTANCE, new CompoundTag())
            .ifSuccess(tag -> compound.put("SlidingBlocks", tag));
        Direction.CODEC.encode(this.moveDirection, NbtOps.INSTANCE, EndTag.INSTANCE)
            .ifSuccess(tag -> compound.put("MovingDirection", tag));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        SlidingBlockSection.CODEC.decode(NbtOps.INSTANCE, compound.getCompound("SlidingBlocks"))
            .ifSuccess(data -> this.section = data.getFirst());
        Direction.CODEC.decode(NbtOps.INSTANCE, Objects.requireNonNull(compound.get("MovingDirection")))
            .ifSuccess(data -> this.moveDirection = data.getFirst());
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public void fillCrashReportCategory(CrashReportCategory category) {
        super.fillCrashReportCategory(category);
        category.setDetail("Sliding Blocks", this.section.toString());
    }

    @Override
    public boolean onlyOpCanSetNbt() {
        return true;
    }

    @Override
    public Vec3 getLookAngle() {
        return Vec3.ZERO.relative(this.moveDirection, 1);
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean canCollideWith(Entity e) {
        return e instanceof SlidingBlockEntity;
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        return EntityDimensions.scalable(0.98f, 0.98f);
    }
}
