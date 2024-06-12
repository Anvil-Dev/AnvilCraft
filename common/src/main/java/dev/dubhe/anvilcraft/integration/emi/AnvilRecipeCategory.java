package dev.dubhe.anvilcraft.integration.emi;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipeType;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiRenderable;
import dev.emi.emi.api.stack.EmiStack;
import lombok.Getter;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Getter
public class AnvilRecipeCategory extends EmiRecipeCategory {
    public static final Set<AnvilRecipeCategory> ALL = new HashSet<>();

    public static final EmiRecipeCategory STAMPING = AnvilRecipeCategory.register(AnvilRecipeType.STAMPING,
        EmiStack.of(ModBlocks.STAMPING_PLATFORM));
    public static final EmiRecipeCategory SIEVING = AnvilRecipeCategory.register(AnvilRecipeType.SIEVING,
        EmiStack.of(Blocks.SCAFFOLDING));
    public static final EmiRecipeCategory BULGING = AnvilRecipeCategory.register(AnvilRecipeType.BULGING,
        EmiStack.of(Blocks.WATER_CAULDRON));
    public static final EmiRecipeCategory BULGING_LIKE = AnvilRecipeCategory.register(AnvilRecipeType.BULGING_LIKE,
        EmiStack.of(Blocks.CAULDRON));
    public static final EmiRecipeCategory FLUID_HANDLING = AnvilRecipeCategory.register(AnvilRecipeType.FLUID_HANDLING,
        EmiStack.of(Blocks.WATER_CAULDRON));
    public static final EmiRecipeCategory CRYSTALLIZE = AnvilRecipeCategory.register(AnvilRecipeType.CRYSTALLIZE,
        EmiStack.of(Blocks.POWDER_SNOW_CAULDRON));
    public static final EmiRecipeCategory COMPACTION = AnvilRecipeCategory.register(AnvilRecipeType.COMPACTION,
        EmiStack.of(Blocks.ANVIL));
    public static final EmiRecipeCategory COMPRESS = AnvilRecipeCategory.register(AnvilRecipeType.COMPRESS,
        EmiStack.of(Blocks.ANVIL));
    public static final EmiRecipeCategory COOKING = AnvilRecipeCategory.register(AnvilRecipeType.COOKING,
        EmiStack.of(Blocks.CAMPFIRE));
    public static final EmiRecipeCategory BOIL = AnvilRecipeCategory.register(AnvilRecipeType.BOIL,
        EmiStack.of(Blocks.CAMPFIRE));
    public static final EmiRecipeCategory ITEM_INJECT = AnvilRecipeCategory.register(AnvilRecipeType.ITEM_INJECT,
        EmiStack.of(Blocks.IRON_ORE));
    public static final EmiRecipeCategory BLOCK_SMASH = AnvilRecipeCategory.register(AnvilRecipeType.BLOCK_SMASH,
        EmiStack.of(Blocks.ANVIL));
    public static final EmiRecipeCategory ITEM_SMASH = AnvilRecipeCategory.register(AnvilRecipeType.ITEM_SMASH,
        EmiStack.of(Blocks.IRON_TRAPDOOR));
    public static final EmiRecipeCategory SQUEEZE = AnvilRecipeCategory.register(AnvilRecipeType.SQUEEZE,
        EmiStack.of(Blocks.ANVIL));
    public static final EmiRecipeCategory SUPER_HEATING = AnvilRecipeCategory.register(AnvilRecipeType.SUPER_HEATING,
        EmiStack.of(ModBlocks.HEATER));
    public static final EmiRecipeCategory TIMEWARP = AnvilRecipeCategory.register(AnvilRecipeType.TIMEWARP,
        EmiStack.of(ModBlocks.CORRUPTED_BEACON));
    public static final EmiRecipeCategory GENERIC = AnvilRecipeCategory.register(AnvilRecipeType.GENERIC,
        EmiStack.of(Blocks.ANVIL));

    private final AnvilRecipeType type;

    public AnvilRecipeCategory(@NotNull AnvilRecipeType type, EmiRenderable icon) {
        super(AnvilCraft.of(type.toString()), icon);
        this.type = type;
    }

    private static @NotNull EmiRecipeCategory register(@NotNull AnvilRecipeType type, EmiRenderable icon) {
        AnvilRecipeCategory category = new AnvilRecipeCategory(type, icon);
        AnvilRecipeCategory.ALL.add(category);
        return category;
    }

    public static @NotNull EmiRecipeCategory getByType(AnvilRecipeType type) {
        Optional<AnvilRecipeCategory> first = ALL.stream().filter(c -> c.getType() == type).findFirst();
        return first.isEmpty() ? GENERIC : first.get();
    }
}
