package dev.dubhe.anvilcraft.block.cauldron;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.util.ModInteractionMap;

public class MilkCauldronBlock extends Layered4LevelCauldronBlock implements IHammerRemovable {
    public MilkCauldronBlock(Properties properties) {
        super(properties, ModInteractionMap.MILK);
    }
}
