package dev.dubhe.anvilcraft.block.cfa.item;

import dev.dubhe.anvilcraft.block.item.FlexibleMultiPartBlockItem;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.DirectionCube232PartHalf;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public class CelestialForgingAnvilAmplifierBlockItem
    extends FlexibleMultiPartBlockItem<DirectionCube232PartHalf, DirectionProperty, Direction> {
    public CelestialForgingAnvilAmplifierBlockItem(
        FlexibleMultiPartBlock<DirectionCube232PartHalf, DirectionProperty, Direction> block,
        Properties properties
    ) {
        super(block, properties);
    }
}
