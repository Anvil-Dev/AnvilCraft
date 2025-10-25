package dev.dubhe.anvilcraft.integration.appeng.data;

import appeng.core.definitions.AEBlocks;
import com.tterrag.registrate.providers.RegistrateTagsProvider;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import net.minecraft.world.level.block.Block;

public class AppEngBlockTagLoader {
    public static void init(RegistrateTagsProvider<Block> provider) {
        provider.addTag(ModBlockTags.SPECTRAL_CAN_THROUGH)
            .addOptional(AEBlocks.CABLE_BUS.id());
    }
}
