package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.block.IVoidEnergyAmplifier;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.tooltip.providers.IHasAffectRange;
import dev.dubhe.anvilcraft.block.power.generator.InfiniteCollectorBlock;
import dev.dubhe.anvilcraft.block.power.generator.VoidEnergyCollectorBlock;
import dev.dubhe.anvilcraft.block.storage.VoidMatterBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.util.TriggerUtil;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;

public class VoidEnergyCollectorBlockEntity extends BlockEntity implements IPowerProducer, IHasAffectRange {
    public static final int RANGE = 2;
    private static final int COOLDOWN = 2;
    // private static final int DECAY_COOLDOWN = 60;

    private int cooldownCount = 2;
    // 注意：这里的decayCooldownCount初始值为1会让机器第一次发电（2秒后）时同时衰变一次空气
    // 这是为了防止放下来之后在“发电了”之后，在“衰变空气”之前破坏方块，来稳定白嫖电力
    private int decayCooldownCount = 1;
    private int blockCount = 0;
    private @Nullable PowerGrid grid = null;
    private int power = 0;
    @Getter
    private int time = 0;
    @Getter
    private float rotation = 0;

    public VoidEnergyCollectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public VoidEnergyCollectorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.VOID_ENERGY_COLLECTOR.get(), pos, blockState);
    }

    public static VoidEnergyCollectorBlockEntity createBlockEntity(
        BlockEntityType<?> type,
        BlockPos pos,
        BlockState blockState
    ) {
        return new VoidEnergyCollectorBlockEntity(type, pos, blockState);
    }

    @Override
    public Level getCurrentLevel() {
        return this.level;
    }

    @Override
    public BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public void setGrid(@Nullable PowerGrid grid) {
        this.grid = grid;
    }

    @Override
    public @Nullable PowerGrid getGrid() {
        return this.grid;
    }

    @Override
    public int getOutputPower() {
        return this.power;
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.cooldownCount = input.getIntOr("cooldownCount", 0);
        this.decayCooldownCount = input.getIntOr("decayCooldownCount", 0);
        this.blockCount = input.getIntOr("blockCount", 0);
        this.power = input.getIntOr("power", 0);
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("cooldownCount", this.cooldownCount);
        output.putInt("decayCooldownCount", this.decayCooldownCount);
        output.putInt("blockCount", this.blockCount);
        output.putInt("power", this.power);
    }

    private static int getPowerFromBlockCount(int count) {
        if (count >= 21) return 0;
        if (count >= 11) return 128;
        if (count >= 3) return 256;
        if (count >= -10) return 512;
        if (count >= -20) return 1024;
        if (count >= -40) return 2048;
        return 4096;
    }

    @Override
    public void gridTick() {
        if (this.level == null || this.level.isClientSide()) return;
        if (this.cooldownCount-- > 1) return;
        this.cooldownCount = VoidEnergyCollectorBlockEntity.COOLDOWN;
        final int oldPower = this.power;
        this.blockCount = this.countBlocksInRange();
        this.power = VoidEnergyCollectorBlockEntity.getPowerFromBlockCount(this.blockCount);
        if (this.power > 0) {
            if (this.decayCooldownCount-- <= 1) {
                this.makeBlocksDecay();
                this.decayCooldownCount = this.level.getRandom().nextInt(0, 60);
            }
            TriggerUtil.voidCollectorWork(this.level, this.getPos());
        }
        if (this.power != oldPower && this.grid != null) this.grid.markChanged();
        this.blockCount = 0;
        this.time++;
    }

    public static boolean isOutOfBuildLimits(Level level, BlockPos pos) {
        int minHeight = level.getMinY();
        int maxHeight = level.getMaxY();
        int y = pos.getY();
        return y < minHeight || y >= maxHeight;
    }

    public static boolean isAnotherCollectorNearby(Level level, BlockPos pos) {
        int infiniteOverlap = RANGE + AnvilCraft.CONFIG.machines.infiniteCollectorRange;
        int range = Math.max(RANGE * 2, infiniteOverlap);
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        for (int i = -range; i <= range; i++) {
            for (int j = -range; j <= range; j++) {
                for (int k = -range; k <= range; k++) {
                    if (i == 0 && j == 0 && k == 0) continue;
                    mpos.set(pos).move(i, j, k);
                    if (level.isOutsideBuildHeight(mpos) || !level.isLoaded(mpos)) continue;
                    BlockState blockState = level.getBlockState(mpos);
                    int distance = Math.max(Math.max(Math.abs(i), Math.abs(j)), Math.abs(k));
                    if ((distance <= RANGE * 2 && blockState.getBlock() instanceof VoidEnergyCollectorBlock)
                        || (distance <= infiniteOverlap && blockState.getBlock() instanceof InfiniteCollectorBlock)) return true;
                }
            }
        }
        return false;
    }

    /// Counts the number of blocks in 5x5x5 area.
    /// Also detects whether there is another void energy collector in 9x9x9;
    /// if there does be another void energy collector, this function returns 125 to disable power generation.
    ///
    /// @return count(normal) - count(negative) IF active ELSE 125
    private int countBlocksInRange() {
        if (this.level == null || this.level.isClientSide()) return 125;
        if (VoidEnergyCollectorBlockEntity.isAnotherCollectorNearby(this.level, this.getBlockPos())) return 125;
        return VoidEnergyCollectorBlockEntity.countMassInRange(this.level, this.getBlockPos(), RANGE);
    }

    public static int countMassInRange(Level level, BlockPos pos, int range) {
        int count = 0;
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        for (int i = -range; i <= range; i++) {
            for (int j = -range; j <= range; j++) {
                for (int k = -range; k <= range; k++) {
                    mpos.set(pos).move(i, j, k);
                    if (level.isOutsideBuildHeight(mpos) || !level.isLoaded(mpos)) continue;
                    BlockState blockState = level.getBlockState(mpos);
                    if (blockState.is(ModBlockTags.CONTAINS_VOID_ENERGY)) continue;
                    if (blockState.getBlock() instanceof IVoidEnergyAmplifier amplifier) {
                        count += amplifier.getVoidEnergyAmplification();
                    } else if (!blockState.isAir()) {
                        count += 1;
                    }
                }
            }
        }
        return count;
    }

    private void makeBlocksDecay() {
        if (this.level == null || this.level.isClientSide()) return;
        RandomSource random = this.level.getRandom();
        ArrayList<BlockPos> list = new ArrayList<>();
        for (int i = -RANGE; i <= RANGE; i++) {
            for (int j = -RANGE; j <= RANGE; j++) {
                for (int k = -RANGE; k <= RANGE; k++) {
                    BlockPos thisPos = this.getBlockPos();
                    BlockPos bp = new BlockPos(
                        thisPos.getX() + i,
                        thisPos.getY() + j,
                        thisPos.getZ() + k);
                    if (VoidEnergyCollectorBlockEntity.isOutOfBuildLimits(this.level, bp)) continue;
                    BlockState b = this.level.getBlockState(bp);
                    if (b.isAir()) {
                        list.add(bp);
                    }
                }
            }
        }
        if (list.isEmpty()) return;
        int i = random.nextInt(list.size());
        BlockPos bp = list.get(i);
        this.level.setBlockAndUpdate(bp, VoidMatterBlock.voidDecay(this.level, random));
    }

    @Override
    public int getRange() {
        return RANGE;
    }

    @Override
    public AABB shape() {
        return AABB.ofSize(this.getBlockPos().getCenter(), RANGE * 2 + 1, RANGE * 2 + 1, RANGE * 2 + 1);
    }

    public void clientTick() {
        this.rotation += (float) (Math.log(this.getServerPower() + 1) * 2.5);
    }
}
