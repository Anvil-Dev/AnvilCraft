package dev.dubhe.anvilcraft.util;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Function7;
import com.mojang.datafixers.util.Function8;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModRegistries;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipeOutcome;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipeTrigger;
import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CodecUtil {
    public static final Codec<IRecipeTrigger> TRIGGER_CODEC = ModRegistries.TRIGGER_REGISTRY
        .byNameCodec();

    public static final Codec<IRecipePredicate<?>> PREDICATE_CODEC = ModRegistries.PREDICATE_TYPE_REGISTRY
        .byNameCodec()
        .dispatch(
            IRecipePredicate::getType,
            type -> Objects.requireNonNull(ModRegistries.PREDICATE_TYPE_REGISTRY.get(type.getId())).codec()
        );

    public static final Codec<IRecipeOutcome<?>> OUTCOME_CODEC = ModRegistries.OUTCOME_TYPE_REGISTRY
        .byNameCodec()
        .dispatch(
            IRecipeOutcome::getType,
            type -> Objects.requireNonNull(ModRegistries.OUTCOME_TYPE_REGISTRY.get(type.getId())).codec()
        );

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

        return new StreamCodec<>() {
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

    public static <T extends Enum<T>> Codec<T> enumCodecInInt(Class<T> clazz) {
        return Codec.INT.xmap(index -> clazz.getEnumConstants()[index], Enum::ordinal);
    }

    public static <T extends Enum<T>> Codec<T> enumCodecInLowerName(Class<T> clazz) {
        return Codec.STRING.xmap(
            name -> Enum.valueOf(clazz, name.toUpperCase(Locale.ROOT)),
            value -> value.name().toLowerCase(Locale.ROOT));
    }

    public static <B extends ByteBuf, T extends Enum<T>> StreamCodec<B, T> enumStreamCodec(Class<T> clazz) {
        return ByteBufCodecs.VAR_INT.<B>cast().map(index -> clazz.getEnumConstants()[index], Enum::ordinal);
    }

    public static <B, F, S> StreamCodec<B, Pair<F, S>> createPairStreamCodec(StreamCodec<? super B, F> first, StreamCodec<? super B, S> second) {
        return StreamCodec.composite(first, Pair::getFirst, second, Pair::getSecond, Pair::new);
    }

    public static <T> Codec<LinkedList<T>> linkedListOf(Codec<T> codec) {
        return dequeOf(codec, LinkedList::new);
    }

    public static <T, D extends Deque<T>> Codec<D> dequeOf(Codec<T> codec, Function<? super List<T>, ? extends D> dequeFac) {
        return codec.listOf().xmap(dequeFac, List::copyOf);
    }

    public static final StreamCodec<ByteBuf, Vec3i> VEC3I_STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull Vec3i decode(ByteBuf buffer) {
            long packedPos = buffer.readLong();
            return new Vec3i(BlockPos.getX(packedPos), BlockPos.getY(packedPos), BlockPos.getZ(packedPos));
        }

        @Override
        public void encode(ByteBuf buffer, Vec3i value) {
            buffer.writeLong(BlockPos.asLong(value.getX(), value.getY(), value.getZ()));
        }
    };

    public static final Codec<NumberProvider> NUMBER_PROVIDER_CODEC = Codec.either(
        Codec.INT.xmap(ConstantValue::new, value -> Math.round(value.value())),
        NumberProviders.CODEC
    ).xmap(Either::unwrap, provider -> {
        if (!(provider instanceof ConstantValue(float value)) || value - Math.floor(value) >= 1E-5) {
            return Either.right(provider);
        }
        return Either.left((ConstantValue) provider);
    });

    public static <B, C, T1, T2, T3, T4, T5, T6, T7> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> codec1,
        final Function<C, T1> getter1,
        final StreamCodec<? super B, T2> codec2,
        final Function<C, T2> getter2,
        final StreamCodec<? super B, T3> codec3,
        final Function<C, T3> getter3,
        final StreamCodec<? super B, T4> codec4,
        final Function<C, T4> getter4,
        final StreamCodec<? super B, T5> codec5,
        final Function<C, T5> getter5,
        final StreamCodec<? super B, T6> codec6,
        final Function<C, T6> getter6,
        final StreamCodec<? super B, T7> codec7,
        final Function<C, T7> getter7,
        final Function7<T1, T2, T3, T4, T5, T6, T7, C> factory
    ) {
        return new StreamCodec<>() {
            @Override
            public @NotNull C decode(@NotNull B buffer) {
                T1 t1 = codec1.decode(buffer);
                T2 t2 = codec2.decode(buffer);
                T3 t3 = codec3.decode(buffer);
                T4 t4 = codec4.decode(buffer);
                T5 t5 = codec5.decode(buffer);
                T6 t6 = codec6.decode(buffer);
                T7 t7 = codec7.decode(buffer);
                return factory.apply(t1, t2, t3, t4, t5, t6, t7);
            }

            @Override
            public void encode(@NotNull B buffer, @NotNull C value) {
                codec1.encode(buffer, getter1.apply(value));
                codec2.encode(buffer, getter2.apply(value));
                codec3.encode(buffer, getter3.apply(value));
                codec4.encode(buffer, getter4.apply(value));
                codec5.encode(buffer, getter5.apply(value));
                codec6.encode(buffer, getter6.apply(value));
                codec7.encode(buffer, getter7.apply(value));
            }
        };
    }

    public static <B, C, T1, T2, T3, T4, T5, T6, T7, T8> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> codec1,
        final Function<C, T1> getter1,
        final StreamCodec<? super B, T2> codec2,
        final Function<C, T2> getter2,
        final StreamCodec<? super B, T3> codec3,
        final Function<C, T3> getter3,
        final StreamCodec<? super B, T4> codec4,
        final Function<C, T4> getter4,
        final StreamCodec<? super B, T5> codec5,
        final Function<C, T5> getter5,
        final StreamCodec<? super B, T6> codec6,
        final Function<C, T6> getter6,
        final StreamCodec<? super B, T7> codec7,
        final Function<C, T7> getter7,
        final StreamCodec<? super B, T8> codec8,
        final Function<C, T8> getter8,
        final Function8<T1, T2, T3, T4, T5, T6, T7, T8, C> factory
    ) {
        return new StreamCodec<>() {
            @Override
            public @NotNull C decode(@NotNull B buffer) {
                T1 t1 = codec1.decode(buffer);
                T2 t2 = codec2.decode(buffer);
                T3 t3 = codec3.decode(buffer);
                T4 t4 = codec4.decode(buffer);
                T5 t5 = codec5.decode(buffer);
                T6 t6 = codec6.decode(buffer);
                T7 t7 = codec7.decode(buffer);
                T8 t8 = codec8.decode(buffer);
                return factory.apply(t1, t2, t3, t4, t5, t6, t7, t8);
            }

            @Override
            public void encode(@NotNull B buffer, @NotNull C value) {
                codec1.encode(buffer, getter1.apply(value));
                codec2.encode(buffer, getter2.apply(value));
                codec3.encode(buffer, getter3.apply(value));
                codec4.encode(buffer, getter4.apply(value));
                codec5.encode(buffer, getter5.apply(value));
                codec6.encode(buffer, getter6.apply(value));
                codec7.encode(buffer, getter7.apply(value));
                codec8.encode(buffer, getter8.apply(value));
            }
        };
    }
}