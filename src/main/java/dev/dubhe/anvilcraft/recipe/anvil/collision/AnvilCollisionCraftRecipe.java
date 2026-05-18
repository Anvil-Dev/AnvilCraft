package dev.dubhe.anvilcraft.recipe.anvil.collision;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeSerializers;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.input.IItemsInput;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 铁砧碰撞工艺配方类，用于定义铁砧与方块碰撞时的工艺配方
 * 该类定义了铁砧碰撞特定方块时的输入、输出和转换规则
 *
 * @param anvil           铁砧输入方块
 * @param consume         是否消耗铁砧
 * @param hitBlock        碰撞的方块
 * @param transformBlocks 方块转换列表
 * @param outputItems     输出物品列表
 * @param speed           最低撞击速度(m/tick)
 */
public record AnvilCollisionCraftRecipe(
    BlockStatePredicate anvil,
    boolean consume,
    BlockStatePredicate hitBlock,
    List<BlockTransform> transformBlocks,
    List<ChanceItemStack> outputItems,
    int speed
) implements Recipe<AnvilCollisionCraftRecipe.Input> {
    public static final RecipeSerializer<AnvilCollisionCraftRecipe> SERIALIZER = new RecipeSerializer<>(
        RecordCodecBuilder.mapCodec(it -> it.group(
            BlockStatePredicate.CODEC
                .fieldOf("anvil")
                .forGetter(AnvilCollisionCraftRecipe::anvil),
            Codec.BOOL
                .fieldOf("consume")
                .forGetter(AnvilCollisionCraftRecipe::consume),
            BlockStatePredicate.CODEC
                .fieldOf("hitBlock")
                .forGetter(AnvilCollisionCraftRecipe::hitBlock),
            BlockTransform.CODEC
                .listOf()
                .fieldOf("transform_blocks")
                .forGetter(AnvilCollisionCraftRecipe::transformBlocks),
            ChanceItemStack.CODEC
                .listOf()
                .fieldOf("output_items")
                .forGetter(AnvilCollisionCraftRecipe::outputItems),
            Codec.INT
                .fieldOf("speed")
                .forGetter(AnvilCollisionCraftRecipe::speed)
        ).apply(it, AnvilCollisionCraftRecipe::new)),
        StreamCodec.of(AnvilCollisionCraftRecipe.Serializer::encode, AnvilCollisionCraftRecipe.Serializer::decode)
    );

    /**
     * 判断配方是否匹配给定的输入和世界
     *
     * @param input 输入
     * @param level 世界
     *
     * @return 是否匹配
     */
    @Override
    public boolean matches(Input input, Level level) {
        return false;
    }

    /**
     * 组装配方结果
     *
     * @param input 输入
     *
     * @return 配方结果物品堆
     */
    @Override
    public ItemStack assemble(Input input) {
        return ItemStack.EMPTY;
    }

    /**
     * 获取配方类型
     *
     * @return 配方类型
     */
    @Override
    public RecipeType<AnvilCollisionCraftRecipe> getType() {
        return ModRecipeTypes.ANVIL_COLLISION_CRAFT.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    /**
     * 获取配方序列化器
     *
     * @return 配方序列化器
     */
    @Override
    public RecipeSerializer<AnvilCollisionCraftRecipe> getSerializer() {
        return SERIALIZER;
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

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "anvil_collision_craft";
    }

    /**
     * 铁砧碰撞工艺配方输入记录类
     */
    public record Input(
        ItemStack source, // 源物品堆
        List<ItemStack> items // 物品堆列表
    ) implements RecipeInput, IItemsInput {

        /**
         * 获取指定索引的物品堆
         *
         * @param index 索引
         *
         * @return 物品堆
         */
        @Override
        public ItemStack getItem(int index) {
            return this.items.get(index);
        }

        /**
         * 获取物品堆列表的大小
         *
         * @return 大小
         */
        @Override
        public int size() {
            return this.items.size();
        }
    }

    /**
     * 铁砧碰撞工艺配方序列化器类
     */
    public static class Serializer {
        /**
         * 编码配方到字节缓冲区
         *
         * @param buf    字节缓冲区
         * @param recipe 配方
         */
        private static void encode(RegistryFriendlyByteBuf buf, AnvilCollisionCraftRecipe recipe) {
            BlockStatePredicate.STREAM_CODEC.encode(buf, recipe.anvil);
            buf.writeBoolean(recipe.consume);
            BlockStatePredicate.STREAM_CODEC.encode(buf, recipe.hitBlock);
            writeList(buf, recipe.transformBlocks, BlockTransform.STREAM_CODEC);
            writeList(buf, recipe.outputItems, ChanceItemStack.STREAM_CODEC);
            buf.writeVarInt(recipe.speed);
        }

        /**
         * 从字节缓冲区解码配方
         *
         * @param buf 字节缓冲区
         *
         * @return 配方
         */
        private static AnvilCollisionCraftRecipe decode(RegistryFriendlyByteBuf buf) {
            return new AnvilCollisionCraftRecipe(
                BlockStatePredicate.STREAM_CODEC.decode(buf),
                buf.readBoolean(),
                BlockStatePredicate.STREAM_CODEC.decode(buf),
                readList(buf, BlockTransform.STREAM_CODEC),
                readList(buf, ChanceItemStack.STREAM_CODEC),
                buf.readVarInt()
            );
        }

        /**
         * 将列表编码到字节缓冲区
         *
         * @param buf        字节缓冲区
         * @param list       列表
         * @param steamCodec 流编解码器
         * @param <T>        列表元素类型
         */
        private static <T> void writeList(RegistryFriendlyByteBuf buf, List<T> list, StreamCodec<RegistryFriendlyByteBuf, T> steamCodec) {
            buf.writeVarInt(list.size());
            for (T t : list) {
                steamCodec.encode(buf, t);
            }
        }

        /**
         * 从字节缓冲区解码列表
         *
         * @param buf        字节缓冲区
         * @param steamCodec 流编解码器
         * @param <T>        列表元素类型
         *
         * @return 列表
         */
        private static <T> List<T> readList(RegistryFriendlyByteBuf buf, StreamCodec<RegistryFriendlyByteBuf, T> steamCodec) {
            ArrayList<T> list = new ArrayList<>();
            int size = buf.readVarInt();
            for (int i = 0; i < size; i++) {
                list.add(steamCodec.decode(buf));
            }
            return list;
        }
    }

    /**
     * 铁砧碰撞工艺配方构建器类
     */
    @SuppressWarnings("unused")
    @Accessors(fluent = true, chain = true)
    public static class Builder extends AbstractRecipeBuilder<AnvilCollisionCraftRecipe> {
        /**
         * 铁砧输入方块
         */
        private BlockStatePredicate anvil;

        /**
         * 是否消耗铁砧
         */
        private boolean consume = true;

        /**
         * 碰撞的方块
         */
        private BlockStatePredicate hitBlock;

        /**
         * 方块转换列表
         */
        @Setter
        private List<BlockTransform> transformBlocks = new ArrayList<>();

        /**
         * 输出物品列表
         */
        @Setter
        private List<ChanceItemStack> outputItems = new ArrayList<>();
        private int speed = 32;

        /**
         * 设置铁砧方块
         *
         * @param anvil 铁砧方块
         *
         * @return 构建器实例
         */
        public Builder anvil(Block anvil) {
            this.anvil = BlockStatePredicate.builder().of(anvil).build();
            return this;
        }

        /**
         * 设置铁砧方块标签
         *
         * @param anvil 铁砧方块标签
         *
         * @return 构建器实例
         */
        public Builder anvil(TagKey<Block> anvil) {
            this.anvil = BlockStatePredicate.builder().of(anvil).build();
            return this;
        }

        /**
         * 设置是否消耗铁砧
         *
         * @param consume 是否消耗铁砧
         *
         * @return 构建器实例
         */
        public Builder consume(boolean consume) {
            this.consume = consume;
            return this;
        }

        /**
         * 设置碰撞的方块
         *
         * @param hitBlock 碰撞的方块
         *
         * @return 构建器实例
         */
        public Builder hitBlock(BlockStatePredicate hitBlock) {
            this.hitBlock = hitBlock;
            return this;
        }

        /**
         * 设置碰撞的方块标签
         *
         * @param blockTagKey 方块标签
         *
         * @return 构建器实例
         */
        public Builder hitBlock(TagKey<Block> blockTagKey) {
            return this.hitBlock(BlockStatePredicate.builder().of(blockTagKey).build());
        }

        /**
         * 设置碰撞的方块
         *
         * @param block  方块
         * @param states 方块状态
         *
         * @return 构建器实例
         */
        public Builder hitBlock(Block block, Map<String, String> states) {
            return this.hitBlock(BlockStatePredicate.builder().of(block).build());
        }

        /**
         * 设置碰撞的方块
         *
         * @param block 方块
         *
         * @return 构建器实例
         */
        public Builder hitBlock(Block block) {
            return this.hitBlock(BlockStatePredicate.builder().of(block).build());
        }

        /**
         * 添加方块转换
         *
         * @param inputBlock  输入方块
         * @param outputBlock 输出方块
         * @param maxCount    最大数量
         *
         * @return 构建器实例
         */
        public Builder transformBlock(
            BlockStatePredicate inputBlock,
            ChanceBlockState outputBlock,
            int maxCount
        ) {
            this.transformBlocks.add(new BlockTransform(inputBlock, outputBlock, maxCount));
            return this;
        }

        /**
         * 添加输出物品
         *
         * @param outputItem 输出物品
         *
         * @return 构建器实例
         */
        public Builder outputItem(ChanceItemStack outputItem) {
            this.outputItems.add(outputItem);
            return this;
        }

        /**
         * 添加输出物品
         *
         * @param item   物品
         * @param count  数量
         * @param chance 概率
         *
         * @return 构建器实例
         */
        public Builder outputItem(Item item, int count, float chance) {
            return this.outputItem(ChanceItemStack.of(new ItemStackTemplate(item, count), chance));
        }

        /**
         * 添加输出物品
         *
         * @param item  物品
         * @param count 数量
         *
         * @return 构建器实例
         */
        public Builder outputItem(Item item, int count) {
            return this.outputItem(ChanceItemStack.of(new ItemStackTemplate(item, count), 1F));
        }

        /**
         * 添加输出物品
         *
         * @param item   物品
         * @param chance 概率
         *
         * @return 构建器实例
         */
        public Builder outputItem(Item item, float chance) {
            return this.outputItem(ChanceItemStack.of(new ItemStackTemplate(item, 1), chance));
        }

        /**
         * 添加输出物品
         *
         * @param item 物品
         *
         * @return 构建器实例
         */
        public Builder outputItem(Item item) {
            return this.outputItem(ChanceItemStack.of(new ItemStackTemplate(item, 1), 1F));
        }

        public Builder speed(int speed) {
            this.speed = speed;
            return this;
        }

        /**
         * 获取配方结果物品
         *
         * @return 配方结果物品
         */
        @Override
        public ItemStackTemplate getResult() {
            if (this.anvil.getStatesCache().isEmpty()) {
                return new ItemStackTemplate(Blocks.ANVIL.asItem());
            }
            return new ItemStackTemplate(this.anvil.getStatesCache().getFirst().getBlock().asItem());
        }

        /**
         * 构建配方
         *
         * @return 铁砧碰撞工艺配方
         */
        @Override
        public AnvilCollisionCraftRecipe buildRecipe() {
            return new AnvilCollisionCraftRecipe(
                this.anvil,
                this.consume,
                this.hitBlock,
                this.transformBlocks,
                this.outputItems,
                this.speed
            );
        }

        /**
         * 验证配方参数
         *
         * @param id 配方ID
         */
        @Override
        public void validate(Identifier id) {
            if (this.anvil == null) {
                throw new IllegalArgumentException("Recipe has no anvil, RecipeId:" + id);
            }
            if (this.hitBlock == null) {
                throw new IllegalArgumentException("Recipe has no hitBlock, RecipeId:" + id);
            }
        }

        /**
         * 获取配方类型
         *
         * @return 配方类型
         */
        @Override
        public String getType() {
            return "anvil_collision";
        }

        /**
         * 保存配方
         *
         * @param recipeOutput 配方输出
         */
        @Override
        public void save(RecipeOutput recipeOutput) {
            save(
                recipeOutput,
                AnvilCraft.of(this.anvil.getKey().getPath() + "_and_" + this.hitBlock.getKey().getPath() + "_" + this.speed)
                    .withPrefix(this.getType() + "/")
            );
        }
    }

    /**
     * 创建一个新的铁砧碰撞工艺配方构建器
     *
     * @return 配方构建器
     */
    public static AnvilCollisionCraftRecipe.Builder builder() {
        return new AnvilCollisionCraftRecipe.Builder();
    }
}
