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
 * @param inputTable  转化前的方块
 * @param tool        手持的工具物品
 * @param outputTable 转化后的方块
 */
public record ProcessingTableConversionRecipe(
    Block inputTable,
    Item tool,
    Block outputTable
) {
    public ProcessingTableConversionRecipe {
        if (inputTable.asItem() == Items.AIR || tool == Items.AIR || outputTable.asItem() == Items.AIR) {
            throw new IllegalArgumentException("Conversion recipe cannot contain empty entries");
        }
    }

    public ItemStack inputStack() {
        return new ItemStack(this.inputTable);
    }

    public ItemStack toolStack() {
        return new ItemStack(this.tool);
    }

    public ItemStack outputStack() {
        return new ItemStack(this.outputTable);
    }

    public static List<ProcessingTableConversionRecipe> getAllRecipes() {
        Block stamping = ModBlocks.STAMPING_PLATFORM.get();
        Block crushing = ModBlocks.CRUSHING_TABLE.get();
        Block sifting = ModBlocks.SIFTING_TABLE.get();
        Block unpacking = ModBlocks.UNPACKING_TABLE.get();
        Item hammer = ModItems.ANVIL_HAMMER.get();
        return List.of(
            new ProcessingTableConversionRecipe(stamping, Items.GRINDSTONE, crushing),
            new ProcessingTableConversionRecipe(stamping, Blocks.SCAFFOLDING.asItem(), sifting),
            new ProcessingTableConversionRecipe(stamping, Items.IRON_TRAPDOOR, unpacking),
            new ProcessingTableConversionRecipe(crushing, hammer, stamping),
            new ProcessingTableConversionRecipe(sifting, hammer, stamping),
            new ProcessingTableConversionRecipe(unpacking, hammer, stamping)
        );
    }
}
