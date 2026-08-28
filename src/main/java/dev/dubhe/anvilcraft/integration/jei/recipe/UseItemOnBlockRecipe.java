package dev.dubhe.anvilcraft.integration.jei.recipe;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * 加工台方块的右键转化配方。
 *
 * @param inputBlock  转化前的方块
 * @param item        手持的物品
 * @param outputBlock 转化后的方块
 */
public record UseItemOnBlockRecipe(
    Block inputBlock,
    Item item,
    Block outputBlock
) {
    public UseItemOnBlockRecipe {
        if (inputBlock.asItem() == Items.AIR || item == Items.AIR || outputBlock.asItem() == Items.AIR) {
            throw new IllegalArgumentException("Conversion recipe cannot contain empty entries");
        }
    }

    public ItemStack inputStack() {
        return new ItemStack(this.inputBlock);
    }

    public ItemStack toolStack() {
        return new ItemStack(this.item);
    }

    public ItemStack outputStack() {
        return new ItemStack(this.outputBlock);
    }

    public static List<UseItemOnBlockRecipe> getAllRecipes() {

        return List.of(
            new UseItemOnBlockRecipe(
                ModBlocks.STAMPING_PLATFORM.get(),
                Items.GRINDSTONE,
                ModBlocks.CRUSHING_TABLE.get()
            ),

            new UseItemOnBlockRecipe(
                ModBlocks.STAMPING_PLATFORM.get(),
                Blocks.SCAFFOLDING.asItem(),
                ModBlocks.SIFTING_TABLE.get()
            ),

            new UseItemOnBlockRecipe(
                ModBlocks.STAMPING_PLATFORM.get(),
                Items.IRON_TRAPDOOR,
                ModBlocks.UNPACKING_TABLE.get()
            ),

            new UseItemOnBlockRecipe(
                ModBlocks.CRUSHING_TABLE.get(),
                ModItems.ANVIL_HAMMER.get(),
                ModBlocks.STAMPING_PLATFORM.get()
            ),

            new UseItemOnBlockRecipe(
                ModBlocks.SIFTING_TABLE.get(),
                ModItems.ANVIL_HAMMER.get(),
                ModBlocks.STAMPING_PLATFORM.get()
            ),

            new UseItemOnBlockRecipe(
                ModBlocks.UNPACKING_TABLE.get(),
                ModItems.ANVIL_HAMMER.get(),
                ModBlocks.STAMPING_PLATFORM.get()
            ),

            new UseItemOnBlockRecipe(
                ModBlocks.SINGULARITY_CRYSTAL.get(),
                ModItems.HYPERDIMENSION_TERMINAL.get(),
                ModBlocks.HYPERDIMENSION_UPLOADER.get()
            ),

            new UseItemOnBlockRecipe(
                ModBlocks.HOLLOW_MAGNET_BLOCK.get(),
                ModItems.MAGNET_INGOT.get(),
                ModBlocks.MAGNET_BLOCK.get()
            ),

            new UseItemOnBlockRecipe(
                ModBlocks.HOLLOW_MAGNET_BLOCK.get(),
                Items.IRON_INGOT,
                ModBlocks.FERRITE_CORE_MAGNET_BLOCK.get()
            ),

            new UseItemOnBlockRecipe(
                ModBlocks.BATCH_CRAFTER.get(),
                Items.STONECUTTER,
                ModBlocks.BATCH_CUTTER.get()
            ),

            new UseItemOnBlockRecipe(
                ModBlocks.BATCH_CUTTER.get(),
                Items.CRAFTER,
                ModBlocks.BATCH_CRAFTER.get()
            )
        );
    }
}
