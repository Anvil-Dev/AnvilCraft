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
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;

public class MineralFountainBlockEntity extends BlockEntity {
    private int tickCount = 0;
    /** 当前生效的加热定义；为空表示不加热。 */
    private MineralFountainHeat activeHeat = null;

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
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.tickCount = tag.getInt("tickCount");
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("tickCount", this.tickCount);
    }

    /**
     * 矿物涌泉tick
     */
    public void tick() {
        if (this.level == null) return;
        if (this.tickCount > -1) this.tickCount--;
        if (this.tickCount != 0) return;
        if (!(this.level instanceof ServerLevel serverLevel)) return;

        List<BlockState> around = this.getAroundBlocks();
        BlockState aboveState = this.level.getBlockState(this.getBlockPos().above());
        MineralFountainRecipeInput input =
            new MineralFountainRecipeInput(aboveState, around, this.getBlockPos());

        // 主动生产优先，命中后允许替换生产改写结果
        RecipeHolder<MineralFountainActiveRecipe> active = this.pickActive(input, true);
        if (active != null) {
            this.produce(this.resolveReplacement(active, input), serverLevel);
        } else {
            // 无主动生产匹配时执行被动生产
            MineralFountainPassiveRecipe passive = this.pickPassive(input);
            if (passive != null) this.produce(passive, serverLevel);
        }

        // 上方为容器时抽出四周的熔岩灌入，这一步不产出方块
        this.tryFillDrain(around, serverLevel);

        // 加热只看维度、高度与周围方块，与上方方块无关
        this.setHeat(this.pickHeat(input), serverLevel);
    }

    /**
     * 四周皆为熔岩且上方为容器时，把熔岩灌入容器。
     *
     * <p>判据是**周围**方块而非上方方块：上方放的是排水口，其方块状态永远不是熔岩。
     * 与旧版一致，只认熔岩源，流动熔岩不计入。</p>
     */
    private void tryFillDrain(List<BlockState> around, ServerLevel serverLevel) {
        if (this.level == null || !isLavaSourceAround(around)) return;
        if (!(this.level.getBlockEntity(this.getBlockPos().above()) instanceof DrainBlockEntity drain)) return;
        var handler = drain.getFluidHandler();
        FluidStack lava = new FluidStack(Fluids.LAVA, FluidType.BUCKET_VOLUME);
        if (handler.fill(lava, IFluidHandler.FluidAction.SIMULATE) == lava.getAmount()) {
            handler.fill(lava, IFluidHandler.FluidAction.EXECUTE);
        }
        this.resetTickCount();
    }

    /**
     * 四面是否皆为熔岩源。
     */
    private static boolean isLavaSourceAround(List<BlockState> around) {
        if (around.size() < HORIZONTAL_DIRECTION.length) return false;
        return around.stream().allMatch(state ->
            state.is(Blocks.LAVA) && state.getFluidState().isSource()
        );
    }

    /**
     * 让主动配方的结果被替换配方改写；没有命中替换时返回主动配方自身。
     */
    private AbstractMineralFountainRecipe resolveReplacement(
        RecipeHolder<MineralFountainActiveRecipe> active,
        MineralFountainRecipeInput input
    ) {
        if (this.level == null) return active.value();
        List<RecipeHolder<MineralFountainReplacementRecipe>> candidates = this.level.getRecipeManager()
            .getAllRecipesFor(ModRecipeTypes.MINERAL_FOUNTAIN_REPLACEMENT.get())
            .stream()
            .filter(holder -> holder.value().matches(input, this.level))
            .filter(holder -> holder.value().appliesTo(active.id()))
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

    /**
     * 按配方的概率产出方块。
     */
    private void produce(AbstractMineralFountainRecipe recipe, ServerLevel serverLevel) {
        if (this.level == null) return;
        ChanceBlockState result = recipe.getToBlock();
        if (this.level.getRandom().nextDouble() <= recipe.chance(serverLevel)) {
            this.level.setBlockAndUpdate(this.getBlockPos().above(), result.state());
        }
    }

    /**
     * 取当前应生效的加热定义：只看维度、高度与周围方块，与上方方块无关。
     */
    @Nullable
    private MineralFountainHeat pickHeat(MineralFountainRecipeInput input) {
        RecipeHolder<MineralFountainActiveRecipe> active = this.pickActive(input, false);
        if (active != null && active.value().getHeat().isPresent()) {
            return active.value().getHeat().get();
        }
        return null;
    }

    /**
     * 登记加热定义；与当前相同时保留既有登记，避免每 tick 重复增删。
     */
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

    /**
     * 取优先级最高的匹配主动配方；同优先级时随机取一个。
     *
     * @param includeFrom 是否把材料方块计入匹配；加热查询时传 false
     */
    @Nullable
    private RecipeHolder<MineralFountainActiveRecipe> pickActive(MineralFountainRecipeInput input, boolean includeFrom) {
        if (this.level == null) return null;
        List<RecipeHolder<MineralFountainActiveRecipe>> matches = this.level.getRecipeManager()
            .getAllRecipesFor(ModRecipeTypes.MINERAL_FOUNTAIN_ACTIVE.get())
            .stream()
            .filter(holder -> includeFrom
                ? holder.value().matches(input, this.level)
                : holder.value().matchesContextAndAround(input, this.level))
            .toList();
        return this.pickByPriority(matches, holder -> holder.value().getPriority());
    }

    /**
     * 取优先级最高的匹配被动配方；同优先级时按权重随机取一。
     */
    @Nullable
    private MineralFountainPassiveRecipe pickPassive(MineralFountainRecipeInput input) {
        if (this.level == null) return null;
        List<RecipeHolder<MineralFountainPassiveRecipe>> matches = this.level.getRecipeManager()
            .getAllRecipesFor(ModRecipeTypes.MINERAL_FOUNTAIN_PASSIVE.get())
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
        // 同优先级并列时按权重随机，权重由配方数据包给出
        return AbstractMineralFountainRecipe.pickWeighted(
            this.level.getRandom(), top, holder -> weightOf(holder.value())
        );
    }

    /**
     * 取配方的抽取权重；非矿物涌泉配方视为等权。
     */
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

    /**
     * 四个水平相邻方块，交由配方的需求表判定。
     */
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
