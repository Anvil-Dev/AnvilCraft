package net.minecraft.nbt;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class CompoundTag implements Tag {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Codec<CompoundTag> CODEC = Codec.PASSTHROUGH
        .comapFlatMap(
            t -> {
                Tag tag = t.convert(NbtOps.INSTANCE).getValue();
                return tag instanceof CompoundTag compoundTag
                    ? DataResult.success(compoundTag == t.getValue() ? compoundTag.copy() : compoundTag)
                    : DataResult.error(() -> "Not a compound tag: " + tag);
            },
            t -> new Dynamic<>(NbtOps.INSTANCE, t.copy())
        );
    private static final int SELF_SIZE_IN_BYTES = 48;
    private static final int MAP_ENTRY_SIZE_IN_BYTES = 32;
    public static final TagType<CompoundTag> TYPE = new TagType.VariableSize<CompoundTag>() {
        public CompoundTag load(DataInput input, NbtAccounter accounter) throws IOException {
            accounter.pushDepth();

            CompoundTag var3;
            try {
                var3 = loadCompound(input, accounter);
            } finally {
                accounter.popDepth();
            }

            return var3;
        }

        private static byte readNamedTagType(DataInput input, NbtAccounter accounter) throws IOException {
            accounter.accountBytes(2);
            return input.readByte();
        }

        private static CompoundTag loadCompound(DataInput input, NbtAccounter accounter) throws IOException {
            accounter.accountBytes(48L);
            Map<String, Tag> values = Maps.newHashMap();

            byte tagType;
            while ((tagType = readNamedTagType(input, accounter)) != 0) {
                String key = accounter.readUTF(input.readUTF());
                accounter.accountBytes(4); //Forge: 4 extra bytes for the object allocation.
                Tag tag = CompoundTag.readNamedTagData(TagTypes.getType(tagType), key, input, accounter);
                if (values.put(key, tag) == null) {
                    accounter.accountBytes(36L);
                }
            }

            return new CompoundTag(values);
        }

        @Override
        public StreamTagVisitor.ValueResult parse(DataInput input, StreamTagVisitor output, NbtAccounter accounter) throws IOException {
            accounter.pushDepth();

            StreamTagVisitor.ValueResult var4;
            try {
                var4 = parseCompound(input, output, accounter);
            } finally {
                accounter.popDepth();
            }

            return var4;
        }

        private static StreamTagVisitor.ValueResult parseCompound(DataInput input, StreamTagVisitor output, NbtAccounter accounter) throws IOException {
            accounter.accountBytes(48L);

            byte tagTypeId;
            label35:
            while ((tagTypeId = input.readByte()) != 0) {
                TagType<?> tagType = TagTypes.getType(tagTypeId);
                switch (output.visitEntry(tagType)) {
                    case HALT:
                        return StreamTagVisitor.ValueResult.HALT;
                    case BREAK:
                        StringTag.skipString(input);
                        tagType.skip(input, accounter);
                        break label35;
                    case SKIP:
                        StringTag.skipString(input);
                        tagType.skip(input, accounter);
                        break;
                    default:
                        String key = readString(input, accounter);
                        switch (output.visitEntry(tagType, key)) {
                            case HALT:
                                return StreamTagVisitor.ValueResult.HALT;
                            case BREAK:
                                tagType.skip(input, accounter);
                                break label35;
                            case SKIP:
                                tagType.skip(input, accounter);
                                break;
                            default:
                                accounter.accountBytes(36L);
                                switch (tagType.parse(input, output, accounter)) {
                                    case HALT:
                                        return StreamTagVisitor.ValueResult.HALT;
                                    case BREAK:
                                }
                        }
                }
            }

            if (tagTypeId != 0) {
                while ((tagTypeId = input.readByte()) != 0) {
                    StringTag.skipString(input);
                    TagTypes.getType(tagTypeId).skip(input, accounter);
                }
            }

            return output.visitContainerEnd();
        }

        private static String readString(DataInput input, NbtAccounter accounter) throws IOException {
            String key = input.readUTF();
            accounter.accountBytes(28L);
            accounter.accountBytes(2L, key.length());
            return key;
        }

        @Override
        public void skip(DataInput input, NbtAccounter accounter) throws IOException {
            accounter.pushDepth();

            byte tagTypeId;
            try {
                while ((tagTypeId = input.readByte()) != 0) {
                    StringTag.skipString(input);
                    TagTypes.getType(tagTypeId).skip(input, accounter);
                }
            } finally {
                accounter.popDepth();
            }
        }

        @Override
        public String getName() {
            return "COMPOUND";
        }

        @Override
        public String getPrettyName() {
            return "TAG_Compound";
        }
    };
    private final Map<String, Tag> tags;

    CompoundTag(Map<String, Tag> tags) {
        this.tags = tags;
    }

    public CompoundTag() {
        this(new HashMap<>());
    }

    /**
     * Neo: create a compound tag that is generally suitable to hold the given amount of entries
     * without needing to resize the internal map.
     *
     * @param expectedEntries the expected number of entries that the compound tag will have
     * @see HashMap#newHashMap(int)
     */
    public CompoundTag(int expectedEntries) {
        this(HashMap.newHashMap(expectedEntries));
    }

    @Override
    public void write(DataOutput output) throws IOException {
        for (String key : this.tags.keySet()) {
            Tag tag = this.tags.get(key);
            writeNamedTag(key, tag, output);
        }

        output.writeByte(0);
    }

    @Override
    public int sizeInBytes() {
        int size = 48;

        for (Entry<String, Tag> entry : this.tags.entrySet()) {
            size += 28 + 2 * entry.getKey().length();
            size += 36;
            size += entry.getValue().sizeInBytes();
        }

        return size;
    }

    public Set<String> keySet() {
        return this.tags.keySet();
    }

    public Set<Entry<String, Tag>> entrySet() {
        return this.tags.entrySet();
    }

    public Collection<Tag> values() {
        return this.tags.values();
    }

    public void forEach(BiConsumer<String, Tag> consumer) {
        this.tags.forEach(consumer);
    }

    @Override
    public byte getId() {
        return 10;
    }

    @Override
    public TagType<CompoundTag> getType() {
        return TYPE;
    }

    public int size() {
        return this.tags.size();
    }

    public @Nullable Tag put(String name, Tag tag) {
        if (tag == null) throw new IllegalArgumentException("Invalid null NBT value with key " + name);
        return this.tags.put(name, tag);
    }

    public void putByte(String name, byte value) {
        this.tags.put(name, ByteTag.valueOf(value));
    }

    public void putShort(String name, short value) {
        this.tags.put(name, ShortTag.valueOf(value));
    }

    public void putInt(String name, int value) {
        this.tags.put(name, IntTag.valueOf(value));
    }

    public void putLong(String name, long value) {
        this.tags.put(name, LongTag.valueOf(value));
    }

    public void putFloat(String name, float value) {
        this.tags.put(name, FloatTag.valueOf(value));
    }

    public void putDouble(String name, double value) {
        this.tags.put(name, DoubleTag.valueOf(value));
    }

    public void putString(String name, String value) {
        this.tags.put(name, StringTag.valueOf(value));
    }

    public void putByteArray(String name, byte[] value) {
        this.tags.put(name, new ByteArrayTag(value));
    }

    public void putIntArray(String name, int[] value) {
        this.tags.put(name, new IntArrayTag(value));
    }

    public void putLongArray(String name, long[] value) {
        this.tags.put(name, new LongArrayTag(value));
    }

    public void putBoolean(String name, boolean value) {
        this.tags.put(name, ByteTag.valueOf(value));
    }

    public @Nullable Tag get(String name) {
        return this.tags.get(name);
    }

    public boolean contains(String name) {
        return this.tags.containsKey(name);
    }

    private Optional<Tag> getOptional(String name) {
        return Optional.ofNullable(this.tags.get(name));
    }

    public Optional<Byte> getByte(String name) {
        return this.getOptional(name).flatMap(Tag::asByte);
    }

    public byte getByteOr(String name, byte defaultValue) {
        return this.tags.get(name) instanceof NumericTag tag ? tag.byteValue() : defaultValue;
    }

    public Optional<Short> getShort(String name) {
        return this.getOptional(name).flatMap(Tag::asShort);
    }

    public short getShortOr(String name, short defaultValue) {
        return this.tags.get(name) instanceof NumericTag tag ? tag.shortValue() : defaultValue;
    }

    public Optional<Integer> getInt(String name) {
        return this.getOptional(name).flatMap(Tag::asInt);
    }

    public int getIntOr(String name, int defaultValue) {
        return this.tags.get(name) instanceof NumericTag tag ? tag.intValue() : defaultValue;
    }

    public Optional<Long> getLong(String name) {
        return this.getOptional(name).flatMap(Tag::asLong);
    }

    public long getLongOr(String name, long defaultValue) {
        return this.tags.get(name) instanceof NumericTag tag ? tag.longValue() : defaultValue;
    }

    public Optional<Float> getFloat(String name) {
        return this.getOptional(name).flatMap(Tag::asFloat);
    }

    public float getFloatOr(String name, float defaultValue) {
        return this.tags.get(name) instanceof NumericTag tag ? tag.floatValue() : defaultValue;
    }

    public Optional<Double> getDouble(String name) {
        return this.getOptional(name).flatMap(Tag::asDouble);
    }

    public double getDoubleOr(String name, double defaultValue) {
        return this.tags.get(name) instanceof NumericTag tag ? tag.doubleValue() : defaultValue;
    }

    public Optional<String> getString(String name) {
        return this.getOptional(name).flatMap(Tag::asString);
    }

    public String getStringOr(String name, String defaultValue) {
        return this.tags.get(name) instanceof StringTag(String var8) ? var8 : defaultValue;
    }

    public Optional<byte[]> getByteArray(String name) {
        return this.tags.get(name) instanceof ByteArrayTag tag ? Optional.of(tag.getAsByteArray()) : Optional.empty();
    }

    public Optional<int[]> getIntArray(String name) {
        return this.tags.get(name) instanceof IntArrayTag tag ? Optional.of(tag.getAsIntArray()) : Optional.empty();
    }

    public Optional<long[]> getLongArray(String name) {
        return this.tags.get(name) instanceof LongArrayTag tag ? Optional.of(tag.getAsLongArray()) : Optional.empty();
    }

    public Optional<CompoundTag> getCompound(String name) {
        return this.tags.get(name) instanceof CompoundTag tag ? Optional.of(tag) : Optional.empty();
    }

    public CompoundTag getCompoundOrEmpty(String name) {
        return this.getCompound(name).orElseGet(CompoundTag::new);
    }

    public Optional<ListTag> getList(String name) {
        return this.tags.get(name) instanceof ListTag tag ? Optional.of(tag) : Optional.empty();
    }

    public ListTag getListOrEmpty(String name) {
        return this.getList(name).orElseGet(ListTag::new);
    }

    public Optional<Boolean> getBoolean(String name) {
        return this.getOptional(name).flatMap(Tag::asBoolean);
    }

    public boolean getBooleanOr(String string, boolean defaultValue) {
        return this.getByteOr(string, (byte)(defaultValue ? 1 : 0)) != 0;
    }

    public @Nullable Tag remove(String name) {
        return this.tags.remove(name);
    }

    @Override
    public String toString() {
        StringTagVisitor visitor = new StringTagVisitor();
        visitor.visitCompound(this);
        return visitor.build();
    }

    public boolean isEmpty() {
        return this.tags.isEmpty();
    }

    protected CompoundTag shallowCopy() {
        return new CompoundTag(new HashMap<>(this.tags));
    }

    public CompoundTag copy() {
        HashMap<String, Tag> newTags = new HashMap<>();
        this.tags.forEach((key, tag) -> newTags.put(key, tag.copy()));
        return new CompoundTag(newTags);
    }

    @Override
    public Optional<CompoundTag> asCompound() {
        return Optional.of(this);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj ? true : obj instanceof CompoundTag && Objects.equals(this.tags, ((CompoundTag)obj).tags);
    }

    @Override
    public int hashCode() {
        return this.tags.hashCode();
    }

    private static void writeNamedTag(String name, Tag tag, DataOutput output) throws IOException {
        output.writeByte(tag.getId());
        if (tag.getId() != 0) {
            output.writeUTF(name);
            tag.write(output);
        }
    }

    private static Tag readNamedTagData(TagType<?> type, String name, DataInput input, NbtAccounter accounter) {
        try {
            return type.load(input, accounter);
        } catch (IOException var7) {
            CrashReport report = CrashReport.forThrowable(var7, "Loading NBT data");
            CrashReportCategory category = report.addCategory("NBT Tag");
            category.setDetail("Tag name", name);
            category.setDetail("Tag type", type.getName());
            throw new ReportedNbtException(report);
        }
    }

    public CompoundTag merge(CompoundTag other) {
        for (String tagName : other.tags.keySet()) {
            Tag otherTag = other.tags.get(tagName);
            if (otherTag instanceof CompoundTag otherCompound && this.tags.get(tagName) instanceof CompoundTag selfCompound) {
                selfCompound.merge(otherCompound);
            } else {
                this.put(tagName, otherTag.copy());
            }
        }

        return this;
    }

    @Override
    public void accept(TagVisitor visitor) {
        visitor.visitCompound(this);
    }

    @Override
    public StreamTagVisitor.ValueResult accept(StreamTagVisitor visitor) {
        for (Entry<String, Tag> entry : this.tags.entrySet()) {
            Tag value = entry.getValue();
            TagType<?> type = value.getType();
            StreamTagVisitor.EntryResult entryParseResult = visitor.visitEntry(type);
            switch (entryParseResult) {
                case HALT:
                    return StreamTagVisitor.ValueResult.HALT;
                case BREAK:
                    return visitor.visitContainerEnd();
                case SKIP:
                    break;
                default:
                    entryParseResult = visitor.visitEntry(type, entry.getKey());
                    switch (entryParseResult) {
                        case HALT:
                            return StreamTagVisitor.ValueResult.HALT;
                        case BREAK:
                            return visitor.visitContainerEnd();
                        case SKIP:
                            break;
                        default:
                            StreamTagVisitor.ValueResult valueResult = value.accept(visitor);
                            switch (valueResult) {
                                case HALT:
                                    return StreamTagVisitor.ValueResult.HALT;
                                case BREAK:
                                    return visitor.visitContainerEnd();
                            }
                    }
            }
        }

        return visitor.visitContainerEnd();
    }

    public <T> void store(String name, Codec<T> codec, T value) {
        this.store(name, codec, NbtOps.INSTANCE, value);
    }

    public <T> void storeNullable(String name, Codec<T> codec, @Nullable T value) {
        if (value != null) {
            this.store(name, codec, value);
        }
    }

    public <T> void store(String name, Codec<T> codec, DynamicOps<Tag> ops, T value) {
        this.put(name, codec.encodeStart(ops, value).getOrThrow());
    }

    public <T> void storeNullable(String name, Codec<T> codec, DynamicOps<Tag> ops, @Nullable T value) {
        if (value != null) {
            this.store(name, codec, ops, value);
        }
    }

    public <T> void store(MapCodec<T> codec, T value) {
        this.store(codec, NbtOps.INSTANCE, value);
    }

    public <T> void store(MapCodec<T> codec, DynamicOps<Tag> ops, T value) {
        this.merge((CompoundTag)codec.encoder().encodeStart(ops, value).getOrThrow());
    }

    public <T> Optional<T> read(String name, Codec<T> codec) {
        return this.read(name, codec, NbtOps.INSTANCE);
    }

    public <T> Optional<T> read(String name, Codec<T> codec, DynamicOps<Tag> ops) {
        Tag tag = this.get(name);
        return tag == null
            ? Optional.empty()
            : codec.parse(ops, tag).resultOrPartial(error -> LOGGER.error("Failed to read field ({}={}): {}", name, tag, error));
    }

    public <T> Optional<T> read(MapCodec<T> codec) {
        return this.read(codec, NbtOps.INSTANCE);
    }

    public <T> Optional<T> read(MapCodec<T> codec, DynamicOps<Tag> ops) {
        return codec.decode(ops, ops.getMap(this).getOrThrow()).resultOrPartial(error -> LOGGER.error("Failed to read value ({}): {}", this, error));
    }
}
