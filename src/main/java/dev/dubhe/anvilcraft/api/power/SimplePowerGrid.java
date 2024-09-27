package dev.dubhe.anvilcraft.api.power;

import dev.dubhe.anvilcraft.client.renderer.PowerGridRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
        .apply(ins, SimplePowerGrid::new)
    );

    private final int hash;
    private final String level;
    private final BlockPos pos;
    private final List<BlockPos> blocks = new ArrayList<>();
    private final List<PowerComponentInfo> powerComponentInfoList = new ArrayList<>();
    private final List<Line> powerTransmitterLines = new ArrayList<>();
    private final int generate; // 发电功率
    private final int consume; // 耗电功率

    @Getter
    private final VoxelShape cachedOutlineShape;

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
        this.powerComponentInfoList.addAll(powerComponentInfoList);
        cachedOutlineShape = createMergedOutlineShape();
        createTransmitterVisualLines();
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

    /**
     * 获得指定坐标的电网元件信息
     */
    public Optional<PowerComponentInfo> getInfoForPos(BlockPos pos) {
        return powerComponentInfoList.stream()
            .filter(it -> it.pos().equals(pos))
            .findFirst();
    }

    /**
     * @param grid 电网
     */
    public SimplePowerGrid(@NotNull PowerGrid grid) {
        this.hash = grid.hashCode();
        this.level = grid.getLevel().dimension().location().toString();
        this.pos = grid.getPos();
        Set<IPowerComponent> powerComponents = new HashSet<>();
        powerComponents.addAll(grid.storages);
        powerComponents.addAll(grid.producers);
        powerComponents.addAll(grid.consumers);
        powerComponents.addAll(grid.transmitters);
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
        cachedOutlineShape = Shapes.block();
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

    private VoxelShape createMergedOutlineShape() {
        return this.powerComponentInfoList.stream()
            .map(it -> Shapes.box(
                    -it.range(),
                    -it.range(),
                    -it.range(),
                    it.range() + 1,
                    it.range() + 1,
                    it.range() + 1
                ).move(
                    this.offset(it.pos()).getX(),
                    this.offset(it.pos()).getY(),
                    this.offset(it.pos()).getZ()
                )
            ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
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

    public record Line(Vec3 start, Vec3 end, float distance) {

        public void render(PoseStack pose, VertexConsumer vertex, Vec3 camera, int color) {
            float dx = (float) (this.start().x - this.end().x);
            float dy = (float) (this.start().y - this.end().y);
            float dz = (float) (this.start().z - this.end().z);
            Matrix4f mat = pose.last().pose();
            vertex.addVertex(
                    mat,
                    (float) (this.start().x - camera.x),
                    (float) (this.start().y - camera.y),
                    (float) (this.start().z - camera.z)
                ).setColor(color)
                .setNormal(pose.last(), dx /= this.distance(), dy /= this.distance(), dz /= this.distance());
            vertex.addVertex(
                    mat,
                    (float) (this.end().x - camera.x),
                    (float) (this.end().y - camera.y),
                    (float) (this.end().z - camera.z)
                ).setColor(color)
                .setNormal(pose.last(), dx, dy, dz);
        }
    }
}
