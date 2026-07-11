package dev.dubhe.anvilcraft.data.generator;

import dev.anvilcraft.lib.v2.registrum.providers.DataGenContext;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumBlockstateProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.RedstoneWireBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.neoforged.neoforge.client.model.generators.ModelFile;

public class RedstoneWireBlockStateGenerator {
    public static <T extends Block> void generate(
        DataGenContext<Block, T> context,
        RegistrumBlockstateProvider provider
    ) {
        ModelFile dot = provider.models().getExistingFile(AnvilCraft.of("block/redstone_wire_dot"));
        ModelFile side = provider.models().getExistingFile(AnvilCraft.of("block/redstone_wire_side"));
        ModelFile up = provider.models().getExistingFile(AnvilCraft.of("block/redstone_wire_up"));

        var builder = provider.getMultipartBuilder(context.get());
        builder.part().modelFile(dot).addModel().condition(RedstoneWireBlock.DOT, true).end();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            int rotation = switch (direction) {
                case EAST -> 90;
                case SOUTH -> 180;
                case WEST -> 270;
                default -> 0;
            };
            var property = RedstoneWireBlock.PROPERTY_BY_DIRECTION.get(direction);
            builder.part().modelFile(side).rotationY(rotation).addModel()
                .condition(property, RedstoneSide.SIDE, RedstoneSide.UP).end();
            builder.part().modelFile(up).rotationY(rotation).addModel()
                .condition(property, RedstoneSide.UP).end();
        }
    }
}
