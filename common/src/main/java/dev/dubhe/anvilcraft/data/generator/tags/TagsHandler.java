package dev.dubhe.anvilcraft.data.generator.tags;

import dev.anvilcraft.lib.data.provider.RegistratorTagsProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public class TagsHandler {
    public static void initItem(RegistratorTagsProvider<Item> provider) {
        ItemTagLoader.init(provider);
    }

    public static void initBlock(RegistratorTagsProvider<Block> provider) {
        BlockTagLoader.init(provider);
    }

    public static void initFluid(RegistratorTagsProvider<Fluid> provider) {
        FluidTagLoader.init(provider);
    }
}
