package dev.dubhe.anvilcraft.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.codec.StreamEncoder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CodecUtil {
    public static <T> Codec<Optional<T>> createOptionalCodec(Codec<T> elementCodec) {
        return RecordCodecBuilder.create(ins -> ins.group(
                Codec.BOOL.fieldOf("isPresent").forGetter(Optional::isPresent),
                elementCodec.optionalFieldOf("content").forGetter(o -> o))
            .apply(ins, (isPresent, content) -> isPresent && content.isPresent() ? content : Optional.empty()));
    }

    public static MapCodec<NonNullList<Ingredient>> createIngredientListCodec(
        String fieldName, int size, String recipeType) {
        return Ingredient.CODEC_NONEMPTY
            .listOf(1, size)
            .fieldOf(fieldName)
            .flatXmap(
                i -> {
                    Ingredient[] ingredients = i.toArray(Ingredient[]::new);
                    if (ingredients.length == 0) {
                        return DataResult.error(() -> "No ingredients for %s recipe".formatted(recipeType));
                    } else {
                        return ingredients.length > size
                            ? DataResult.error(
                            () -> "Too many ingredients for %s recipe. The maximum is: %d"
                                .formatted(recipeType, size))
                            : DataResult.success(NonNullList.of(Ingredient.EMPTY, ingredients));
                    }
                },
                DataResult::success);
    }

    public static final Codec<Item> ITEM_CODEC = Codec.STRING.flatXmap(
        s -> {
            try {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(s));
                if (item == Items.AIR) {
                    return DataResult.error(() -> "failed parse item key: " + s);
                } else {
                    return DataResult.success(item);
                }
            } catch (Exception e) {
                return DataResult.error(e::getMessage);
            }
        },
        i -> {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(i);
            if (key.equals(ResourceLocation.parse("air"))) {
                return DataResult.error(() -> "failed parse item: " + i);
            } else {
                return DataResult.success(key.toString());
            }
        });

    public static final StreamCodec<RegistryFriendlyByteBuf, Item> ITEM_STREAM_CODEC = StreamCodec.of(
        (buf, item) -> buf.writeUtf(BuiltInRegistries.ITEM.getKey(item).toString()),
        buf -> BuiltInRegistries.ITEM.get(ResourceLocation.parse(buf.readUtf())));

    public static final Codec<Block> BLOCK_CODEC = Codec.STRING.flatXmap(
        s -> {
            try {
                Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(s));
                if (block == Blocks.AIR) {
                    return DataResult.error(() -> "failed parse block key: " + s);
                } else {
                    return DataResult.success(block);
                }
            } catch (Exception e) {
                return DataResult.error(e::getMessage);
            }
        },
        b -> {
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(b);
            if (key.equals(ResourceLocation.parse("air"))) {
                return DataResult.error(() -> "failed parse block: " + b);
            } else {
                return DataResult.success(key.toString());
            }
        });

    public static final StreamCodec<RegistryFriendlyByteBuf, Block> BLOCK_STREAM_CODEC = StreamCodec.of(
        (buf, block) -> buf.writeUtf(BuiltInRegistries.BLOCK.getKey(block).toString()),
        buf -> BuiltInRegistries.BLOCK.get(ResourceLocation.parse(buf.readUtf())));

    public static final Codec<EntityType<?>> ENTITY_CODEC = ResourceLocation.CODEC.flatXmap(
        id -> {
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                return DataResult.error(() ->
                    "Could not find entity type " + id + " as it does not exist in ENTITY_TYPE registry.");
            }
            EntityType<?> e = BuiltInRegistries.ENTITY_TYPE.get(id);
            return DataResult.success(e);
        },
        b -> {
            ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(b);
            if (!BuiltInRegistries.ENTITY_TYPE.containsValue(b)) {
                return DataResult.error(() -> "Could not find key of entity type " + key
                    + " as it does not exist in ENTITY_TYPE registry.");
            } else {
                return DataResult.success(key);
            }
        });

    public static final StreamCodec<RegistryFriendlyByteBuf, EntityType<?>> ENTITY_STREAM_CODEC = StreamCodec.of(
        (buf, e) -> buf.writeResourceLocation(BuiltInRegistries.ENTITY_TYPE.getKey(e)),
        buf -> BuiltInRegistries.ENTITY_TYPE.get(buf.readResourceLocation()));

    public static final Codec<Character> CHAR_CODEC =
        Codec.STRING.flatXmap(s -> DataResult.success(s.charAt(0)), c -> DataResult.success(c.toString()));

    public static final StreamCodec<RegistryFriendlyByteBuf, Character> CHAR_STREAM_CODEC =
        StreamCodec.of((buf, character) -> buf.writeUtf(character.toString()), buf -> buf.readUtf()
            .charAt(0));

    public static <T> StreamCodec<? super FriendlyByteBuf, T> nbtWrapped(Codec<T> codec) {

        return new StreamCodec<FriendlyByteBuf, T>() {
            @Override
            @ParametersAreNonnullByDefault
            @NotNull
            public T decode(FriendlyByteBuf buffer) {
                return codec.decode(NbtOps.INSTANCE, buffer.readNbt()).getOrThrow().getFirst();
            }

            @Override
            @ParametersAreNonnullByDefault
            @MethodsReturnNonnullByDefault
            public void encode(FriendlyByteBuf buffer, T value) {
                buffer.writeNbt(codec.encodeStart(NbtOps.INSTANCE, value).getOrThrow());
            }
        };
    }

    public static final StreamCodec<? super ByteBuf, BlockState> BLOCK_STATE_STREAM_CODEC =
        StreamCodec.of(
            (buf, blockState) -> buf.writeInt(Block.getId(blockState)),
            (buf) -> Block.stateById(buf.readInt())
        );
}
