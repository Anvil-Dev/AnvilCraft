package dev.dubhe.anvilcraft.integration.rei;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipeType;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.integration.rei.client.DoubleBlockIcon;
import dev.dubhe.anvilcraft.integration.rei.client.widget.BlockWidget;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;

import java.util.HashMap;

public class AnvilCraftCategoryIdentifiers {
    public static HashMap<AnvilRecipeType, AnvilCraftCategoryIdentifier> ALL = new HashMap<>();
    public static final AnvilCraftCategoryIdentifier STAMPING = register(
            AnvilRecipeType.STAMPING,
            AnvilCraftCategoryIdentifier.creat()
                    .setCategoryIdentifier(CategoryIdentifier.of(AnvilCraft.MOD_ID, "stamping"))
                    .setTitle(Component.translatable("emi.category.anvilcraft.stamping"))
                    .setIcon(new BlockWidget(ModBlocks.STAMPING_PLATFORM.getDefaultState(), 0, 0, 0)));
    public static final AnvilCraftCategoryIdentifier SIEVING = register(
            AnvilRecipeType.SIEVING,
            AnvilCraftCategoryIdentifier.creat()
                    .setCategoryIdentifier(CategoryIdentifier.of(AnvilCraft.MOD_ID, "sieving"))
                    .setTitle(Component.translatable("emi.category.anvilcraft.sieving"))
                    .setIcon(new BlockWidget(Blocks.SCAFFOLDING.defaultBlockState(), 0, 0, 0)));
    public static final AnvilCraftCategoryIdentifier BULGING = register(
            AnvilRecipeType.BULGING,
            AnvilCraftCategoryIdentifier.creat()
                    .setCategoryIdentifier(CategoryIdentifier.of(AnvilCraft.MOD_ID, "bulging"))
                    .setTitle(Component.translatable("emi.category.anvilcraft.bulging"))
                    .setIcon(new BlockWidget(Blocks.WATER_CAULDRON.defaultBlockState()
                            .setValue(LayeredCauldronBlock.LEVEL, 3), 0, 0, 0)));
    public static final AnvilCraftCategoryIdentifier BULGING_LIKE = register(
            AnvilRecipeType.BULGING_LIKE,
            AnvilCraftCategoryIdentifier.creat()
                    .setCategoryIdentifier(CategoryIdentifier.of(AnvilCraft.MOD_ID, "bulging_like"))
                    .setTitle(Component.translatable("emi.category.anvilcraft.bulging_like"))
                    .setIcon(new BlockWidget(Blocks.CAULDRON.defaultBlockState(), 0, 0, 0)));
    public static final AnvilCraftCategoryIdentifier FLUID_HANDLING = register(
            AnvilRecipeType.FLUID_HANDLING,
            AnvilCraftCategoryIdentifier.creat()
                    .setCategoryIdentifier(CategoryIdentifier.of(AnvilCraft.MOD_ID, "fluid_handling"))
                    .setTitle(Component.translatable("emi.category.anvilcraft.fluid_handling"))
                    .setIcon(new BlockWidget(Blocks.WATER_CAULDRON.defaultBlockState()
                            .setValue(LayeredCauldronBlock.LEVEL, 3), 0, 0, 0)));
    public static final AnvilCraftCategoryIdentifier CRYSTALLIZE = register(
            AnvilRecipeType.CRYSTALLIZE,
            AnvilCraftCategoryIdentifier.creat()
                    .setCategoryIdentifier(CategoryIdentifier.of(AnvilCraft.MOD_ID, "crystallize"))
                    .setTitle(Component.translatable("emi.category.anvilcraft.crystallize"))
                    .setIcon(new BlockWidget(Blocks.POWDER_SNOW_CAULDRON.defaultBlockState()
                            .setValue(LayeredCauldronBlock.LEVEL, 3), 0, 0, 0)));
    public static final AnvilCraftCategoryIdentifier COMPACTION = register(
            AnvilRecipeType.COMPACTION,
            AnvilCraftCategoryIdentifier.creat()
                    .setCategoryIdentifier(CategoryIdentifier.of(AnvilCraft.MOD_ID, "compaction"))
                    .setTitle(Component.translatable("emi.category.anvilcraft.compaction"))
                    .setIcon(new BlockWidget(Blocks.ANVIL.defaultBlockState(), 0, 0, 0)));
    public static final AnvilCraftCategoryIdentifier COMPRESS = register(
            AnvilRecipeType.COMPRESS,
            AnvilCraftCategoryIdentifier.creat()
                    .setCategoryIdentifier(CategoryIdentifier.of(AnvilCraft.MOD_ID, "compress"))
                    .setTitle(Component.translatable("emi.category.anvilcraft.compress"))
                    .setIcon(new BlockWidget(Blocks.CAULDRON.defaultBlockState(), 0, 0, 0)));
    public static final AnvilCraftCategoryIdentifier COOKING = register(
            AnvilRecipeType.COOKING,
            AnvilCraftCategoryIdentifier.creat()
                    .setCategoryIdentifier(CategoryIdentifier.of(AnvilCraft.MOD_ID, "cooking"))
                    .setTitle(Component.translatable("emi.category.anvilcraft.cooking"))
                    .setIcon(DoubleBlockIcon.of(Blocks.CAULDRON, Blocks.CAMPFIRE)));
    public static final AnvilCraftCategoryIdentifier ITEM_INJECT = register(
            AnvilRecipeType.ITEM_INJECT,
            AnvilCraftCategoryIdentifier.creat()
                    .setCategoryIdentifier(CategoryIdentifier.of(AnvilCraft.MOD_ID, "item_inject"))
                    .setTitle(Component.translatable("emi.category.anvilcraft.item_inject"))
                    .setIcon(new BlockWidget(Blocks.IRON_ORE.defaultBlockState(), 0, 0, 0)));
    public static final AnvilCraftCategoryIdentifier BLOCK_SMASH = register(
            AnvilRecipeType.BLOCK_SMASH,
            AnvilCraftCategoryIdentifier.creat()
                    .setCategoryIdentifier(CategoryIdentifier.of(AnvilCraft.MOD_ID, "block_smash"))
                    .setTitle(Component.translatable("emi.category.anvilcraft.block_smash"))
                    .setIcon(new BlockWidget(Blocks.ANVIL.defaultBlockState(), 0, 0, 0)));
    public static final AnvilCraftCategoryIdentifier ITEM_SMASH = register(
            AnvilRecipeType.ITEM_SMASH,
            AnvilCraftCategoryIdentifier.creat()
                    .setCategoryIdentifier(CategoryIdentifier.of(AnvilCraft.MOD_ID, "item_smash"))
                    .setTitle(Component.translatable("emi.category.anvilcraft.item_smash"))
                    .setIcon(new BlockWidget(Blocks.IRON_TRAPDOOR.defaultBlockState(), 0, 0, 0)));
    public static final AnvilCraftCategoryIdentifier SQUEEZE = register(
            AnvilRecipeType.SQUEEZE,
            AnvilCraftCategoryIdentifier.creat()
                    .setCategoryIdentifier(CategoryIdentifier.of(AnvilCraft.MOD_ID, "squeeze"))
                    .setTitle(Component.translatable("emi.category.anvilcraft.squeeze"))
                    .setIcon(new BlockWidget(Blocks.ANVIL.defaultBlockState(), 0, 0, 0)));
    public static final AnvilCraftCategoryIdentifier SUPER_HEATING = register(
            AnvilRecipeType.SUPER_HEATING,
            AnvilCraftCategoryIdentifier.creat()
                    .setCategoryIdentifier(CategoryIdentifier.of(AnvilCraft.MOD_ID, "super_heating"))
                    .setTitle(Component.translatable("emi.category.anvilcraft.super_heating"))
                    .setIcon(DoubleBlockIcon.of(Blocks.CAULDRON, ModBlocks.HEATER.get())));

    public static final AnvilCraftCategoryIdentifier TIMEWARP = register(
            AnvilRecipeType.TIMEWARP,
            AnvilCraftCategoryIdentifier.creat()
                    .setCategoryIdentifier(CategoryIdentifier.of(AnvilCraft.MOD_ID, "timewarp"))
                    .setTitle(Component.translatable("emi.category.anvilcraft.timewarp"))
                    .setIcon(DoubleBlockIcon.of(Blocks.CAULDRON, ModBlocks.CORRUPTED_BEACON.get())));

    public static final AnvilCraftCategoryIdentifier GENERIC = register(
            AnvilRecipeType.GENERIC,
            AnvilCraftCategoryIdentifier.creat()
                    .setCategoryIdentifier(CategoryIdentifier.of(AnvilCraft.MOD_ID, "generic"))
                    .setTitle(Component.translatable("emi.category.anvilcraft.generic"))
                    .setIcon(new BlockWidget(Blocks.ANVIL.defaultBlockState(), 0, 0, 0)));

    public static AnvilCraftCategoryIdentifier register(
            AnvilRecipeType anvilRecipeType,
            AnvilCraftCategoryIdentifier anvilCraftCategoryIdentifier
    ) {
        ALL.put(anvilRecipeType, anvilCraftCategoryIdentifier);
        return anvilCraftCategoryIdentifier;
    }

    public static AnvilCraftCategoryIdentifier get(
            AnvilRecipeType anvilRecipeType
    ) {
        if (!ALL.containsKey(anvilRecipeType)) return AnvilCraftCategoryIdentifiers.GENERIC;
        return ALL.get(anvilRecipeType);
    }
}
