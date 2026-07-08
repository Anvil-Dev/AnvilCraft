package dev.dubhe.anvilcraft.block.entity.fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class PipeCheckValveBlockEntity extends BlockEntity {
    private static final String TAG_POWERED = "Powered";
    private static final String TAG_VALVES = "Valves";

    private static final Codec<Direction> DIRECTION_CODEC = Codec.INT.xmap(
        Direction::from3DDataValue,
        Direction::get3DDataValue
    );

    private final Map<Direction, Direction> baseFlow = new EnumMap<>(Direction.class);

    @Getter
    private boolean powered = false;

    public PipeCheckValveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public boolean hasValveOn(Direction face) {
        return this.baseFlow.containsKey(face);
    }

    public boolean isEmpty() {
        return this.baseFlow.isEmpty();
    }

    public void setValve(Direction face, Direction flowOut) {
        this.baseFlow.put(face, flowOut);
        this.setChanged();
    }

    public void removeValve(Direction face) {
        this.baseFlow.remove(face);
        this.setChanged();
    }

    @Nullable
    public Direction getBaseFlow(Direction face) {
        return this.baseFlow.get(face);
    }

    @Nullable
    public Direction effectiveFlow(Direction face) {
        Direction base = this.baseFlow.get(face);
        if (base == null) return null;
        return this.powered ? base.getOpposite() : base;
    }

    public Map<Direction, Direction> effectiveFlows() {
        Map<Direction, Direction> result = new EnumMap<>(Direction.class);
        for (Map.Entry<Direction, Direction> entry : this.baseFlow.entrySet()) {
            result.put(entry.getKey(), this.powered ? entry.getValue().getOpposite() : entry.getValue());
        }
        return result;
    }

    public Map<Direction, Direction> baseFlowCopy() {
        return new EnumMap<>(this.baseFlow);
    }

    public void restore(Map<Direction, Direction> saved, boolean powered) {
        this.baseFlow.clear();
        this.baseFlow.putAll(saved);
        this.powered = powered;
        this.setChanged();
    }

    public boolean setPowered(boolean powered) {
        if (this.powered == powered) return false;
        this.powered = powered;
        this.setChanged();
        return true;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean(TAG_POWERED, this.powered);
        this.writeValves(output);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.powered = input.getBooleanOr(TAG_POWERED, false);
        this.readValves(input);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(new ProblemReporter.Collector(this.problemPath()), registries);
        output.putBoolean(TAG_POWERED, this.powered);
        this.writeValves(output);
        return output.buildResult();
    }

    private void writeValves(ValueOutput output) {
        ValueOutput.TypedOutputList<ValveData> list = output.list(TAG_VALVES, ValveData.CODEC);
        for (Map.Entry<Direction, Direction> entry : this.baseFlow.entrySet()) {
            list.add(new ValveData(entry.getKey(), entry.getValue()));
        }
    }

    private void readValves(ValueInput input) {
        this.baseFlow.clear();
        for (ValveData valve : input.listOrEmpty(TAG_VALVES, ValveData.CODEC)) {
            this.baseFlow.put(valve.face(), valve.flow());
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

    private record ValveData(Direction face, Direction flow) {
        private static final Codec<ValveData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DIRECTION_CODEC.fieldOf("Face").forGetter(ValveData::face),
            DIRECTION_CODEC.fieldOf("Flow").forGetter(ValveData::flow)
        ).apply(instance, ValveData::new));
    }
}
