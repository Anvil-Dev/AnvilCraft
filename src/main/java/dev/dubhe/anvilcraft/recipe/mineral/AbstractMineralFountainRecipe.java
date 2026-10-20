package dev.dubhe.anvilcraft.recipe.mineral;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import lombok.Getter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStackTemplate;

import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

@Getter
public abstract class AbstractMineralFountainRecipe implements Recipe<MineralFountainRecipeInput> {

    public static final BlockStatePredicate DEFAULT_FROM = BlockStatePredicate.builder().of(Blocks.AIR).build();

    public static final int DEFAULT_WEIGHT = 1;

    private final Optional<Identifier> dimension;
    private final HeightRange height;

    private final int priority;

    private final int weight;
    private final BlockStatePredicate fromBlock;
    private final ChanceBlockState toBlock;
    private final Optional<MineralFountainHeat> heat;

    private final Optional<FluidStackTemplate> toFluid;

    protected AbstractMineralFountainRecipe(
        Optional<Identifier> dimension,
        HeightRange height,
        int priority,
        int weight,
        BlockStatePredicate fromBlock,
        ChanceBlockState toBlock,
        Optional<MineralFountainHeat> heat,
        Optional<FluidStackTemplate> toFluid
    ) {
        this.dimension = dimension;
        this.height = height;
        this.priority = priority;
        this.weight = weight;
        this.fromBlock = fromBlock;
        this.toBlock = toBlock;
        this.heat = heat;
        this.toFluid = toFluid;
    }

    protected boolean matchesContext(MineralFountainRecipeInput input, Level level) {
        if (this.dimension.isPresent() && !this.dimension.get().equals(level.dimension().identifier())) {
            return false;
        }
        return this.height.test(level, input.pos());
    }

    protected boolean matchesCommon(MineralFountainRecipeInput input, Level level) {
        return this.matchesContext(input, level) && this.fromBlock.testWithoutEntity(input.from());
    }

    public double chance(ServerLevel level) {
        return this.toBlock.chance().getFloat(RecipeUtil.emptyLootContext(level));
    }

    public static <T> T pickWeighted(
        RandomSource random,
        List<T> picked,
        ToIntFunction<T> weight
    ) {
        if (picked.size() == 1) return picked.getFirst();
        long total = 0;
        for (T candidate : picked) {
            total += Math.max(0, weight.applyAsInt(candidate));
        }
        if (total <= 0) return picked.get(random.nextInt(picked.size()));

        double roll = random.nextDouble() * total;
        for (T candidate : picked) {
            roll -= Math.max(0, weight.applyAsInt(candidate));
            if (roll < 0) return candidate;
        }
        return picked.getLast();
    }

    @Override
    public ItemStack assemble(MineralFountainRecipeInput input) {
        return this.toBlock.state().getBlock().asItem() == Items.AIR
               ? ItemStack.EMPTY
               : new ItemStack(this.toBlock.state().getBlock());
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public boolean isSpecial() {
        return true;
    }
}
