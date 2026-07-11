package dev.dubhe.anvilcraft.api.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.Line;
import dev.dubhe.anvilcraft.client.support.PowerGridSupport;
import dev.dubhe.anvilcraft.util.ColorUtil;
import lombok.Getter;
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
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
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
    private final List<Line> powerTransmitterLines = new ArrayList<>();
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
        List<Map.Entry<BlockPos, AABB>> shapes = this.powerComponentInfoList.stream()
            .filter(it -> it.type() == PowerComponentType.TRANSMITTER)
            .map(it -> Map.entry(it.pos(), it.boundingBox()))
            .toList();

        for (int i = 0; i < shapes.size(); i++) {
            Map.Entry<BlockPos, AABB> e1 = shapes.get(i);
            for (int j = i + 1; j < shapes.size(); j++) {
                Map.Entry<BlockPos, AABB> e2 = shapes.get(j);
                AABB a = e1.getValue();
                AABB b = e2.getValue();
                if (a.intersects(b)) {
                    Vec3 start = e1.getKey().getCenter();
                    Vec3 end = e2.getKey().getCenter();
                    this.powerTransmitterLines.add(new Line(start, end));
                }
            }
        }
    }

    private void createMergedOutlineShape() {
        if (SimplePowerGrid.EXECUTOR == null || SimplePowerGrid.EXECUTOR.isShutdown()) {
            SimplePowerGrid.recreateExecutorLimitedParallelism();
        }
        this.shapeFuture = SimplePowerGrid.EXECUTOR.submit(() -> {
            List<VoxelShape> input = new ArrayList<>();
            for (PowerComponentInfo it : this.powerComponentInfoList) {
                input.add(Shapes.create(it.boundingBox()));
            }
            // noinspection CatchMayIgnoreException
            try {
                Future<VoxelShape> future = ShapeUtil.threadedJoin(input, BooleanOp.OR, EXECUTOR);
                VoxelShape shape = future.get();
                List<Line> lines = new ArrayList<>();
                shape.forAllEdges((minX, minY, minZ, maxX, maxY, maxZ) -> {
                    Vec3 min = new Vec3(minX, minY, minZ);
                    Vec3 max = new Vec3(maxX, maxY, maxZ);
                    lines.add(new Line(min, max));
                });
                this.powerGridBoundLines = lines;
            } catch (Throwable e) {
                if (e instanceof ExecutionException) {
                    AnvilCraft.LOGGER.error("Exception thrown while building power grid shape.", e);
                }
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
