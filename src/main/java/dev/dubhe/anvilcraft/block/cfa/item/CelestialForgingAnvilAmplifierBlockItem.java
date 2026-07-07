package dev.dubhe.anvilcraft.block.cfa.item;

import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.DirectionCube232PartHalf;
import dev.dubhe.anvilcraft.item.block.FlexibleMultiPartBlockItem;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class CelestialForgingAnvilAmplifierBlockItem
    extends FlexibleMultiPartBlockItem<DirectionCube232PartHalf, EnumProperty<Direction>, Direction> {
    public CelestialForgingAnvilAmplifierBlockItem(
        FlexibleMultiPartBlock<DirectionCube232PartHalf, EnumProperty<Direction>, Direction> block,
        Properties properties
    ) {
        super(block, properties);
    }
}
