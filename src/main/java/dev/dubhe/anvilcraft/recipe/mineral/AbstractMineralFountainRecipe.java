package dev.dubhe.anvilcraft.recipe.mineral;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

/**
 * 矿物涌泉配方的公共部分：高度范围、世界类型、优先级、权重、材料方块与结果方块。
 */
@Getter
public abstract class AbstractMineralFountainRecipe implements Recipe<MineralFountainRecipeInput> {
    /** 材料方块默认值：空气，即「上方为空」时也可生效。 */
    public static final BlockStatePredicate DEFAULT_FROM = BlockStatePredicate.builder().of(Blocks.AIR).build();
    /** 权重默认值：同优先级并列时等概率。 */
    public static final int DEFAULT_WEIGHT = 1;

    /** 为空表示任意世界。 */
    private final Optional<ResourceLocation> dimension;
    private final HeightRange height;
    /** 越大越先尝试。 */
    private final int priority;
    /** 同优先级并列时的抽取权重。 */
    private final int weight;
    private final BlockStatePredicate fromBlock;
    private final ChanceBlockState toBlock;
    private final Optional<MineralFountainHeat> heat;
    /**
     * 上方为排水口时的流体产出。
     *
     * <p>与 {@link #heat} 同样不判材料方块：上方是排水口而非空气，材料条件天然不成立。</p>
     */
    private final Optional<FluidStack> toFluid;

    protected AbstractMineralFountainRecipe(
        Optional<ResourceLocation> dimension,
        HeightRange height,
        int priority,
        int weight,
        BlockStatePredicate fromBlock,
        ChanceBlockState toBlock,
        Optional<MineralFountainHeat> heat,
        Optional<FluidStack> toFluid
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

    /** 只判世界类型与高度范围，不含材料方块。 */
    protected boolean matchesContext(MineralFountainRecipeInput input, Level level) {
        if (this.dimension.isPresent() && !this.dimension.get().equals(level.dimension().location())) {
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

    /**
     * 在一组同优先级的候选中按权重随机取一个。
     *
     * <p>权重非正的条目不参与；若全部非正则退回等概率，避免整体不产出。</p>
     */
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
        // 权重之和可能超出 int，这里用 double 抽取，避免溢出
        double roll = random.nextDouble() * total;
        for (T candidate : picked) {
            roll -= Math.max(0, weight.applyAsInt(candidate));
            if (roll < 0) return candidate;
        }
        return picked.getLast();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack assemble(MineralFountainRecipeInput input, HolderLookup.Provider registries) {
        return this.getResultItem(registries);
    }

    @Override
    public ItemStack getResultItem(@Nullable HolderLookup.Provider registries) {
        return this.toBlock.state().getBlock().asItem() == Items.AIR
               ? ItemStack.EMPTY
               : new ItemStack(this.toBlock.state().getBlock());
    }

    @Override
    public boolean isSpecial() {
        return true;
    }
}
