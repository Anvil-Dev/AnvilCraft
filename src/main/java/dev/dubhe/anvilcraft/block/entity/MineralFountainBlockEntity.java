package dev.dubhe.anvilcraft.block.entity;

import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.block.entity.fluid.DrainBlockEntity;
import dev.dubhe.anvilcraft.init.ModHeaterInfos;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.mineral.AbstractMineralFountainRecipe;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainActiveRecipe;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainHeat;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainPassiveRecipe;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainRecipeInput;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainReplacementRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;

public class MineralFountainBlockEntity extends BlockEntity {
    private int tickCount = 0;

    private @Nullable MineralFountainHeat activeHeat = null;

    public MineralFountainBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MINERAL_FOUNTAIN.get(), pos, blockState);
    }

    private MineralFountainBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static MineralFountainBlockEntity createBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState blockState
    ) {
        return new MineralFountainBlockEntity(type, pos, blockState);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.tickCount = input.getIntOr("tickCount", 0);
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("tickCount", this.tickCount);
    }

    public void tick() {
        if (this.level == null) return;
        if (this.tickCount > -1) this.tickCount--;
        if (this.tickCount != 0) return;
        if (!(this.level instanceof ServerLevel serverLevel)) return;

        List<BlockState> around = this.getAroundBlocks();
        BlockState aboveState = this.level.getBlockState(this.getBlockPos().above());
        MineralFountainRecipeInput input =
            new MineralFountainRecipeInput(aboveState, around, this.getBlockPos());

        RecipeHolder<MineralFountainActiveRecipe> drainRecipe = this.pickFluidOutput(input);

        if (drainRecipe != null) {

            this.resetTickCount();
            this.fillDrain(drainRecipe);
        } else {
            RecipeHolder<MineralFountainActiveRecipe> active = this.pickActive(input, true);
            if (active != null) {

                this.produce(this.resolveReplacement(active, input), serverLevel);
            } else {
                MineralFountainPassiveRecipe passive = this.pickPassive(input);
                if (passive != null) this.produce(passive, serverLevel);
            }
        }

        this.setHeat(this.pickHeat(input), serverLevel);
    }

    private void fillDrain(RecipeHolder<MineralFountainActiveRecipe> recipe) {
        if (this.level == null) return;
        if (!(this.level.getBlockEntity(this.getBlockPos().above()) instanceof DrainBlockEntity drain)) return;
        FluidStack fluid = recipe.value().getToFluid().map(FluidStackTemplate::create).orElse(null);
        if (fluid == null || fluid.isEmpty()) return;
        try (Transaction transaction = Transaction.openRoot()) {
            drain.getFluidHandler().insert(FluidResource.of(fluid), fluid.getAmount(), transaction);
            transaction.commit();
        }
    }

    @Nullable
    private RecipeHolder<MineralFountainActiveRecipe> pickFluidOutput(MineralFountainRecipeInput input) {
        if (!(this.level instanceof ServerLevel serverLevel)) return null;
        if (!(this.level.getBlockEntity(this.getBlockPos().above()) instanceof DrainBlockEntity)) return null;
        List<RecipeHolder<MineralFountainActiveRecipe>> matches = serverLevel.getServer().getRecipeManager().recipeMap()
            .byType(ModRecipeTypes.MINERAL_FOUNTAIN_ACTIVE.get())
            .stream()
            .filter(holder -> holder.value().getToFluid().isPresent())
            .filter(holder -> holder.value().matchesContextAndAround(input, this.level))
            .toList();
        return this.pickByPriority(matches, holder -> holder.value().getPriority());
    }

    private AbstractMineralFountainRecipe resolveReplacement(
        RecipeHolder<MineralFountainActiveRecipe> active,
        MineralFountainRecipeInput input
    ) {
        if (!(this.level instanceof ServerLevel serverLevel)) return active.value();
        List<RecipeHolder<MineralFountainReplacementRecipe>> candidates = serverLevel.getServer().getRecipeManager().recipeMap()
            .byType(ModRecipeTypes.MINERAL_FOUNTAIN_REPLACEMENT.get())
            .stream()
            .filter(holder -> holder.value().matches(input, this.level))
            .filter(holder -> holder.value().appliesTo(active.id().identifier()))
            .sorted(Comparator.comparingInt(
                (RecipeHolder<MineralFountainReplacementRecipe> holder) -> holder.value().getPriority()
            ).reversed())
            .toList();
        for (RecipeHolder<MineralFountainReplacementRecipe> holder : candidates) {
            MineralFountainReplacementRecipe recipe = holder.value();
            if (this.level.getRandom().nextDouble() <= recipe.getReplaceChance()) {
                return recipe;
            }
        }
        return active.value();
    }

    private void produce(AbstractMineralFountainRecipe recipe, ServerLevel serverLevel) {
        if (this.level == null) return;
        this.resetTickCount();
        if (this.level.getBlockEntity(this.getBlockPos().above()) != null) return;
        ChanceBlockState result = recipe.getToBlock();
        if (this.level.getRandom().nextDouble() <= recipe.chance(serverLevel)) {
            this.level.setBlockAndUpdate(this.getBlockPos().above(), result.state());
        }
    }

    @Nullable
    private MineralFountainHeat pickHeat(MineralFountainRecipeInput input) {
        RecipeHolder<MineralFountainActiveRecipe> active = this.pickActive(input, false);
        if (active != null && active.value().getHeat().isPresent()) {
            return active.value().getHeat().get();
        }
        return null;
    }

    private void setHeat(@Nullable MineralFountainHeat heat, ServerLevel serverLevel) {
        if (Objects.equals(this.activeHeat, heat)) return;
        if (this.activeHeat != null) {
            HeaterManager.removeProducer(
                this.getBlockPos(), serverLevel, ModHeaterInfos.mineralFountain(this.activeHeat)
            );
        }
        this.activeHeat = heat;
        if (heat != null) {
            HeaterManager.addProducer(this.getBlockPos(), serverLevel, ModHeaterInfos.mineralFountain(heat));
        }
    }

    @Nullable
    private RecipeHolder<MineralFountainActiveRecipe> pickActive(MineralFountainRecipeInput input, boolean includeFrom) {
        if (!(this.level instanceof ServerLevel serverLevel)) return null;
        List<RecipeHolder<MineralFountainActiveRecipe>> matches = serverLevel.getServer().getRecipeManager().recipeMap()
            .byType(ModRecipeTypes.MINERAL_FOUNTAIN_ACTIVE.get())
            .stream()
            .filter(holder -> includeFrom
                ? holder.value().matches(input, this.level)
                : holder.value().matchesContextAndAround(input, this.level))
            .toList();
        return this.pickByPriority(matches, holder -> holder.value().getPriority());
    }

    @Nullable
    private MineralFountainPassiveRecipe pickPassive(MineralFountainRecipeInput input) {
        if (!(this.level instanceof ServerLevel serverLevel)) return null;
        List<RecipeHolder<MineralFountainPassiveRecipe>> matches = serverLevel.getServer().getRecipeManager().recipeMap()
            .byType(ModRecipeTypes.MINERAL_FOUNTAIN_PASSIVE.get())
            .stream()
            .filter(holder -> holder.value().matches(input, this.level))
            .toList();
        RecipeHolder<MineralFountainPassiveRecipe> picked =
            this.pickByPriority(matches, holder -> holder.value().getPriority());
        return picked == null ? null : picked.value();
    }

    @Nullable
    private <T extends Recipe<?>> RecipeHolder<T> pickByPriority(
        List<RecipeHolder<T>> matches,
        ToIntFunction<RecipeHolder<T>> priority
    ) {
        if (matches.isEmpty() || this.level == null) return null;
        int max = matches.stream().mapToInt(priority).max().orElse(0);
        List<RecipeHolder<T>> top = matches.stream().filter(holder -> priority.applyAsInt(holder) == max).toList();

        return AbstractMineralFountainRecipe.pickWeighted(
            this.level.getRandom(), top, holder -> weightOf(holder.value())
        );
    }

    private static int weightOf(Recipe<?> recipe) {
        return recipe instanceof AbstractMineralFountainRecipe fountain
               ? fountain.getWeight()
               : AbstractMineralFountainRecipe.DEFAULT_WEIGHT;
    }

    private static final Direction[] HORIZONTAL_DIRECTION = {
        Direction.NORTH,
        Direction.WEST,
        Direction.EAST,
        Direction.SOUTH
    };

    public List<BlockState> getAroundBlocks() {
        if (this.level == null) return List.of();
        return Arrays.stream(HORIZONTAL_DIRECTION)
            .map(direction -> this.level.getBlockState(this.getBlockPos().relative(direction)))
            .toList();
    }

    public void resetTickCount() {
        if (this.tickCount <= 0) this.tickCount = AnvilCraft.CONFIG.machines.mineralFountainInterval;
    }
}
