package dev.dubhe.anvilcraft.block.state;

import dev.dubhe.anvilcraft.util.BlockMiningEffect;
import net.minecraft.util.StringRepresentable;

public enum LensType implements StringRepresentable {
    NONE("none", BlockMiningEffect.NORMAL),
    ROYAL("royal", BlockMiningEffect.SILK_TOUCH),
    FROST("frost", BlockMiningEffect.DISINTEGRATION),
    EMBER("ember", BlockMiningEffect.MAX_SMELTING);

    private final String name;
    private final BlockMiningEffect miningEffect;

    LensType(String name, BlockMiningEffect miningEffect) {
        this.name = name;
        this.miningEffect = miningEffect;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public BlockMiningEffect getMiningEffect() {
        return miningEffect;
    }
}
