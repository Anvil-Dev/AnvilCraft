package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.RipeningManager;
import dev.dubhe.anvilcraft.api.SpawningManager;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.tooltip.providers.IHasAffectRange;
import dev.dubhe.anvilcraft.block.InductionLightBlock;
import dev.dubhe.anvilcraft.util.AabbUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

@Setter
@Getter
public class InductionLightBlockEntity extends BlockEntity implements IPowerConsumer, IHasAffectRange {
    @Getter(AccessLevel.NONE)
    private int ripeningRangeCache = AnvilCraft.CONFIG.inductionLightBlockRipeningRange;
    private AABB ripeningArea;
    private AABB blockingArea;
    private PowerGrid grid;

    public InductionLightBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static InductionLightBlockEntity createBlockEntity(
        BlockEntityType<?> type,
        BlockPos pos,
        BlockState blockState
    ) {
        return new InductionLightBlockEntity(type, pos, blockState);
    }

    public void tick(Level level) {
        this.flushState(level, this.getBlockPos());
        if (InductionLightBlock.canCropGrow(this.getBlockState()) && InductionLightBlock.isLit(this.getBlockState())) {
            RipeningManager.from(level).doRipen(this.getBlockPos());
        } else if (InductionLightBlock.canBlockMobSummoning(this.getBlockState())) {
            SpawningManager.addLightBlock(this.getBlockPos(), level, false);
        } else if (InductionLightBlock.canBlockAnimalSummoning(this.getBlockState())) {
            SpawningManager.addLightBlock(this.getBlockPos(), level, true);
        }
    }

    @Override
    public int getInputPower() {
        if (this.level == null) return 1;
        return this.getBlockState().getValue(InductionLightBlock.POWERED)
            ? 0
            : this.getBlockState().getValue(InductionLightBlock.COLOR).dissipation;
    }

    @Override
    @Nullable
    public Level getCurrentLevel() {
        if (this.level != null) {
            return this.level;
        } else {
            return null;
        }
    }

    @Override
    public BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public @Nullable AABB shape() {
        return switch (this.getBlockState().getValue(InductionLightBlock.COLOR)) {
            case PRIMARY -> null;
            case PINK -> this.getRipeningArea();
            case YELLOW, DARK -> this.getBlockingArea();
        };
    }

    public AABB getRipeningArea() {
        if (this.ripeningRangeCache != AnvilCraft.CONFIG.inductionLightBlockRipeningRange) {
            this.ripeningRangeCache = AnvilCraft.CONFIG.inductionLightBlockRipeningRange;
        }
        if (this.ripeningArea == null) {
            this.ripeningArea = AABB.ofSize(
                this.getPos().getCenter(),
                this.ripeningRangeCache,
                this.ripeningRangeCache,
                this.ripeningRangeCache
            );
        }
        return this.ripeningArea;
    }

    public AABB getBlockingArea() {
        if (this.blockingArea == null) this.blockingArea = AabbUtil.centerSectionTo3x3x3(this.getBlockPos());
        return this.blockingArea;
    }

    public boolean isInRange(Vec3 pos) {
        return this.getBlockingArea().contains(pos);
    }
}
