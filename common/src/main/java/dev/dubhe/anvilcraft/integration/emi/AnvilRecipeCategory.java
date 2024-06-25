package dev.dubhe.anvilcraft.integration.emi;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipeType;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.integration.emi.stack.BlockStateEmiStack;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiRenderable;
import lombok.Getter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Getter
public class AnvilRecipeCategory extends EmiRecipeCategory {
    public static final Set<AnvilRecipeCategory> ALL = new HashSet<>();

    public static final EmiRecipeCategory STAMPING = AnvilRecipeCategory.register(
        AnvilRecipeType.STAMPING,
        BlockStateEmiStack.of(ModBlocks.STAMPING_PLATFORM)
    );
    public static final EmiRecipeCategory SIEVING = AnvilRecipeCategory.register(
        AnvilRecipeType.SIEVING,
        BlockStateEmiStack.of(Blocks.SCAFFOLDING)
    );
    public static final EmiRecipeCategory BULGING = AnvilRecipeCategory.register(
        AnvilRecipeType.BULGING,
        BlockStateEmiStack.of(Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3))
    );
    public static final EmiRecipeCategory BULGING_LIKE = AnvilRecipeCategory.register(
        AnvilRecipeType.BULGING_LIKE,
        BlockStateEmiStack.of(Blocks.CAULDRON)
    );
    public static final EmiRecipeCategory FLUID_HANDLING = AnvilRecipeCategory.register(
        AnvilRecipeType.FLUID_HANDLING,
        BlockStateEmiStack.of(Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3))
    );
    public static final EmiRecipeCategory CRYSTALLIZE = AnvilRecipeCategory.register(
        AnvilRecipeType.CRYSTALLIZE,
        BlockStateEmiStack.of(Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3))
    );
    public static final EmiRecipeCategory COMPACTION = AnvilRecipeCategory.register(
        AnvilRecipeType.COMPACTION,
        BlockStateEmiStack.of(Blocks.ANVIL)
    );
    public static final EmiRecipeCategory COMPRESS = AnvilRecipeCategory.register(
        AnvilRecipeType.COMPRESS,
        BlockStateEmiStack.of(Blocks.CAULDRON)
    );
    public static final EmiRecipeCategory COOKING = AnvilRecipeCategory.register(
        AnvilRecipeType.COOKING,
        DoubleBlockIcon.of(Blocks.CAULDRON, Blocks.CAMPFIRE)
    );
    public static final EmiRecipeCategory ITEM_INJECT = AnvilRecipeCategory.register(
        AnvilRecipeType.ITEM_INJECT,
        BlockStateEmiStack.of(Blocks.IRON_ORE)
    );
    public static final EmiRecipeCategory BLOCK_SMASH = AnvilRecipeCategory.register(
        AnvilRecipeType.BLOCK_SMASH,
        BlockStateEmiStack.of(Blocks.ANVIL)
    );
    public static final EmiRecipeCategory ITEM_SMASH = AnvilRecipeCategory.register(
        AnvilRecipeType.ITEM_SMASH,
        BlockStateEmiStack.of(Blocks.IRON_TRAPDOOR)
    );
    public static final EmiRecipeCategory SQUEEZE = AnvilRecipeCategory.register(
        AnvilRecipeType.SQUEEZE,
        BlockStateEmiStack.of(Blocks.ANVIL)
    );
    public static final EmiRecipeCategory SUPER_HEATING = AnvilRecipeCategory.register(
        AnvilRecipeType.SUPER_HEATING,
        DoubleBlockIcon.of(Blocks.CAULDRON, ModBlocks.HEATER.get())
    );
    public static final EmiRecipeCategory TIMEWARP = AnvilRecipeCategory.register(
        AnvilRecipeType.TIMEWARP,
        DoubleBlockIcon.of(Blocks.CAULDRON, ModBlocks.CORRUPTED_BEACON.get())
    );
    public static final EmiRecipeCategory GENERIC = AnvilRecipeCategory.register(
        AnvilRecipeType.GENERIC,
        BlockStateEmiStack.of(Blocks.ANVIL)
    );

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
