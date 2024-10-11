package dev.dubhe.anvilcraft.entity;

import dev.dubhe.anvilcraft.api.event.anvil.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.block.SpectralAnvilBlock;
import dev.dubhe.anvilcraft.init.ModEntities;

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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;

import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
public class FallingSpectralBlockEntity extends FallingBlockEntity {
    private boolean isGhostEntity;
    private float fallDistance = 0;

    public FallingSpectralBlockEntity(EntityType<? extends FallingSpectralBlockEntity> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * @param isGhostEntity 是否为分身
     */
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
        this.dropItem = isGhostEntity;
        this.cancelDrop = !isGhostEntity;
        this.noPhysics = true;
    }

    @Override
    public void callOnBrokenAfterFall(@NotNull Block block, @NotNull BlockPos pos) {
    }

    @Override
    public void move(MoverType type, Vec3 pos) {
        this.setPos(this.getX() + pos.x, this.getY() + pos.y, this.getZ() + pos.z);
    }

    @Override
    public void tick() {
        if (this.blockState.isAir()) {
            this.discard();
            return;
        }
        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0));
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
        fallDistance -= (float) this.getDeltaMovement().y;
        if (this.level().isClientSide) return;
        BlockPos current = this.blockPosition();
        BlockPos below = current.below();
        BlockState blockStateDown = this.level().getBlockState(below);
        if (current.getY() < -160) {
            discard();
        }
        if (!shouldIgnoreBlockInMovement(blockStateDown)) {
            this.discard();
            if (!isGhostEntity) {
                if (!blockStateDown.isFaceSturdy(level(), below, Direction.UP)) {
                    this.spawnAtLocation(this.blockState.getBlock());
                    return;
                }
                level().setBlockAndUpdate(below, blockState);
                return;
            }
            level().playSound(
                this,
                below,
                SoundEvents.ANVIL_LAND,
                SoundSource.BLOCKS,
                SoundType.ANVIL.volume,
                SoundType.ANVIL.pitch
            );
            Predicate<Entity> predicate =
                EntitySelector.NO_CREATIVE_OR_SPECTATOR.and(EntitySelector.LIVING_ENTITY_STILL_ALIVE);
            level().getEntitiesOfClass(
                LivingEntity.class,
                new AABB(current),
                predicate
            ).forEach(it -> it.hurt(
                    level().damageSources().anvil(this),
                    Math.min(40f, fallDistance * 2)
                )
            );
            NeoForge.EVENT_BUS.post(new AnvilFallOnLandEvent(level(), current, this, fallDistance));
        }
        this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag compound) {
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
        Predicate<Entity> predicate =
            EntitySelector.NO_CREATIVE_OR_SPECTATOR.and(EntitySelector.LIVING_ENTITY_STILL_ALIVE);
        float f = (float) Math.min(Mth.floor((float) dist * 2), 40);
        this.level().getEntities(this, this.getBoundingBox(), predicate).forEach(entity -> entity.hurt(source, f));
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

    protected static boolean shouldIgnoreBlockInMovement(BlockState blockState) {
        return (blockState.isAir()
            || blockState.is(Tags.Blocks.GLASS_BLOCKS)
            || blockState.is(Tags.Blocks.GLASS_PANES)
            || blockState.getBlock() instanceof TransparentBlock
            || blockState.canBeReplaced())
            && !(blockState.getBlock() instanceof SpectralAnvilBlock);
    }

    /**
     * 落下幻灵实体
     */
    public static FallingSpectralBlockEntity fall(
        Level level, BlockPos pos, BlockState blockState, boolean updateBlock, boolean isGhostEntity) {
        FallingSpectralBlockEntity fallingBlockEntity = new FallingSpectralBlockEntity(
            level,
            (double) pos.getX() + 0.5,
            pos.getY(),
            (double) pos.getZ() + 0.5,
            blockState.hasProperty(BlockStateProperties.WATERLOGGED)
                ? blockState.setValue(BlockStateProperties.WATERLOGGED, false)
                : blockState,
            isGhostEntity);
        if (updateBlock) {
            level.setBlock(pos, blockState.getFluidState().createLegacyBlock(), 3);
        }
        level.addFreshEntity(fallingBlockEntity);
        return fallingBlockEntity;
    }
}
