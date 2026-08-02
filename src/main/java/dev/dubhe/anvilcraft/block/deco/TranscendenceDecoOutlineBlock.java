package dev.dubhe.anvilcraft.block.deco;

import dev.dubhe.anvilcraft.api.block.ITranscendiumBlock;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.StainedGlassBlock;

public class TranscendenceDecoOutlineBlock extends StainedGlassBlock implements ITranscendiumBlock {
    public TranscendenceDecoOutlineBlock(Properties properties) {
        super(DyeColor.PURPLE, properties);
    }
}
