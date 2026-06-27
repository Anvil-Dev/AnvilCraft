package dev.dubhe.anvilcraft.entity;

import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.init.entity.ModEntities;
import dev.dubhe.anvilcraft.util.AnvilUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

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

    // 标记是否处于活塞推动状态
    private boolean wasMoving = false;
    // 目标位置
    private BlockPos targetPos = null;

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
        this.setCauldronPos(cauldronPos);
        this.setAttachedDirection(attachedDirection);
        this.setCauldronState(level.getBlockState(cauldronPos));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            BlockState currentState = this.level().getBlockState(this.getCauldronPos());

            // 0. 防止滑步
            // 如果有目标位置，在到达之前不要进行新的检测，防止连环推时输出口滑过好几格
            if (this.targetPos != null) {
                BlockState targetState = this.level().getBlockState(this.targetPos);

                if (targetState.is(BlockTags.CAULDRONS)) { // A：目标已经变成了炼药锅 -> 移动结束，落地
                    moveToBlock(this.targetPos, targetState);
                    // 落地瞬间暂不处理物品
                    return;
                } else if (targetState.is(Blocks.MOVING_PISTON)) { // B：目标还是移动活塞 -> 正在动画中，原地等待
                    this.wasMoving = true;
                    return;
                } else { // C：目标变成了空气或其他 -> 追踪丢失 ，进入下面的步骤 3 尝试找回
                    this.targetPos = null;
                }
            }

            // 1. 主动移动检测
            // 检查脚下的方块是不是变成了b36
            if (currentState.is(Blocks.MOVING_PISTON)) {
                BlockEntity be = this.level().getBlockEntity(this.getCauldronPos());
                if (be instanceof PistonMovingBlockEntity pistonBe) {
                    Direction moveDir = getMovementDirection(pistonBe);

                    if (pistonBe.isSourcePiston()) {
                        // A：我是源头。炼药锅正在离我而去 -> 立即追过去
                        BlockPos destPos = this.getCauldronPos().relative(moveDir);
                        manualMove(destPos);
                        return;
                    } else {
                        // B：我是目的地。说明有方块正在推入这里 -> 检查是不是连环推
                        BlockPos nextPos = this.getCauldronPos().relative(moveDir);
                        BlockState nextState = this.level().getBlockState(nextPos);

                        boolean isChainPush = false;
                        if (nextState.is(Blocks.MOVING_PISTON)) {
                            BlockEntity nextBe = this.level().getBlockEntity(nextPos);
                            // 检查链条一致性
                            if (nextBe instanceof PistonMovingBlockEntity nextPistonBe && nextPistonBe.getMovedState()
                                .is(BlockTags.CAULDRONS) && !nextPistonBe.isSourcePiston() && getMovementDirection(nextPistonBe).equals(
                                moveDir)) {
                                isChainPush = true;
                            }
                        }

                        if (isChainPush) {
                            // 确认是连环推 -> 移动到下一格
                            manualMove(nextPos);
                        } else {
                            // 只是普通的被推入，原地等待变回实体
                            this.wasMoving = true;
                        }
                        return;
                    }
                }
                this.wasMoving = true;
                return;
            }

            // 3. 扫描周围的方块
            // 只有当没有锁定目标时才扫描
            if (this.targetPos == null) {
                boolean foundPullingPiston = false;
                for (Direction dir : Direction.values()) {
                    BlockPos neighborPos = this.getCauldronPos().relative(dir);
                    BlockState neighborState = this.level().getBlockState(neighborPos);

                    if (neighborState.is(Blocks.MOVING_PISTON)) {
                        BlockEntity be = this.level().getBlockEntity(neighborPos);
                        if (be instanceof PistonMovingBlockEntity pistonBe && !pistonBe.isSourcePiston()) {
                            Direction moveDir = getMovementDirection(pistonBe);
                            BlockPos originPos = neighborPos.relative(moveDir.getOpposite());

                            if (originPos.equals(this.getCauldronPos())) {
                                // 发现拉取 -> 锁定目标到邻居
                                manualMove(neighborPos);
                                foundPullingPiston = true;
                                break;
                            }
                        }
                    }
                }
                if (foundPullingPiston) return;
            }

            // 4. 确认状态
            if (currentState.is(BlockTags.CAULDRONS)) {
                // 安全：脚下是炼药锅
                this.wasMoving = false;
                this.targetPos = null;

                if (currentState != this.getCauldronState()) {
                    this.setCauldronState(currentState);
                }
                // 继续执行物品逻辑
            } else {
                if (!this.wasMoving && this.targetPos == null) {
                    // 如果不在移动状态且没有目标位置，则销毁实体
                    this.kill();
                }
                return;
            }
        }

        // 物品输出逻辑
        BlockPos cauldronPos = this.getCauldronPos();
        AABB aabb = new AABB(
            cauldronPos.getX() - 0.01,
            cauldronPos.getY() - 0.01,
            cauldronPos.getZ() - 0.01,
            cauldronPos.getX() + 1.01,
            cauldronPos.getY() + 1.01,
            cauldronPos.getZ() + 1.01
        );
        Direction attachedDirection = this.getAttachedDirection();
        // 输出口前方的方块，如果是容器则优先输入进容器，输入不进的部分再作为掉落物输出
        IItemHandler target = null;
        if (!this.level().isClientSide) {
            List<IItemHandler> targets = ItemHandlerUtil.getTargetItemHandlerList(
                cauldronPos.relative(attachedDirection),
                attachedDirection.getOpposite(),
                this.level()
            );
            if (targets != null) {
                for (IItemHandler handler : targets) {
                    if (handler != null) {
                        target = handler;
                        break;
                    }
                }
            }
        }
        final IItemHandler containerTarget = target;
        // 前方没有容器时，若开口被有碰撞的方块堵住则不输出（探测开口处的方块碰撞形状）
        final boolean blocked = containerTarget == null
            && !this.level().isClientSide
            && AnvilUtil.isOutletBlocked(
                this.level(), cauldronPos.relative(attachedDirection), this.position(), attachedDirection);
        // 物品输出仅在服务端处理：通过销毁原实体并生成新实体来输出，避免对已被客户端追踪的
        // 实体使用传送包导致的插值拉扯（客户端会先按自身速度穿过去再被同步拉回）
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        level().getEntities(EntityType.ITEM, aabb, entity -> !entity.anvilcraft$isAdsorbable()).forEach(entity -> {
            if (containerTarget != null) {
                ItemStack stack = entity.getItem();
                ItemStack remainder = ItemHandlerUtil.insertItem(containerTarget, stack, false);
                if (remainder.isEmpty()) {
                    entity.discard();
                    return;
                }
                if (remainder.getCount() != stack.getCount()) {
                    entity.setItem(remainder);
                }
            } else if (blocked) {
                // 开口被堵住，物品留在原地，下一 tick 再尝试
                return;
            }

            Vec3 ejectPos = this.position()
                .add(attachedDirection.getStepX() * 0.25, attachedDirection.getStepY() * 0.25, attachedDirection.getStepZ() * 0.25);
            Vec3 motion = new Vec3(
                attachedDirection.getStepX() * 0.1,
                attachedDirection.getStepY() * 0.1,
                attachedDirection.getStepZ() * 0.1
            );
            // 销毁原实体并在输出位置生成新实体，客户端会收到干净的生成包并自行模拟物理
            ItemStack outputStack = entity.getItem().copy();
            entity.discard();
            ItemEntity ejected = new ItemEntity(
                serverLevel, ejectPos.x, ejectPos.y, ejectPos.z, outputStack, motion.x, motion.y, motion.z
            );
            ejected.anvilcraft$setIsAdsorbable(true);
            serverLevel.addFreshEntity(ejected);
        });
    }

    // 辅助方法：统一处理移动和锁定
    private void manualMove(BlockPos destPos) {
        // 更新物理位置
        moveToBlock(destPos, this.level().getBlockState(destPos));
        // 设置状态
        this.wasMoving = true;
        // 锁定目标
        this.targetPos = destPos;
    }

    /**
     * 获取活塞移动方块的真实移动方向。
     * 推出时(Extending)：方向为活塞朝向。
     * 拉回时(!Extending)：方向为活塞朝向的反方向。
     */
    private Direction getMovementDirection(PistonMovingBlockEntity be) {
        return be.isExtending() ? be.getDirection() : be.getDirection().getOpposite();
    }

    private void moveToBlock(BlockPos pos, BlockState state) {
        // 计算偏移量，保持实体相对于方块的位置不变
        Vec3 offset = this.position().subtract(Vec3.atLowerCornerOf(this.getCauldronPos()));
        this.setCauldronPos(pos);
        this.setCauldronState(state);
        this.setPos(Vec3.atLowerCornerOf(pos).add(offset));
        // 重置状态
        this.wasMoving = false;
        this.targetPos = null;
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
        builder.define(DATA_CAULDRON_POS, BlockPos.ZERO)
            .define(DATA_ATTACHED_DIRECTION, Direction.UP)
            .define(DATA_CAULDRON_STATE, Blocks.AIR.defaultBlockState());
    }

    public BlockPos getCauldronPos() {
        return this.entityData.get(DATA_CAULDRON_POS);
    }

    public void setCauldronPos(BlockPos pos) {
        this.entityData.set(DATA_CAULDRON_POS, pos);
    }

    public Direction getAttachedDirection() {
        return this.entityData.get(DATA_ATTACHED_DIRECTION);
    }

    public void setAttachedDirection(Direction direction) {
        this.entityData.set(DATA_ATTACHED_DIRECTION, direction);
    }

    public BlockState getCauldronState() {
        return this.entityData.get(DATA_CAULDRON_STATE);
    }

    public void setCauldronState(BlockState state) {
        this.entityData.set(DATA_CAULDRON_STATE, state);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        this.setCauldronPos(NbtUtils.readBlockPos(compoundTag, "CauldronPos").orElse(BlockPos.ZERO));
        this.setAttachedDirection(Direction.from3DDataValue(compoundTag.getInt("AttachedDirection")));
        this.setCauldronState(NbtUtils.readBlockState(this.level().holderLookup(Registries.BLOCK),
            compoundTag.getCompound("CauldronState")));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        compoundTag.put("CauldronState", NbtUtils.writeBlockState(this.getCauldronState()));
        compoundTag.put("CauldronPos", NbtUtils.writeBlockPos(this.getCauldronPos()));
        compoundTag.putInt("AttachedDirection", this.getAttachedDirection().get3DDataValue());
    }

    @Override
    protected AABB makeBoundingBox() {
        return EntityDimensions.scalable(0.375f, 0.375f).makeBoundingBox(this.position());
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

}