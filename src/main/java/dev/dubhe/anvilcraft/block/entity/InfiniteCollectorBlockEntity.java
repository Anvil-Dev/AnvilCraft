package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.chargecollector.ChargeCollectorManager;
import dev.dubhe.anvilcraft.api.heat.collector.HeatCollectorManager;
import dev.dubhe.anvilcraft.api.heat.collector.IHeatCollector;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.tooltip.providers.IHasAffectRange;
import dev.dubhe.anvilcraft.block.InfiniteCollectorBlock;
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
import org.jetbrains.annotations.Nullable;

public class InfiniteCollectorBlockEntity extends BlockEntity implements IPowerProducer, IHasAffectRange, IHeatCollector {
    public static final int BASE_OUTPUT_POWER = 256000;
    public static final int RANGE = 3;

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

    public InfiniteCollectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static InfiniteCollectorBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        return new InfiniteCollectorBlockEntity(type, pos, state);
    }

    @Override
    public int getRange() {
        return RANGE;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("tickCache", this.time);
        tag.putInt("inputtingPower", this.inputtingPower);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.time = tag.getInt("tickCache");
        this.inputtingPower = tag.getInt("inputtingPower");
    }

    @Override
    public void gridTick() {
        if (!this.isWorking() || level == null || level.isClientSide()) return;
        int oldPower = this.outputPower;
        this.outputPower = BASE_OUTPUT_POWER + this.inputtingPower;
        if (this.outputPower > 0 && this.getBlockState().getBlock() instanceof InfiniteCollectorBlock collector) {
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
        HeatCollectorManager.addInfiniteCollector(this.getPos(), this.getCurrentLevel());
        ChargeCollectorManager.getInstance(this.getCurrentLevel()).addInfiniteCollector(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (this.getCurrentLevel() == null) return;
        HeatCollectorManager.removeInfiniteCollector(this.getPos(), this.getCurrentLevel());
        ChargeCollectorManager.getInstance(this.getCurrentLevel()).removeInfiniteCollector(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.getCurrentLevel() == null) return;
        HeatCollectorManager.removeInfiniteCollector(this.getPos(), this.getCurrentLevel());
        ChargeCollectorManager.getInstance(this.getCurrentLevel()).removeInfiniteCollector(this);
    }

    public void clientTick() {
        if (!this.isWorking()) return;
        rotation += (float) (Math.log(getServerPower() + 1) * 0.5);
    }

    public boolean isWorking() {
        return this.result.isWorking();
    }

    /**
     * 向收集器添加热能
     *
     * @param num 添加至收集器的热能
     * @return 溢出的热能(即未被添加至该收集器的热能)
     */
    public int inputtingHeat(int num) {
        if (!this.isWorking()) return num;
        this.inputtingPower += num;
        return 0;
    }

    /**
     * 向收集器添加电荷
     *
     * @param num    添加至收集器的电荷数
     * @param srcPos 电荷来源位置
     * @return 溢出的电荷数(即未被添加至收集器的电荷数)
     */
    public double incomingCharge(double num, BlockPos srcPos) {
        if (!this.isWorking()) return num;
        this.inputtingPower += (int) Math.floor(num);
        return 0;
    }

    @Override
    public @Nullable Level getCurrentLevel() {
        return this.getLevel();
    }

    @Override
    public BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public AABB shape() {
        return AABB.ofSize(getBlockPos().getCenter(), RANGE * 2 + 1, RANGE * 2 + 1, RANGE * 2 + 1);
    }

    @Override
    public BlockPos getCollectorPos() {
        return this.getBlockPos();
    }

    @Override
    public boolean isCollectorWorking() {
        return this.isWorking();
    }

    @Override
    public void setCollectorWorking(boolean working) {
        this.result = working ? WorkResult.SUCCESS : WorkResult.TOO_CLOSE;
    }

    @Override
    public int inputHeat(int amount) {
        return this.inputtingHeat(amount);
    }

    @Override
    public int getCollectorRange() {
        return this.getRange();
    }

    public enum WorkResult {
        SUCCESS(""),
        TOO_CLOSE("block.anvilcraft.infinite_collector.placement_too_close_to_another"),
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
