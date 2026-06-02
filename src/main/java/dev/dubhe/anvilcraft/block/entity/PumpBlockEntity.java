package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.PumpBlock;
import dev.dubhe.anvilcraft.block.entity.fluid.AbstractPipeBlockEntity;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 泵的 BlockEntity。消费 32kW 电力，提供输入端 +10 / 输出端 -10 的等效高度偏移。
 *
 * <p>工作状态判定：
 * <ul>
 *   <li>有红石信号 → 关闭（heightBonus=0，阻塞流体）</li>
 *   <li>电网过载 → 关闭（OVERLOAD=true，阻塞流体）</li>
 *   <li>正常供电 → 工作（输入端 +10，输出端 -10）</li>
 * </ul>
 *
 * <p>方向映射：{@link dev.dubhe.anvilcraft.block.state.Orientation#getDirection()} 返回输出方向。
 * 输入端为该方向的反方向。泵的输出端高度降低，输入端高度抬升。
 */
@Getter
@Setter
public class PumpBlockEntity extends AbstractPipeBlockEntity implements IPowerConsumer {

    private static final int PUMP_POWER = 32;   // 32 kW 电力消耗
    private static final int PUMP_HEAD = 10;    // 10 米扬程

    private PowerGrid grid;
    private boolean working;

    public PumpBlockEntity(BlockEntityType<? extends PumpBlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static PumpBlockEntity create(BlockEntityType<PumpBlockEntity> type, BlockPos pos, BlockState state) {
        return new PumpBlockEntity(type, pos, state);
    }

    @Override
    public int getInputPower() {
        return PUMP_POWER;
    }

    @Override
    public PowerComponentType getComponentType() {
        return PowerComponentType.CONSUMER;
    }

    @Override
    public @Nullable Level getCurrentLevel() {
        return getLevel();
    }

    @Override
    public BlockPos getPos() {
        return getBlockPos();
    }

    // ---- NBT ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Working", working);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        working = tag.getBoolean("Working");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("Working", working);
        return tag;
    }

    // ---- Tick ----

    /**
     * Per-tick：刷新电力/红石/过载状态，更新 heightBonus。
     * <ul>
     *   <li>正常工作 → heightBonus = +PUMP_HEAD（输入端），-PUMP_HEAD（输出端）</li>
     *   <li>关闭/过载 → heightBonus = 0（阻塞流体）</li>
     * </ul>
     */
    public static void tick(Level level, BlockPos pos, BlockState state, PumpBlockEntity entity) {
        if (level.isClientSide) return;

        boolean powered = state.getValue(PumpBlock.POWERED);
        boolean overload = state.getValue(PumpBlock.OVERLOAD);

        entity.flushState(level, pos);

        boolean wasWorking = entity.working;
        entity.working = !powered && !overload && entity.grid != null && entity.grid.isWorking();

        if (entity.working != wasWorking) {
            entity.setChanged();
            if (!level.isClientSide()) entity.sendUpdate();
        }

        // 设置等效高度偏移：工作中时提供 ±PUMP_HEAD 的扬程，否则为 0
        entity.heightBonus = entity.working ? PUMP_HEAD : 0;
    }
}
