package dev.dubhe.anvilcraft.recipe.anvil.wrap.components;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.injection.block.state.IBlockStateExtension;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.SetBlock;
import dev.dubhe.anvilcraft.util.CodecUtil;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.phys.Vec3;

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
     * 出现概率
     */
    private final NumberProvider chance;

    /**
     * 构造一个带有概率的方块状态
     *
     * @param state  方块状态
     * @param chance 出现概率
     */
    public ChanceBlockState(BlockState state, NumberProvider chance) {
        this.state = state;
        this.chance = chance;
    }

    /**
     * 构造一个带有固定概率的方块状态
     *
     * @param state  方块状态
     * @param chance 出现概率（固定值）
     */
    public ChanceBlockState(BlockState state, float chance) {
        this.state = state;
        this.chance = ConstantValue.exactly(chance);
    }

    /**
     * ChanceBlockState的编解码器
     */
    public static final MapCodec<ChanceBlockState> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            IBlockStateExtension.MAP_CODEC
                .forGetter(ChanceBlockState::getState),
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
        return SetBlock.builder().block(this.getState()).offset(offset).chance(this.chance).build();
    }
}