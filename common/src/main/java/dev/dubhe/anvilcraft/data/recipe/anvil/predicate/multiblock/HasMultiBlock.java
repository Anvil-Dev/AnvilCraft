package dev.dubhe.anvilcraft.data.recipe.anvil.predicate.multiblock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilCraftingContext;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipePredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HasMultiBlock implements RecipePredicate {

    public static final PrimitiveCodec<Character> CHAR = new PrimitiveCodec<>() {
        @Override
        public <T> DataResult<Character> read(final DynamicOps<T> ops, final T input) {
            return ops.getStringValue(input).map(it -> it.charAt(0));
        }

        @Override
        public <T> T write(final DynamicOps<T> ops, final Character value) {
            return ops.createString(value.toString());
        }

        @Override
        public String toString() {
            return "char";
        }
    };

    public static final Codec<HasMultiBlock> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("type").forGetter(HasMultiBlock::getType),
            CraftingLayer.CODEC.listOf().fieldOf("layers").forGetter(o -> o.layers),
            Codec.unboundedMap(
                    CHAR,
                    Codec.either(
                            ResourceLocation.CODEC,
                            BlockStatePredicate.CODEC
                    )
            ).fieldOf("key").forGetter(o -> {
                Map<Character, Either<ResourceLocation, BlockStatePredicate>> map = new HashMap<>();
                o.blockDef.forEach((character, blockStatePredicate) -> {
                    if (blockStatePredicate.getStatePredicate() == null
                            || blockStatePredicate.getStatePredicate().isEmpty()
                    ) {
                        map.put(character, Either.left(blockStatePredicate.getIsBlock()));
                    } else {
                        map.put(character, Either.right(blockStatePredicate));
                    }
                });
                return map;
            })
    ).apply(ins, HasMultiBlock::new));

    private final List<CraftingLayer> layers;
    private final Map<Character, BlockStatePredicate> blockDef;

    HasMultiBlock(
            String type,
            List<CraftingLayer> layers,
            Map<Character, Either<ResourceLocation, BlockStatePredicate>> blockDef
    ) {
        if (layers.size() != 3) throw new IllegalArgumentException(
                "HasMultiBlock has and can only have 3 elements."
        );
        this.layers = layers;
        this.blockDef = new HashMap<>();
        blockDef.forEach((character, either) -> {
            either.ifRight(it -> this.blockDef.put(character, it))
                    .ifLeft(it -> this.blockDef.put(character, BlockStatePredicate.of(it)));
        });
    }

    /**
     * json反序列化
     */
    public static HasMultiBlock decodeFromJson(JsonElement jsElem) {
        if (jsElem instanceof JsonObject) {
            return HasMultiBlockSerializer.fromJson((JsonObject) jsElem);
        } else {
            throw new IllegalArgumentException("Not a json object: " + jsElem);
        }
    }

    public static HasMultiBlock decodeFromNetworkBuf(FriendlyByteBuf buf) {
        return HasMultiBlockSerializer.fromNetwork(buf);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getType() {
        return "has_multi_block";
    }

    @Override
    public boolean matches(AnvilCraftingContext context) {
        BlockPos topCenterPos = context.getPos();
        System.out.println("topCenterPos = " + topCenterPos);
        return false;
    }

    @Override
    public boolean process(AnvilCraftingContext context) {
        return true;
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.getType());
        HasMultiBlockSerializer.toNetwork(buffer, this);
    }

    @Override
    public @NotNull JsonElement toJson() {
        return HasMultiBlock.CODEC
                .encodeStart(JsonOps.INSTANCE, this)
                .getOrThrow(false, ignored -> {
                });
    }

    public static class Builder {
        private final List<CraftingLayer> layers = new ArrayList<>();
        private final Map<Character, Either<ResourceLocation, BlockStatePredicate>> blockDef = new HashMap<>();

        Builder() {
        }

        public Builder layer(String... args) {
            layers.add(CraftingLayer.of(args));
            return this;
        }

        public Builder define(char ch, ResourceLocation block) {
            blockDef.put(ch, Either.left(block));
            return this;
        }

        public Builder define(char ch, Block block) {
            blockDef.put(ch, Either.left(BuiltInRegistries.BLOCK.getKey(block)));
            return this;
        }

        @SafeVarargs
        public final Builder define(char ch, Block block, Map.Entry<Property<?>, StringRepresentable>... states) {
            blockDef.put(ch, Either.right(BlockStatePredicate.of(block, states)));
            return this;
        }

        public HasMultiBlock build() {
            return new HasMultiBlock("has_multi_block",layers, blockDef);
        }
    }


}
