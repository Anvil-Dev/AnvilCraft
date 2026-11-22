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
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity.RecipePreview;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.RoyalPreferenceOutcome;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.anvil.util.RecipeOutputTracker;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCompressRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.StampingDiffRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SuperHeatingRecipe;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.Identifier;
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
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class FishTankPreviews {
    private FishTankPreviews() {
    }

    static Cache get(FishTankBlockEntity tank, Level level, @Nullable Cache cached, boolean allowOutputs) {
        var map = RecipesRecord.getRecipes(level);
        long time = level.getGameTime();
        BlockPos pos = tank.getBlockPos();
        BlockState floor = level.getBlockState(pos.below());
        BlockState above = level.getBlockState(pos.above());
        BlockState anvil = above.is(BlockTags.ANVIL) ? above : Blocks.ANVIL.defaultBlockState();
        var index = map == null ? null : LargeCauldronRecipes.Previews.get(map, false);
        Object revision = index == null ? null : index.revision;
        if (cached != null && cached.revision == revision && cached.allowOutputs == allowOutputs && cached.floor == floor
            && cached.anvil == anvil && time >= cached.time && time - cached.time < 10) {
            return cached;
        }
        List<RecipePreview> results = List.of();
        if (index != null) {
            var inputs = stacks(tank.getInputHandler());
            var outputs = stacks(tank.getOutputHandler());
            boolean useOutputs = allowOutputs && inputs.stream().allMatch(ItemStack::isEmpty);
            List<ItemStack> source = useOutputs ? outputs : inputs;
            var world = new Snapshot(level, pos, pos.above().getBottomCenter(), anvil, new BlockCache(level),
                Range.of(pos.getCenter(), new Vec3(1, 1, 1)));
            for (var holder : index.forTank(source)) {
                var recipe = holder.value();
                if (recipe instanceof StampingDiffRecipe || (!useOutputs && recipe instanceof ItemCompressRecipe
                    && RecipeOutputTracker.requiresRecipeOutputForCompression(floor))) {
                    continue;
                }
                var match = CauldronPreviewMatcher.match(recipe, new Ingredients(world, source));
                if (match == null) {
                    continue;
                }
                if (!(recipe instanceof AbstractProcessRecipe<?> process) || process.getHasCauldron() == null
                    || fits(tank, recipe, match, outputs, useOutputs)) {
                    results = match.previews(holder, useOutputs);
                }
                // 鱼缸会停在第一个匹配的配方，即使此时产物槽已经装满。
                break;
            }
        }
        return new Cache(time, revision, floor, anvil, allowOutputs, results);
    }

    record Cache(long time, @Nullable Object revision, BlockState floor, BlockState anvil,
                 boolean allowOutputs, List<RecipePreview> results) {
    }

    private record Snapshot(Level level, BlockPos pos, Vec3 impact, BlockState anvil, BlockCache blocks, Range bounds) {
    }

    private static List<ItemStack> stacks(ResourceHandler<ItemResource> handler) {
        List<ItemStack> result = new ArrayList<>(handler.size());
        for (int slot = 0; slot < handler.size(); slot++) {
            result.add(handler.getResource(slot).toStack(handler.getAmountAsInt(slot)));
        }
        return result;
    }

    private static class Ingredients implements CauldronPreviewMatcher.State<Ingredients> {
        private final Snapshot world;
        private final List<ItemStack> stacks;
        private final int[] reserved;
        private final int[] consumed;
        private final List<HasCauldron> fluids;
        private @Nullable Map<Identifier, SavedComponent> saved;
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

        private List<RecipePreview> previews(RecipeHolder<InWorldRecipe> holder, boolean output) {
            List<RecipePreview> result = new ArrayList<>();
            var predicates = List.copyOf(this.fluids);
            for (int slot = 0; slot < this.reserved.length; slot++) {
                if (this.reserved[slot] > 0) {
                    result.add(RecipePreview.of(output ? -1 : slot, output ? slot : -1, holder, predicates));
                }
            }
            return result.isEmpty() && !predicates.isEmpty() ? List.of(RecipePreview.of(-1, -1, holder, predicates)) : List.copyOf(result);
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

    private static boolean fits(
        FishTankBlockEntity tank, InWorldRecipe recipe, Ingredients match, List<ItemStack> outputs, boolean shared
    ) {
        int limit = Math.min(recipe.maxEfficiency(), AnvilLibRecipe.CONFIG.inWorldRecipeMaxEfficiency);
        if (limit <= 0) {
            return false;
        }
        var planned = products(tank, recipe, match, shared);
        List<ItemStack> products = planned.stacks;
        int unknownSlots = planned.unknownSlots;
        if (unknownSlots > 0) {
            int emptySlots = 0;
            for (int slot = 0; slot < outputs.size(); slot++) {
                if (outputs.get(slot).isEmpty() || (shared && outputs.get(slot).getCount() == match.consumed[slot])) {
                    emptySlots++;
                }
            }
            for (ItemStack stack : products) {
                unknownSlots += Math.ceilDiv(stack.getCount(), stack.getMaxStackSize());
            }
            return emptySlots >= unknownSlots;
        }
        if (products.isEmpty()) {
            return true;
        }
        ItemBatchPlan.SlotCapacity capacity = (slot, stack) -> stack.getMaxStackSize();
        if (!shared) {
            return ItemBatchPlan.separate(match.stacks, outputs, 1, capacity, batch -> match.fillBatch(batch, products)).batches() > 0;
        }
        if (ItemBatchPlan.shared(outputs, 1, capacity, batch -> match.fillBatch(batch, products)).batches() > 0) {
            return true;
        }
        // 共用槽位时后续批次可以腾出位置；在鱼缸副本上跟踪各批的剩余流体和原料。
        var simulated = tank.copyForRecipe();
        var blocks = new BlockCache(match.world.level);
        blocks.setBlockEntity(match.world.pos, simulated);
        var world = new Snapshot(match.world.level, match.world.pos, match.world.impact, match.world.anvil, blocks, match.world.bounds);
        List<ItemStack> remaining = new ArrayList<>(outputs);
        return ItemBatchPlan.shared(outputs, limit, capacity, batch -> {
            var next = CauldronPreviewMatcher.match(recipe, new Ingredients(world, remaining));
            if (next == null) {
                return false;
            }
            var current = planned.variesByInput ? products(tank, recipe, next, true) : planned;
            if (current.unknownSlots > 0 || !next.fillBatch(batch, current.stacks)) {
                return false;
            }
            boolean freed = false;
            for (int slot = 0; slot < remaining.size(); slot++) {
                if (next.consumed[slot] > 0) {
                    ItemStack before = remaining.get(slot);
                    remaining.set(slot, before.copyWithCount(before.getCount() - next.consumed[slot]));
                    freed = true;
                }
            }
            if (freed) {
                for (HasCauldron fluid : next.fluids) {
                    if (fluid.chance() >= 1) {
                        FluidStack stored = simulated.getFluidHandler().getStack();
                        int amount = stored.getAmount() - fluid.consume() + fluid.produce();
                        FluidStack result = fluid.transforms().isEmpty() ? stored : fluid.transforms().getFirst().create();
                        simulated.getFluidHandler().set(FluidResource.of(result), amount);
                    }
                }
                simulated.setIgnited(simulated.isIgnited() && FishTankBlockEntity.canIgnite(simulated.getFluidHandler().getStack()));
            }
            return freed;
        }).batches() > 0;
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
                    ItemStack product = item.item().withCount(count).create();
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
                    products.add(bonus.result().stack().create().copyWithCount(count));
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
