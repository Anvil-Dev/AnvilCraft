package dev.dubhe.anvilcraft.block.storage;

import dev.dubhe.anvilcraft.api.block.ITranscendiumBlock;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import net.minecraft.world.level.block.Block;

public class SimpleConfinementAnvilonBlock extends Block implements IHammerRemovable, ITranscendiumBlock {
    public SimpleConfinementAnvilonBlock(Properties properties) {
        super(properties);
    }
}
