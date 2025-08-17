package dev.dubhe.anvilcraft.api.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.Line;
import dev.dubhe.anvilcraft.client.support.PowerGridSupport;
import dev.dubhe.anvilcraft.util.ColorUtil;
import dev.dubhe.anvilcraft.util.ShapeUtil;
import dev.dubhe.anvilcraft.util.VirtualThreadFactoryImpl;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.FastColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Getter
public class SimplePowerGrid {
    private static ExecutorService EXECUTOR;
    public static final Codec<SimplePowerGrid> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.fieldOf("hash").forGetter(o -> o.id),
            Codec.STRING.fieldOf("level").forGetter(o -> o.level),
            BlockPos.CODEC.fieldOf("pos").forGetter(o -> o.pos),
            PowerComponentInfo.CODEC
                .listOf()
                .fieldOf("powerComponentInfoList")
                .forGetter(it -> it.powerComponentInfoList),
            Codec.INT.fieldOf("generate").forGetter(o -> o.generate),
            Codec.INT.fieldOf("consume").forGetter(o -> o.consume))
        .apply(ins, SimplePowerGrid::new)
    );

    static {
        recreateExecutor();
    }

    private final Random random = new Random();
    private final int[] EMPTY = {};
    private final int id;
    private final String level;
    private final BlockPos pos;
    private final List<BlockPos> blocks = new ArrayList<>();
    private final List<PowerComponentInfo> powerComponentInfoList = new ArrayList<>();
    private final List<Line> powerTransmitterLines = new ArrayList<>();
    private final int generate; // 发电功率
    private final int consume; // 耗电功率
    private final int color;
    private List<Line> powerGridBoundLines = new ArrayList<>();
    private Future<?> shapeFuture;

    /**
     * 简单电网
     */
    public SimplePowerGrid(
        int id,
        String level,
        BlockPos pos,
        @NotNull List<PowerComponentInfo> powerComponentInfoList,
        int generate,
        int consume
    ) {
        this.pos = pos;
        this.level = level;
        this.id = id;
        random.setSeed(id);
        int[] colors = ColorUtil.hsvToRgb(random.nextInt(360), 80, 80);
        this.color = FastColor.ARGB32.color((int) (0.4 * 255), colors[0], colors[1], colors[2]);
        this.generate = generate;
        this.consume = consume;
        blocks.addAll(powerComponentInfoList.stream().map(PowerComponentInfo::pos).toList());
        this.powerComponentInfoList.addAll(powerComponentInfoList);
        createMergedOutlineShape();
        createTransmitterVisualLines();
    }

    /**
     * @param grid 电网
     */
    public SimplePowerGrid(@NotNull PowerGrid grid) {
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
                    powerComponentInfoList.add(
                        new PowerComponentInfo(
                            it.getPos(),
                            0,
                            0,
                            it.getPowerAmount(),
                            it.getCapacity(),
                            it.getRange(),
                            PowerComponentType.STORAGE
                        )
                    );
                }
                case CONSUMER -> {
                    IPowerConsumer it = (IPowerConsumer) component;
                    powerComponentInfoList.add(
                        new PowerComponentInfo(
                            it.getPos(),
                            it.getInputPower(),
                            0,
                            0,
                            0,
                            it.getRange(),
                            PowerComponentType.CONSUMER
                        )
                    );
                }
                case PRODUCER -> {
                    IPowerProducer it = (IPowerProducer) component;
                    powerComponentInfoList.add(
                        new PowerComponentInfo(
                            it.getPos(),
                            0,
                            it.getOutputPower(),
                            0,
                            0,
                            it.getRange(),
                            PowerComponentType.PRODUCER
                        )
                    );
                }

                case TRANSMITTER -> {
                    IPowerTransmitter it = (IPowerTransmitter) component;
                    powerComponentInfoList.add(
                        new PowerComponentInfo(
                            it.getPos(),
                            0,
                            0,
                            0,
                            0,
                            it.getRange(),
                            PowerComponentType.TRANSMITTER
                        )
                    );
                }

                default -> powerComponentInfoList.add(
                    new PowerComponentInfo(
                        component.getPos(),
                        0,
                        0,
                        0,
                        0,
                        component.getRange(),
                        PowerComponentType.INVALID
                    )
                );
            }
        }
        this.consume = grid.getConsume();
        this.generate = grid.getGenerate();
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

    public static void recreateExecutor() {
        if (EXECUTOR != null) {
            EXECUTOR.shutdownNow();
        }
        EXECUTOR = Executors.newThreadPerTaskExecutor(new VirtualThreadFactoryImpl());
    }

    /**
     * @param buf 缓冲区
     */
    public void encode(@NotNull FriendlyByteBuf buf) {
        Tag tag = CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow();
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
        return powerComponentInfoList.stream()
            .filter(it -> it.pos().equals(pos))
            .findFirst();
    }

    public boolean isOverloaded() {
        return this.getConsume() > this.getGenerate();
    }

    public boolean shouldRender(Vec3 cameraPos) {
        int renderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;
        return powerComponentInfoList.stream()
            .anyMatch(it -> it.pos().getCenter().distanceTo(cameraPos) < renderDistance);
    }

    private void createTransmitterVisualLines() {
        List<Map.Entry<BlockPos, AABB>> shapes = this.powerComponentInfoList.stream()
            .filter(it -> it.type() == PowerComponentType.TRANSMITTER)
            .map(it -> Map.entry(
                it.pos(),
                new AABB(
                    -it.range() + it.pos().getX(),
                    -it.range() + it.pos().getY(),
                    -it.range() + it.pos().getZ(),
                    it.range() + 1 + it.pos().getX(),
                    it.range() + 1 + it.pos().getY(),
                    it.range() + 1 + it.pos().getZ()
                )
            )).toList();

        for (int i = 0; i < shapes.size(); i++) {
            Map.Entry<BlockPos, AABB> e1 = shapes.get(i);
            for (int j = i + 1; j < shapes.size(); j++) {
                Map.Entry<BlockPos, AABB> e2 = shapes.get(j);
                AABB a = e1.getValue();
                AABB b = e2.getValue();
                if (a.intersects(b)) {
                    Vec3 start = e1.getKey().getCenter();
                    Vec3 end = e2.getKey().getCenter();
                    powerTransmitterLines.add(new Line(start, end, (float) start.distanceTo(end)));
                }
            }
        }
    }

    private void createMergedOutlineShape() {
        if (SimplePowerGrid.EXECUTOR.isShutdown()) SimplePowerGrid.recreateExecutor();
        this.shapeFuture = SimplePowerGrid.EXECUTOR.submit(() -> {
            List<VoxelShape> input = new ArrayList<>();
            for (PowerComponentInfo it : powerComponentInfoList) {
                Vec3 center = it.pos().getCenter();
                float size = it.range() * 2 + 1;
                input.add(Shapes.create(AABB.ofSize(center, size, size, size)));
            }
            //noinspection CatchMayIgnoreException
            try {
                Future<VoxelShape> future = ShapeUtil.threadedJoin(input, BooleanOp.OR, EXECUTOR);
                VoxelShape shape = future.get();
                List<Line> lines = new ArrayList<>();
                shape.forAllEdges((minX, minY, minZ, maxX, maxY, maxZ) -> {
                    Vec3 min = new Vec3(minX, minY, minZ);
                    Vec3 max = new Vec3(maxX, maxY, maxZ);
                    lines.add(new Line(min, max, (float) min.distanceTo(max)));
                });
                this.powerGridBoundLines = lines;
            } catch (Throwable e) {
                if (e instanceof ExecutionException) {
                    AnvilCraft.LOGGER.error("Exception thrown while building power grid shape.", e);
                }
            }
        });
    }

    private @NotNull BlockPos offset(@NotNull BlockPos pos) {
        return pos.subtract(this.pos);
    }

    public void destroy() {
        if (!shapeFuture.isDone()) {
            shapeFuture.cancel(true);
        }
    }
}
