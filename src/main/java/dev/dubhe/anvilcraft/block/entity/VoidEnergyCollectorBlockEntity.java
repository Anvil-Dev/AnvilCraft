package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.tooltip.providers.IHasAffectRange;
import dev.dubhe.anvilcraft.block.NegativeMatterBlock;
import dev.dubhe.anvilcraft.block.VoidEnergyCollectorBlock;
import dev.dubhe.anvilcraft.block.VoidMatterBlock;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class VoidEnergyCollectorBlockEntity extends BlockEntity implements IPowerProducer, IHasAffectRange {
    private static final int COOLDOWN = 2;
    private static final int DECAY_COOLDOWN = 60;

    private int cooldownCount = 2;
    //注意：这里的decayCooldownCount初始值为1会让机器第一次发电（2秒后）时同时衰变一次空气
    //这是为了防止放下来之后在“发电了”之后，在“衰变空气”之前破坏方块，来稳定白嫖电力
    private int decayCooldownCount = 1;
    private int blockCount = 0;
    private PowerGrid grid = null;
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

    public static @NotNull VoidEnergyCollectorBlockEntity createBlockEntity(
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
    public @NotNull BlockPos getPos() {
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
    public void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        this.cooldownCount = tag.getInt("cooldownCount");
        this.decayCooldownCount = tag.getInt("decayCooldownCount");
        this.blockCount = tag.getInt("blockCount");
        this.power = tag.getInt("power");
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        tag.putInt("cooldownCount", this.cooldownCount);
        tag.putInt("decayCooldownCount", this.decayCooldownCount);
        tag.putInt("blockCount", this.blockCount);
        tag.putInt("power", this.power);
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
        if (level == null || level.isClientSide()) return;
        if (this.cooldownCount-- > 1) return;
        this.cooldownCount = COOLDOWN;
        int oldPower = this.power;
        this.blockCount = countBlocksInRange();
        this.power = getPowerFromBlockCount(this.blockCount);
        if (this.power > 0 && this.getBlockState().getBlock() instanceof VoidEnergyCollectorBlock voidEnergyCollector) {
            voidEnergyCollector.activate(this.level, this.getBlockPos(), this.getBlockState());
            if (this.decayCooldownCount-- <= 1) {
                makeBlocksDecay();
                this.decayCooldownCount = DECAY_COOLDOWN;
            }
        }
        if (power != oldPower && grid != null) grid.markChanged();
        this.blockCount = 0;
        time++;
    }

    public static boolean isOutOfBuildLimits(Level level, BlockPos pos) {
        int minHeight = level.getMinBuildHeight();
        int maxHeight = level.getMaxBuildHeight();
        int y = pos.getY();
        return y < minHeight || y >= maxHeight;
    }

    /**
     * Counts the number of blocks in 5x5x5 area.
     * Also detects whether there is another void energy collector in 9x9x9;
     * if there does be another void energy collector, this function returns 125 to disable power generation.
     *
     * @return count(normal) - count(negative) IF active ELSE 125
     */
    private int countBlocksInRange() {
        if (level == null || level.isClientSide()) return 125;
        int count = 0;
        for (int i = -4; i <= 4; i++) {
            for (int j = -4; j <= 4; j++) {
                for (int k = -4; k <= 4; k++) {
                    BlockPos thisPos = this.getBlockPos();
                    BlockPos bp = new BlockPos(
                        thisPos.getX() + i,
                        thisPos.getY() + j,
                        thisPos.getZ() + k);
                    if (isOutOfBuildLimits(level, bp)) continue;
                    BlockState b = level.getBlockState(bp);
                    //this disables the collector when there is another in 9x9x9
                    if ((i != 0 || j != 0 || k != 0) && b.getBlock() instanceof VoidEnergyCollectorBlock)
                        return 125;
                    //below is the 5x5x5 detection that counts how many blocks are there
                    if (i >= -2 && i <= 2 && j >= -2 && j <= 2 && k >= -2 && k <= 2) {
                        if (b.getBlock() instanceof NegativeMatterBlock)
                            count -= 1;
                        else if (!b.isAir()
                            && !(b.getBlock() instanceof VoidMatterBlock)
                            && !(b.getBlock() instanceof VoidEnergyCollectorBlock)
                        )
                            count += 1;
                    }
                }
            }
        }
        return count;
    }

    private void makeBlocksDecay() {
        if (level == null || level.isClientSide()) return;
        RandomSource random = level.getRandom();
        ArrayList<BlockPos> list = new ArrayList<>();
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                for (int k = -2; k <= 2; k++) {
                    BlockPos thisPos = this.getBlockPos();
                    BlockPos bp = new BlockPos(
                        thisPos.getX() + i,
                        thisPos.getY() + j,
                        thisPos.getZ() + k);
                    if (isOutOfBuildLimits(level, bp)) continue;
                    BlockState b = level.getBlockState(bp);
                    if (b.isAir()) {
                        list.add(bp);
                    }
                }
            }
        }
        if (list.isEmpty()) return;
        int i = random.nextInt(list.size());
        BlockPos bp = list.get(i);
        BlockState b = level.getBlockState(bp);
        level.setBlockAndUpdate(bp, VoidMatterBlock.voidDecay(level, bp, b, random));
    }

    @Override
    public int getRange() {
        return 2;
    }

    @Override
    public AABB shape() {
        return AABB.ofSize(getBlockPos().getCenter(), 5, 5, 5);
    }

    public void clientTick() {
        rotation += (float) (getServerPower() * 0.03);
    }
}
