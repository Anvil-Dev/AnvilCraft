package dev.dubhe.anvilcraft.recipe.anvil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeSerializers;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.conditions.NotCondition;
import net.neoforged.neoforge.common.conditions.TagEmptyCondition;

/**
 * 质量注入配方类，用于定义向物品注入质量的配方
 * 该类继承自 SingleItemRecipe，表示一种特殊的单物品配方
 */
@Getter
public class MassInjectRecipe extends SingleItemRecipe {
    public static final RecipeSerializer<MassInjectRecipe> SERIALIZER = new RecipeSerializer<>(
        RecordCodecBuilder.mapCodec(inst -> inst.group(
            Ingredient.CODEC
                .fieldOf("ingredient")
                .forGetter(MassInjectRecipe::getIngredient),
            Codec.INT
                .fieldOf("mass")
                .forGetter(MassInjectRecipe::getMass)
        ).apply(inst, MassInjectRecipe::new)),
        StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC,
            MassInjectRecipe::getIngredient,
            ByteBufCodecs.VAR_INT,
            MassInjectRecipe::getMass,
            MassInjectRecipe::new
        )
    );

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
        super(
            new CommonInfo(false),
            ingredient,
            new ItemStackTemplate(Items.AIR)
        );
        this.mass = mass;
    }

    /**
     * 创建一个新的配方构建器
     *
     * @return 配方构建器实例
     */
    public static Builder builder(HolderGetter<Item> items) {
        return new Builder(items);
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
     *
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
        return this.input();
    }

    @Override
    public String group() {
        return "mass_inject";
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    /**
     * 获取配方类型
     *
     * @return 配方类型
     */
    @Override
    public RecipeType<MassInjectRecipe> getType() {
        return ModRecipeTypes.MASS_INJECT.get();
    }

    /**
     * 获取配方序列化器
     *
     * @return 配方序列化器
     */
    @Override
    public RecipeSerializer<MassInjectRecipe> getSerializer() {
        return SERIALIZER;
    }

    /**
     * 判断配方是否匹配给定的输入和世界
     *
     * @param input 配方输入
     * @param level 世界
     *
     * @return 是否匹配
     */
    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.input().test(input.item());
    }

    /**
     * 组装配方结果
     *
     * @param input      配方输入
     *
     * @return 配方结果物品堆
     */
    @Override
    public ItemStack assemble(SingleRecipeInput input) {
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
     * 质量注入配方构建器类
     */
    @Accessors(fluent = true, chain = true)
    public static class Builder extends AbstractRecipeBuilder<MassInjectRecipe> {
        /// 物品持有者获取器
        private final HolderGetter<Item> items;

        /// 配方原料
        private Ingredient ingredient = null;

        /// 质量值
        private int mass = 1;

        /// 默认ID
        private String defaultId = null;

        /// 标签条件
        private TagKey<Item> tagCondition = null;

        public Builder(HolderGetter<Item> items) {
            this.items = items;
        }

        /**
         * 设置配方原料
         *
         * @param ingredient 配方原料
         *
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
         *
         * @return 构建器实例
         */
        public Builder requires(ItemLike item) {
            this.defaultId = BuiltInRegistries.ITEM.getKey(item.asItem()).toString().replace(':', '_');
            return this.requires(Ingredient.of(item));
        }

        /**
         * 设置配方原料标签
         *
         * @param tag 物品标签
         *
         * @return 构建器实例
         */
        public Builder requires(TagKey<Item> tag) {
            this.defaultId = tag.location().toString().replace(':', '_');
            this.tagCondition = tag;
            return this.requires(Ingredient.of(this.items.getOrThrow(tag)));
        }

        /**
         * 设置质量值
         *
         * @param mass 质量值
         *
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
         * @param id 配方ID
         */
        @Override
        public void validate(Identifier id) {
            if (this.ingredient == null) {
                throw new IllegalArgumentException("Recipe ingredient must not be null, RecipeId: " + id);
            }
            if (this.mass <= 0) {
                throw new IllegalArgumentException("Mass value must be non-negative, RecipeId: " + id
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
        public ItemStackTemplate getResult() {
            return new ItemStackTemplate(Items.AIR);
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
         * @param output 配方输出
         * @param id     配方ID
         */
        @Override
        public void save(RecipeOutput output, Identifier id) {
            if (this.tagCondition != null) {
                output = output.withConditions(new NotCondition(new TagEmptyCondition<>(this.tagCondition)));
            }
            super.save(output, id);
        }
    }
}
