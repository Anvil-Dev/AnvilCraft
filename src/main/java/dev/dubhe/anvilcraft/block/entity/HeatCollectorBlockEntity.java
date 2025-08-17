package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.heat.collector.HeatCollectorManager;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.tooltip.providers.IHasAffectRange;
import dev.dubhe.anvilcraft.block.HeatCollectorBlock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HeatCollectorBlockEntity extends BlockEntity implements IPowerProducer, IHasAffectRange {
    private static final int MAX_OUTPUT_POWER = 4096;
    @Getter
    private int time = 0;
    @Getter
    @Setter
    private PowerGrid grid = null;
    @Getter
    private int outputPower = 0;
    private int inputtingPower = 0;
    @Getter
    private float rotation = 0;
    @Getter
    @Setter
    private WorkResult result = WorkResult.SUCCESS;

    public HeatCollectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static HeatCollectorBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        return new HeatCollectorBlockEntity(type, pos, state);
    }

    @Override
    public int getRange() {
        return 2;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("tickCache", this.time);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.time = tag.getInt("tickCache");
    }

    @Override
    public void gridTick() {
        if (!this.isWorking() || level == null || level.isClientSide()) return;
        int oldPower = this.outputPower;
        this.outputPower = this.inputtingPower;
        if (this.outputPower > 0 && this.getBlockState().getBlock() instanceof HeatCollectorBlock collector) {
            collector.activate(this.level, this.getBlockPos(), this.getBlockState());
        }
        if (this.outputPower != oldPower && grid != null) grid.markChanged();
        this.inputtingPower = 0;
        this.time++;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.getCurrentLevel() == null) return;
        HeatCollectorManager.addHeatCollector(this.getPos(), this.getCurrentLevel());
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (this.getCurrentLevel() == null) return;
        HeatCollectorManager.removeHeatCollector(this.getPos(), this.getCurrentLevel());
    }

    public void clientTick() {
        if (!this.isWorking()) return;
        rotation += (float) (getServerPower() * 0.03);
    }

    public boolean isWorking() {
        return this.result.isWorking();
    }

    /**
     * 向集热器添加热能
     *
     * @param num 添加至收集器的热能
     * @return 溢出的热能(即未被添加至该收集器的热能)
     */
    public int inputtingHeat(int num) {
        if (!this.isWorking()) return num;
        int overflow = num - (MAX_OUTPUT_POWER - this.inputtingPower);
        if (overflow < 0) {
            overflow = 0;
        }
        int acceptableChargeCount = num - overflow;
        this.inputtingPower += acceptableChargeCount;
        return overflow;
    }

    @Override
    public @Nullable Level getCurrentLevel() {
        return this.getLevel();
    }

    @Override
    public @NotNull BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public AABB shape() {
        return AABB.ofSize(getBlockPos().getCenter(), 5, 5, 5);
    }

    public enum WorkResult {
        SUCCESS(""),
        TOO_CLOSE("block.anvilcraft.heat_collector.placement_too_close_to_another"),
        ;

        private final String key;

        WorkResult(String key) {
            this.key = key;
        }

        public String getTranslateKey() {
            return this.key;
        }

        public boolean isWorking() {
            return this == SUCCESS;
        }
    }
}
