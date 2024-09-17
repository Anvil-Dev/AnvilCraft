package dev.dubhe.anvilcraft.recipe.transform;

import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.SnbtPrinterTagVisitor;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.StringRepresentable;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 对生成出来的生物进行nbt修改
 */
public class TagModification implements Consumer<Tag> {
    public static final Codec<TagModification> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                    Codec.STRING.fieldOf("path").forGetter(o -> o.path),
                    ModifyOperation.CODEC.fieldOf("op").forGetter(o -> o.op),
                    Codec.INT
                            .optionalFieldOf("index")
                            .forGetter(o -> o.index < 0 ? Optional.empty() : Optional.of(o.index)),
                    Codec.STRING.fieldOf("tag").forGetter(o -> {
                        SnbtPrinterTagVisitor visitor = new SnbtPrinterTagVisitor();
                        return visitor.visit(o.tag);
                    }))
            .apply(ins, TagModification::new));

    /**
     * 初始化 TagModification
     *
     * @param tag snbt表示的nbt标签
     */
    protected TagModification(String path, ModifyOperation op, Optional<Integer> index, String tag) {
        this.path = path;
        this.op = op;
        this.index = index.orElse(0);
        try {
            StringReader reader = new StringReader(tag);
            TagParser parser = new TagParser(reader);
            this.tag = parser.readValue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 初始化 TagModification
     */
    public TagModification(String path, ModifyOperation op, int index, Tag tag) {
        this.path = path;
        this.op = op;
        this.index = index;
        this.tag = tag;
    }

    private final String path;
    private final ModifyOperation op;
    private final int index;
    private final Tag tag;

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void accept(Tag input) {
        String path = this.path;
        if (op == ModifyOperation.SET) {
            try {
                int index = path.lastIndexOf('.');
                path = this.path.substring(0, index == -1 ? path.length() : index);
                StringReader reader = new StringReader(path);
                NbtPathArgument argument = new NbtPathArgument();
                NbtPathArgument.NbtPath nbtPath = argument.parse(reader);
                List<Tag> contract = nbtPath.get(input);
                if (contract.size() >= 2)
                    throw new IllegalArgumentException("TagModification does not allow multiple tag at path: " + path);
                if (contract.isEmpty()) return;
                Tag value = contract.get(0);
                op.accept(value, tag, 0, this.path.substring(index + 1));
                return;
            } catch (CommandSyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            StringReader reader = new StringReader(path);
            NbtPathArgument argument = new NbtPathArgument();
            NbtPathArgument.NbtPath nbtPath = argument.parse(reader);
            List<Tag> contract = nbtPath.get(tag);
            if (contract.size() >= 2)
                throw new IllegalArgumentException("TagModification does not allow multiple tag at path: " + path);
            if (contract.isEmpty()) return;
            Tag value = contract.get(0);
            op.accept(value, tag, index, path);
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public enum ModifyOperation implements StringRepresentable {
        SET {
            @Override
            public void accept(Tag inputSrc, Tag tag, int index, String key) {
                if (inputSrc instanceof CompoundTag compoundTag) {
                    compoundTag.put(key, tag);
                } else {
                    throw new RuntimeException("Expected CompoundTag, got " + inputSrc.getAsString());
                }
            }
        },
        APPEND {
            @Override
            public void accept(Tag inputSrc, Tag tag, int index, String key) {
                if (inputSrc instanceof ListTag listTag) {
                    listTag.add(tag);
                } else {
                    throw new RuntimeException("Expected list, got " + inputSrc.getAsString());
                }
            }
        },
        INSERT {
            @Override
            public void accept(Tag inputSrc, Tag tag, int index, String key) {
                if (inputSrc instanceof ListTag listTag) {
                    listTag.add(index, tag);
                } else {
                    throw new RuntimeException("Expected list, got " + inputSrc.getAsString());
                }
            }
        },
        MERGE {
            @Override
            public void accept(Tag inputSrc, Tag tag, int index, String key) {
                if (inputSrc instanceof ListTag listTag && tag instanceof ListTag tag2) {
                    listTag.addAll(tag2);
                } else {
                    throw new RuntimeException(
                            "Expected list, got " + inputSrc.getAsString() + ", " + tag.getAsString());
                }
            }
        },
        PREPEND {
            @Override
            public void accept(Tag inputSrc, Tag tag, int index, String key) {
                if (inputSrc instanceof ListTag listTag) {
                    listTag.add(0, tag);
                } else {
                    throw new RuntimeException("Expected list, got " + inputSrc.getAsString());
                }
            }
        };

        public static final Codec<ModifyOperation> CODEC = StringRepresentable.fromEnum(ModifyOperation::values);

        public abstract void accept(Tag inputSrc, Tag tag, int index, String key);

        @Override
        @NotNull public String getSerializedName() {
            return name();
        }
    }

    public static class Builder {
        private String path = "";
        private ModifyOperation op;
        private int index = -1;
        private Tag tag;

        Builder() {}

        public Builder path(String tagKeyPath) {
            this.path = tagKeyPath;
            return this;
        }

        public Builder operation(ModifyOperation fn) {
            this.op = fn;
            return this;
        }

        public Builder index(int value) {
            this.index = value;
            return this;
        }

        public Builder value(Tag tag) {
            this.tag = tag;
            return this;
        }

        public TagModification build() {
            return new TagModification(path, op, index, tag);
        }
    }
}
