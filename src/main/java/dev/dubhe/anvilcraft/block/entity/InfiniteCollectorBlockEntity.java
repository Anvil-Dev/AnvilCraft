package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.heat.collector.IHeatCollector;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.tooltip.providers.IHasAffectRange;
import dev.dubhe.anvilcraft.block.InfiniteCollectorBlock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class InfiniteCollectorBlockEntity extends BlockEntity implements IPowerProducer, IHasAffectRange, IHeatCollector {
    public static final int BASE_OUTPUT_POWER = 256;
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

    @Override
    public int getRange() {
        return RANGE;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("tickCache", this.time);
        output.putInt("inputtingPower", this.inputtingPower);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.time = input.getIntOr("tickCache", 0);
        this.inputtingPower = input.getIntOr("inputtingPower", 0);
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
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
    }

    public void clientTick() {
        if (!this.isWorking()) return;
        rotation += (float) (Math.log(getServerPower() + 1) * 0.5);
    }

    public boolean isWorking() {
        return this.result.isWorking();
    }

    public int inputtingHeat(int num) {
        if (!this.isWorking()) return num;
        this.inputtingPower += num;
        return 0;
    }

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
