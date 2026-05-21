package dev.dubhe.anvilcraft.item.block;

import dev.dubhe.anvilcraft.block.container.ShulkerContainerBlock;
import dev.dubhe.anvilcraft.block.state.OpenedCube3x3PartHalf;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class ShulkerContainerBlockItem extends FlexibleMultiPartBlockItem<OpenedCube3x3PartHalf, BooleanProperty, Boolean> {
    public ShulkerContainerBlockItem(ShulkerContainerBlock block, Properties properties) {
        super(block, properties);
    }
}