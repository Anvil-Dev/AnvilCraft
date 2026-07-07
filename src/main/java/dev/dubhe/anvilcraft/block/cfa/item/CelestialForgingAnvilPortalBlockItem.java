package dev.dubhe.anvilcraft.block.cfa.item;

import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.DirectionGate331PartHalf;
import dev.dubhe.anvilcraft.item.block.FlexibleMultiPartBlockItem;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class CelestialForgingAnvilPortalBlockItem
    extends FlexibleMultiPartBlockItem<DirectionGate331PartHalf, EnumProperty<Direction>, Direction> {
    public CelestialForgingAnvilPortalBlockItem(
        FlexibleMultiPartBlock<DirectionGate331PartHalf, EnumProperty<Direction>, Direction> block,
        Properties properties
    ) {
        super(block, properties);
    }
}
