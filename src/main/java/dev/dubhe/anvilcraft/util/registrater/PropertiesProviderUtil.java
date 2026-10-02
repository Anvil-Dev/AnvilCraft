package dev.dubhe.anvilcraft.util.registrater;

import dev.anvilcraft.lib.v2.registrum.builders.ItemBuilder;
import dev.anvilcraft.lib.v2.registrum.providers.ProviderType;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class PropertiesProviderUtil {
    public static <T extends Item, P> ItemBuilder<T, P> blockItem(ItemBuilder<T, P> builder) {
        return builder.properties(Item.Properties::useBlockDescriptionPrefix)
            .setData(ProviderType.LANG, (_, _) -> {});
    }

    public static BlockBehaviour.Properties metalSound(BlockBehaviour.Properties properties) {
        return properties.sound(SoundType.METAL);
    }

    public static SoundType metalSound(SoundType sound) {
        return sound == SoundType.IRON ? SoundType.METAL : sound;
    }

    public static BlockBehaviour.Properties confinedAnvilon(BlockBehaviour.Properties properties) {
        return properties
            .lightLevel(_ -> 15)
            .noOcclusion()
            .requiresCorrectToolForDrops()
            .strength(1.5F, 6.0F)
            .explosionResistance(1200)
            .emissiveRendering(ModBlocks::always)
            .isValidSpawn(ModBlocks::never)
            .isRedstoneConductor(ModBlocks::never)
            .isSuffocating(ModBlocks::never)
            .isViewBlocking(ModBlocks::never);
    }
}
