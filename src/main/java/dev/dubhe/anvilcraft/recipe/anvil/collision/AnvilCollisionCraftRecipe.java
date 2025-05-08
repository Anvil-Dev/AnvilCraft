package dev.dubhe.anvilcraft.recipe.anvil.collision;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.input.IItemsInput;
import dev.dubhe.anvilcraft.recipe.elements.InputBlock;
import dev.dubhe.anvilcraft.recipe.elements.OutputBlock;
import dev.dubhe.anvilcraft.recipe.elements.OutputItem;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record AnvilCollisionCraftRecipe(
    InputBlock anvil,
    boolean consume,
    InputBlock hitBlock,
    List<BlockTransform> transformBlocks,
    List<OutputItem> outputItems
) implements Recipe<AnvilCollisionCraftRecipe.Input> {

    @Override
    public boolean matches(Input input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider provider) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int i, int i1) {
        return false;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        if (anvil.getBlock() == null) {
            return Blocks.ANVIL.asItem().getDefaultInstance();
        }
        return anvil.getBlock().asItem().getDefaultInstance();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.ANVIL_COLLISION_CRAFT_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.ANVIL_COLLISION_CRAFT.get();
    }

    public record Input(ItemStack source, List<ItemStack> items) implements RecipeInput, IItemsInput {

        @Override
        public ItemStack getItem(int index) {
            return items.get(index);
        }

        @Override
        public int size() {
            return items.size();
        }
    }

    public static class Serializer implements RecipeSerializer<AnvilCollisionCraftRecipe> {

        private static final MapCodec<AnvilCollisionCraftRecipe> CODEC = RecordCodecBuilder.mapCodec(it -> it.group(
            InputBlock.CODEC.fieldOf("anvil").forGetter(AnvilCollisionCraftRecipe::anvil),
            Codec.BOOL.fieldOf("consume").forGetter(AnvilCollisionCraftRecipe::consume),
            InputBlock.CODEC.fieldOf("hitBlock").forGetter(AnvilCollisionCraftRecipe::hitBlock),
            BlockTransform.CODEC.listOf().fieldOf("transform_blocks").forGetter(AnvilCollisionCraftRecipe::transformBlocks),
            OutputItem.CODEC.listOf().fieldOf("output_items").forGetter(AnvilCollisionCraftRecipe::outputItems)
        ).apply(it, AnvilCollisionCraftRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, AnvilCollisionCraftRecipe> STREAM_CODEC = StreamCodec.of(
            AnvilCollisionCraftRecipe.Serializer::encode, AnvilCollisionCraftRecipe.Serializer::decode
        );

        @Override
        public MapCodec<AnvilCollisionCraftRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AnvilCollisionCraftRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static void encode(RegistryFriendlyByteBuf buf, AnvilCollisionCraftRecipe recipe) {
            InputBlock.STREAM_CODEC.encode(buf, recipe.anvil);
            buf.writeBoolean(recipe.consume);
            InputBlock.STREAM_CODEC.encode(buf, recipe.hitBlock);
            writeList(buf, recipe.transformBlocks, BlockTransform.STREAM_CODEC);
            writeList(buf, recipe.outputItems, OutputItem.STREAM_CODEC);
        }

        private static AnvilCollisionCraftRecipe decode(RegistryFriendlyByteBuf buf) {
            return new AnvilCollisionCraftRecipe(
                InputBlock.STREAM_CODEC.decode(buf),
                buf.readBoolean(),
                InputBlock.STREAM_CODEC.decode(buf),
                readList(buf, BlockTransform.STREAM_CODEC),
                readList(buf, OutputItem.STREAM_CODEC)
            );
        }

        private static <T> void writeList(RegistryFriendlyByteBuf buf, List<T> list, StreamCodec<RegistryFriendlyByteBuf, T> steamCodec) {
            buf.writeVarInt(list.size());
            for (T t : list) {
                steamCodec.encode(buf, t);
            }
        }

        private static <T> List<T> readList(RegistryFriendlyByteBuf buf, StreamCodec<RegistryFriendlyByteBuf, T> steamCodec) {
            ArrayList<T> list = new ArrayList<>();
            int size = buf.readVarInt();
            for (int i = 0; i < size; i++) {
                list.add(steamCodec.decode(buf));
            }
            return list;
        }
    }

    @SuppressWarnings("unused")
    @Accessors(fluent = true, chain = true)
    public static class Builder extends AbstractRecipeBuilder<AnvilCollisionCraftRecipe> {
        private InputBlock anvil;
        private boolean consume = true;
        private InputBlock hitBlock;
        @Setter
        private List<BlockTransform> transformBlocks = new ArrayList<>();
        @Setter
        private List<OutputItem> outputItems = new ArrayList<>();

        public Builder anvil(Block anvil) {
            this.anvil = new InputBlock(anvil, Map.of());
            return this;
        }

        public Builder anvil(TagKey<Block> anvil) {
            this.anvil = new InputBlock(anvil);
            return this;
        }

        public Builder consume(boolean consume) {
            this.consume = consume;
            return this;
        }

        public Builder hitBlock(InputBlock hitBlock) {
            this.hitBlock = hitBlock;
            return this;
        }

        public Builder hitBlock(TagKey<Block> blockTagKey) {
            return hitBlock(new InputBlock(blockTagKey));
        }

        public Builder hitBlock(Block block, Map<String, String> states) {
            return hitBlock(new InputBlock(block, states));
        }

        public Builder hitBlock(Block block) {
            return hitBlock(new InputBlock(block, Map.of()));
        }

        public Builder transformBlock(
            InputBlock inputBlock,
            OutputBlock outputBlock,
            float chance,
            int maxCount
        ) {
            this.transformBlocks.add(new BlockTransform(inputBlock, outputBlock, chance, maxCount));
            return this;
        }

        public Builder transformBlock(
            InputBlock inputBlock,
            OutputBlock outputBlock,
            int maxCount
        ) {
            return transformBlock(inputBlock, outputBlock, 1f, maxCount);
        }

        public Builder outputItem(OutputItem outputItem) {
            this.outputItems.add(outputItem);
            return this;
        }

        public Builder outputItem(Item item, int count, float chance) {
            return outputItem(new OutputItem(new ItemStack(item, count), chance));
        }

        public Builder outputItem(Item item, int count) {
            return outputItem(new OutputItem(new ItemStack(item, count), 1f));
        }

        public Builder outputItem(Item item, float chance) {
            return outputItem(new OutputItem(new ItemStack(item, 1), chance));
        }

        public Builder outputItem(Item item) {
            return outputItem(new OutputItem(new ItemStack(item, 1), 1f));
        }

        @Override
        public Item getResult() {
            if (anvil.getBlock() == null) {
                return Blocks.ANVIL.asItem();
            }
            return anvil.getBlock().asItem();
        }

        @Override
        public AnvilCollisionCraftRecipe buildRecipe() {
            return new AnvilCollisionCraftRecipe(anvil, consume, hitBlock, transformBlocks, outputItems);
        }

        @Override
        public void validate(ResourceLocation recipeId) {
            if (anvil == null) {
                throw new IllegalArgumentException("Recipe has no anvil, RecipeId:" + recipeId);
            }
            if (hitBlock == null) {
                throw new IllegalArgumentException("Recipe has no hitBlock, RecipeId:" + recipeId);
            }
        }

        @Override
        public String getType() {
            return "anvil_collision";
        }

        @Override
        public void save(RecipeOutput recipeOutput) {
            save(
                recipeOutput,
                AnvilCraft.of(this.anvil.getKey() + "_and_" + this.hitBlock.getKey())
                    .withPrefix(getType() + "/"));
        }
    }

    public static AnvilCollisionCraftRecipe.Builder builder() {
        return new AnvilCollisionCraftRecipe.Builder();
    }
}
