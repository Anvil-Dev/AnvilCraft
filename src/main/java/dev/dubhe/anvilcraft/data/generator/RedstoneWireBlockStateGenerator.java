package dev.dubhe.anvilcraft.data.generator;

import com.mojang.math.Quadrant;
import dev.anvilcraft.lib.v2.registrum.providers.DataGenContext;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumBlockModelGenerator;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumLegacyBlockModelBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.RedstoneWireBlock;
import dev.dubhe.anvilcraft.block.RedstoneWireBlock.ConnectionType;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 生成红石导线六种附着方向及四向连接组合使用的 multipart 方块状态和部件模型。
 *
 * <p>模型先在统一的“附着面局部坐标系”中描述，再转换到世界方块坐标，因此不需要手工维护六套几何数据。</p>
 */
public class RedstoneWireBlockStateGenerator {
    private static final TextureSlot BASE = TextureSlot.create("0");
    private static final TextureSlot OVERLAY = TextureSlot.create("1");

    /** 为红石导线注册中心点、平面线段、跨面拐角和爬升线段的 multipart 条件。 */
    public static <T extends Block> void generate(
        DataGenContext<Block, T> context,
        RegistrumBlockModelGenerator provider
    ) {
        MultiPartGenerator multipart = MultiPartGenerator.multiPart(context.get());
        for (Direction attachment : Direction.values()) {
            Identifier dot = dotModel(provider, attachment)
                .build(modelLocation(provider, attachment, "dot"));
            multipart.with(
                BlockModelGenerators.condition()
                    .term(RedstoneWireBlock.ATTACHMENT, attachment)
                    .term(RedstoneWireBlock.DOT, true),
                BlockModelGenerators.plainVariant(dot)
            );

            for (int index = 0; index < 4; index++) {
                Identifier side = sideModel(provider, attachment, index)
                    .build(modelLocation(provider, attachment, "side_" + index));
                Identifier up = upModel(provider, attachment, index)
                    .build(modelLocation(provider, attachment, "up_" + index));
                var property = RedstoneWireBlock.CONNECTION_PROPERTIES.get(index);
                multipart.with(
                    BlockModelGenerators.condition()
                        .term(RedstoneWireBlock.ATTACHMENT, attachment)
                        .term(property, ConnectionType.SIDE, ConnectionType.UP),
                    BlockModelGenerators.plainVariant(side)
                );
                if (attachment.getAxis().isHorizontal()) {
                    Identifier sideCorner = sideCornerModel(provider, attachment, index)
                        .build(modelLocation(provider, attachment, "side_corner_" + index));
                    multipart.with(
                        BlockModelGenerators.condition()
                            .term(RedstoneWireBlock.ATTACHMENT, attachment)
                            .term(property, ConnectionType.CORNER),
                        BlockModelGenerators.plainVariant(sideCorner)
                    );
                }
                multipart.with(
                    BlockModelGenerators.condition()
                        .term(RedstoneWireBlock.ATTACHMENT, attachment)
                        .term(property, ConnectionType.UP),
                    BlockModelGenerators.plainVariant(up)
                );
            }
        }
        provider.blockStateOutput.accept(multipart);
    }

    /** 生成分叉或转角处用于覆盖线段接缝的中心点模型。 */
    private static RegistrumLegacyBlockModelBuilder dotModel(RegistrumBlockModelGenerator provider, Direction attachment) {
        Direction tangent = RedstoneWireBlock.getLocalDirection(attachment, 0);
        RegistrumLegacyBlockModelBuilder model = model(provider, attachment, "dot")
            .texture(BASE, AnvilCraft.of("block/redstone_wire_dot"))
            .texture(OVERLAY, AnvilCraft.of("block/redstone_wire_dot_overlay"))
            .texture(TextureSlot.PARTICLE, AnvilCraft.of("block/redstone_wire_dot"));
        // 底层提供固定材质边缘，上层使用 tintindex 0 随 POWER 改变颜色，与原版红石粉视觉一致。
        addBox(model, attachment, tangent, 4.0, -0.5, 4.0, 12.0, 1.5, 12.0, List.of(
            face(Direction.NORTH, "#0", 4, 4, 12, 6),
            transformedUvFace(attachment.getAxis().isHorizontal(), Direction.EAST, "#0", 4, 4, 12, 6),
            face(Direction.SOUTH, "#0", 4, 4, 12, 6),
            transformedUvFace(attachment.getAxis().isHorizontal(), Direction.WEST, "#0", 4, 4, 12, 6),
            face(Direction.UP, "#0", 4, 4, 12, 12),
            face(Direction.DOWN, "#0", 4, 4, 12, 12, 0, false, Direction.DOWN)
        ));
        addBox(model, attachment, tangent, 5.0, 1.5, 5.0, 11.0, 2.5, 11.0, List.of(
            face(Direction.NORTH, "#1", 5, 5, 11, 6, 0, true, null),
            transformedUvFace(
                attachment.getAxis().isHorizontal(), Direction.EAST, "#1", 5, 5, 11, 6, 0, true, null
            ),
            face(Direction.SOUTH, "#1", 5, 5, 11, 6, 0, true, null),
            transformedUvFace(
                attachment.getAxis().isHorizontal(), Direction.WEST, "#1", 5, 5, 11, 6, 0, true, null
            ),
            face(Direction.UP, "#1", 5, 5, 11, 11, 0, true, null)
        ));
        return model;
    }

    /** 生成沿附着面从中心延伸到一个端点的基础线段。 */
    private static RegistrumLegacyBlockModelBuilder sideModel(
        RegistrumBlockModelGenerator provider, Direction attachment, int index
    ) {
        Direction tangent = RedstoneWireBlock.getLocalDirection(attachment, index);
        RegistrumLegacyBlockModelBuilder model = model(provider, attachment, "side_" + index)
            .texture(BASE, AnvilCraft.of("block/redstone_wire_line"))
            .texture(OVERLAY, AnvilCraft.of("block/redstone_wire_line_overlay"))
            .texture(TextureSlot.PARTICLE, AnvilCraft.of("block/redstone_wire_line"));
        // 线段会旋转到不同表面，侧面 UV 也要随局部基旋转，否则同一纹理会在部分朝向上镜像或倒置。
        addBoxWithRotatedUvs(model, attachment, tangent, 5.0, 0.0, 0.0, 11.0, 1.0, 8.0, List.of(
            face(Direction.NORTH, "#0", 5, 0, 11, 1, 0, false, Direction.NORTH),
            face(Direction.EAST, "#0", 10, 0, 11, 8, 90, false, null),
            face(Direction.WEST, "#0", 5, 8, 6, 0, 90, false, null),
            face(Direction.UP, "#0", 5, 0, 11, 8),
            face(Direction.DOWN, "#0", 11, 0, 5, 8, 0, false, Direction.DOWN)
        ));
        addBoxWithRotatedUvs(model, attachment, tangent, 6.0, 1.0, 0.0, 10.0, 2.0, 8.0, List.of(
            face(Direction.NORTH, "#1", 6, 0, 10, 1, 0, true, Direction.NORTH),
            face(Direction.EAST, "#1", 6, 0, 7, 8, 90, true, null),
            face(Direction.WEST, "#1", 6, 0, 7, 8, 90, true, null),
            face(Direction.UP, "#1", 6, 0, 10, 8, 0, true, null)
        ));
        return model;
    }

    /** 生成墙面导线绕支撑方块边缘连接时使用的加长线段。 */
    private static RegistrumLegacyBlockModelBuilder sideCornerModel(
        RegistrumBlockModelGenerator provider, Direction attachment, int index
    ) {
        Direction tangent = RedstoneWireBlock.getLocalDirection(attachment, index);
        RegistrumLegacyBlockModelBuilder model = model(provider, attachment, "side_corner_" + index)
            .texture(BASE, AnvilCraft.of("block/redstone_wire_line"))
            .texture(OVERLAY, AnvilCraft.of("block/redstone_wire_line_overlay"))
            .texture(TextureSlot.PARTICLE, AnvilCraft.of("block/redstone_wire_line"));
        // 负局部 Z 部分越过当前方块边界，用来遮住两个不同附着面模型在实体边缘留下的缝隙。
        addBoxWithRotatedUvs(model, attachment, tangent, 5.0, 0.0, -1.0, 11.0, 1.0, 8.0, List.of(
            face(Direction.NORTH, "#0", 5, 0, 11, 1, 0, false, Direction.NORTH),
            face(Direction.EAST, "#0", 10, 0, 11, 9, 90, false, null),
            face(Direction.WEST, "#0", 5, 9, 6, 0, 90, false, null),
            face(Direction.UP, "#0", 5, 0, 11, 9),
            face(Direction.DOWN, "#0", 11, 0, 5, 9, 0, false, Direction.DOWN)
        ));
        addBoxWithRotatedUvs(model, attachment, tangent, 6.0, 0.0, -2.0, 10.0, 2.0, 8.0, List.of(
            face(Direction.NORTH, "#1", 6, 0, 10, 1, 0, true, Direction.NORTH),
            face(Direction.EAST, "#1", 6, 0, 7, 10, 90, true, null),
            face(Direction.WEST, "#1", 6, 0, 7, 10, 90, true, null),
            face(Direction.UP, "#1", 6, 0, 10, 10, 0, true, null)
        ));
        return model;
    }

    /** 生成导线沿前方完整方块侧面向上爬升的竖直部分。 */
    private static RegistrumLegacyBlockModelBuilder upModel(
        RegistrumBlockModelGenerator provider, Direction attachment, int index
    ) {
        Direction tangent = RedstoneWireBlock.getLocalDirection(attachment, index);
        RegistrumLegacyBlockModelBuilder model = model(provider, attachment, "up_" + index)
            .texture(BASE, AnvilCraft.of("block/redstone_wire_line"))
            .texture(OVERLAY, AnvilCraft.of("block/redstone_wire_line_overlay"))
            .texture(TextureSlot.PARTICLE, AnvilCraft.of("block/redstone_wire_line"));
        // 爬升段跨满 16 像素高度，并与基础 side 模型叠加，形成从当前表面到高一格表面的连续导线。
        addBox(model, attachment, tangent, 5.0, 1.0, -0.1, 11.0, 17.0, 1.0, List.of(
            face(Direction.NORTH, "#0", 11, 0, 5, 16, 180, false, Direction.NORTH),
            face(Direction.EAST, "#0", 10, 0, 11, 16),
            face(Direction.SOUTH, "#0", 5, 0, 11, 16),
            face(Direction.WEST, "#0", 5, 16, 6, 0, 180, false, null),
            face(Direction.UP, "#0", 5, 0, 11, 1, 180, false, Direction.UP),
            face(Direction.DOWN, "#0", 5, 15, 11, 16)
        ), attachment.getAxis().isHorizontal());
        addBox(model, attachment, tangent, 6.0, 2.0, 0.0, 10.0, 18.0, 2.0, List.of(
            face(Direction.EAST, "#1", 6, 0, 8, 16, 0, true, null),
            face(Direction.SOUTH, "#1", 6, 0, 10, 16, 0, true, null),
            face(Direction.WEST, "#1", 6, 0, 8, 16, 180, true, null),
            face(Direction.UP, "#1", 6, 0, 10, 1, 180, true, Direction.UP),
            face(Direction.DOWN, "#1", 6, 15, 10, 16)
        ), attachment.getAxis().isHorizontal());
        return model;
    }

    /** 创建一个关闭环境光遮蔽的导线部件模型构建器。 */
    private static RegistrumLegacyBlockModelBuilder model(
        RegistrumBlockModelGenerator provider, Direction attachment, String part
    ) {
        return provider.getBuilder().ambientOcclusion(false);
    }

    private static Identifier modelLocation(
        RegistrumBlockModelGenerator provider, Direction attachment, String part
    ) {
        return provider.modLoc(
            "block/redstone_wire/" + attachment.getSerializedName() + "/" + part
        );
    }

    /** 添加一个使用原始面 UV 方向的局部坐标盒。 */
    private static void addBox(
        RegistrumLegacyBlockModelBuilder model,
        Direction attachment,
        Direction tangent,
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ,
        List<FaceSpec> faces
    ) {
        addBox(model, attachment, tangent, minX, minY, minZ, maxX, maxY, maxZ, faces, false);
    }

    /** 将局部坐标盒及其各面规格转换为实际世界朝向后写入模型。 */
    private static void addBox(
        RegistrumLegacyBlockModelBuilder model,
        Direction attachment,
        Direction tangent,
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ,
        List<FaceSpec> faces,
        boolean rotateUvs
    ) {
        float[] box = RedstoneWireBlock.transformBox(
            attachment, tangent, minX, minY, minZ, maxX, maxY, maxZ
        );
        model.transformTemplate(template -> template.element(element -> {
            element.from(box[0], box[1], box[2]).to(box[3], box[4], box[5]);
            for (FaceSpec spec : faces) {
                Direction worldFace = RedstoneWireBlock.transformDirection(
                    attachment, tangent, spec.direction()
                );
                element.face(worldFace, face -> {
                    face.texture(textureSlot(spec.texture()))
                        .uvs(spec.u1(), spec.v1(), spec.u2(), spec.v2())
                        .rotation(rotation(rotateUvs || spec.transformUv()
                            ? transformedFaceRotation(
                                attachment, tangent, spec.direction(), spec.rotation()
                            )
                            : spec.rotation()));
                    if (spec.tinted()) {
                        face.tintindex(0);
                    }
                    if (spec.cullFace() != null) {
                        face.cullface(RedstoneWireBlock.transformDirection(
                            attachment, tangent, spec.cullFace()
                        ));
                    }
                });
            }
        }));
    }

    /** 添加一个会随附着方向修正所有面 UV 旋转的局部坐标盒。 */
    private static void addBoxWithRotatedUvs(
        RegistrumLegacyBlockModelBuilder model,
        Direction attachment,
        Direction tangent,
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ,
        List<FaceSpec> faces
    ) {
        addBox(model, attachment, tangent, minX, minY, minZ, maxX, maxY, maxZ, faces, true);
    }

    /** 计算一个局部模型面变换到世界方向后，为保持纹理顶点朝向所需的 UV 旋转。 */
    private static int transformedFaceRotation(
        Direction attachment, Direction tangent, Direction localFace, int rotation
    ) {
        Direction worldFace = RedstoneWireBlock.transformDirection(attachment, tangent, localFace);
        // 用目标面的第一个标准顶点作为锚点，查找局部面哪个顶点在基变换后落到该位置。
        int[] targetVertex = faceVertices(worldFace)[0];
        int[][] localVertices = faceVertices(localFace);
        Direction worldX = RedstoneWireBlock.transformDirection(attachment, tangent, Direction.EAST);
        Direction worldY = RedstoneWireBlock.transformDirection(attachment, tangent, Direction.UP);
        Direction worldZ = RedstoneWireBlock.transformDirection(attachment, tangent, Direction.SOUTH);
        for (int index = 0; index < localVertices.length; index++) {
            int[] local = localVertices[index];
            int worldVertexX = worldX.getStepX() * local[0]
                + worldY.getStepX() * local[1]
                + worldZ.getStepX() * local[2];
            int worldVertexY = worldX.getStepY() * local[0]
                + worldY.getStepY() * local[1]
                + worldZ.getStepY() * local[2];
            int worldVertexZ = worldX.getStepZ() * local[0]
                + worldY.getStepZ() * local[1]
                + worldZ.getStepZ() * local[2];
            if (worldVertexX == targetVertex[0]
                && worldVertexY == targetVertex[1]
                && worldVertexZ == targetVertex[2]) {
                // 每错开一个顶点就对应 90 度 UV 旋转，再叠加该面的显式基础旋转。
                return Math.floorMod(rotation + index * 90, 360);
            }
        }
        throw new IllegalStateException("Unable to transform UV rotation for " + localFace);
    }

    /** 按 Minecraft 模型 UV 的标准环绕顺序返回指定面的四个单位立方体顶点。 */
    private static int[][] faceVertices(Direction direction) {
        return switch (direction) {
            case DOWN -> new int[][]{{-1, -1, 1}, {-1, -1, -1}, {1, -1, -1}, {1, -1, 1}};
            case UP -> new int[][]{{-1, 1, -1}, {-1, 1, 1}, {1, 1, 1}, {1, 1, -1}};
            case NORTH -> new int[][]{{1, 1, -1}, {1, -1, -1}, {-1, -1, -1}, {-1, 1, -1}};
            case SOUTH -> new int[][]{{-1, 1, 1}, {-1, -1, 1}, {1, -1, 1}, {1, 1, 1}};
            case WEST -> new int[][]{{-1, 1, -1}, {-1, -1, -1}, {-1, -1, 1}, {-1, 1, 1}};
            case EAST -> new int[][]{{1, 1, 1}, {1, -1, 1}, {1, -1, -1}, {1, 1, -1}};
        };
    }

    /** 创建一个无需着色、剔除或额外旋转的模型面规格。 */
    private static FaceSpec face(
        Direction direction, String texture, float u1, float v1, float u2, float v2
    ) {
        return face(direction, texture, u1, v1, u2, v2, 0, false, null);
    }

    /** 创建具有完整渲染参数的模型面规格。 */
    private static FaceSpec face(
        Direction direction,
        String texture,
        float u1,
        float v1,
        float u2,
        float v2,
        int rotation,
        boolean tinted,
        @Nullable Direction cullFace
    ) {
        return new FaceSpec(direction, texture, u1, v1, u2, v2, rotation, tinted, cullFace, false);
    }

    /** 创建一个可按条件启用坐标变换后 UV 修正的简单模型面规格。 */
    private static FaceSpec transformedUvFace(
        boolean transformUv, Direction direction, String texture, float u1, float v1, float u2, float v2
    ) {
        return transformedUvFace(transformUv, direction, texture, u1, v1, u2, v2, 0, false, null);
    }

    /** 创建一个可按条件启用坐标变换后 UV 修正的完整模型面规格。 */
    private static FaceSpec transformedUvFace(
        boolean transformUv,
        Direction direction,
        String texture,
        float u1,
        float v1,
        float u2,
        float v2,
        int rotation,
        boolean tinted,
        @Nullable Direction cullFace
    ) {
        return new FaceSpec(direction, texture, u1, v1, u2, v2, rotation, tinted, cullFace, transformUv);
    }

    private static Quadrant rotation(int degrees) {
        return switch (Math.floorMod(degrees, 360)) {
            case 90 -> Quadrant.R90;
            case 180 -> Quadrant.R180;
            case 270 -> Quadrant.R270;
            default -> Quadrant.R0;
        };
    }

    private static TextureSlot textureSlot(String texture) {
        return switch (texture) {
            case "#0" -> BASE;
            case "#1" -> OVERLAY;
            default -> throw new IllegalArgumentException("Unknown texture slot: " + texture);
        };
    }

    /** 一个局部模型面的纹理、UV、着色、剔除及 UV 变换参数。 */
    private record FaceSpec(
        Direction direction,
        String texture,
        float u1,
        float v1,
        float u2,
        float v2,
        int rotation,
        boolean tinted,
        @Nullable Direction cullFace,
        boolean transformUv
    ) {
    }
}
