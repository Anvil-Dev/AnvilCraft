package dev.dubhe.anvilcraft.recipe.anvil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.conditions.NotCondition;
import net.neoforged.neoforge.common.conditions.TagEmptyCondition;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * 质量注入配方类，用于定义向物品注入质量的配方
 * 该类继承自 SingleItemRecipe，表示一种特殊的单物品配方
 */
@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MassInjectRecipe extends SingleItemRecipe {
    /**
     * 质量值
     */
    private final int mass;

    /**
     * 构造一个新的质量注入配方
     *
     * @param ingredient 配方原料
     * @param mass       质量值
     */
    public MassInjectRecipe(Ingredient ingredient, int mass) {
        super(ModRecipeTypes.MASS_INJECT_TYPE.get(),
            ModRecipeTypes.MASS_INJECT_SERIALIZER.get(),
            "mass_inject",
            ingredient,
            ItemStack.EMPTY);
        this.mass = mass;
    }

    /**
     * 创建一个新的配方构建器
     *
     * @return 配方构建器实例
     */
    @Contract(" -> new")
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 显示质量值的组件
     *
     * @return 显示质量值的组件
     */
    public Component displayMassValue() {
        return displayMassValue(this.mass);
    }

    /**
     * 显示指定质量值的组件
     *
     * @param mass 质量值
     * @return 显示质量值的组件
     */
    public static Component displayMassValue(long mass) {
        if (mass <= 0) return Component.literal("0");
        if (mass % 100 == 0) return Component.literal(String.valueOf(mass / 100));
        if (mass % 10 == 0) return Component.literal(String.valueOf(mass / 100) + '.' + (mass % 100) / 10);
        long rem = mass % 100;
        return Component.literal((mass / 100) + (rem < 10 ? ".0" : ".") + (mass % 100));
    }

    /**
     * 获取配方原料
     *
     * @return 配方原料
     */
    public Ingredient getIngredient() {
        return this.ingredient;
    }

    /**
     * 获取配方类型
     *
     * @return 配方类型
     */
    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.MASS_INJECT_TYPE.get();
    }

    /**
     * 获取配方序列化器
     *
     * @return 配方序列化器
     */
    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.MASS_INJECT_SERIALIZER.get();
    }

    /**
     * 判断配方是否匹配给定的输入和世界
     *
     * @param input 配方输入
     * @param level 世界
     * @return 是否匹配
     */
    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.ingredient.test(input.item());
    }

    /**
     * 获取配方结果物品堆
     *
     * @param pRegistries 注册表提供器
     * @return 配方结果物品堆
     */
    @Override
    public ItemStack getResultItem(HolderLookup.Provider pRegistries) {
        return ItemStack.EMPTY;
    }

    /**
     * 组装配方结果
     *
     * @param pInput      配方输入
     * @param pRegistries 注册表提供器
     * @return 配方结果物品堆
     */
    @Override
    public ItemStack assemble(SingleRecipeInput pInput, HolderLookup.Provider pRegistries) {
        return ItemStack.EMPTY;
    }

    /**
     * 判断是否为特殊配方
     *
     * @return 是否为特殊配方
     */
    @Override
    public boolean isSpecial() {
        return true;
    }

    /**
     * 质量注入配方序列化器类
     */
    public static class Serializer implements RecipeSerializer<MassInjectRecipe> {
        /**
         * Map编解码器
         */
        public static final MapCodec<MassInjectRecipe> CODEC =
            RecordCodecBuilder.mapCodec(
                inst -> inst.group(
                        Ingredient.CODEC_NONEMPTY.fieldOf("ingredient")
                            .forGetter(m -> m.ingredient),
                        Codec.INT.fieldOf("mass").forGetter(MassInjectRecipe::getMass)
                    )
                    .apply(inst, MassInjectRecipe::new)
            );

        /**
         * 流编解码器
         */
        public static final StreamCodec<RegistryFriendlyByteBuf, MassInjectRecipe> STREAM_CODEC =
            StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC,
                m -> m.ingredient,
                ByteBufCodecs.VAR_INT,
                MassInjectRecipe::getMass,
                MassInjectRecipe::new
            );

        /**
         * 获取Map编解码器
         *
         * @return Map编解码器
         */
        @Override
        public MapCodec<MassInjectRecipe> codec() {
            return CODEC;
        }

        /**
         * 获取流编解码器
         *
         * @return 流编解码器
         */
        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MassInjectRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

    /**
     * 质量注入配方构建器类
     */
    @MethodsReturnNonnullByDefault
    @Accessors(fluent = true, chain = true)
    public static class Builder extends AbstractRecipeBuilder<MassInjectRecipe> {
        /**
         * 配方原料
         */
        private Ingredient ingredient = null;

        /**
         * 质量值
         */
        private int mass = 1;

        /**
         * 默认ID
         */
        private String defaultId = null;

        /**
         * 标签条件
         */
        private TagKey<Item> tagCondition = null;

        /**
         * 设置配方原料
         *
         * @param ingredient 配方原料
         * @return 构建器实例
         */
        public Builder requires(Ingredient ingredient) {
            this.ingredient = ingredient;
            return this;
        }

        /**
         * 设置配方原料
         *
         * @param item 物品
         * @return 构建器实例
         */
        public Builder requires(ItemLike item) {
            this.defaultId = BuiltInRegistries.ITEM.getKey(item.asItem()).toString().replace(':', '_');
            return requires(Ingredient.of(item));
        }

        /**
         * 设置配方原料标签
         *
         * @param tag 物品标签
         * @return 构建器实例
         */
        public Builder requires(TagKey<Item> tag) {
            this.defaultId = tag.location().toString().replace(':', '_');
            this.tagCondition = tag;
            return requires(Ingredient.of(tag));
        }

        /**
         * 设置质量值
         *
         * @param mass 质量值
         * @return 构建器实例
         */
        public Builder mass(int mass) {
            this.mass = mass;
            return this;
        }

        /**
         * 构建配方
         *
         * @return 质量注入配方实例
         */
        @Override
        public MassInjectRecipe buildRecipe() {
            return new MassInjectRecipe(this.ingredient, this.mass);
        }

        /**
         * 验证配方参数
         *
         * @param pId 配方ID
         */
        @Override
        public void validate(ResourceLocation pId) {
            if (this.ingredient == null) {
                throw new IllegalArgumentException("Recipe ingredient must not be null, RecipeId: " + pId);
            }
            if (this.mass <= 0) {
                throw new IllegalArgumentException("Mass value must be non-negative, RecipeId: " + pId
                    + "value: " + this.mass);
            }
        }

        /**
         * 获取配方类型
         *
         * @return 配方类型
         */
        @Override
        public String getType() {
            return "mass_inject";
        }

        /**
         * 获取配方结果物品
         *
         * @return 配方结果物品
         */
        @Override
        public Item getResult() {
            return Items.AIR;
        }

        /**
         * 保存配方
         *
         * @param recipeOutput 配方输出
         */
        @Override
        public void save(RecipeOutput recipeOutput) {
            if (this.defaultId == null) this.defaultId = Integer.toHexString(this.hashCode());
            this.save(recipeOutput, AnvilCraft.of("mass_inject/" + this.defaultId));
        }

        /**
         * 保存配方到指定位置
         *
         * @param recipeOutput 配方输出
         * @param id           配方ID
         */
        @Override
        public void save(RecipeOutput recipeOutput, ResourceLocation id) {
            if (this.tagCondition != null) {
                recipeOutput = recipeOutput.withConditions(new NotCondition(new TagEmptyCondition(this.tagCondition)));
            }
            super.save(recipeOutput, id);
        }
    }
}