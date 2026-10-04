package dev.dubhe.anvilcraft.block.storage;

import dev.dubhe.anvilcraft.api.block.INegativeMatterBlock;
import dev.dubhe.anvilcraft.api.block.IVoidEnergyAmplifier;
import net.minecraft.world.level.block.Block;

public class NegativeMatterBlock extends Block implements INegativeMatterBlock, IVoidEnergyAmplifier {
    public NegativeMatterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int getVoidEnergyAmplification() {
        return -1;
    }
}
