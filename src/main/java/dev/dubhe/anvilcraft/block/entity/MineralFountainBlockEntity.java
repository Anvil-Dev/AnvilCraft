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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
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

    public void tick() {
        if (this.level == null) return;
        if (this.tickCount > -1) this.tickCount--;
        if (this.tickCount != 0) return;
        if (!(this.level instanceof ServerLevel serverLevel)) return;

        List<BlockState> around = this.getAroundBlocks();
        BlockState aboveState = this.level.getBlockState(this.getBlockPos().above());
        MineralFountainRecipeInput input =
            new MineralFountainRecipeInput(aboveState, around, this.getBlockPos());

        // 有排水口且存在流体配方时走滴液，跳过方块产出（放置方块会盖掉排水口）
        RecipeHolder<MineralFountainActiveRecipe> drainRecipe = this.pickFluidOutput(input);

        if (drainRecipe != null) {
            // 排水口装满后不会向服务端邻居发方块更新（只有 UPDATE_CLIENTS），
            // 这里必须无条件重置，否则 tickCount 降到 -1 后永久早退、抽空也无法恢复
            this.resetTickCount();
            this.fillDrain(drainRecipe);
        } else {
            RecipeHolder<MineralFountainActiveRecipe> active = this.pickActive(input, true);
            if (active != null) {
                // 主动生产优先，命中后允许替换生产改写结果
                this.produce(this.resolveReplacement(active, input), serverLevel);
            } else {
                MineralFountainPassiveRecipe passive = this.pickPassive(input);
                if (passive != null) this.produce(passive, serverLevel);
            }
        }

        this.setHeat(this.pickHeat(input), serverLevel);
    }

    /** 把流体灌入上方的排水口；交给储罐按剩余空间灌注，允许只灌一部分。 */
    private void fillDrain(RecipeHolder<MineralFountainActiveRecipe> recipe) {
        if (this.level == null) return;
        if (!(this.level.getBlockEntity(this.getBlockPos().above()) instanceof DrainBlockEntity drain)) return;
        FluidStack fluid = recipe.value().getToFluid().orElse(null);
        if (fluid == null || fluid.isEmpty()) return;
        drain.getFluidHandler().fill(fluid, IFluidHandler.FluidAction.EXECUTE);
    }

    /** 取优先级最高的、声明了流体产出且匹配周围方块的主动配方；上方不是排水口时恒为 {@code null}。 */
    @Nullable
    private RecipeHolder<MineralFountainActiveRecipe> pickFluidOutput(MineralFountainRecipeInput input) {
        if (this.level == null) return null;
        if (!(this.level.getBlockEntity(this.getBlockPos().above()) instanceof DrainBlockEntity)) return null;
        List<RecipeHolder<MineralFountainActiveRecipe>> matches = this.level.getRecipeManager()
            .getAllRecipesFor(ModRecipeTypes.MINERAL_FOUNTAIN_ACTIVE.get())
            .stream()
            .filter(holder -> holder.value().getToFluid().isPresent())
            .filter(holder -> holder.value().matchesContextAndAround(input, this.level))
            .toList();
        return this.pickByPriority(matches, holder -> holder.value().getPriority());
    }

    /** 让主动配方的结果被替换配方改写；没有命中时返回主动配方自身。 */
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

    /** 按配方的概率产出方块；上方是方块实体时跳过，避免覆盖排水口之类的机器。 */
    private void produce(AbstractMineralFountainRecipe recipe, ServerLevel serverLevel) {
        if (this.level == null) return;
        this.resetTickCount();
        if (this.level.getBlockEntity(this.getBlockPos().above()) != null) return;
        ChanceBlockState result = recipe.getToBlock();
        if (this.level.getRandom().nextDouble() <= recipe.chance(serverLevel)) {
            this.level.setBlockAndUpdate(this.getBlockPos().above(), result.state());
        }
    }

    /** 取当前应生效的加热定义：只看维度、高度与周围方块，与上方方块无关。 */
    @Nullable
    private MineralFountainHeat pickHeat(MineralFountainRecipeInput input) {
        RecipeHolder<MineralFountainActiveRecipe> active = this.pickActive(input, false);
        if (active != null && active.value().getHeat().isPresent()) {
            return active.value().getHeat().get();
        }
        return null;
    }

    /** 登记加热定义；与当前相同时跳过，避免每 tick 重复增删。 */
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
     * 取优先级最高的匹配主动配方。
     *
     * @param includeFrom 是否把材料方块计入匹配；加热与滴液查询传 {@code false}
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

    /** 取优先级最高的匹配被动配方。 */
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

    /** 非矿物涌泉配方视为等权。 */
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

    /** 四个水平相邻方块，交由配方需求表判定。 */
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
