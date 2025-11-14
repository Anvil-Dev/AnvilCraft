package dev.dubhe.anvilcraft.entity;

import dev.dubhe.anvilcraft.init.entity.ModEntities;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class CauldronOutletEntity extends Entity {
    private static final EntityDataAccessor<BlockPos> DATA_CAULDRON_POS = SynchedEntityData.defineId(
        CauldronOutletEntity.class,
        EntityDataSerializers.BLOCK_POS
    );
    private static final EntityDataAccessor<Direction> DATA_ATTACHED_DIRECTION = SynchedEntityData.defineId(
        CauldronOutletEntity.class,
        EntityDataSerializers.DIRECTION
    );
    private static final EntityDataAccessor<BlockState> DATA_CAULDRON_STATE = SynchedEntityData.defineId(
        CauldronOutletEntity.class,
        EntityDataSerializers.BLOCK_STATE
    );

    @Getter
    private BlockPos cauldronPos = BlockPos.ZERO;
    private Direction attachedDirection = Direction.UP;
    private BlockState cauldronState;

    public CauldronOutletEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setInvulnerable(true);
    }

    public CauldronOutletEntity(Level level, Vec3 pos, BlockPos cauldronPos, Direction attachedDirection) {
        super(ModEntities.CAULDRON_OUTLET.get(), level);
        this.setPos(pos);
        this.xo = pos.x;
        this.yo = pos.y;
        this.zo = pos.z;
        this.noPhysics = true;
        this.setInvulnerable(true);
        this.cauldronPos = cauldronPos;
        this.attachedDirection = attachedDirection;
        this.cauldronState = level.getBlockState(cauldronPos);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && !this.level().getBlockState(this.cauldronPos).is(this.cauldronState.getBlock())) {
            this.kill();
            return;
        }
        AABB aabb = new AABB(
            cauldronPos.getX() - 0.01,
            cauldronPos.getY() - 0.01,
            cauldronPos.getZ() - 0.01,
            cauldronPos.getX() + 1.01,
            cauldronPos.getY() + 1.01,
            cauldronPos.getZ() + 1.01
        );
        level().getEntities(EntityType.ITEM, aabb, entity -> !entity.anvilcraft$isAdsorbable()).forEach(entity -> {
            // 将物品从口的方向推出，先移动到口外一点，再给0.1动量
            Vec3 ejectPos = this.position()
                .add(attachedDirection.getStepX() * 0.25, attachedDirection.getStepY() * 0.25, attachedDirection.getStepZ() * 0.25);
            Vec3 motion = new Vec3(
                attachedDirection.getStepX() * 0.1,
                attachedDirection.getStepY() * 0.1,
                attachedDirection.getStepZ() * 0.1
            );
            entity.moveTo(ejectPos);
            entity.setDeltaMovement(motion);
            // 移除铁砧加工标记，防止被其他炼药锅口连续吸走
            entity.anvilcraft$setIsAdsorbable(true);
            if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
                serverLevel.getChunkSource().broadcast(entity, new ClientboundTeleportEntityPacket(entity));
                serverLevel.getChunkSource().broadcast(entity, new ClientboundSetEntityMotionPacket(entity));
            }
        });
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_CAULDRON_POS, BlockPos.ZERO)
            .define(DATA_ATTACHED_DIRECTION, Direction.UP)
            .define(DATA_CAULDRON_STATE, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        this.cauldronPos = NbtUtils.readBlockPos(compoundTag, "CauldronPos").orElse(BlockPos.ZERO);
        this.attachedDirection = Direction.from3DDataValue(compoundTag.getInt("AttachedDirection"));
        this.cauldronState = NbtUtils.readBlockState(this.level().holderLookup(Registries.BLOCK), compoundTag.getCompound("CauldronState"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        compoundTag.put("CauldronState", NbtUtils.writeBlockState(this.cauldronState));
        compoundTag.put("CauldronPos", NbtUtils.writeBlockPos(this.cauldronPos));
        compoundTag.putInt("AttachedDirection", this.attachedDirection.get3DDataValue());
    }

    @Override
    protected AABB makeBoundingBox() {
        return EntityDimensions.scalable(0.375f, 0.25f).makeBoundingBox(this.position());
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }
}