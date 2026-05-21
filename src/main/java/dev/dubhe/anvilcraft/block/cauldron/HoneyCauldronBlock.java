package dev.dubhe.anvilcraft.block.cauldron;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.util.ModInteractionMap;

public class HoneyCauldronBlock extends Layered4LevelCauldronBlock implements IHammerRemovable {
    public HoneyCauldronBlock(Properties properties) {
        super(properties, ModInteractionMap.HONEY);
    }
}
