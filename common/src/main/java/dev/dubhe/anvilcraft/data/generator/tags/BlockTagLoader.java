package dev.dubhe.anvilcraft.data.generator.tags;

import com.tterrag.registrate.providers.RegistrateTagsProvider;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

public class BlockTagLoader {
    public static void init(@NotNull RegistrateTagsProvider<Block> provider) {
        provider.addTag(ModBlockTags.REDSTONE_TORCH).setReplace(false)
                .add(Blocks.REDSTONE_WALL_TORCH)
                .add(Blocks.REDSTONE_TORCH);
        provider.addTag(ModBlockTags.MUSHROOM_BLOCK).setReplace(false)
                .add(Blocks.BROWN_MUSHROOM_BLOCK)
                .add(Blocks.RED_MUSHROOM_BLOCK)
                .add(Blocks.MUSHROOM_STEM);
    }
}
