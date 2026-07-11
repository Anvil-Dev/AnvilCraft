package dev.dubhe.anvilcraft.data.generator;

import dev.anvilcraft.lib.v2.registrum.providers.DataGenContext;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumBlockstateProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.RedstoneWireBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;

import java.util.List;
import javax.annotation.Nullable;

public class RedstoneWireBlockStateGenerator {
    public static <T extends Block> void generate(
        DataGenContext<Block, T> context,
        RegistrumBlockstateProvider provider
    ) {
        var multipart = provider.getMultipartBuilder(context.get());
        ModelFile upCorner = provider.models().getExistingFile(AnvilCraft.of("block/redstone_wire_up_corner"));
        ModelFile downCorner = provider.models().getExistingFile(AnvilCraft.of("block/redstone_wire_down_corner"));
        for (Direction attachment : Direction.values()) {
            ModelFile dot = dotModel(provider, attachment);
            multipart.part().modelFile(dot).addModel()
                .condition(RedstoneWireBlock.ATTACHMENT, attachment)
                .condition(RedstoneWireBlock.DOT, true)
                .end();

            for (int index = 0; index < 4; index++) {
                ModelFile side = sideModel(provider, attachment, index);
                Direction tangent = RedstoneWireBlock.getLocalDirection(attachment, index);
                var property = RedstoneWireBlock.CONNECTION_PROPERTIES.get(index);
                boolean corner = attachment.getAxis().isHorizontal() && tangent.getAxis() == Direction.Axis.Y;
                if (corner) {
                    var opposite = RedstoneWireBlock.CONNECTION_PROPERTIES.get((index + 2) % 4);
                    multipart.part().modelFile(side).addModel()
                        .condition(RedstoneWireBlock.ATTACHMENT, attachment)
                        .condition(property, RedstoneSide.SIDE)
                        .condition(opposite, RedstoneSide.NONE, RedstoneSide.SIDE)
                        .end();
                    multipart.part()
                        .modelFile(tangent == Direction.UP ? upCorner : downCorner)
                        .rotationY(wallRotation(attachment))
                        .addModel()
                        .condition(RedstoneWireBlock.ATTACHMENT, attachment)
                        .condition(property, RedstoneSide.UP)
                        .end();
                } else {
                    ModelFile up = upModel(provider, attachment, index);
                    multipart.part().modelFile(side).addModel()
                        .condition(RedstoneWireBlock.ATTACHMENT, attachment)
                        .condition(property, RedstoneSide.SIDE, RedstoneSide.UP)
                        .end();
                    multipart.part().modelFile(up).addModel()
                        .condition(RedstoneWireBlock.ATTACHMENT, attachment)
                        .condition(property, RedstoneSide.UP)
                        .end();
                }
            }
        }
    }

    private static int wallRotation(Direction attachment) {
        return switch (attachment) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }

    private static BlockModelBuilder dotModel(RegistrumBlockstateProvider provider, Direction attachment) {
        Direction tangent = RedstoneWireBlock.getLocalDirection(attachment, 0);
        BlockModelBuilder model = model(provider, attachment, "dot")
            .texture("0", AnvilCraft.of("block/redstone_wire_dot"))
            .texture("1", AnvilCraft.of("block/redstone_wire_dot_overlay"))
            .texture("particle", AnvilCraft.of("block/redstone_wire_dot"));
        addBox(model, attachment, tangent, 4.0, -0.5, 4.0, 12.0, 1.5, 12.0, List.of(
            face(Direction.NORTH, "#0", 4, 4, 12, 6),
            face(Direction.EAST, "#0", 4, 4, 12, 6),
            face(Direction.SOUTH, "#0", 4, 4, 12, 6),
            face(Direction.WEST, "#0", 4, 4, 12, 6),
            face(Direction.UP, "#0", 4, 4, 12, 12),
            face(Direction.DOWN, "#0", 4, 4, 12, 12, 0, false, Direction.DOWN)
        ));
        addBox(model, attachment, tangent, 5.0, 1.5, 5.0, 11.0, 2.5, 11.0, List.of(
            face(Direction.NORTH, "#1", 5, 5, 11, 6, 0, true, null),
            face(Direction.EAST, "#1", 5, 5, 11, 6, 0, true, null),
            face(Direction.SOUTH, "#1", 5, 5, 11, 6, 0, true, null),
            face(Direction.WEST, "#1", 5, 5, 11, 6, 0, true, null),
            face(Direction.UP, "#1", 5, 5, 11, 11, 0, true, null)
        ));
        return model;
    }

    private static BlockModelBuilder sideModel(
        RegistrumBlockstateProvider provider, Direction attachment, int index
    ) {
        Direction tangent = RedstoneWireBlock.getLocalDirection(attachment, index);
        BlockModelBuilder model = model(provider, attachment, "side_" + index)
            .texture("0", AnvilCraft.of("block/redstone_wire_line"))
            .texture("1", AnvilCraft.of("block/redstone_wire_line_overlay"))
            .texture("particle", AnvilCraft.of("block/redstone_wire_line"));
        addBox(model, attachment, tangent, 5.0, 0.0, 0.0, 11.0, 1.0, 8.0, List.of(
            face(Direction.NORTH, "#0", 5, 0, 11, 1, 0, false, Direction.NORTH),
            face(Direction.EAST, "#0", 10, 0, 11, 8, 90, false, null),
            face(Direction.WEST, "#0", 5, 8, 6, 0, 90, false, null),
            face(Direction.UP, "#0", 5, 0, 11, 8),
            face(Direction.DOWN, "#0", 11, 0, 5, 8, 0, false, Direction.DOWN)
        ));
        addBox(model, attachment, tangent, 6.0, 1.0, 0.0, 10.0, 2.0, 8.0, List.of(
            face(Direction.NORTH, "#1", 6, 0, 10, 1, 0, true, Direction.NORTH),
            face(Direction.EAST, "#1", 6, 0, 7, 8, 90, true, null),
            face(Direction.WEST, "#1", 6, 0, 7, 8, 90, true, null),
            face(Direction.UP, "#1", 6, 0, 10, 8, 0, true, null)
        ));
        return model;
    }

    private static BlockModelBuilder upModel(
        RegistrumBlockstateProvider provider, Direction attachment, int index
    ) {
        Direction tangent = RedstoneWireBlock.getLocalDirection(attachment, index);
        BlockModelBuilder model = model(provider, attachment, "up_" + index)
            .texture("0", AnvilCraft.of("block/redstone_wire_line"))
            .texture("1", AnvilCraft.of("block/redstone_wire_line_overlay"))
            .texture("particle", AnvilCraft.of("block/redstone_wire_line"));
        addBox(model, attachment, tangent, 5.0, 1.0, -0.1, 11.0, 17.0, 1.0, List.of(
            face(Direction.NORTH, "#0", 11, 0, 5, 16, 180, false, Direction.NORTH),
            face(Direction.EAST, "#0", 10, 0, 11, 16),
            face(Direction.SOUTH, "#0", 5, 0, 11, 16),
            face(Direction.WEST, "#0", 5, 16, 6, 0, 180, false, null),
            face(Direction.UP, "#0", 5, 0, 11, 1, 180, false, Direction.UP),
            face(Direction.DOWN, "#0", 5, 15, 11, 16)
        ));
        addBox(model, attachment, tangent, 6.0, 2.0, 0.0, 10.0, 18.0, 2.0, List.of(
            face(Direction.EAST, "#1", 6, 0, 8, 16, 0, true, null),
            face(Direction.SOUTH, "#1", 6, 0, 10, 16, 0, true, null),
            face(Direction.WEST, "#1", 6, 0, 8, 16, 180, true, null),
            face(Direction.UP, "#1", 6, 0, 10, 1, 180, true, Direction.UP),
            face(Direction.DOWN, "#1", 6, 15, 10, 16)
        ));
        return model;
    }

    private static BlockModelBuilder model(
        RegistrumBlockstateProvider provider, Direction attachment, String part
    ) {
        return provider.models().getBuilder("block/redstone_wire/" + attachment.getSerializedName() + "/" + part)
            .ao(false);
    }

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
        float[] box = RedstoneWireBlock.transformBox(
            attachment, tangent, minX, minY, minZ, maxX, maxY, maxZ
        );
        var element = model.element().from(box[0], box[1], box[2]).to(box[3], box[4], box[5]);
        for (FaceSpec spec : faces) {
            Direction worldFace = RedstoneWireBlock.transformDirection(attachment, tangent, spec.direction());
            var face = element.face(worldFace)
                .texture(spec.texture())
                .uvs(spec.u1(), spec.v1(), spec.u2(), spec.v2())
                .rotation(rotation(spec.rotation()));
            if (spec.tinted()) {
                face.tintindex(0);
            }
            if (spec.cullFace() != null) {
                face.cullface(RedstoneWireBlock.transformDirection(attachment, tangent, spec.cullFace()));
            }
        }
        element.end();
    }

    private static FaceSpec face(
        Direction direction, String texture, float u1, float v1, float u2, float v2
    ) {
        return face(direction, texture, u1, v1, u2, v2, 0, false, null);
    }

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
        return new FaceSpec(direction, texture, u1, v1, u2, v2, rotation, tinted, cullFace);
    }

    private static ModelBuilder.FaceRotation rotation(int degrees) {
        return switch (Math.floorMod(degrees, 360)) {
            case 90 -> ModelBuilder.FaceRotation.CLOCKWISE_90;
            case 180 -> ModelBuilder.FaceRotation.UPSIDE_DOWN;
            case 270 -> ModelBuilder.FaceRotation.COUNTERCLOCKWISE_90;
            default -> ModelBuilder.FaceRotation.ZERO;
        };
    }

    private record FaceSpec(
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
    }
}
