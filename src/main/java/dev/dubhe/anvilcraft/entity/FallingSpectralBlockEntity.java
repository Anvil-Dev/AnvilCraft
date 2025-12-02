package dev.dubhe.anvilcraft.entity;

import com.google.common.collect.ImmutableList;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.block.SpectralAnvilBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.entity.ModEntities;
import it.unimi.dsi.fastutil.floats.FloatArraySet;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.floats.FloatSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.function.Predicate;

public class FallingSpectralBlockEntity extends FallingBlockEntity {
    private boolean isGhostEntity;

    public FallingSpectralBlockEntity(EntityType<? extends FallingSpectralBlockEntity> entityType, Level level) {
        super(entityType, level);
    }

    private FallingSpectralBlockEntity(
        Level level, double x, double y, double z, BlockState state, boolean isGhostEntity
    ) {
        this(ModEntities.FALLING_SPECTRAL_BLOCK.get(), level);
        this.blockState = state;
        this.blocksBuilding = true;
        this.setPos(x, y, z);
        this.setDeltaMovement(Vec3.ZERO);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.setStartPos(this.blockPosition());
        this.isGhostEntity = isGhostEntity;
        this.dropItem = !isGhostEntity;
        this.cancelDrop = !isGhostEntity;
    }

    @Override
    public void callOnBrokenAfterFall(Block block, BlockPos pos) {
    }

    @Override
    public boolean anvilcraft$isSpectral() {
        return true;
    }

    @Override
    public float anvilcraft$getFallDistance() {
        return 1.0F;
    }

    @Override
    public void tick() {
        if (this.blockState.isAir()) {
            this.discard();
        } else {
            this.time++;
            this.applyGravity();
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.handlePortal();
            Block block = this.blockState.getBlock();
            if (!this.level().isClientSide() && (this.isAlive() || this.forceTickAfterTeleportToDuplicate)) {
                BlockPos blockPos = this.blockPosition();
                if (this.onGround()) {
                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.7, -0.5, 0.7));
                    AnvilEvent.OnLand event = new AnvilEvent.OnLand(this.level(), blockPos, this, this.anvilcraft$getFallDistance());
                    NeoForge.EVENT_BUS.post(event);
                    this.level().playSound(null, blockPos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.0F, 1.0F);
                    this.discard();
                    this.callOnBrokenAfterFall(block, blockPos);
                    if (this.dropItem && this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                        this.spawnAtLocation(block);
                    }
                } else if (
                    !this.level().isClientSide()
                    && (
                        this.time > 100
                        && (
                            blockPos.getY() <= this.level().getMinBuildHeight()
                            || blockPos.getY() > this.level().getMaxBuildHeight()
                        )
                        || this.time > 600
                    )
                ) {
                    this.discard();
                    if (this.dropItem && this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                        this.spawnAtLocation(block);
                    }
                }
            }
            this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
        }
    }

    @Override
    public Vec3 collide(Vec3 vec) {
        AABB aabb = this.getBoundingBox();
        List<VoxelShape> list = this.level().getEntityCollisions(this, aabb.expandTowards(vec));
        Vec3 vec3 = vec.lengthSqr() == 0.0 ? vec : collideBoundingBox(this, vec, aabb, this.level(), list);
        boolean flag = vec.x != vec3.x;
        boolean flag1 = vec.y != vec3.y;
        boolean flag2 = vec.z != vec3.z;
        boolean flag3 = flag1 && vec.y < 0.0;
        if (this.maxUpStep() > 0.0F && (flag3 || this.onGround()) && (flag || flag2)) {
            AABB aabb1 = flag3 ? aabb.move(0.0, vec3.y, 0.0) : aabb;
            AABB aabb2 = aabb1.expandTowards(vec.x, this.maxUpStep(), vec.z);
            if (!flag3) {
                aabb2 = aabb2.expandTowards(0.0, -1.0E-5F, 0.0);
            }

            List<VoxelShape> list1 = collectColliders(this, this.level(), list, aabb2);
            float f = (float) vec3.y;
            float[] afloat = collectCandidateStepUpHeights(aabb1, list1, this.maxUpStep(), f);

            for (float f1 : afloat) {
                Vec3 vec31 = collideWithShapes(new Vec3(vec.x, f1, vec.z), aabb1, list1);
                if (vec31.horizontalDistanceSqr() > vec3.horizontalDistanceSqr()) {
                    double d0 = aabb.minY - aabb1.minY;
                    return vec31.add(0.0, -d0, 0.0);
                }
            }
        }
        return vec3;
    }

    public static Vec3 collideBoundingBox(
        @Nullable Entity entity,
        Vec3 vec,
        AABB collisionBox,
        Level level,
        List<VoxelShape> potentialHits
    ) {
        List<VoxelShape> list = collectColliders(entity, level, potentialHits, collisionBox.expandTowards(vec));
        return collideWithShapes(vec, collisionBox, list);
    }

    private static @Unmodifiable List<VoxelShape> collectColliders(
        @Nullable Entity entity,
        Level level,
        List<VoxelShape> collisions,
        AABB boundingBox
    ) {
        ImmutableList.Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(collisions.size() + 1);
        if (!collisions.isEmpty()) {
            builder.addAll(collisions);
        }

        WorldBorder worldborder = level.getWorldBorder();
        boolean flag = entity != null && worldborder.isInsideCloseToBorder(entity, boundingBox);
        if (flag) {
            builder.add(worldborder.getCollisionShape());
        }

        builder.addAll(getBlockCollisions(level, entity, boundingBox));
        return builder.build();
    }

    private static float[] collectCandidateStepUpHeights(AABB box, List<VoxelShape> colliders, float deltaY, float maxUpStep) {
        FloatSet floatset = new FloatArraySet(4);

        for (VoxelShape voxelshape : colliders) {
            for (double d0 : voxelshape.getCoords(Direction.Axis.Y)) {
                float f = (float) (d0 - box.minY);
                if (!(f < 0.0F) && f != maxUpStep) {
                    if (f > deltaY) {
                        break;
                    }

                    floatset.add(f);
                }
            }
        }

        float[] afloat = floatset.toFloatArray();
        FloatArrays.unstableSort(afloat);
        return afloat;
    }

    private static Iterable<VoxelShape> getBlockCollisions(Level level, @Nullable Entity entity, AABB collisionBox) {
        return () -> new BlockCollisions<>(
            level,
            entity,
            collisionBox,
            false,
            (pos, shape) -> {
                BlockState state = level.getBlockState(pos);
                if (shouldIgnoreBlockInMovement(state)) {
                    return Shapes.empty();
                }
                return shape;
            }
        );
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Ghost", this.isGhostEntity);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("Ghost")) {
            this.isGhostEntity = compound.getBoolean("Ghost");
        } else {
            this.isGhostEntity = true;
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        int dist = Mth.ceil(fallDistance - 1.0F);
        if (dist < 0) {
            return false;
        }
        Predicate<Entity> predicate = EntitySelector.NO_CREATIVE_OR_SPECTATOR.and(EntitySelector.LIVING_ENTITY_STILL_ALIVE);
        float f = (float) Math.min(Mth.floor((float) dist * 2), 40);
        DamageSource damageSource = this.blockState.getBlock() instanceof Fallable fallable
                                    ? fallable.getFallDamageSource(this)
                                    : this.damageSources().fallingBlock(this);
        this.level().getEntities(this, this.getBoundingBox(), predicate).forEach(entity -> {
            entity.hurt(damageSource, f);
            NeoForge.EVENT_BUS.post(new AnvilEvent.HurtEntity(this, this.getOnPos(), this.level(), entity, f));
        });
        boolean isAnvil = this.blockState.is(BlockTags.ANVIL);
        if (isAnvil && f > 0.0F && this.random.nextFloat() < 0.05F + (float) dist * 0.05F) {
            BlockState blockState = AnvilBlock.damage(this.blockState);
            if (blockState == null) {
                this.cancelDrop = true;
            } else {
                this.blockState = blockState;
            }
        }
        return false;
    }

    protected static boolean shouldIgnoreBlockInMovement(BlockState state) {
        // noinspection deprecation
        return (
                   state.isAir()
                   || state.is(BlockTags.FIRE)
                   || state.liquid()
                   || state.is(ModBlockTags.SPECTRAL_CAN_THROUGH)
                   || state.getBlock() instanceof TransparentBlock
                   || state.canBeReplaced()
                   || (
                       !state.getBlock().properties().hasCollision
                       && !state.is(Blocks.SCAFFOLDING)
                   )
               )
               && !(state.getBlock() instanceof SpectralAnvilBlock);
    }

    /**
     * 落下幻灵实体
     */
    public static FallingSpectralBlockEntity fall(
        Level level, BlockPos pos, BlockState blockState, boolean updateBlock, boolean isGhostEntity
    ) {
        FallingSpectralBlockEntity fallingBlockEntity = new FallingSpectralBlockEntity(
            level,
            (double) pos.getX() + 0.5,
            pos.getY(),
            (double) pos.getZ() + 0.5,
            blockState.hasProperty(BlockStateProperties.WATERLOGGED)
                ? blockState.setValue(BlockStateProperties.WATERLOGGED, false)
                : blockState,
            isGhostEntity
        );
        if (updateBlock) {
            level.setBlock(pos, blockState.getFluidState().createLegacyBlock(), 3);
        }
        level.addFreshEntity(fallingBlockEntity);
        return fallingBlockEntity;
    }
}
