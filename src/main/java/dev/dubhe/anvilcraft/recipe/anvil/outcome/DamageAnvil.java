package dev.dubhe.anvilcraft.recipe.anvil.outcome;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModRecipeOutcomeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipeOutcome;
import dev.dubhe.anvilcraft.recipe.anvil.InWorldRecipeContext;
import dev.dubhe.anvilcraft.recipe.anvil.InWorldRecipeData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

/**
 * 损坏铁砧配方结果类，用于定义使铁砧损坏的配方结果
 * 该类实现了 IRecipeOutcome 接口，表示一种特殊的配方结果类型
 */
public class DamageAnvil implements IRecipeOutcome<DamageAnvil> {
    /**
     * 损坏铁砧的配方数据键
     */
    public static final InWorldRecipeData<Boolean> DAMAGE_ANVIL = InWorldRecipeData.of(AnvilCraft.of("damage_anvil"), false);

    /**
     * 获取配方结果类型
     *
     * @return 配方结果类型
     */
    @Override
    public IRecipeOutcome.Type<DamageAnvil> getType() {
        return ModRecipeOutcomeTypes.DAMAGE_ANVIL.get();
    }

    /**
     * 接受配方上下文并处理损坏铁砧的结果
     *
     * @param context 配方上下文
     */
    @Override
    public void accept(@NotNull InWorldRecipeContext context) {
        context.put(DAMAGE_ANVIL, true);
    }

    /**
     * 损坏铁砧配方结果类型类
     */
    public static class Type implements IRecipeOutcome.Type<DamageAnvil> {
        /**
         * 获取MapCodec编解码器
         *
         * @return MapCodec编解码器
         */
        @Override
        public @NotNull MapCodec<DamageAnvil> codec() {
            return MapCodec.unit(new DamageAnvil());
        }

        /**
         * 获取StreamCodec编解码器
         *
         * @return StreamCodec编解码器
         */
        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, DamageAnvil> streamCodec() {
            return StreamCodec.unit(new DamageAnvil());
        }
    }
}