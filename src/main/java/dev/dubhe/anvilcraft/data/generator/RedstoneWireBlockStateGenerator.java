package dev.dubhe.anvilcraft.data.generator;

import dev.anvilcraft.lib.v2.registrum.providers.DataGenContext;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumBlockstateProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.RedstoneWireBlock;
import dev.dubhe.anvilcraft.block.RedstoneWireBlock.ConnectionType;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;

import java.util.List;
import javax.annotation.Nullable;

/**
 * 生成红石导线六种附着方向及四向连接组合使用的 multipart 方块状态和部件模型。
 *
 * <p>模型先在统一的“附着面局部坐标系”中描述，再转换到世界方块坐标，因此不需要手工维护六套几何数据。</p>
 */
public class RedstoneWireBlockStateGenerator {
    /** 为红石导线注册中心点、平面线段、跨面拐角和爬升线段的 multipart 条件。 */
    public static <T extends Block> void generate(
        DataGenContext<Block, T> context,
        RegistrumBlockstateProvider provider
    ) {
        var multipart = provider.getMultipartBuilder(context.get());
        for (Direction attachment : Direction.values()) {
            ModelFile dot = dotModel(provider, attachment);
            // 中心点独立受 DOT 控制，使直线只拼接两段线模型，减少重叠面和不必要的过度绘制。
            multipart.part().modelFile(dot).addModel()
                .condition(RedstoneWireBlock.ATTACHMENT, attachment)
                .condition(RedstoneWireBlock.DOT, true)
                .end();

            for (int index = 0; index < 4; index++) {
                ModelFile side = sideModel(provider, attachment, index);
                ModelFile up = upModel(provider, attachment, index);
                var property = RedstoneWireBlock.CONNECTION_PROPERTIES.get(index);
                multipart.part().modelFile(side).addModel()
                    .condition(RedstoneWireBlock.ATTACHMENT, attachment)
                    // UP 由贴面半段和竖直半段叠加组成，所以两种状态都需要基础 side 模型。
                    .condition(property, ConnectionType.SIDE, ConnectionType.UP)
                    .end();
                if (attachment.getAxis().isHorizontal()) {
                    // 只有墙面导线绕支撑块边缘时需要向模型边界外延伸；地面和天花板没有这种显示形态。
                    ModelFile sideCorner = sideCornerModel(provider, attachment, index);
                    multipart.part().modelFile(sideCorner).addModel()
                        .condition(RedstoneWireBlock.ATTACHMENT, attachment)
                        .condition(property, ConnectionType.CORNER)
                        .end();
                    if (RedstoneWireBlock.getLocalDirection(attachment, index) == Direction.UP) {
                        ModelFile sideCornerSp = sideCornerSpModel(provider, attachment, index);
                        multipart.part().modelFile(sideCornerSp).addModel()
                            .condition(RedstoneWireBlock.ATTACHMENT, attachment)
                            .condition(property, ConnectionType.CORNER_SP)
                            .end();
                    }
                }
                multipart.part().modelFile(up).addModel()
                    .condition(RedstoneWireBlock.ATTACHMENT, attachment)
                    .condition(property, ConnectionType.UP)
                    .end();
            }
        }
    }

    /** 生成分叉或转角处用于覆盖线段接缝的中心点模型。 */
    private static BlockModelBuilder dotModel(RegistrumBlockstateProvider provider, Direction attachment) {
        Direction tangent = RedstoneWireBlock.getLocalDirection(attachment, 0);
        BlockModelBuilder model = model(provider, attachment, "dot")
            .texture("0", AnvilCraft.of("block/redstone_wire_dot"))
            .texture("1", AnvilCraft.of("block/redstone_wire_dot_overlay"))
            .texture("particle", AnvilCraft.of("block/redstone_wire_dot"));
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
    private static BlockModelBuilder sideModel(
        RegistrumBlockstateProvider provider, Direction attachment, int index
    ) {
        Direction tangent = RedstoneWireBlock.getLocalDirection(attachment, index);
        BlockModelBuilder model = model(provider, attachment, "side_" + index)
            .texture("0", AnvilCraft.of("block/redstone_wire_line"))
            .texture("1", AnvilCraft.of("block/redstone_wire_line_overlay"))
            .texture("particle", AnvilCraft.of("block/redstone_wire_line"));
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
    private static BlockModelBuilder sideCornerModel(
        RegistrumBlockstateProvider provider, Direction attachment, int index
    ) {
        Direction tangent = RedstoneWireBlock.getLocalDirection(attachment, index);
        BlockModelBuilder model = model(provider, attachment, "side_corner_" + index)
            .texture("0", AnvilCraft.of("block/redstone_wire_line"))
            .texture("1", AnvilCraft.of("block/redstone_wire_line_overlay"))
            .texture("particle", AnvilCraft.of("block/redstone_wire_line"));
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

    /** 生成墙面向上断口连接支撑方块顶面红石粉时使用的专用短拐角。 */
    private static BlockModelBuilder sideCornerSpModel(
        RegistrumBlockstateProvider provider, Direction attachment, int index
    ) {
        Direction tangent = RedstoneWireBlock.getLocalDirection(attachment, index);
        BlockModelBuilder model = model(provider, attachment, "side_corner_sp_" + index)
            .texture("0", AnvilCraft.of("block/redstone_wire_line"))
            .texture("1", AnvilCraft.of("block/redstone_wire_line_overlay"))
            .texture("particle", AnvilCraft.of("block/redstone_wire_line"));
        addBoxWithRotatedUvs(model, attachment, tangent, 5.0, 0.0, 0.0, 11.0, 1.0, 8.0, List.of(
            face(Direction.NORTH, "#0", 5, 0, 11, 1, 0, false, Direction.NORTH),
            face(Direction.EAST, "#0", 10, 0, 11, 8, 90, false, null),
            face(Direction.WEST, "#0", 5, 8, 6, 0, 90, false, null),
            face(Direction.UP, "#0", 5, 0, 11, 8),
            face(Direction.DOWN, "#0", 11, 0, 5, 8, 0, false, Direction.DOWN)
        ));
        addBoxWithRotatedUvs(model, attachment, tangent, 6.0, 0.01, -1.0, 10.0, 2.0, 8.0, List.of(
            face(Direction.NORTH, "#1", 6, 0, 10, 1, 0, true, Direction.NORTH),
            face(Direction.EAST, "#1", 6, 0, 7, 9, 90, true, null),
            face(Direction.WEST, "#1", 6, 0, 7, 9, 90, true, null),
            face(Direction.UP, "#1", 6, 0, 10, 9, 0, true, null),
            face(Direction.DOWN, "#1", 6, 0, 10, 9, 0, true, null)
        ));
        return model;
    }

    /** 生成导线沿前方完整方块侧面向上爬升的竖直部分。 */
    private static BlockModelBuilder upModel(
        RegistrumBlockstateProvider provider, Direction attachment, int index
    ) {
        Direction tangent = RedstoneWireBlock.getLocalDirection(attachment, index);
        BlockModelBuilder model = model(provider, attachment, "up_" + index)
            .texture("0", AnvilCraft.of("block/redstone_wire_line"))
            .texture("1", AnvilCraft.of("block/redstone_wire_line_overlay"))
            .texture("particle", AnvilCraft.of("block/redstone_wire_line"));
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
    private static BlockModelBuilder model(
        RegistrumBlockstateProvider provider, Direction attachment, String part
    ) {
        // 导线很薄且多个部件会互相重叠，AO 会在接缝处产生与信号强度无关的黑边。
        return provider.models().getBuilder("block/redstone_wire/" + attachment.getSerializedName() + "/" + part)
            .ao(false);
    }

    /** 添加一个使用原始面 UV 方向的局部坐标盒。 */
    private static void addBox(
        BlockModelBuilder model,
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
        BlockModelBuilder model,
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
        var element = model.element().from(box[0], box[1], box[2]).to(box[3], box[4], box[5]);
        for (FaceSpec spec : faces) {
            // 几何盒变换后仍是轴对齐盒，但每个局部面对应的世界 Direction 可能已经交换或翻转。
            Direction worldFace = RedstoneWireBlock.transformDirection(attachment, tangent, spec.direction());
            var face = element.face(worldFace)
                .texture(spec.texture())
                .uvs(spec.u1(), spec.v1(), spec.u2(), spec.v2())
                .rotation(rotation(rotateUvs || spec.transformUv()
                                   ? transformedFaceRotation(attachment, tangent, spec.direction(), spec.rotation())
                                   : spec.rotation()));
            if (spec.tinted()) {
                // tintindex 0 由 RegisterColorHandlersEventListener 按方块 POWER 映射为红石颜色。
                face.tintindex(0);
            }
            if (spec.cullFace() != null) {
                // 剔除面也必须应用同一坐标变换，否则引擎会按错误邻接方向隐藏可见表面。
                face.cullface(RedstoneWireBlock.transformDirection(attachment, tangent, spec.cullFace()));
            }
        }
        element.end();
    }

    /** 添加一个会随附着方向修正所有面 UV 旋转的局部坐标盒。 */
    private static void addBoxWithRotatedUvs(
        BlockModelBuilder model,
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

    /** 将任意整数角度规范化为模型 API 支持的四种面旋转。 */
    private static ModelBuilder.FaceRotation rotation(int degrees) {
        // 上游只产生 90 度倍数；floorMod 同时正确处理坐标变换可能得到的负角度。
        return switch (Math.floorMod(degrees, 360)) {
            case 90 -> ModelBuilder.FaceRotation.CLOCKWISE_90;
            case 180 -> ModelBuilder.FaceRotation.UPSIDE_DOWN;
            case 270 -> ModelBuilder.FaceRotation.COUNTERCLOCKWISE_90;
            default -> ModelBuilder.FaceRotation.ZERO;
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
