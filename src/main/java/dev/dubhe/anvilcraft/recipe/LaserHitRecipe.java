package dev.dubhe.anvilcraft.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.block.entity.heatable.HeatableBlockEntity;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.util.BlockMiningEffect;
import dev.dubhe.anvilcraft.util.BreakBlockUtil;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Getter
public class LaserHitRecipe implements Recipe<LaserHitRecipe.Input> {
    public static final MapCodec<LaserHitRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        BlockStatePredicate.CODEC.fieldOf("input").forGetter(LaserHitRecipe::getInput),
        ResourceLocation.CODEC.optionalFieldOf("dimension").forGetter(LaserHitRecipe::getDimension),
        Codec.intRange(1, Integer.MAX_VALUE).fieldOf("laser_strength").forGetter(LaserHitRecipe::getLaserStrength),
        Codec.intRange(1, Integer.MAX_VALUE).fieldOf("hit_time").forGetter(LaserHitRecipe::getHitTime),
        ItemStack.CODEC.listOf().optionalFieldOf("result_items", List.of()).forGetter(LaserHitRecipe::getResultItems),
        BlockState.CODEC.optionalFieldOf("result_block", Blocks.AIR.defaultBlockState()).forGetter(LaserHitRecipe::getResultBlock),
        Codec.BOOL.optionalFieldOf("use_block_loot", false).forGetter(LaserHitRecipe::isUseBlockLoot),
        Codec.BOOL.optionalFieldOf("requires_lens", false).forGetter(LaserHitRecipe::isRequiresLens),
        Codec.INT.optionalFieldOf("priority", 0).forGetter(LaserHitRecipe::getPriority),
        LaserType.CODEC.optionalFieldOf("laser_type", LaserType.NORMAL).forGetter(LaserHitRecipe::getLaserType),
        Codec.intRange(0, 24000).optionalFieldOf("heat_duration", 0).forGetter(LaserHitRecipe::getHeatDuration)
    ).apply(inst, LaserHitRecipe::new));
    private static final Comparator<RecipeHolder<LaserHitRecipe>> MATCH_ORDER =
        Comparator.<RecipeHolder<LaserHitRecipe>>comparingInt(holder -> holder.value().getPriority()).reversed()
            .thenComparing(Comparator.comparingInt(
                (RecipeHolder<LaserHitRecipe> holder) -> holder.value().getLaserStrength()
            ).reversed())
            .thenComparing(RecipeHolder::id);

    private final BlockStatePredicate input;
    private final Optional<ResourceLocation> dimension;
    private final int laserStrength;
    private final int hitTime;
    private final List<ItemStack> resultItems;
    private final BlockState resultBlock;
    private final boolean useBlockLoot;
    private final boolean requiresLens;
    private final int priority;
    private final LaserType laserType;
    private final int heatDuration;

    public LaserHitRecipe(
        BlockStatePredicate input, Optional<ResourceLocation> dimension, int laserStrength, int hitTime,
        List<ItemStack> resultItems, BlockState resultBlock, boolean useBlockLoot, boolean requiresLens, int priority
    ) {
        this(input, dimension, laserStrength, hitTime, resultItems, resultBlock, useBlockLoot, requiresLens, priority, LaserType.NORMAL, 0);
    }

    public LaserHitRecipe(
        BlockStatePredicate input, Optional<ResourceLocation> dimension, int laserStrength, int hitTime,
        List<ItemStack> resultItems, BlockState resultBlock, boolean useBlockLoot, boolean requiresLens, int priority,
        LaserType laserType, int heatDuration
    ) {
        if (laserStrength < 1 || hitTime < 1) throw new IllegalArgumentException("Laser strength and mining time must be positive");
        this.input = input;
        this.dimension = dimension;
        this.laserStrength = laserStrength;
        this.hitTime = hitTime;
        this.resultItems = resultItems.stream().map(ItemStack::copy).toList();
        this.resultBlock = resultBlock;
        this.useBlockLoot = useBlockLoot;
        this.requiresLens = requiresLens;
        this.priority = priority;
        this.laserType = laserType;
        this.heatDuration = heatDuration;
    }

    @Override
    public boolean matches(Input input, Level level) {
        return input.strength() >= laserStrength
            && input.gamma() == (laserType == LaserType.GAMMA)
            && (!requiresLens || input.lens())
            && dimension.map(value -> value.equals(level.dimension().location())).orElse(true)
            && this.input.test(level, input.state(), level.getBlockEntity(input.pos()));
    }

    public static Optional<RecipeHolder<LaserHitRecipe>> find(ServerLevel level, Input input) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.LASER_HIT_TYPE.get()).stream()
            .filter(holder -> holder.value().matches(input, level))
            .min(MATCH_ORDER);
    }

    public List<ItemStack> createDrops(ServerLevel level, BlockPos pos, BlockMiningEffect effect) {
        List<ItemStack> drops = new ArrayList<>();
        resultItems.forEach(stack -> drops.add(stack.copy()));
        if (useBlockLoot) drops.addAll(BreakBlockUtil.dropForLaser(level, pos, effect));
        return drops;
    }

    public void applyHeat(ServerLevel level, BlockPos pos) {
        if (heatDuration > 0 && level.getBlockEntity(pos) instanceof HeatableBlockEntity heatable) {
            heatable.addDurationInTick(heatDuration);
            HeaterManager.addHeatableBlock(pos, level);
        }
    }

    public enum LaserType implements StringRepresentable {
        NORMAL, GAMMA;

        public static final Codec<LaserType> CODEC = StringRepresentable.fromEnum(LaserType::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return getResultItem(registries);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return resultItems.isEmpty() ? ItemStack.EMPTY : resultItems.getFirst().copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.LASER_HIT_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.LASER_HIT_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public record Input(BlockPos pos, BlockState state, int strength, boolean lens, boolean gamma) implements RecipeInput {
        public Input(BlockPos pos, BlockState state, int strength, boolean lens) {
            this(pos, state, strength, lens, false);
        }

        @Override
        public ItemStack getItem(int index) {
            if (index != 0) throw new IndexOutOfBoundsException(index);
            return new ItemStack(state.getBlock());
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }

    public static class Serializer implements RecipeSerializer<LaserHitRecipe> {
        private static final StreamCodec<RegistryFriendlyByteBuf, LaserHitRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

        @Override
        public MapCodec<LaserHitRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, LaserHitRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
