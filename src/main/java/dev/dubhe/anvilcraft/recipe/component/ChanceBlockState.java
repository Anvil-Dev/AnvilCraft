package dev.dubhe.anvilcraft.recipe.component;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.injection.block.state.IBlockStateExtension;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.SetBlock;
import dev.dubhe.anvilcraft.util.CodecUtil;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 表示一个带有概率的方块状态
 * <p>
 * 该类用于定义在配方中可能出现的方块结果，包含方块状态和出现概率
 * </p>
 */
@Getter
public class ChanceBlockState {
    /**
     * 方块状态
     */
    private final BlockState state;

    /**
     * 方块的 NBT 数据
     */
    private final CompoundTag nbt;

    /**
     * 出现概率
     */
    private final NumberProvider chance;

    /**
     * 构造一个带有概率的方块状态
     *
     * @param state  方块状态
     * @param nbt    方块的 NBT 数据
     * @param chance 出现概率
     */
    public ChanceBlockState(BlockState state, CompoundTag nbt, NumberProvider chance) {
        this.state = state;
        this.nbt = nbt;
        this.chance = chance;
    }

    /**
     * 构造一个带有固定概率的方块状态
     *
     * @param state  方块状态
     * @param chance 出现概率（固定值）
     */
    public ChanceBlockState(BlockState state, NumberProvider chance) {
        this(state, new CompoundTag(), chance);
    }

    /**
     * 构造一个带有固定概率的方块状态
     *
     * @param state  方块状态
     * @param chance 出现概率（固定值）
     */
    public ChanceBlockState(BlockState state, float chance) {
        this(state, ConstantValue.exactly(chance));
    }

    public static @NotNull ChanceBlockState of(@NotNull Supplier<? extends Block> block, CompoundTag nbt) {
        return new ChanceBlockState(block.get().defaultBlockState(), nbt, ConstantValue.exactly(1.0f));
    }

    public static @NotNull ChanceBlockState of(@NotNull Supplier<? extends Block> block) {
        return ChanceBlockState.of(block, new CompoundTag());
    }

    /**
     * ChanceBlockState的编解码器
     */
    public static final MapCodec<ChanceBlockState> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            IBlockStateExtension.MAP_CODEC
                .forGetter(ChanceBlockState::getState),
            CompoundTag.CODEC
                .optionalFieldOf("nbt", new CompoundTag())
                .forGetter(ChanceBlockState::getNbt),
            CodecUtil.NUMBER_PROVIDER_CODEC
                .optionalFieldOf("chance", ConstantValue.exactly(1.0f))
                .forGetter(ChanceBlockState::getChance)
        ).apply(instance, ChanceBlockState::new));

    /**
     * ChanceBlockState的网络流编解码器
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, ChanceBlockState> STREAM_CODEC = StreamCodec.composite(
        BlockState.STREAM_CODEC,
        ChanceBlockState::getState,
        ByteBufCodecs.COMPOUND_TAG,
        ChanceBlockState::getNbt,
        RecipeUtil.NUMBER_PROVIDER_STREAM_CODEC,
        ChanceBlockState::getChance,
        ChanceBlockState::new
    );

    /**
     * 将此ChanceBlockState转换为SetBlock结果
     *
     * @param offset 偏移量
     * @return SetBlock结果
     */
    public SetBlock toSetBlock(Vec3 offset) {
        return SetBlock.builder().block(this.getState()).offset(offset).nbt(this.nbt).chance(this.chance).build();
    }

    public Map.Entry<BlockState, CompoundTag> getResult(ServerLevel level) {
        LootContext context = new LootContext.Builder(new LootParams(level, Map.of(), Map.of(), 0)).create(Optional.empty());
        if (level.getRandom().nextFloat() > this.chance.getFloat(context)) return null;
        return Map.entry(this.state, this.nbt);
    }
}