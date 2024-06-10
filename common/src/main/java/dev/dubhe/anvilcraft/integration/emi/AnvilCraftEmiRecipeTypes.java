package dev.dubhe.anvilcraft.integration.emi;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiRenderable;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import java.util.LinkedHashMap;
import java.util.Map;

public class AnvilCraftEmiRecipeTypes {

    public static final Map<ResourceLocation, EmiRecipeCategory> ALL = new LinkedHashMap<>();
    public static final EmiRecipeCategory STAMPING = register("stamping", EmiStack.of(ModBlocks.STAMPING_PLATFORM));
    public static final EmiRecipeCategory SIEVING = register("sieving", EmiStack.of(Blocks.SCAFFOLDING));
    public static final EmiRecipeCategory BULGING = register("bulging", EmiStack.of(Blocks.WATER_CAULDRON));
    public static final EmiRecipeCategory BULGING_LIKE = register("bulging_like", EmiStack.of(Blocks.CAULDRON));
    public static final EmiRecipeCategory FLUID_HANDLING = register("fluid_handling",
            EmiStack.of(Blocks.WATER_CAULDRON));
    public static final EmiRecipeCategory CRYSTALLIZE = register("crystallize",
            EmiStack.of(Blocks.POWDER_SNOW_CAULDRON));
    public static final EmiRecipeCategory COMPACTION = register("compaction", EmiStack.of(Blocks.ANVIL));
    public static final EmiRecipeCategory COMPRESS = register("compress", EmiStack.of(Blocks.ANVIL));
    public static final EmiRecipeCategory COOKING = register("cooking", EmiStack.of(Blocks.CAMPFIRE));
    public static final EmiRecipeCategory BOIL = register("boil", EmiStack.of(Blocks.CAMPFIRE));
    public static final EmiRecipeCategory ITEM_INJECT = register("item_inject", EmiStack.of(Blocks.IRON_ORE));
    public static final EmiRecipeCategory BLOCK_SMASH = register("block_smash", EmiStack.of(Blocks.ANVIL));
    public static final EmiRecipeCategory ITEM_SMASH = register("item_smash", EmiStack.of(Blocks.IRON_TRAPDOOR));
    public static final EmiRecipeCategory SQUEEZE = register("squeeze", EmiStack.of(Blocks.ANVIL));
    public static final EmiRecipeCategory SUPER_HEATING = register("super_heating", EmiStack.of(ModBlocks.HEATER));
    public static final EmiRecipeCategory TIMEWARP = register("timewarp", EmiStack.of(ModBlocks.CORRUPTED_BEACON));
    public static final EmiRecipeCategory GENERIC = register("generic", EmiStack.of(Blocks.ANVIL));

    private static EmiRecipeCategory register(String name, EmiRenderable icon) {
        ResourceLocation id = AnvilCraft.of(name);
        EmiRecipeCategory category = new EmiRecipeCategory(id, icon);
        ALL.put(id, category);
        return category;
    }
}
