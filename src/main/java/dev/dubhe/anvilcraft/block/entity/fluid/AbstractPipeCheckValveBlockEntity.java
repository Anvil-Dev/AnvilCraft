package dev.dubhe.anvilcraft.block.entity.fluid;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * 管道止逆阀数据的 BlockEntity 基类 —— 止逆功能<b>与管道结合</b>的存储载体。
 *
 * <p>止逆阀不是独立方块，而是管道<b>某个连接面上的单向流动约束</b>：
 * 每个装有止逆阀的面记录一个"允许流出的世界方向"（{@link #baseFlow}），
 * 流体只能沿该方向穿过这个面。普通管道仅在 {@code HAS_CHECK_VALVE} 为 {@code true} 时
 * 创建具体 BE；玻璃管道因流体显示需要常驻 BE，并复用这里的止逆阀数据。
 *
 * <h3>方向语义</h3>
 * {@code baseFlow.get(face)} = 无红石信号时该面允许的流出方向（通常即 {@code face} 本身，
 * 表示"沿臂向外流出"）。收到红石信号（{@link #powered}）时全部反向，见 {@link #effectiveFlow}。
 *
 * <h3>无 ticker</h3>
 * 纯数据 BE，不参与 tick；流体分配由 {@link dev.dubhe.anvilcraft.api.fluid.network.FluidPipeNetwork}
 * 扫描时读取本 BE 的面约束完成。
 */
public abstract class AbstractPipeCheckValveBlockEntity extends BlockEntity {
    /** 各装有止逆阀的面 → 无红石信号时允许流出的世界方向（基准方向）。 */
    private final Map<Direction, Direction> baseFlow = new EnumMap<>(Direction.class);

    /** 红石反向：任意侧收到红石信号则所有面流向反转。 */
    @Getter
    private boolean powered = false;

    protected AbstractPipeCheckValveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /** 该面是否装有止逆阀。 */
    public boolean hasValveOn(Direction face) {
        return baseFlow.containsKey(face);
    }

    /** 是否一个面都没有（用于判定是否应清除 {@code HAS_CHECK_VALVE}）。 */
    public boolean isEmpty() {
        return baseFlow.isEmpty();
    }

    /**
     * 在某面添加/覆盖止逆阀。
     *
     * @param face     装阀的面
     * @param flowOut  无红石信号时允许流出的世界方向
     */
    public void setValve(Direction face, Direction flowOut) {
        baseFlow.put(face, flowOut);
        this.setChanged();
    }

    /** 移除某面的止逆阀。 */
    public void removeValve(Direction face) {
        baseFlow.remove(face);
        this.setChanged();
    }

    /** 某面止逆阀的基准（无红石）允许流出方向；无阀返回 {@code null}。 */
    @Nullable
    public Direction getBaseFlow(Direction face) {
        return baseFlow.get(face);
    }

    /**
     * 某面止逆阀当前<b>实际</b>允许的流出方向（计入红石反向）；无阀返回 {@code null}。
     * 流体仅允许沿此方向穿过该面。
     */
    @Nullable
    public Direction effectiveFlow(Direction face) {
        Direction base = baseFlow.get(face);
        if (base == null) {
            return null;
        }
        return powered ? base.getOpposite() : base;
    }

    /** 各装阀面的当前实际允许流出方向快照（供网络扫描 / 渲染使用）。 */
    public Map<Direction, Direction> effectiveFlows() {
        Map<Direction, Direction> result = new EnumMap<>(Direction.class);
        for (Map.Entry<Direction, Direction> entry : baseFlow.entrySet()) {
            result.put(entry.getKey(), powered ? entry.getValue().getOpposite() : entry.getValue());
        }
        return result;
    }

    /** 基准面映射的一份拷贝（用于跨管型转换时迁移数据）。 */
    public Map<Direction, Direction> baseFlowCopy() {
        return new EnumMap<>(baseFlow);
    }

    /** 用迁移快照恢复面映射与红石状态。 */
    public void restore(Map<Direction, Direction> saved, boolean powered) {
        baseFlow.clear();
        baseFlow.putAll(saved);
        this.powered = powered;
        this.setChanged();
    }

    /** 更新红石反向状态；发生变化返回 {@code true}（调用方据此使网络缓存失效）。 */
    public boolean setPowered(boolean powered) {
        if (this.powered == powered) {
            return false;
        }
        this.powered = powered;
        this.setChanged();
        return true;
    }

    // ---- NBT ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Powered", powered);
        tag.put("Valves", writeValves());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.powered = tag.getBoolean("Powered");
        readValves(tag.getList("Valves", Tag.TAG_COMPOUND));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("Powered", powered);
        tag.put("Valves", writeValves());
        return tag;
    }

    private ListTag writeValves() {
        ListTag list = new ListTag();
        for (Map.Entry<Direction, Direction> entry : baseFlow.entrySet()) {
            CompoundTag e = new CompoundTag();
            e.putInt("Face", entry.getKey().get3DDataValue());
            e.putInt("Flow", entry.getValue().get3DDataValue());
            list.add(e);
        }
        return list;
    }

    private void readValves(ListTag list) {
        baseFlow.clear();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            Direction face = Direction.from3DDataValue(e.getInt("Face"));
            Direction flow = Direction.from3DDataValue(e.getInt("Flow"));
            baseFlow.put(face, flow);
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void sendUpdate() {
        if (this.level != null) {
            this.level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
