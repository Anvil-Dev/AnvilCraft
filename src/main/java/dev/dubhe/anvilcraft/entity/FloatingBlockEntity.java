package dev.dubhe.anvilcraft.entity;

import dev.dubhe.anvilcraft.init.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.NotNull;

public class FloatingBlockEntity extends FallingBlockEntity {

    private boolean underCeiling = false;

    /**
     */
    public FloatingBlockEntity(EntityType<? extends FallingBlockEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.refreshDimensions();
    }

    private FloatingBlockEntity(Level level, double x, double y, double z, BlockState state) {
        this(ModEntities.FLOATING_BLOCK.get(), level);
        this.blockState = state;
        this.blocksBuilding = true;
        this.setPos(x, y, z);
        this.setDeltaMovement(Vec3.ZERO);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.setStartPos(this.blockPosition());
    }

    /**
     * @param level 世界
     * @param pos 方块坐标
     * @param blockState 方块状态
     */
    public static FloatingBlockEntity _float(Level level, BlockPos pos, BlockState blockState) {
        FloatingBlockEntity floatingBlockEntity = new FloatingBlockEntity(
                level,
                (double) pos.getX() + 0.5,
                pos.getY(),
                (double) pos.getZ() + 0.5,
                blockState.hasProperty(BlockStateProperties.WATERLOGGED)
                        ? blockState.setValue(BlockStateProperties.WATERLOGGED, false)
                        : blockState);
        level.setBlock(pos, Fluids.WATER.defaultFluidState().createLegacyBlock(), 3);
        level.addFreshEntity(floatingBlockEntity);
        return floatingBlockEntity;
    }

    @Override
    public void tick() {
        if (this.blockState.isAir()) {
            this.discard();
        } else {
            Block block = this.blockState.getBlock();
            ++this.time;
            BlockPos blockPos = this.blockPosition();

            if (this.level().getFluidState(blockPos.above()).is(FluidTags.WATER) && !underCeiling) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.04, 0.0));
            } else {
                if (!this.level().isClientSide) {
                    {
                        if (underCeiling) {
                            blockPos = blockPos.above();
                        }
                        BlockState blockState = this.level().getBlockState(blockPos);
                        this.setDeltaMovement(this.getDeltaMovement().multiply(0.7, -0.5, 0.7));
                        if (!blockState.is(Blocks.MOVING_PISTON)) {
                            if (!this.cancelDrop) {
                                boolean bl3 = blockState.canBeReplaced(new DirectionalPlaceContext(
                                        this.level(), blockPos, Direction.DOWN, ItemStack.EMPTY, Direction.UP));
                                boolean bl5 = this.blockState.canSurvive(this.level(), blockPos);
                                if (bl3 && bl5) {
                                    if (this.blockState.hasProperty(BlockStateProperties.WATERLOGGED)
                                            && this.level().getFluidState(blockPos).getType() == Fluids.WATER) {
                                        this.blockState =
                                                this.blockState.setValue(BlockStateProperties.WATERLOGGED, true);
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
                                        if (block instanceof Fallable) {
                                            ((Fallable) block)
                                                    .onLand(this.level(), blockPos, this.blockState, blockState, this);
                                        }
                                    } else if (this.dropItem
                                            && this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                                        this.discard();
                                        this.callOnBrokenAfterFall(block, blockPos);
                                        this.spawnAtLocation(block);
                                    }
                                } else {
                                    this.discard();
                                    if (this.dropItem
                                            && this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                                        this.callOnBrokenAfterFall(block, blockPos);
                                        this.spawnAtLocation(block);
                                    }
                                }
                            } else {
                                this.discard();
                                this.callOnBrokenAfterFall(block, blockPos);
                            }
                        }
                    }
                }
            }
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
        }
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        return EntityDimensions.scalable(0.98f, 0.98f);
    }

    @Override
    public void move(@NotNull MoverType type, @NotNull Vec3 pos) {
        super.move(type, pos);
        Vec3 vec3 = this.collide(pos);
        this.verticalCollision = pos.y != vec3.y;
        this.underCeiling = this.verticalCollision && pos.y > 0;
    }
}
