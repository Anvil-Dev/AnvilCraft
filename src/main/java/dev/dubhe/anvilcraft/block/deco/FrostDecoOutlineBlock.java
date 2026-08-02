package dev.dubhe.anvilcraft.block.deco;

import dev.dubhe.anvilcraft.api.block.IFrostBlock;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.StainedGlassBlock;

public class FrostDecoOutlineBlock extends StainedGlassBlock implements IFrostBlock {
    public FrostDecoOutlineBlock(Properties properties) {
        super(DyeColor.WHITE, properties);
    }
}
