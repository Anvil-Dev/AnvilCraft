package dev.dubhe.anvilcraft.api.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.OutlineUtil;
import dev.anvilcraft.lib.v2.util.client.Line;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.PowerGridSupport;
import dev.dubhe.anvilcraft.util.ColorUtil;
import dev.dubhe.anvilcraft.util.geometry.DelaunayTriangulator;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
        Codec.BOOL.optionalFieldOf("infinitePower", false).forGetter(o -> o.infinitePower)
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
    @Setter
    private Set<Line> powerTransmitterLines = Set.of();
    private final int generate; // 发电功率
    private final int consume; // 耗电功率
    private final boolean infinitePower; // 是否包含无限电力源
    private final int color;
    private List<Line> powerGridBoundLines = new ArrayList<>();
    private @Nullable Future<?> shapeFuture;

    /// 简单电网
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
        this.random.setSeed(id);
        int[] colors = ColorUtil.hsvToRgb(this.random.nextInt(360), 80, 80);
        this.color = ARGB.color((int) (0.4 * 255), colors[0], colors[1], colors[2]);
        this.generate = generate;
        this.consume = consume;
        this.infinitePower = infinitePower;
        this.blocks.addAll(powerComponentInfoList.stream().map(PowerComponentInfo::pos).toList());
        this.powerComponentInfoList.addAll(powerComponentInfoList);
        this.createTransmitterVisualLines();
    }

    public SimplePowerGrid(PowerGrid grid) {
        this.id = grid.hashCode();
        this.level = grid.getLevel().dimension().identifier().toString();
        this.pos = Objects.requireNonNull(grid.getPos());
        Set<IPowerComponent> powerComponents = new HashSet<>();
        powerComponents.addAll(grid.storages);
        powerComponents.addAll(grid.producers);
        powerComponents.addAll(grid.consumers);
        powerComponents.addAll(grid.transmitters);
        this.color = 0;
        for (IPowerComponent component : powerComponents) {
            this.powerComponentInfoList.add(component.toPowerComponentInfo());
        }
        this.consume = grid.getConsume();
        this.generate = grid.getGenerate();
        this.infinitePower = grid.isHasInfinitePower();
    }

    /// 寻找电网
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
        return CODEC.decode(NbtOps.INSTANCE, buf.readNbt().get("data")).getOrThrow().getFirst();
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

    /// 获得指定坐标的电网元件信息
    public Optional<PowerComponentInfo> getInfoForPos(BlockPos pos) {
        return this.powerComponentInfoList.stream().filter(it -> it.pos().equals(pos)).findFirst();
    }

    public boolean isOverloaded() {
        return this.getConsume() > this.getGenerate();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean shouldRender(Vec3 cameraPos) {
        int renderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;
        return this.powerComponentInfoList.stream().anyMatch(it -> it.pos().getCenter().distanceTo(cameraPos) < renderDistance);
    }

    private void createTransmitterVisualLines() {
        if (!this.getPowerTransmitterLines().isEmpty()) return;
        List<Map.Entry<Vec3, Integer>> shapes = this.getPowerComponentInfoList().stream()
            .filter(it -> it.type() == PowerComponentType.TRANSMITTER)
            .map(it -> Map.entry(it.pos().getCenter(), it.range()))
            .toList();
        if (shapes.size() <= 2) {
            if (shapes.size() == 2) {
                this.setPowerTransmitterLines(
                    Set.of(
                        new Line(
                            shapes.get(0).getKey(),
                            shapes.get(1).getKey(),
                            (float) shapes.get(0).getKey().distanceTo(shapes.get(1).getKey())
                        )
                    )
                );
            }
            return;
        }
        Map<Vec3, Integer> map = Map.ofEntries(shapes.<Map.Entry<Vec3, Integer>>toArray(Map.Entry[]::new));
        List<Vec3> points = shapes.stream().map(Map.Entry::getKey).toList();
        Set<Line> lines = new HashSet<>();
        for (DelaunayTriangulator.Edge edge : DelaunayTriangulator.triangulate(
            points.size(), index -> {
                Vec3 vec3 = points.get(index);
                double offset = (vec3.y - 1024) / 2048;
                return new DelaunayTriangulator.Point(index, vec3.x + offset, vec3.z + offset);
            }
        )) {
            Vec3 vec3 = points.get(edge.a());
            Vec3 vec4 = points.get(edge.b());
            int i1 = map.getOrDefault(vec3, 0);
            int i2 = map.getOrDefault(vec4, 0);
            if (!SimplePowerGrid.isOverlap(vec3, i1, vec4, i2)) continue;
            lines.add(new Line(
                vec3,
                vec4,
                (float) vec3.distanceTo(vec4)
            ));
        }
        this.setPowerTransmitterLines(lines);
    }

    public static boolean isOverlap(Vec3 a, int rangeA, Vec3 b, int rangeB) {
        return a.x - rangeA - 0.5 < b.x + rangeB + 0.5
               && a.x + rangeA + 0.5 > b.x - rangeB - 0.5
               && a.y - rangeA - 0.5 < b.y + rangeB + 0.5
               && a.y + rangeA + 0.5 > b.y - rangeB - 0.5
               && a.z - rangeA - 0.5 < b.z + rangeB + 0.5
               && a.z + rangeA + 0.5 > b.z - rangeB - 0.5;
    }

    private void createMergedOutlineShape() {
        if (SimplePowerGrid.EXECUTOR == null || SimplePowerGrid.EXECUTOR.isShutdown()) {
            SimplePowerGrid.recreateExecutorLimitedParallelism();
        }
        this.shapeFuture = SimplePowerGrid.EXECUTOR.submit(() -> {
            long startTime = System.nanoTime();
            List<AABB> input = new ArrayList<>();
            try {
                for (PowerComponentInfo powerComponentInfo : this.powerComponentInfoList) {
                    AABB boundingBox = powerComponentInfo.boundingBox();
                    input.add(boundingBox);
                }
                this.powerGridBoundLines = OutlineUtil.extractOutline(input);
            } catch (RuntimeException e) {
                AnvilCraft.LOGGER.error("Exception thrown while building power grid shape.", e);
            } finally {
                double elapsedMillis = (System.nanoTime() - startTime) / 1_000_000.0;
                AnvilCraft.LOGGER.debug(
                    "Build power grid outline from {} shapes took {} ms",
                    input.size(),
                    String.format(Locale.ROOT, "%.3f", elapsedMillis)
                );
            }
        });
    }

    private BlockPos offset(BlockPos pos) {
        return pos.subtract(this.pos);
    }

    public void destroy() {
        if (this.shapeFuture != null && !this.shapeFuture.isDone()) {
            this.shapeFuture.cancel(true);
        }
    }

    /// 显式请求电网边框
    public void requestGridOutline() {
        if (this.shapeFuture == null) {
            this.createMergedOutlineShape();
        }
    }
}
