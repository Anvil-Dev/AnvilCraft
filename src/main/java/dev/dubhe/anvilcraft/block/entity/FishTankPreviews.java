package dev.dubhe.anvilcraft.block.entity;

import dev.anvilcraft.lib.v2.recipe.AnvilLibRecipe;
import dev.anvilcraft.lib.v2.recipe.InWorldRecipe;
import dev.anvilcraft.lib.v2.recipe.cache.BlockCache;
import dev.anvilcraft.lib.v2.recipe.outcome.SpawnItem;
import dev.anvilcraft.lib.v2.recipe.outcome.function.ApplyTagToComponent;
import dev.anvilcraft.lib.v2.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.v2.recipe.predicate.function.SaveComponentToTag;
import dev.anvilcraft.lib.v2.recipe.predicate.item.HasItemIngredient;
import dev.anvilcraft.lib.v2.recipe.util.ItemBatchPlan;
import dev.anvilcraft.lib.v2.recipe.util.Range;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity.FluidKey;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity.RecipePreview;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.RoyalPreferenceOutcome;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.anvil.util.RecipeOutputTracker;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCompressRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SuperHeatingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

final class FishTankPreviews {
    private FishTankPreviews() {
    }

    static Cache get(FishTankBlockEntity tank, Level level, @Nullable Cache cached, boolean allowOutputs) {
        var map = level.getRecipeManager();
        long time = level.getGameTime();
        BlockPos pos = tank.getBlockPos();
        BlockState floor = level.getBlockState(pos.below());
        BlockState above = level.getBlockState(pos.above());
        BlockState anvil = above.is(BlockTags.ANVIL) ? above : Blocks.ANVIL.defaultBlockState();
        var index = LargeCauldronRecipes.Previews.get(map, false);
        Object revision = index.revision;
        if (cached != null && cached.revision == revision && cached.allowOutputs == allowOutputs && cached.floor == floor
            && cached.anvil == anvil && time >= cached.time && time - cached.time < 10) {
            return cached;
        }
        List<RecipePreview> results = List.of();
        var inputs = stacks(tank.getInputHandler());
        var outputs = stacks(tank.getOutputHandler());
        boolean useOutputs = allowOutputs && inputs.stream().allMatch(ItemStack::isEmpty);
        List<ItemStack> source = useOutputs ? outputs : inputs;
        var world = new Snapshot(level, pos, pos.above().getBottomCenter(), anvil, new BlockCache(level),
            Range.of(pos.getCenter(), new Vec3(1, 1, 1)));
        for (var holder : index.forTank(source)) {
            var recipe = holder.value();
            if (!useOutputs && recipe instanceof ItemCompressRecipe && RecipeOutputTracker.requiresRecipeOutputForCompression(floor)) {
                continue;
            }
            var match = CauldronPreviewMatcher.match(recipe, new Ingredients(world, source));
            if (match == null) {
                continue;
            }
            results = previews(tank, holder, match, outputs, useOutputs);
            // 鱼缸会停在第一个匹配的配方，即使此时产物槽已经装满。
            break;
        }
        return new Cache(time, revision, floor, anvil, allowOutputs, results);
    }

    record Cache(long time, @Nullable Object revision, BlockState floor, BlockState anvil,
                 boolean allowOutputs, List<RecipePreview> results) {
    }

    private record Snapshot(Level level, BlockPos pos, Vec3 impact, BlockState anvil, BlockCache blocks, Range bounds) {
    }

    private static List<ItemStack> stacks(IItemHandler handler) {
        List<ItemStack> result = new ArrayList<>(handler.getSlots());
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            result.add(handler.getStackInSlot(slot));
        }
        return result;
    }

    private static class Ingredients implements CauldronPreviewMatcher.State<Ingredients> {
        private final Snapshot world;
        private final List<ItemStack> stacks;
        private final int[] reserved;
        private final int[] consumed;
        private final List<HasCauldron> fluids;
        private @Nullable Map<ResourceLocation, SavedComponent> saved;
        private boolean unknownTags;

        private Ingredients(Snapshot world, List<ItemStack> stacks) {
            this.world = world;
            this.stacks = stacks;
            this.reserved = new int[stacks.size()];
            this.consumed = new int[stacks.size()];
            this.fluids = new ArrayList<>();
        }

        private Ingredients(Ingredients source) {
            this.world = source.world;
            this.stacks = source.stacks;
            this.reserved = source.reserved.clone();
            this.consumed = source.consumed.clone();
            this.fluids = new ArrayList<>(source.fluids);
            this.saved = source.saved == null ? null : new HashMap<>(source.saved);
            this.unknownTags = source.unknownTags;
        }

        @Override
        public Ingredients copy() {
            return new Ingredients(this);
        }

        @Override
        public boolean apply(IRecipePredicate<?> predicate) {
            if (predicate instanceof HasCauldron fluid) {
                if (!BlockPos.containing(this.world.impact.add(fluid.offset())).equals(this.world.pos)
                    || !fluid.matchesSingleTank(this.world.blocks, this.world.pos)) {
                    return false;
                }
                this.fluids.add(fluid);
                return true;
            }
            if (predicate instanceof HasItemIngredient item) {
                if (!this.world.bounds.cross(this.world.impact.add(item.getOffset()), item.getRange())) {
                    return false;
                }
                int needed = item.getItem().count();
                for (int slot = 0; slot < this.stacks.size() && needed > 0; slot++) {
                    ItemStack stack = this.stacks.get(slot);
                    int available = stack.getCount() - this.reserved[slot];
                    if (available <= 0 || !item.getItem().testIgnoreCount(stack)) {
                        continue;
                    }
                    int take = Math.min(available, needed);
                    this.reserved[slot] += take;
                    if (item.isConsume()) {
                        this.consumed[slot] += take;
                    }
                    for (var function : item.getFunctions()) {
                        if (function instanceof SaveComponentToTag<?> save) {
                            if (this.saved == null) {
                                this.saved = new HashMap<>();
                            }
                            this.saved.put(save.path(), new SavedComponent(save.component(), stack));
                        } else {
                            this.unknownTags = true;
                        }
                    }
                    needed -= take;
                }
                return needed == 0;
            }
            return CauldronPreviewMatcher.matchesWorld(predicate, this.world.level, this.world.impact, this.world.anvil);
        }

        private boolean applyComponents(SpawnItem outcome, ItemStack product) {
            for (var function : outcome.functions()) {
                if (!(function instanceof ApplyTagToComponent<?> apply) || this.unknownTags) {
                    return false;
                }
                SavedComponent source = this.saved == null ? null : this.saved.get(apply.path());
                if (source != null && !source.copyTo(apply.component(), product)) {
                    return false;
                }
            }
            return true;
        }

        private record SavedComponent(DataComponentType<?> type, ItemStack source) {
            private <T> boolean copyTo(DataComponentType<T> component, ItemStack product) {
                if (this.type != component) {
                    return false;
                }
                T value = this.source.get(component);
                if (value != null) {
                    product.set(component, value);
                }
                return true;
            }
        }

        private boolean fillBatch(ItemBatchPlan.Batch batch, List<ItemStack> products) {
            for (int slot = 0; slot < this.consumed.length; slot++) {
                int count = this.consumed[slot];
                if (count > 0) {
                    ItemStack stack = this.stacks.get(slot);
                    if (batch.take(item -> ItemStack.isSameItemSameComponents(item, stack), count).isEmpty()) {
                        return false;
                    }
                }
            }
            products.forEach(batch::produce);
            return true;
        }
    }

    private static List<RecipePreview> previews(
        FishTankBlockEntity tank, RecipeHolder<InWorldRecipe> holder, Ingredients first, List<ItemStack> outputs, boolean shared
    ) {
        var recipe = holder.value();
        int limit = Math.min(recipe.maxEfficiency(), AnvilLibRecipe.CONFIG.inWorldRecipeMaxEfficiency);
        if (limit <= 0 || (Arrays.stream(first.consumed).noneMatch(count -> count > 0)
            && first.fluids.stream().noneMatch(fluid -> fluid.consume() > 0 && fluid.chance() > 0))) {
            return List.of();
        }
        var simulated = tank.copyForRecipe();
        var blocks = new BlockCache(first.world.level);
        blocks.setBlockEntity(first.world.pos, simulated);
        var world = new Snapshot(first.world.level, first.world.pos, first.world.impact, first.world.anvil, blocks, first.world.bounds);
        List<ItemStack> remaining = new ArrayList<>(first.stacks);
        Map<Integer, Integer> itemCounts = new HashMap<>();
        Map<FluidKey, Integer> fluidAmounts = new HashMap<>();
        boolean checkOutputs = recipe instanceof AbstractProcessRecipe<?> process && process.getProperty().getHasCauldron() != null;
        Products initial = checkOutputs ? products(tank, recipe, first, shared) : new Products(List.of(), 0, false);
        Predicate<ItemBatchPlan.Batch> prepare = batch -> {
            var match = CauldronPreviewMatcher.match(recipe, new Ingredients(world, remaining));
            if (match == null) {
                return false;
            }
            Products current = initial.variesByInput ? products(tank, recipe, match, shared) : initial;
            if (current.unknownSlots > 0 && !hasSpaceForUnknown(outputs, match, current, shared)) {
                return false;
            }
            if (!match.fillBatch(batch, current.stacks)) {
                return false;
            }
            for (int slot = 0; slot < remaining.size(); slot++) {
                if (match.consumed[slot] > 0) {
                    var before = remaining.get(slot);
                    remaining.set(slot, before.copyWithCount(before.getCount() - match.consumed[slot]));
                }
            }
            Map<FluidKey, Integer> batchFluids = new HashMap<>();
            for (HasCauldron fluid : match.fluids) {
                if (fluid.chance() <= 0) {
                    continue;
                }
                FluidStack stored = simulated.getFluidHandler().getFluid();
                if (fluid.consume() > 0 && !stored.isEmpty()) {
                    batchFluids.merge(FluidKey.of(stored), fluid.consume(), Integer::sum);
                }
                if (fluid.consume() > 0 || !fluid.transforms().isEmpty()) {
                    int amount = stored.getAmount() - fluid.consume() + fluid.produce();
                    FluidStack result = fluid.transforms().isEmpty() ? stored : fluid.transforms().getFirst();
                    simulated.getFluidHandler().setFluid(result.copyWithAmount(amount));
                }
            }
            simulated.setIgnited(simulated.isIgnited() && FishTankBlockEntity.canIgnite(simulated.getFluidHandler().getFluid()));
            // 只有产物能完整放入的批次才计入预览；共用槽位时后续批次可能腾出空间。
            batch.afterCommit(() -> {
                for (int slot = 0; slot < match.consumed.length; slot++) {
                    if (match.consumed[slot] > 0) {
                        itemCounts.merge(slot, match.consumed[slot], Integer::sum);
                    }
                }
                batchFluids.forEach((fluid, amount) -> fluidAmounts.merge(fluid, amount, Integer::sum));
            });
            return true;
        };
        // 自定义产物函数的组件和堆叠上限未知，只展示已经预留独立空槽的一批。
        int batches = initial.unknownSlots > 0 ? 1 : limit;
        ItemBatchPlan.SlotCapacity capacity = (slot, stack) -> tank.getOutputHandler().getSlotLimit(slot);
        var plan = shared ? ItemBatchPlan.shared(outputs, batches, capacity, prepare)
            : ItemBatchPlan.separate(first.stacks, outputs, batches, capacity, prepare);
        plan.commit(ignored -> {});
        if (itemCounts.isEmpty() && fluidAmounts.isEmpty()) {
            return List.of();
        }
        return List.of(RecipePreview.of(holder, shared, itemCounts, fluidAmounts));
    }

    private static boolean hasSpaceForUnknown(List<ItemStack> outputs, Ingredients match, Products products, boolean shared) {
        int empty = 0;
        for (int slot = 0; slot < outputs.size(); slot++) {
            if (outputs.get(slot).isEmpty() || (shared && outputs.get(slot).getCount() == match.consumed[slot])) {
                empty++;
            }
        }
        int required = products.unknownSlots;
        for (ItemStack stack : products.stacks) {
            required += Math.ceilDiv(stack.getCount(), stack.getMaxStackSize());
        }
        return empty >= required;
    }

    private record Products(List<ItemStack> stacks, int unknownSlots, boolean variesByInput) {
    }

    private static Products products(FishTankBlockEntity tank, InWorldRecipe recipe, Ingredients match, boolean shared) {
        List<ItemStack> products = new ArrayList<>();
        int unknownSlots = 0;
        boolean variesByInput = false;
        for (var outcome : recipe.outcomes()) {
            if (outcome instanceof SpawnItem item && BlockPos.containing(match.world.impact.add(item.offset())).equals(match.world.pos)) {
                variesByInput |= !item.functions().isEmpty();
                int count = Math.clamp(minimumCount(item.count()), 0, 99);
                if (count > 0) {
                    ItemStack product = item.item().copyWithCount(count);
                    if (match.applyComponents(item, product)) {
                        products.add(product);
                    } else {
                        // 自定义函数可能改变产物及堆叠上限，每个产物按独立空槽检查。
                        unknownSlots += count;
                    }
                }
            } else if (outcome instanceof RoyalPreferenceOutcome bonus && recipe instanceof SuperHeatingRecipe
                && RoyalPreferenceOutcome.hasPreferredSource(match.world.level, match.world.impact,
                    shared ? tank.getOutputHandler() : tank.getInputHandler())) {
                int count = minimumCount(bonus.result().count());
                if (count > 0) {
                    products.add(bonus.result().stack().copyWithCount(count));
                }
            }
        }
        return new Products(products, unknownSlots, variesByInput);
    }

    private static int minimumCount(NumberProvider count) {
        return switch (count) {
            case ConstantValue value -> (int) Math.floor(value.value());
            case UniformGenerator range -> minimumCount(range.min());
            case BinomialDistributionGenerator binomial -> minimumCount(binomial.p()) >= 1 ? minimumCount(binomial.n()) : 0;
            default -> 0;
        };
    }
}
