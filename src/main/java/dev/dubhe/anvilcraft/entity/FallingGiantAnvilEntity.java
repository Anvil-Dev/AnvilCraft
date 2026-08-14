package dev.dubhe.anvilcraft.entity;

import dev.dubhe.anvilcraft.api.event.GiantAnvilEvent;
import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import dev.dubhe.anvilcraft.init.entity.ModEntities;
import dev.dubhe.anvilcraft.util.AccelerateManager;
import dev.dubhe.anvilcraft.util.GravityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

public class FallingGiantAnvilEntity extends FallingBlockEntity {
    private float fallDistance = 0;

    public FallingGiantAnvilEntity(EntityType<? extends FallingGiantAnvilEntity> entityType, Level level) {
        super(entityType, level);
    }

    private FallingGiantAnvilEntity(Level level, double x, double y, double z, BlockState state) {
        this(ModEntities.FALLING_GIANT_ANVIL.get(), level);
        this.blockState = state;
        this.blocksBuilding = true;
        this.setPos(x, y, z);
        this.setDeltaMovement(Vec3.ZERO);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.setStartPos(this.blockPosition());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag data) {
        super.addAdditionalSaveData(data);
        data.putFloat("anvilcraft$FallDistance", this.fallDistance);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag data) {
        super.readAdditionalSaveData(data);
        this.fallDistance = data.getFloat("anvilcraft$fallDistance");
    }

    public static FallingGiantAnvilEntity fall(Level level, BlockPos pos, BlockState blockState, boolean updateBlock) {
        FallingGiantAnvilEntity fallingBlockEntity = new FallingGiantAnvilEntity(
            level,
            (double) pos.getX() + 0.5,
            pos.getY(),
            (double) pos.getZ() + 0.5,
            blockState.hasProperty(BlockStateProperties.WATERLOGGED)
                ? blockState.setValue(BlockStateProperties.WATERLOGGED, false)
                : blockState);
        if (updateBlock) {
            level.setBlock(pos, blockState.getFluidState().createLegacyBlock(), 3);
        }
        level.addFreshEntity(fallingBlockEntity);
        return fallingBlockEntity;
    }

    @Override
    public void tick() {
        if (NeoForge.EVENT_BUS.post(new GiantAnvilEvent.FallingTick(this)).isCanceled()) {
            return;
        }
        if (this.blockState.isAir()) {
            this.discard();
        } else {
            ++this.time;
            Vec3 gravity = GravityManager.getNetGravityVectorForFallingBlock(this);
            Direction gravityDirection = gravity.lengthSqr() < 1.0E-5
                ? Direction.DOWN
                : Direction.getNearest(gravity.x, gravity.y, gravity.z);
            boolean controlledByRing = AccelerateManager.isControlledByRing(this);
            if (!this.isNoGravity() && !controlledByRing) {
                this.setDeltaMovement(this.getDeltaMovement().add(gravity));
            }

            Vec3 positionBeforeMove = this.position();
            this.move(MoverType.SELF, this.getDeltaMovement());
            double directionalDistance = this.position().subtract(positionBeforeMove).dot(
                Vec3.atLowerCornerOf(gravityDirection.getNormal())
            );
            if (directionalDistance > 0) {
                this.fallDistance += (float) directionalDistance;
            } else if (directionalDistance < 0) {
                this.fallDistance = 0;
            }
            if (!this.level().isClientSide) {
                BlockPos blockPos = this.blockPosition();
                Block block = this.blockState.getBlock();
                boolean landed = !controlledByRing && this.anvilcraft$hasBlockCollision(gravityDirection);
                BlockPos blockingPos = this.anvilcraft$getBlockingFacePos(blockPos, gravityDirection);
                if (landed && blockingPos != null) {
                    BlockState blockingState = this.level().getBlockState(blockingPos);
                    float friction = blockingState.getFriction(this.level(), blockingPos, this);
                    boolean isMovingSlowly = this.getDeltaMovement().lengthSqr() < 0.04;
                    boolean heldByFriction = isMovingSlowly
                                             && this.anvilcraft$isHeldByFriction(
                                                 gravity, gravityDirection, friction
                                             );
                    if (!heldByFriction && this.anvilcraft$hasSlidingPath(blockPos, gravity, gravityDirection)) {
                        landed = false;
                    }
                }
                if (!landed) {
                    if (!this.level().isClientSide
                        && (this.time > 100
                        && (blockPos.getY() <= this.level().getMinBuildHeight()
                        || blockPos.getY()
                        > this.level().getMaxBuildHeight())
                        || this.time > 600)) {
                        if (this.dropItem && this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                            this.spawnAtLocation(block);
                        }

                        this.discard();
                    }
                } else {
                    BlockState blockState = this.level().getBlockState(blockPos);
                    this.anvilcraft$reflectVelocity(gravityDirection);
                    DirectionalPlaceContext placeContext = new DirectionalPlaceContext(
                        this.level(), blockPos, gravityDirection, ItemStack.EMPTY, gravityDirection.getOpposite());
                    boolean isMovingPiston = false;
                    boolean canBeReplaced = true;
                    boolean canSurvive = this.blockState.canSurvive(this.level(), blockPos.below());
                    for (int i = -1; i <= 1; i++) {
                        for (int j = -1; j <= 1; j++) {
                            for (int k = -1; k <= 1; k++) {
                                canBeReplaced = canBeReplaced
                                    && this.level()
                                    .getBlockState(blockPos.offset(i, k, j))
                                    .canBeReplaced(placeContext);
                            }
                            BlockPos collisionPos = this.anvilcraft$getFacePos(
                                blockPos, gravityDirection, i, j, 1
                            );
                            isMovingPiston = isMovingPiston
                                || this.level().getBlockState(collisionPos).is(Blocks.MOVING_PISTON);
                        }
                    }
                    boolean isFree = blockingPos == null;
                    if (!isMovingPiston) {
                        if (canBeReplaced && canSurvive && !isFree) {
                            if (this.blockState.hasProperty(BlockStateProperties.WATERLOGGED)
                                && this.level().getFluidState(blockPos).getType() == Fluids.WATER) {
                                this.blockState = this.blockState.setValue(BlockStateProperties.WATERLOGGED, true);
                            }

                            if (this.level().setBlock(blockPos, this.blockState, 3)) {
                                ((ServerLevel) this.level())
                                    .getChunkSource()
                                    .chunkMap
                                    .broadcast(
                                        this,
                                        new ClientboundBlockUpdatePacket(
                                            blockPos, this.level().getBlockState(blockPos)));
                                this.discard();
                                if (block instanceof GiantAnvilBlock block1) {
                                    block1.onLand(
                                        this.level(),
                                        blockPos,
                                        this.blockState,
                                        blockState,
                                        this,
                                        this.fallDistance);
                                }
                            } else if (this.dropItem
                                && this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                                this.discard();
                                this.callOnBrokenAfterFall(block, blockPos);
                                this.spawnAtLocation(block);
                            }
                        } else {
                            this.discard();
                            if (this.dropItem && this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                                this.callOnBrokenAfterFall(block, blockPos);
                                this.spawnAtLocation(block);
                            }
                        }
                    }
                }
            }
            this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
        }
    }

    private boolean anvilcraft$hasBlockCollision(Direction gravityDirection) {
        if (gravityDirection == Direction.DOWN && this.onGround()) return true;
        Vec3 normal = Vec3.atLowerCornerOf(gravityDirection.getNormal()).scale(0.001);
        return this.level().getBlockCollisions(this, this.getBoundingBox().move(normal)).iterator().hasNext();
    }

    private BlockPos anvilcraft$getBlockingFacePos(BlockPos center, Direction direction) {
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                BlockPos pos = this.anvilcraft$getFacePos(center, direction, i, j);
                if (!FallingBlock.isFree(this.level().getBlockState(pos))) return pos;
            }
        }
        return null;
    }

    private BlockPos anvilcraft$getFacePos(BlockPos center, Direction direction, int i, int j) {
        return this.anvilcraft$getFacePos(center, direction, i, j, 2);
    }

    private BlockPos anvilcraft$getFacePos(
        BlockPos center,
        Direction direction,
        int i,
        int j,
        int distance
    ) {
        return switch (direction.getAxis()) {
            case X -> center.offset(direction.getStepX() * distance, i, j);
            case Y -> center.offset(i, direction.getStepY() * distance, j);
            case Z -> center.offset(i, j, direction.getStepZ() * distance);
        };
    }

    private boolean anvilcraft$isHeldByFriction(Vec3 gravity, Direction direction, float friction) {
        double normalForce = Math.abs(gravity.get(direction.getAxis()));
        double tangentialForce = Math.sqrt(Math.max(0, gravity.lengthSqr() - normalForce * normalForce));
        return tangentialForce < normalForce * (1.0 - friction) * 2.0;
    }

    private boolean anvilcraft$hasSlidingPath(BlockPos center, Vec3 gravity, Direction primaryDirection) {
        if (this.anvilcraft$canSlide(center, gravity.x, Direction.EAST, Direction.WEST, primaryDirection)) {
            return true;
        }
        if (this.anvilcraft$canSlide(center, gravity.y, Direction.UP, Direction.DOWN, primaryDirection)) {
            return true;
        }
        return this.anvilcraft$canSlide(center, gravity.z, Direction.SOUTH, Direction.NORTH, primaryDirection);
    }

    private boolean anvilcraft$canSlide(
        BlockPos center,
        double gravityComponent,
        Direction positive,
        Direction negative,
        Direction primaryDirection
    ) {
        if (Math.abs(gravityComponent) <= 1.0E-5) return false;
        Direction direction = gravityComponent > 0 ? positive : negative;
        return direction != primaryDirection && this.anvilcraft$getBlockingFacePos(center, direction) == null;
    }

    private void anvilcraft$reflectVelocity(Direction gravityDirection) {
        Vec3 movement = this.getDeltaMovement();
        this.setDeltaMovement(switch (gravityDirection.getAxis()) {
            case X -> movement.multiply(-0.5, 0.7, 0.7);
            case Y -> movement.multiply(0.7, -0.5, 0.7);
            case Z -> movement.multiply(0.7, 0.7, -0.5);
        });
    }

    @Override
    protected AABB makeBoundingBox() {
        return EntityDimensions.scalable(3, 3).makeBoundingBox(this.position().add(0, -1, 0));
    }
}
