package dev.dubhe.anvilcraft.api.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.Line;
import dev.dubhe.anvilcraft.client.support.PowerGridSupport;
import dev.dubhe.anvilcraft.util.ColorUtil;
import dev.dubhe.anvilcraft.util.geometry.DelaunayTriangulator;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.FastColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Getter
public class SimplePowerGrid {
    private static @Nullable ExecutorService EXECUTOR;
    public static final Codec<SimplePowerGrid> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        Codec.INT.fieldOf("hash").forGetter(o -> o.id),
        Codec.STRING.fieldOf("level").forGetter(o -> o.level),
        BlockPos.CODEC.fieldOf("pos").forGetter(o -> o.pos),
        PowerComponentInfo.CODEC.listOf().fieldOf("powerComponentInfoList").forGetter(it -> it.powerComponentInfoList),
        Codec.INT.fieldOf("generate").forGetter(o -> o.generate),
        Codec.INT.fieldOf("consume").forGetter(o -> o.consume),
        Codec.BOOL.fieldOf("infinitePower").forGetter(o -> o.infinitePower)
    ).apply(ins, SimplePowerGrid::new));
    public static final StreamCodec<FriendlyByteBuf, SimplePowerGrid> STREAM_CODEC = StreamCodec.of(
        SimplePowerGrid::encode,
        SimplePowerGrid::decode
    );

    static {
        recreateExecutorLimitedParallelism();
    }

    private final Random random = new Random();
    private final int id;
    private final String level;
    private final BlockPos pos;
    private final List<BlockPos> blocks = new ArrayList<>();
    private final List<PowerComponentInfo> powerComponentInfoList = new ArrayList<>();
    private Set<Line> powerTransmitterLines = Set.of();
    private final int generate; // 发电功率
    private final int consume; // 耗电功率
    private final boolean infinitePower; // 是否有无限电力
    private final int color;
    private volatile List<Line> powerGridBoundLines = List.of();
    private @Nullable Future<?> shapeFuture;

    /**
     * 简单电网
     */
    public SimplePowerGrid(
        int id,
        String level,
        BlockPos pos,
        List<PowerComponentInfo> powerComponentInfoList,
        int generate,
        int consume,
        boolean infinitePower
    ) {
        this.pos = pos;
        this.level = level;
        this.id = id;
        random.setSeed(id);
        int[] colors = ColorUtil.hsvToRgb(random.nextInt(360), 80, 80);
        this.color = FastColor.ARGB32.color((int) (0.4 * 255), colors[0], colors[1], colors[2]);
        this.generate = generate;
        this.consume = consume;
        this.infinitePower = infinitePower;
        blocks.addAll(powerComponentInfoList.stream().map(PowerComponentInfo::pos).toList());
        this.powerComponentInfoList.addAll(powerComponentInfoList);
    }

    public SimplePowerGrid(PowerGrid grid) {
        this.id = grid.hashCode();
        this.level = grid.getLevel().dimension().location().toString();
        this.pos = grid.getPos();
        Set<IPowerComponent> powerComponents = new HashSet<>();
        powerComponents.addAll(grid.storages);
        powerComponents.addAll(grid.producers);
        powerComponents.addAll(grid.consumers);
        powerComponents.addAll(grid.transmitters);
        this.color = 0;
        for (IPowerComponent component : powerComponents) {
            switch (component.getComponentType()) {
                case STORAGE -> {
                    IPowerStorage it = (IPowerStorage) component;
                    powerComponentInfoList.add(new PowerComponentInfo(
                        it.getPos(),
                        0,
                        0,
                        it.getPowerAmount(),
                        it.getCapacity(),
                        it.getRange(),
                        PowerComponentType.STORAGE,
                        false
                    ));
                }
                case CONSUMER -> {
                    IPowerConsumer it = (IPowerConsumer) component;
                    powerComponentInfoList.add(new PowerComponentInfo(
                        it.getPos(),
                        it.getInputPower(),
                        0,
                        0,
                        0,
                        it.getRange(),
                        PowerComponentType.CONSUMER,
                        false
                    ));
                }
                case PRODUCER -> {
                    IPowerProducer it = (IPowerProducer) component;
                    powerComponentInfoList.add(new PowerComponentInfo(
                        it.getPos(),
                        0,
                        it.getOutputPower(),
                        0,
                        0,
                        it.getRange(),
                        PowerComponentType.PRODUCER,
                        it.isInfinitePower()
                    ));
                }

                case TRANSMITTER -> {
                    IPowerTransmitter it = (IPowerTransmitter) component;
                    powerComponentInfoList.add(new PowerComponentInfo(
                        it.getPos(),
                        0,
                        0,
                        0,
                        0,
                        it.getRange(),
                        PowerComponentType.TRANSMITTER,
                        false
                    ));
                }

                default -> powerComponentInfoList.add(new PowerComponentInfo(
                    component.getPos(),
                    0,
                    0,
                    0,
                    0,
                    component.getRange(),
                    PowerComponentType.INVALID,
                    false
                ));
            }
        }
        this.consume = grid.getConsume();
        this.generate = grid.getGenerate();
        this.infinitePower = grid.isHasInfinitePower();
    }

    /**
     * 寻找电网
     */
    public static Optional<SimplePowerGrid> findPowerGrid(BlockPos pos) {
        for (SimplePowerGrid value : PowerGridSupport.getGridMap().values()) {
            for (BlockPos block : value.blocks) {
                if (block.equals(pos)) {
                    return Optional.of(value);
                }
            }
        }
        return Optional.empty();
    }

    public static void recreateExecutorLimitedParallelism() {
        if (EXECUTOR != null) {
            EXECUTOR.shutdownNow();
        }
        EXECUTOR = Executors.newFixedThreadPool(
            Math.max(
                Runtime.getRuntime().availableProcessors() / 4,
                4
            )
        );
    }

    public static SimplePowerGrid decode(FriendlyByteBuf buf) {
        // 电网同步包来自服务端（可信），但仍给 NBT 设置一个大上限，避免无限制读取。
        Tag tag = buf.readNbt(NbtAccounter.create(128 * 1024 * 1024));
        if (!(tag instanceof CompoundTag compoundTag)) {
            throw new IllegalStateException("Power grid sync data is not a compound tag");
        }
        return CODEC.decode(NbtOps.INSTANCE, compoundTag.get("data")).getOrThrow().getFirst();
    }

    public static void encode(FriendlyByteBuf buf, SimplePowerGrid grid) {
        Tag tag = CODEC.encodeStart(NbtOps.INSTANCE, grid).getOrThrow();
        CompoundTag data = new CompoundTag();
        data.put("data", tag);
        buf.writeNbt(data);
    }

    public boolean collideFast(AABB aabb) {
        for (PowerComponentInfo it : this.powerComponentInfoList) {
            if (new AABB(it.pos()).inflate(it.range()).intersects(aabb)) return true;
        }
        return false;
    }

    /**
     * 获得指定坐标的电网元件信息
     */
    public Optional<PowerComponentInfo> getInfoForPos(BlockPos pos) {
        return powerComponentInfoList.stream().filter(it -> it.pos().equals(pos)).findFirst();
    }

    public boolean isOverloaded() {
        return this.getConsume() > this.getGenerate();
    }

    public boolean shouldRender(Vec3 cameraPos) {
        int renderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;
        return powerComponentInfoList.stream().anyMatch(it -> it.pos().getCenter().distanceTo(cameraPos) < renderDistance);
    }

    private void createTransmitterVisualLines() {
        if (!this.powerTransmitterLines.isEmpty()) return;
        List<Map.Entry<Vec3, Integer>> shapes = this.powerComponentInfoList.stream()
            .filter(it -> it.type() == PowerComponentType.TRANSMITTER)
            .map(it -> Map.entry(it.pos().getCenter(), it.range()))
            .toList();
        if (shapes.size() <= 2) {
            if (shapes.size() == 2) {
                this.powerTransmitterLines = Set.of(
                    new Line(
                        shapes.get(0).getKey(),
                        shapes.get(1).getKey(),
                        (float) shapes.get(0).getKey().distanceTo(shapes.get(1).getKey())
                    )
                );
            }
            return;
        }
        Map<Vec3, Integer> ranges = new HashMap<>();
        for (Map.Entry<Vec3, Integer> shape : shapes) {
            ranges.put(shape.getKey(), shape.getValue());
        }
        List<Vec3> points = shapes.stream().map(Map.Entry::getKey).toList();
        Set<Line> lines = new HashSet<>();
        for (DelaunayTriangulator.Edge edge : DelaunayTriangulator.triangulate(
            points.size(), index -> {
                Vec3 point = points.get(index);
                double offset = (point.y - 1024) / 2048;
                return new DelaunayTriangulator.Point(index, point.x + offset, point.z + offset);
            }
        )) {
            Vec3 start = points.get(edge.a());
            Vec3 end = points.get(edge.b());
            int startRange = ranges.getOrDefault(start, 0);
            int endRange = ranges.getOrDefault(end, 0);
            if (!SimplePowerGrid.isOverlap(start, startRange, end, endRange)) continue;
            lines.add(new Line(start, end, (float) start.distanceTo(end)));
        }
        this.powerTransmitterLines = lines;
    }

    public static boolean isOverlap(Vec3 a, int rangeA, Vec3 b, int rangeB) {
        return a.x - rangeA - 0.5 < b.x + rangeB + 0.5
               && a.x + rangeA + 0.5 > b.x - rangeB - 0.5
               && a.y - rangeA - 0.5 < b.y + rangeB + 0.5
               && a.y + rangeA + 0.5 > b.y - rangeB - 0.5
               && a.z - rangeA - 0.5 < b.z + rangeB + 0.5
               && a.z + rangeA + 0.5 > b.z - rangeB - 0.5;
    }

    public void rebuildTransmitterVisualLines(@Nullable SimplePowerGrid previous) {
        if (previous != null && this.hasSameTransmitters(previous)) {
            this.powerTransmitterLines = previous.powerTransmitterLines;
            return;
        }
        this.createTransmitterVisualLines();
    }

    private boolean hasSameTransmitters(SimplePowerGrid other) {
        Set<PowerComponentInfo> transmitters = new HashSet<>();
        Set<PowerComponentInfo> otherTransmitters = new HashSet<>();
        for (PowerComponentInfo component : this.powerComponentInfoList) {
            if (component.type() == PowerComponentType.TRANSMITTER) {
                transmitters.add(component);
            }
        }
        for (PowerComponentInfo component : other.powerComponentInfoList) {
            if (component.type() == PowerComponentType.TRANSMITTER) {
                otherTransmitters.add(component);
            }
        }
        return transmitters.equals(otherTransmitters);
    }

    private void createMergedOutlineShape() {
        if (SimplePowerGrid.EXECUTOR == null || SimplePowerGrid.EXECUTOR.isShutdown()) {
            SimplePowerGrid.recreateExecutorLimitedParallelism();
        }
        this.shapeFuture = SimplePowerGrid.EXECUTOR.submit(() -> {
            List<VoxelShape> input = new ArrayList<>();
            for (PowerComponentInfo it : powerComponentInfoList) {
                Vec3 center = it.pos().getCenter();
                float size = it.range() * 2 + 1;
                input.add(Shapes.create(AABB.ofSize(center, size, size, size)));
            }
            try {
                VoxelShape shape = mergeShapes(input);
                List<Line> lines = new ArrayList<>();
                shape.forAllEdges((minX, minY, minZ, maxX, maxY, maxZ) -> {
                    Vec3 min = new Vec3(minX, minY, minZ);
                    Vec3 max = new Vec3(maxX, maxY, maxZ);
                    lines.add(new Line(min, max));
                });
                this.powerGridBoundLines = List.copyOf(lines);
            } catch (RuntimeException e) {
                AnvilCraft.LOGGER.error("Exception thrown while building power grid shape.", e);
            }
        });
    }

    private static VoxelShape mergeShapes(List<VoxelShape> input) {
        if (input.isEmpty()) return Shapes.empty();

        // Keep all merge work in this task: nested submissions can starve the bounded executor.
        List<VoxelShape> shapes = input;
        while (shapes.size() > 1) {
            List<VoxelShape> merged = new ArrayList<>((shapes.size() + 1) / 2);
            for (int i = 0; i < shapes.size(); i += 2) {
                if (i + 1 < shapes.size()) {
                    merged.add(Shapes.join(shapes.get(i), shapes.get(i + 1), BooleanOp.OR));
                } else {
                    merged.add(shapes.get(i));
                }
            }
            shapes = merged;
        }
        return shapes.getFirst();
    }

    private BlockPos offset(BlockPos pos) {
        return pos.subtract(this.pos);
    }

    public void destroy() {
        if (this.shapeFuture != null && !this.shapeFuture.isDone()) {
            shapeFuture.cancel(true);
        }
    }

    /// 显式请求电网边框
    public void requestGridOutline() {
        if (this.shapeFuture == null) {
            this.createMergedOutlineShape();
        }
    }
}
