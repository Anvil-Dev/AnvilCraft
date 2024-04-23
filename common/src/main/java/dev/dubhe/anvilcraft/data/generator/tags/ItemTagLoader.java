package dev.dubhe.anvilcraft.data.generator.tags;

import com.tterrag.registrate.providers.RegistrateTagsProvider;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

public class ItemTagLoader {
    /**
     * 物品标签生成器初始化
     *
     * @param provider 提供器
     */
    public static void init(@NotNull RegistrateTagsProvider<Item> provider) {
        provider.addTag(ModItemTags.PLATES).setReplace(false)
            .add(Items.HEAVY_WEIGHTED_PRESSURE_PLATE)
            .add(Items.LIGHT_WEIGHTED_PRESSURE_PLATE);
        provider.addTag(ModItemTags.IRON_PLATES).setReplace(false)
            .add(Items.HEAVY_WEIGHTED_PRESSURE_PLATE);
        provider.addTag(ModItemTags.GOLD_PLATES).setReplace(false)
            .add(Items.LIGHT_WEIGHTED_PRESSURE_PLATE);
        provider.addTag(ModItemTags.ROYAL_STEEL_PICKAXE_BASE).setReplace(false)
            .add(ModItems.AMETHYST_PICKAXE.get())
            .add(Items.GOLDEN_PICKAXE)
            .add(Items.IRON_PICKAXE)
            .add(Items.DIAMOND_PICKAXE);
        provider.addTag(ModItemTags.ROYAL_STEEL_AXE_BASE).setReplace(false)
                .add(ModItems.AMETHYST_AXE.get())
                .add(Items.GOLDEN_AXE)
                .add(Items.IRON_AXE)
                .add(Items.DIAMOND_AXE);
        provider.addTag(ModItemTags.GEMS).setReplace(false)
            .add(Items.EMERALD)
            .add(ModItems.RUBY.get())
            .add(ModItems.SAPPHIRE.get())
            .add(ModItems.TOPAZ.get());
        provider.addTag(ModItemTags.GEMS).setReplace(false)
                .add(Items.EMERALD)
                .add(ModItems.RUBY.get())
                .add(ModItems.SAPPHIRE.get())
                .add(ModItems.TOPAZ.get());
        provider.addTag(ModItemTags.GEM_BLOCKS).setReplace(false)
                .add(Items.EMERALD_BLOCK)
                .add(ModBlocks.RUBY_BLOCK.asItem())
                .add(ModBlocks.SAPPHIRE_BLOCK.asItem())
                .add(ModBlocks.TOPAZ_BLOCK.asItem());
    }
}
