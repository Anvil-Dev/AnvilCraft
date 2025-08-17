package dev.dubhe.anvilcraft.util.registrater;

import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class PropertiesProviderUtil {
    public static BlockBehaviour.Properties confinedAnvilon(BlockBehaviour.Properties properties) {
        return properties
            .lightLevel(it -> 15)
            .noOcclusion()
            .requiresCorrectToolForDrops()
            .strength(1.5F, 6.0F)
            .explosionResistance(1200)
            .emissiveRendering(ModBlocks::always);
    }
}
