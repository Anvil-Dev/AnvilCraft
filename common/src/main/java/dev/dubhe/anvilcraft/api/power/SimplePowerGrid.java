package dev.dubhe.anvilcraft.api.power;

import dev.dubhe.anvilcraft.client.renderer.PowerGridRenderer;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class SimplePowerGrid {

    public static final Codec<SimplePowerGrid> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                    Codec.INT.fieldOf("hash").forGetter(o -> o.hash),
                    Codec.STRING.fieldOf("level").forGetter(o -> o.level),
                    BlockPos.CODEC.fieldOf("pos").forGetter(o -> o.pos),
                    PowerComponentInfo.CODEC
                            .listOf()
                            .fieldOf("powerComponentInfoList")
                            .forGetter(it -> it.powerComponentInfoList),
                    Codec.INT.fieldOf("generate").forGetter(o -> o.generate),
                    Codec.INT.fieldOf("consume").forGetter(o -> o.consume))
            .apply(ins, SimplePowerGrid::new));

    private final int hash;
    private final String level;
    private final BlockPos pos;
    private final Map<BlockPos, Integer> ranges = new HashMap<>();
    private List<BlockPos> blocks = new ArrayList<>();
    private final List<PowerComponentInfo> powerComponentInfoList = new ArrayList<>();
    private int generate = 0; // 发电功率
    private int consume = 0; // 耗电功率

    private final Map<BlockPos, PowerComponentInfo> mappedPowerComponentInfo = new HashMap<>();

    /**
     * 简单电网
     */
    public SimplePowerGrid(
            int hash,
            String level,
            BlockPos pos,
            @NotNull List<PowerComponentInfo> powerComponentInfoList,
            int generate,
            int consume) {
        this.pos = pos;
        this.level = level;
        this.hash = hash;
        this.generate = generate;
        this.consume = consume;
        blocks.addAll(powerComponentInfoList.stream().map(PowerComponentInfo::pos).toList());
        powerComponentInfoList.forEach(it -> {
            mappedPowerComponentInfo.put(it.pos(), it);
            ranges.put(it.pos(), it.range());
        });
    }

    /**
     * @param buf 缓冲区
     */
    public void encode(@NotNull FriendlyByteBuf buf) {
        this.blocks = ranges.keySet().stream().toList();
        var tag1 = CODEC.encodeStart(NbtOps.INSTANCE, this);
        Tag tag = CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow(false, ignored -> {});
        CompoundTag data = new CompoundTag();
        data.put("data", tag);
        buf.writeNbt(data);
    }

    /**
     * @param buf 缓冲区
     */
    @Deprecated
    public SimplePowerGrid(@NotNull FriendlyByteBuf buf) {
        this.hash = buf.readInt();
        this.level = buf.readUtf();
        this.pos = buf.readBlockPos();
        int size = buf.readVarInt();
        for (int i = 0; i < size; i++) {
            this.ranges.put(buf.readBlockPos(), buf.readInt());
        }
    }

    /**
     * @param grid 电网
     */
    public SimplePowerGrid(@NotNull PowerGrid grid) {
        this.hash = grid.hashCode();
        this.level = grid.getLevel().dimension().location().toString();
        this.pos = grid.getPos();
        Map<BlockPos, PowerComponentInfo> infoMap = new HashMap<>();
        grid.storages.forEach(it -> infoMap.put(
                it.getPos(),
                new PowerComponentInfo(
                        it.getPos(), 0, 0, it.getPowerAmount(), it.getCapacity(), it.getRange())));
        grid.producers.forEach(it -> infoMap.put(
                it.getPos(),
                new PowerComponentInfo(it.getPos(), 0, it.getOutputPower(), 0, 0, it.getRange())));
        grid.consumers.forEach(it -> infoMap.put(
                it.getPos(),
                new PowerComponentInfo(it.getPos(), it.getInputPower(), 0, 0, 0, it.getRange())));
        grid.transmitters.forEach(it ->
                infoMap.put(it.getPos(), new PowerComponentInfo(it.getPos(), 0, 0, 0, 0, it.getRange())));
        infoMap.values().forEach(it -> {
            this.ranges.put(it.pos(), it.range());
            this.powerComponentInfoList.add(it);
        });
        this.consume = grid.getConsume();
        this.generate = grid.getGenerate();
    }

    /**
     * @return 获取范围
     */
    public VoxelShape getShape() {
        return this.ranges.entrySet().stream()
                .map(entry -> Shapes.box(
                                -entry.getValue(),
                                -entry.getValue(),
                                -entry.getValue(),
                                entry.getValue() + 1,
                                entry.getValue() + 1,
                                entry.getValue() + 1)
                        .move(
                                this.offset(entry.getKey()).getX(),
                                this.offset(entry.getKey()).getY(),
                                this.offset(entry.getKey()).getZ()))
                .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
                .orElse(Shapes.block());
    }

    private @NotNull BlockPos offset(@NotNull BlockPos pos) {
        return pos.subtract(this.pos);
    }

    /**
     * 寻找电网
     */
    public static List<SimplePowerGrid> findPowerGrid(BlockPos pos) {
        return PowerGridRenderer.getGridMap().values().stream()
                .filter(it -> it.blocks.stream().anyMatch(it1 -> it1.equals(pos)))
                .toList();
    }
}
