package dev.dubhe.anvilcraft.recipe.transform;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.recipe.util.CodecUtil;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.function.BiFunction;

public record NumericTagValuePredicate(String tagKeyPath, ValueFunction requirement, long expected) {
    public static final Codec<NumericTagValuePredicate> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("tagKeyPath").forGetter(o -> o.tagKeyPath),
            ValueFunction.CODEC.fieldOf("requirement").forGetter(o -> o.requirement),
            Codec.LONG.fieldOf("expected").forGetter(it -> it.expected)
        )
        .apply(ins, NumericTagValuePredicate::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, NumericTagValuePredicate> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        NumericTagValuePredicate::tagKeyPath,
        ValueFunction.STREAM_CODEC,
        NumericTagValuePredicate::requirement,
        ByteBufCodecs.VAR_LONG,
        NumericTagValuePredicate::expected,
        NumericTagValuePredicate::new
    );

    public static NumericTagValuePredicate fromJson(JsonObject jsonObject) {
        return CODEC.decode(JsonOps.INSTANCE, jsonObject).getOrThrow().getFirst();
    }

    public JsonElement toJson() {
        return CODEC.encodeStart(JsonOps.INSTANCE, this).getOrThrow();
    }

    public enum ValueFunction implements StringRepresentable {
        EQUAL(Object::equals),
        NOT_EQUAL((a, b) -> !a.equals(b)),
        GREATER((a, b) -> a > b),
        LESS((a, b) -> a < b),
        GREATER_OR_EQUAL((a, b) -> a >= b),
        LESS_OR_EQUAL((a, b) -> a <= b);

        public static final Codec<ValueFunction> CODEC = StringRepresentable.fromEnum(ValueFunction::values);
        public static final StreamCodec<RegistryFriendlyByteBuf, ValueFunction> STREAM_CODEC = CodecUtil.enumStreamCodec(
            ValueFunction.class
        );
        private final BiFunction<Long, Long, Boolean> fn;

        ValueFunction(BiFunction<Long, Long, Boolean> fn) {
            this.fn = fn;
        }

        boolean accept(long l, long r) {
            return fn.apply(l, r);
        }

        @Override
        public String getSerializedName() {
            return name();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String tagKeyPath;
        private ValueFunction requirement;
        private long expected;

        Builder() {
        }

        public Builder path(String tagKeyPath) {
            this.tagKeyPath = tagKeyPath;
            return this;
        }

        public Builder compare(ValueFunction fn) {
            this.requirement = fn;
            return this;
        }

        public Builder expect(long value) {
            this.expected = value;
            return this;
        }

        /**
         * 左操作数
         */
        public Builder lhs(String tagKeyPath) {
            this.tagKeyPath = tagKeyPath;
            return this;
        }

        /**
         * 右操作数
         */
        public Builder rhs(long value) {
            this.expected = value;
            return this;
        }

        public NumericTagValuePredicate build() {
            return new NumericTagValuePredicate(tagKeyPath, requirement, expected);
        }
    }

    public boolean test(CompoundTag tag) {
        try {
            StringReader reader = new StringReader(tagKeyPath);
            NbtPathArgument argument = new NbtPathArgument();
            NbtPathArgument.NbtPath path = argument.parse(reader);
            List<Tag> contract = path.get(tag);
            if (contract.size() >= 2) {
                throw new IllegalArgumentException(
                    "TagValuePredicate does not allow multiple tag at path: " + tagKeyPath);
            }
            if (contract.isEmpty()) return false;
            Tag value = contract.getFirst();
            if (value instanceof NumericTag tag1) {
                return requirement.accept(tag1.getAsLong(), expected);
            }
            return false;
        } catch (CommandSyntaxException e) {
            return false;
        }
    }
}
