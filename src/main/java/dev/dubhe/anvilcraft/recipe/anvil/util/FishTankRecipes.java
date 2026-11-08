package dev.dubhe.anvilcraft.recipe.anvil.util;

import dev.anvilcraft.lib.v2.recipe.AnvilLibRecipe;
import dev.anvilcraft.lib.v2.recipe.InWorldRecipe;
import dev.anvilcraft.lib.v2.recipe.cache.BlockCache;
import dev.anvilcraft.lib.v2.recipe.cache.ItemCache;
import dev.anvilcraft.lib.v2.recipe.cache.item.ICacheElement;
import dev.anvilcraft.lib.v2.recipe.cache.item.ICacheInput;
import dev.anvilcraft.lib.v2.recipe.cache.item.ICacheInputOutputImpl;
import dev.anvilcraft.lib.v2.recipe.cache.item.ItemHandlerCacheElement;
import dev.anvilcraft.lib.v2.recipe.event.InWorldRecipeCommittedEvent;
import dev.anvilcraft.lib.v2.recipe.event.InWorldRecipeEvent;
import dev.anvilcraft.lib.v2.recipe.outcome.IPlannedItemOutcome;
import dev.anvilcraft.lib.v2.recipe.outcome.IRecipeOutcome;
import dev.anvilcraft.lib.v2.recipe.outcome.PlannedItem;
import dev.anvilcraft.lib.v2.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeManager;
import dev.anvilcraft.lib.v2.recipe.util.ItemBatchPlan;
import dev.anvilcraft.lib.v2.recipe.util.Range;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.recipe.CauldronProcessing;
import dev.dubhe.anvilcraft.block.BurningHeaterBlock;
import dev.dubhe.anvilcraft.block.entity.BurningHeaterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTriggers;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.DamageAnvil;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.RoyalPreferenceOutcome;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SuperHeatingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** 在鱼缸副本中匹配加工配方，将容量足够的批次及其流体变化一次提交，避免溢出产物成为掉落物。 */
public final class FishTankRecipes {
    private FishTankRecipes() {
    }

    /** 返回是否已经处理本次触发；其他类型的配方交回世界内配方管理器。 */
    public static boolean trigger(InWorldRecipeManager manager, InWorldRecipeContext impact, FishTankBlockEntity tank) {
        for (RecipeHolder<InWorldRecipe> holder : manager.recipeHolders.get(ModRecipeTriggers.ON_ANVIL_FALL_ON.get())) {
            final InWorldRecipe recipe = holder.value();
            if (!(recipe instanceof AbstractProcessRecipe<?> process) || process.getProperty().getHasCauldron() == null) {
                boolean matches = recipe.matches(impact, impact.getLevel());
                while (!impact.getStack().isEmpty()) {
                    impact.pop(impact.getStack().getLast());
                }
                if (matches) {
                    return false;
                }
                continue;
            }
            FishTankBlockEntity snapshot = tank.copyForRecipe();
            List<ItemStack> inputs = stacks(tank.getInput());
            List<ItemStack> outputs = stacks(tank.getOutputHandler());
            List<BatchState> batches = new ArrayList<>();
            int limit = batchLimit(process, impact, tank.getBlockPos().below());
            Predicate<ItemBatchPlan.Batch> prepare = batch -> prepare(holder, impact, tank, snapshot, batches, batch);
            ItemBatchPlan.SlotCapacity capacity = (slot, stack) -> tank.getOutputHandler().getSlotLimit(slot);
            ItemBatchPlan plan = tank.isProcessingOutput()
                ? ItemBatchPlan.shared(inputs, limit, capacity, prepare)
                : ItemBatchPlan.separate(inputs, outputs, limit, capacity, prepare);
            if (batches.isEmpty()) {
                continue;
            }
            if (plan.batches() > 0) {
                CompoundTag original = tank.saveWithFullMetadata(impact.getLevel().registryAccess());
                BatchState last = batches.get(plan.batches() - 1);
                for (int index = 0; index < plan.batches(); index++) {
                    if (batches.get(index).context().get(DamageAnvil.DAMAGE_ANVIL)) {
                        impact.put(DamageAnvil.DAMAGE_ANVIL, true);
                    }
                }
                impact.putAcceptor(AnvilCraft.of("fish_tank_batch"), ignored -> plan.commit(selected -> {
                    if (!original.equals(tank.saveWithFullMetadata(impact.getLevel().registryAccess()))) {
                        throw new IllegalStateException("Fish tank changed while planning recipes at " + tank.getBlockPos());
                    }
                    tank.applyRecipePlan(selected, last.fluid(), last.ignited());
                }));
            }
            return true;
        }
        return true;
    }

    private static int batchLimit(AbstractProcessRecipe<?> recipe, InWorldRecipeContext impact, BlockPos heaterPos) {
        int limit = Math.min(recipe.maxEfficiency(), AnvilLibRecipe.CONFIG.inWorldRecipeMaxEfficiency);
        var processor = recipe.getProperty().getCauldronProcessor();
        if (processor == null || !(impact.getLevel().getBlockEntity(heaterPos) instanceof BurningHeaterBlockEntity heater)) {
            return limit;
        }
        for (IRecipeOutcome<?> outcome : recipe.outcomes()) {
            if (!(outcome instanceof SuperHeatingRecipe.ConsumeFuel) || impact.get(SuperHeatingRecipe.ConsumeFuel.FUEL_CONSUMED)) {
                continue;
            }
            int remaining = Math.max(0, heater.getBurnTime() - SuperHeatingRecipe.ConsumeFuel.FUEL_COST_TICKS);
            int heat = remaining >= BurningHeaterBlockEntity.LIT_THRESHOLD ? 2 : remaining > 0 ? 1 : 0;
            var after = heater.getBlockState().setValue(BurningHeaterBlock.LEVEL, heat);
            if (!CauldronProcessing.matches(processor, after)) {
                return Math.min(limit, 1);
            }
            break;
        }
        return limit;
    }

    private static boolean prepare(
        RecipeHolder<InWorldRecipe> holder, InWorldRecipeContext impact, FishTankBlockEntity tank,
        FishTankBlockEntity snapshot, List<BatchState> batches, ItemBatchPlan.Batch batch
    ) {
        final InWorldRecipe recipe = holder.value();
        InWorldRecipeContext context = new InWorldRecipeContext(impact.getLevel(), impact.getPos(), impact.getEntity());
        BlockCache blocks = new BlockCache(impact.getLevel());
        blocks.setBlockEntity(tank.getBlockPos(), snapshot);
        context.put(BlockCache.BLOCK_CACHE, blocks);
        SnapshotItems items = new SnapshotItems(context, snapshot.getInput(), tank.getInput(), tank.getBlockPos());
        context.put(ItemCache.ITEM_CACHE, items);
        if (!recipe.matches(context, impact.getLevel())) {
            return false;
        }
        final List<ItemStack> before = stacks(snapshot.getInput());
        context.beginBatch();
        if (recipe instanceof SuperHeatingRecipe) {
            context.put(RoyalPreferenceOutcome.IS_ROYAL_STEEL_RECIPE, true);
        }
        List<IRecipePredicate<?>> predicates = List.copyOf(context.getStack());
        predicates.forEach(predicate -> predicate.accept(context));
        List<PlannedItem> outside = new ArrayList<>();
        List<IRecipeOutcome<?>> effects = new ArrayList<>();
        for (IRecipeOutcome<?> outcome : recipe.outcomes()) {
            if (outcome instanceof IPlannedItemOutcome<?> planned) {
                for (PlannedItem result : planned.planWithChance(context)) {
                    if (BlockPos.containing(result.pos()).equals(tank.getBlockPos())) {
                        batch.produce(result.stack());
                    } else {
                        outside.add(result);
                    }
                }
            } else if (outcome instanceof DamageAnvil) {
                outcome.acceptWithChance(context);
            } else {
                effects.add(outcome);
            }
        }
        items.syncInputs();
        predicates.forEach(predicate -> predicate.clearStack(context));
        context.getStack().clear();
        List<ItemStack> after = stacks(snapshot.getInput());
        for (int slot = 0; slot < before.size(); slot++) {
            ItemStack stack = before.get(slot);
            int consumed = stack.getCount() - after.get(slot).getCount();
            if (consumed > 0 && batch.take(value -> ItemStack.isSameItemSameComponents(value, stack), consumed).isEmpty()) {
                throw new IllegalStateException("Fish tank recipe reservation changed at " + tank.getBlockPos());
            }
        }
        context.put(ItemCache.ITEM_CACHE, new ItemCache(impact.getLevel()));
        FluidStack fluid = snapshot.getFluidHandler().getFluid().copy();
        // 自动点燃会消耗物品，只在最终库存提交后统一处理。
        snapshot.setIgnited(snapshot.isIgnited() && FishTankBlockEntity.canIgnite(fluid));
        BatchState state = new BatchState(context, fluid, snapshot.isIgnited());
        batches.add(state);
        batch.afterCommit(() -> {
            blocks.setBlockEntity(tank.getBlockPos(), tank);
            for (IRecipeOutcome<?> effect : effects) {
                effect.acceptWithChance(effect instanceof SuperHeatingRecipe.ConsumeFuel ? impact : context);
            }
            outside.forEach(result -> result.accept(context));
            NeoForge.EVENT_BUS.post(new InWorldRecipeEvent(recipe.getType(), holder.id(), recipe, context));
            long batchId = context.getBatchId();
            context.afterCommit(() -> NeoForge.EVENT_BUS.post(new InWorldRecipeCommittedEvent(
                recipe.getType(), holder.id(), recipe, context, batchId
            )));
            context.accept();
        });
        return true;
    }

    private static List<ItemStack> stacks(IItemHandler handler) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            stacks.add(handler.getStackInSlot(slot).copy());
        }
        return stacks;
    }

    private record BatchState(InWorldRecipeContext context, FluidStack fluid, boolean ignited) {
    }

    private static final class SnapshotItems extends ItemCache {
        private final List<ICacheElement> inputs = new ArrayList<>();
        private final Range bounds;

        private SnapshotItems(InWorldRecipeContext context, IItemHandler snapshot, IItemHandler source, BlockPos pos) {
            super(context.getLevel());
            Vec3 center = pos.getCenter();
            Vec3 size = new Vec3(1, 1, 1);
            this.bounds = Range.of(center, size);
            for (int slot = 0; slot < snapshot.getSlots(); slot++) {
                this.inputs.add(new ItemHandlerCacheElement(this, snapshot, slot, center, size) {
                    @Override
                    public Object getSource() {
                        return source;
                    }
                });
            }
        }

        @Override
        public ICacheInput getInput(
            Predicate<ItemStack> predicate, Vec3 pos, Vec3 range, Predicate<ICacheElement> sourceFilter
        ) {
            Range area = Range.of(pos, range);
            return new ICacheInputOutputImpl(predicate, this, pos, area,
                this.bounds.cross(area) ? this.inputs : List.of(),
                element -> sourceFilter.test(element) && predicate.test(element.getStack()));
        }

        private void syncInputs() {
            this.inputs.forEach(ICacheElement::sync);
        }
    }
}
