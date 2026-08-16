package dev.dubhe.anvilcraft.util;

import lombok.Data;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagVisitor;

import java.util.Objects;
import javax.annotation.Nullable;

public class NbtUtil {
    private static final String NEW_COMPOUND_PATTERN = "CompoundTag %s = new CompoundTag();\n";
    private static final String NEW_LIST_PATTERN = "ListTag %s = new ListTag();\n";
    private static final String PUT_STRING_PATTERN = "%s.putString(%s, %s);\n";
    private static final String PUT_BYTE_PATTERN = "%s.putByte(%s, %d);\n";
    private static final String PUT_SHORT_PATTERN = "%s.putShort(%s, %d);\n";
    private static final String PUT_INT_PATTERN = "%s.putInt(%s, %d);\n";
    private static final String PUT_LONG_PATTERN = "%s.putLong(%s, %dL);\n";
    private static final String PUT_FLOAT_PATTERN = "%s.putFloat(%s, %fF);\n";
    private static final String PUT_DOUBLE_PATTERN = "%s.putDouble(%s, %f);\n";
    private static final String PUT_BYTE_ARRAY_PATTERN = "%s.putByteArray(%s, new byte[]{%s});\n";
    private static final String PUT_INT_ARRAY_PATTERN = "%s.putIntArray(%s, new int[]{%s});\n";
    private static final String PUT_LONG_ARRAY_PATTERN = "%s.putLongArray(%s, new long[]{%s});\n";
    private static final String PUT_TAG_PATTERN = "%s.put(%s, %s);\n";
    private static final String ADD_TAG_PATTERN = "%s.add(%s);\n";

    public static String toConstructString(CompoundTag tag, State state) {
        StringBuilder builder = new StringBuilder();
        String variableName = "nbt" + state.increaseTotal();
        builder.append(NbtUtil.NEW_COMPOUND_PATTERN.formatted(variableName));

        ConstructStringTagVisitor visitor = new ConstructStringTagVisitor(variableName, builder, state);
        for (String key : tag.getAllKeys()) {
            visitor.currentName = key;
            Objects.requireNonNull(tag.get(key)).accept(visitor);
        }
        return builder.toString();
    }

    private static class ConstructStringTagVisitor implements TagVisitor {
        private final String variableName;
        private final StringBuilder builder;
        private final State state;
        @Nullable
        private String currentName;

        private ConstructStringTagVisitor(String variableName, StringBuilder builder, State state) {
            this.variableName = variableName;
            this.builder = builder;
            this.state = state;
        }

        private static String quoted(String value) {
            return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                + "\"";
        }

        private void putTag(String key, String value) {
            this.builder.append(NbtUtil.PUT_TAG_PATTERN.formatted(this.variableName, ConstructStringTagVisitor.quoted(key), value));
        }

        private void addTag(String value) {
            this.builder.append(NbtUtil.ADD_TAG_PATTERN.formatted(this.variableName, value));
        }

        private void attach(String tagName) {
            if (this.currentName == null) {
                this.addTag(tagName);
            } else {
                this.putTag(this.currentName, tagName);
            }
        }

        @Override
        public void visitString(StringTag tag) {
            String value = ConstructStringTagVisitor.quoted(tag.getAsString());
            if (this.currentName == null) {
                this.addTag("StringTag.valueOf(" + value + ")");
            } else {
                this.builder.append(NbtUtil.PUT_STRING_PATTERN.formatted(
                    this.variableName,
                    ConstructStringTagVisitor.quoted(this.currentName),
                    value
                ));
            }
        }

        @Override
        public void visitByte(ByteTag tag) {
            if (this.currentName == null) {
                this.addTag("ByteTag.valueOf(%d)".formatted(tag.getAsByte()));
            } else {
                this.builder.append(NbtUtil.PUT_BYTE_PATTERN.formatted(
                    this.variableName,
                    ConstructStringTagVisitor.quoted(this.currentName),
                    tag.getAsByte()
                ));
            }
        }

        @Override
        public void visitShort(ShortTag tag) {
            if (this.currentName == null) {
                this.addTag("ShortTag.valueOf(%d)".formatted(tag.getAsShort()));
            } else {
                this.builder.append(NbtUtil.PUT_SHORT_PATTERN.formatted(
                    this.variableName,
                    ConstructStringTagVisitor.quoted(this.currentName),
                    tag.getAsShort()
                ));
            }
        }

        @Override
        public void visitInt(IntTag tag) {
            if (this.currentName == null) {
                this.addTag("IntTag.valueOf(%d)".formatted(tag.getAsInt()));
            } else {
                this.builder.append(NbtUtil.PUT_INT_PATTERN.formatted(
                    this.variableName,
                    ConstructStringTagVisitor.quoted(this.currentName),
                    tag.getAsInt()
                ));
            }
        }

        @Override
        public void visitLong(LongTag tag) {
            if (this.currentName == null) {
                this.addTag("LongTag.valueOf(%dL)".formatted(tag.getAsLong()));
            } else {
                this.builder.append(NbtUtil.PUT_LONG_PATTERN.formatted(
                    this.variableName,
                    ConstructStringTagVisitor.quoted(this.currentName),
                    tag.getAsLong()
                ));
            }
        }

        @Override
        public void visitFloat(FloatTag tag) {
            if (this.currentName == null) {
                this.addTag("FloatTag.valueOf(%fF)".formatted(tag.getAsFloat()));
            } else {
                this.builder.append(NbtUtil.PUT_FLOAT_PATTERN.formatted(
                    this.variableName,
                    ConstructStringTagVisitor.quoted(this.currentName),
                    tag.getAsFloat()
                ));
            }
        }

        @Override
        public void visitDouble(DoubleTag tag) {
            if (this.currentName == null) {
                this.addTag("DoubleTag.valueOf(%f)".formatted(tag.getAsDouble()));
            } else {
                this.builder.append(NbtUtil.PUT_DOUBLE_PATTERN.formatted(
                    this.variableName,
                    ConstructStringTagVisitor.quoted(this.currentName),
                    tag.getAsDouble()
                ));
            }
        }

        @Override
        public void visitByteArray(ByteArrayTag tag) {
            String array = byteArrayToString(tag.getAsByteArray());
            if (this.currentName == null) {
                this.addTag("new ByteArrayTag(new byte[]{%s})".formatted(array));
            } else {
                this.builder.append(NbtUtil.PUT_BYTE_ARRAY_PATTERN.formatted(
                    this.variableName,
                    ConstructStringTagVisitor.quoted(this.currentName), array
                ));
            }
        }

        @Override
        public void visitIntArray(IntArrayTag tag) {
            String array = intArrayToString(tag.getAsIntArray());
            if (this.currentName == null) {
                this.addTag("new IntArrayTag(new int[]{%s})".formatted(array));
            } else {
                this.builder.append(NbtUtil.PUT_INT_ARRAY_PATTERN.formatted(
                    this.variableName,
                    ConstructStringTagVisitor.quoted(this.currentName),
                    array
                ));
            }
        }

        @Override
        public void visitLongArray(LongArrayTag tag) {
            String array = longArrayToString(tag.getAsLongArray());
            if (this.currentName == null) {
                this.addTag("new LongArrayTag(new long[]{%s})".formatted(array));
            } else {
                this.builder.append(NbtUtil.PUT_LONG_ARRAY_PATTERN.formatted(
                    this.variableName,
                    ConstructStringTagVisitor.quoted(this.currentName),
                    array
                ));
            }
        }

        private static String byteArrayToString(byte[] array) {
            StringBuilder builder = new StringBuilder();
            for (byte value : array) {
                builder.append(value).append(", ");
            }
            return trimTrailingComma(builder);
        }

        private static String intArrayToString(int[] array) {
            StringBuilder builder = new StringBuilder();
            for (int value : array) {
                builder.append(value).append(", ");
            }
            return trimTrailingComma(builder);
        }

        private static String longArrayToString(long[] array) {
            StringBuilder builder = new StringBuilder();
            for (long value : array) {
                builder.append(value).append("L, ");
            }
            return trimTrailingComma(builder);
        }

        private static String trimTrailingComma(StringBuilder builder) {
            if (builder.isEmpty()) {
                return "";
            }
            return builder.substring(0, builder.length() - 2);
        }

        @Override
        public void visitList(ListTag tag) {
            String listName = "list" + this.state.increaseList();
            StringBuilder childBuilder = new StringBuilder();
            childBuilder.append(NbtUtil.NEW_LIST_PATTERN.formatted(listName));

            ConstructStringTagVisitor child = new ConstructStringTagVisitor(listName, childBuilder, this.state);
            for (Tag element : tag) {
                child.currentName = null;
                element.accept(child);
            }
            this.builder.append(childBuilder);
            this.attach(listName);
        }

        @Override
        public void visitCompound(CompoundTag tag) {
            String compoundName = "compound" + this.state.increaseCompound();
            StringBuilder childBuilder = new StringBuilder();
            childBuilder.append(NbtUtil.NEW_COMPOUND_PATTERN.formatted(compoundName));

            ConstructStringTagVisitor child = new ConstructStringTagVisitor(compoundName, childBuilder, this.state);
            for (String key : tag.getAllKeys()) {
                child.currentName = key;
                Objects.requireNonNull(tag.get(key)).accept(child);
            }
            this.builder.append(childBuilder);
            this.attach(compoundName);
        }

        @Override
        public void visitEnd(EndTag tag) {
        }
    }

    @Data
    public static class State {
        private int totalCount;
        private int listCount;
        private int compoundCount;

        public int increaseTotal() {
            return ++this.totalCount;
        }

        public int increaseList() {
            return ++this.listCount;
        }

        public int increaseCompound() {
            return ++this.compoundCount;
        }
    }
}
