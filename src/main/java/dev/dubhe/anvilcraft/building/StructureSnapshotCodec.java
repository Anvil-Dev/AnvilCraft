package dev.dubhe.anvilcraft.building;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * 规范快照与原版结构 NBT 之间的解析、校验与确定性序列化。
 * 解析显式报告未知方块、未知实体、损坏调色板与超大声明,绝不把错误静默转换为空气;
 * 尺寸与条目上限在分配大量内存之前检查。内容哈希取规范 NBT 未压缩字节的 SHA-256,
 * 相同逻辑内容的 CompoundTag 键集合相同即产生相同字节,因此哈希与来源格式无关。
 */
public final class StructureSnapshotCodec {
    /** 单轴最大尺寸;损坏文件声明的极端尺寸在此被拒绝。 */
    public static final int MAX_AXIS = 16;
    /** 方块条目上限。 */
    public static final int MAX_BLOCK_ENTRIES = MAX_AXIS * MAX_AXIS * MAX_AXIS;
    /** 实体条目上限。 */
    public static final int MAX_ENTITY_ENTRIES = 4_096;
    /** 调色板条目上限。 */
    public static final int MAX_PALETTE_ENTRIES = MAX_BLOCK_ENTRIES;
    /** 错误详情中列出的未知条目数量上限。 */
    private static final int MAX_REPORTED_IDS = 10;

    private StructureSnapshotCodec() {
    }

    /** 解析结果:规范化快照与不阻断导入的警告列表。 */
    public record ParsedSnapshot(StructureSnapshot snapshot, List<BlueprintWarning> warnings) {
    }

    /** 一条导入警告;reason 为稳定翻译键后缀。 */
    public record BlueprintWarning(String reason, String detail) {
    }

    /** 把原版结构语义的 NBT 解析为规范快照,输入可来自模板管理器、扫描器文件或上传文件。 */
    public static ParsedSnapshot parse(CompoundTag tag, HolderLookup.Provider registries)
        throws ConstructionBlueprintException {
        List<BlueprintWarning> warnings = new ArrayList<>();
        Vec3i size = parseSize(tag);

        ListTag paletteTag = resolvePalette(tag, warnings);
        if (paletteTag.size() > MAX_PALETTE_ENTRIES) {
            throw new ConstructionBlueprintException(
                "oversized",
                "palette entries " + paletteTag.size() + " > " + MAX_PALETTE_ENTRIES
            );
        }
        List<BlockState> palette = parsePaletteStates(paletteTag, registries);

        List<StructureSnapshot.BlockEntry> blocks = parseBlocks(tag, size, palette.size(), warnings);
        List<StructureSnapshot.EntityEntry> entities = parseEntities(tag, registries, warnings);

        StructureSnapshot snapshot = canonicalize(new StructureSnapshot(size, palette, blocks, entities));
        return new ParsedSnapshot(snapshot, List.copyOf(warnings));
    }

    private static Vec3i parseSize(CompoundTag tag) throws ConstructionBlueprintException {
        if (!tag.contains("size", Tag.TAG_LIST)) {
            throw new ConstructionBlueprintException("corrupt_size", "missing size list");
        }
        ListTag sizeTag = tag.getList("size", Tag.TAG_INT);
        if (sizeTag.size() != 3) {
            throw new ConstructionBlueprintException("corrupt_size", "size list length " + sizeTag.size());
        }
        int x = sizeTag.getInt(0);
        int y = sizeTag.getInt(1);
        int z = sizeTag.getInt(2);
        if (x < 1 || y < 1 || z < 1) {
            throw new ConstructionBlueprintException("corrupt_size", x + "x" + y + "x" + z);
        }
        if (x > MAX_AXIS || y > MAX_AXIS || z > MAX_AXIS) {
            throw new ConstructionBlueprintException(
                "oversized",
                x + "x" + y + "x" + z + " > " + MAX_AXIS + " per axis"
            );
        }
        return new Vec3i(x, y, z);
    }

    private static ListTag resolvePalette(CompoundTag tag, List<BlueprintWarning> warnings)
        throws ConstructionBlueprintException {
        if (tag.contains("palette", Tag.TAG_LIST)) {
            return tag.getList("palette", Tag.TAG_COMPOUND);
        }
        if (tag.contains("palettes", Tag.TAG_LIST)) {
            ListTag palettes = tag.getList("palettes", Tag.TAG_LIST);
            if (palettes.isEmpty()) {
                throw new ConstructionBlueprintException("corrupt_palette", "empty palettes list");
            }
            if (palettes.size() > 1) {
                warnings.add(new BlueprintWarning("multiple_palettes", String.valueOf(palettes.size())));
            }
            return palettes.getList(0);
        }
        throw new ConstructionBlueprintException("corrupt_palette", "missing palette");
    }

    private static List<BlockState> parsePaletteStates(ListTag paletteTag, HolderLookup.Provider registries)
        throws ConstructionBlueprintException {
        HolderGetter<Block> lookup = registries.lookupOrThrow(Registries.BLOCK);
        Set<String> unknownIds = new LinkedHashSet<>();
        for (int index = 0; index < paletteTag.size(); index++) {
            CompoundTag stateTag = paletteTag.getCompound(index);
            String rawName = stateTag.getString("Name");
            ResourceLocation id = ResourceLocation.tryParse(rawName);
            if (id == null) {
                throw new ConstructionBlueprintException("corrupt_palette", "invalid block id: " + rawName);
            }
            if (lookup.get(ResourceKey.create(Registries.BLOCK, id)).isEmpty()) {
                unknownIds.add(id.toString());
            }
        }
        if (!unknownIds.isEmpty()) {
            throw new ConstructionBlueprintException("unknown_block", describeUnknownIds(unknownIds));
        }
        List<BlockState> palette = new ArrayList<>(paletteTag.size());
        for (int index = 0; index < paletteTag.size(); index++) {
            palette.add(NbtUtils.readBlockState(lookup, paletteTag.getCompound(index)));
        }
        return palette;
    }

    private static List<StructureSnapshot.BlockEntry> parseBlocks(
        CompoundTag tag,
        Vec3i size,
        int paletteSize,
        List<BlueprintWarning> warnings
    ) throws ConstructionBlueprintException {
        ListTag blocksTag = tag.getList("blocks", Tag.TAG_COMPOUND);
        long volume = (long) size.getX() * size.getY() * size.getZ();
        if (blocksTag.size() > MAX_BLOCK_ENTRIES || blocksTag.size() > volume) {
            throw new ConstructionBlueprintException(
                "oversized",
                "block entries " + blocksTag.size() + " exceed limit"
            );
        }
        Map<Long, StructureSnapshot.BlockEntry> byPosition = new LinkedHashMap<>();
        int duplicates = 0;
        for (int index = 0; index < blocksTag.size(); index++) {
            CompoundTag blockTag = blocksTag.getCompound(index);
            ListTag posTag = blockTag.getList("pos", Tag.TAG_INT);
            if (posTag.size() != 3) {
                throw new ConstructionBlueprintException("corrupt_blocks", "block pos length " + posTag.size());
            }
            BlockPos pos = new BlockPos(posTag.getInt(0), posTag.getInt(1), posTag.getInt(2));
            if (pos.getX() < 0 || pos.getY() < 0 || pos.getZ() < 0
                || pos.getX() >= size.getX() || pos.getY() >= size.getY() || pos.getZ() >= size.getZ()) {
                throw new ConstructionBlueprintException(
                    "position_out_of_bounds",
                    pos.toShortString() + " outside " + size.getX() + "x" + size.getY() + "x" + size.getZ()
                );
            }
            int stateIndex = blockTag.getInt("state");
            if (stateIndex < 0 || stateIndex >= paletteSize) {
                throw new ConstructionBlueprintException("corrupt_palette", "state index " + stateIndex);
            }
            Optional<CompoundTag> nbt = blockTag.contains("nbt", Tag.TAG_COMPOUND)
                ? Optional.of(blockTag.getCompound("nbt").copy())
                : Optional.empty();
            if (byPosition.put(pos.asLong(), new StructureSnapshot.BlockEntry(pos, stateIndex, nbt)) != null) {
                duplicates++;
            }
        }
        if (duplicates > 0) {
            warnings.add(new BlueprintWarning("duplicate_block_position", String.valueOf(duplicates)));
        }
        return new ArrayList<>(byPosition.values());
    }

    private static List<StructureSnapshot.EntityEntry> parseEntities(
        CompoundTag tag,
        HolderLookup.Provider registries,
        List<BlueprintWarning> warnings
    ) throws ConstructionBlueprintException {
        ListTag entitiesTag = tag.getList("entities", Tag.TAG_COMPOUND);
        if (entitiesTag.size() > MAX_ENTITY_ENTRIES) {
            throw new ConstructionBlueprintException(
                "oversized",
                "entity entries " + entitiesTag.size() + " > " + MAX_ENTITY_ENTRIES
            );
        }
        HolderLookup.RegistryLookup<EntityType<?>> lookup = registries.lookupOrThrow(Registries.ENTITY_TYPE);
        Set<String> unknownIds = new LinkedHashSet<>();
        List<StructureSnapshot.EntityEntry> entities = new ArrayList<>(entitiesTag.size());
        int missingIds = 0;
        for (int index = 0; index < entitiesTag.size(); index++) {
            CompoundTag entityTag = entitiesTag.getCompound(index);
            ListTag posTag = entityTag.getList("pos", Tag.TAG_DOUBLE);
            ListTag blockPosTag = entityTag.getList("blockPos", Tag.TAG_INT);
            if (posTag.size() != 3 || blockPosTag.size() != 3) {
                throw new ConstructionBlueprintException("corrupt_entities", "entity position lists malformed");
            }
            CompoundTag nbt = entityTag.getCompound("nbt").copy();
            String rawId = nbt.getString("id");
            if (rawId.isEmpty()) {
                // 原版对乘客实体会保存空数据,加载时同样跳过;保留条目并给出警告。
                missingIds++;
            } else {
                ResourceLocation id = ResourceLocation.tryParse(rawId);
                if (id == null || lookup.get(ResourceKey.create(Registries.ENTITY_TYPE, id)).isEmpty()) {
                    unknownIds.add(rawId);
                }
            }
            entities.add(new StructureSnapshot.EntityEntry(
                new Vec3(posTag.getDouble(0), posTag.getDouble(1), posTag.getDouble(2)),
                new BlockPos(blockPosTag.getInt(0), blockPosTag.getInt(1), blockPosTag.getInt(2)),
                nbt
            ));
        }
        if (!unknownIds.isEmpty()) {
            throw new ConstructionBlueprintException("unknown_entity", describeUnknownIds(unknownIds));
        }
        if (missingIds > 0) {
            warnings.add(new BlueprintWarning("entity_missing_id", String.valueOf(missingIds)));
        }
        return entities;
    }

    /** 未知条目描述:最多列出十项,并附命名空间提示缺失的模组。 */
    private static String describeUnknownIds(Set<String> unknownIds) {
        Set<String> namespaces = new TreeSet<>();
        for (String id : unknownIds) {
            int split = id.indexOf(':');
            namespaces.add(split < 0 ? "minecraft" : id.substring(0, split));
        }
        List<String> shown = unknownIds.stream().limit(MAX_REPORTED_IDS).toList();
        StringBuilder detail = new StringBuilder(String.join(", ", shown));
        if (unknownIds.size() > shown.size()) {
            detail.append(" … +").append(unknownIds.size() - shown.size());
        }
        detail.append(" [").append(String.join(", ", namespaces)).append(']');
        return detail.toString();
    }

    /** 重排方块与实体并按首次出现顺序重建调色板,保证同一内容得到唯一规范形式。 */
    public static StructureSnapshot canonicalize(StructureSnapshot snapshot) {
        List<StructureSnapshot.BlockEntry> sortedBlocks = new ArrayList<>(snapshot.blocks());
        sortedBlocks.sort(Comparator
            .comparingInt((StructureSnapshot.BlockEntry entry) -> entry.pos().getY())
            .thenComparingInt(entry -> entry.pos().getZ())
            .thenComparingInt(entry -> entry.pos().getX()));

        Map<BlockState, Integer> paletteIndex = new LinkedHashMap<>();
        List<BlockState> palette = new ArrayList<>();
        List<StructureSnapshot.BlockEntry> blocks = new ArrayList<>(sortedBlocks.size());
        for (StructureSnapshot.BlockEntry entry : sortedBlocks) {
            BlockState state = snapshot.palette().get(entry.stateIndex());
            Integer index = paletteIndex.get(state);
            if (index == null) {
                index = palette.size();
                paletteIndex.put(state, index);
                palette.add(state);
            }
            blocks.add(new StructureSnapshot.BlockEntry(entry.pos(), index, entry.nbt()));
        }

        List<StructureSnapshot.EntityEntry> entities = new ArrayList<>(snapshot.entities());
        entities.sort(Comparator
            .comparingInt((StructureSnapshot.EntityEntry entry) -> entry.blockPos().getY())
            .thenComparingInt(entry -> entry.blockPos().getZ())
            .thenComparingInt(entry -> entry.blockPos().getX())
            .thenComparingDouble(entry -> entry.pos().y)
            .thenComparingDouble(entry -> entry.pos().z)
            .thenComparingDouble(entry -> entry.pos().x)
            .thenComparing(entry -> entry.nbt().getString("id")));

        return new StructureSnapshot(snapshot.size(), palette, blocks, entities);
    }

    /** 写出规范结构 NBT;输入必须已经过 {@link #canonicalize}。 */
    public static CompoundTag write(StructureSnapshot snapshot) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());

        ListTag sizeTag = new ListTag();
        sizeTag.add(IntTag.valueOf(snapshot.size().getX()));
        sizeTag.add(IntTag.valueOf(snapshot.size().getY()));
        sizeTag.add(IntTag.valueOf(snapshot.size().getZ()));
        tag.put("size", sizeTag);

        ListTag paletteTag = new ListTag();
        for (BlockState state : snapshot.palette()) {
            paletteTag.add(NbtUtils.writeBlockState(state));
        }
        tag.put("palette", paletteTag);

        ListTag blocksTag = new ListTag();
        for (StructureSnapshot.BlockEntry entry : snapshot.blocks()) {
            final CompoundTag blockTag = new CompoundTag();
            ListTag posTag = new ListTag();
            posTag.add(IntTag.valueOf(entry.pos().getX()));
            posTag.add(IntTag.valueOf(entry.pos().getY()));
            posTag.add(IntTag.valueOf(entry.pos().getZ()));
            blockTag.put("pos", posTag);
            blockTag.putInt("state", entry.stateIndex());
            entry.nbt().ifPresent(nbt -> blockTag.put("nbt", nbt.copy()));
            blocksTag.add(blockTag);
        }
        tag.put("blocks", blocksTag);

        ListTag entitiesTag = new ListTag();
        for (StructureSnapshot.EntityEntry entry : snapshot.entities()) {
            final CompoundTag entityTag = new CompoundTag();
            ListTag posTag = new ListTag();
            posTag.add(DoubleTag.valueOf(entry.pos().x));
            posTag.add(DoubleTag.valueOf(entry.pos().y));
            posTag.add(DoubleTag.valueOf(entry.pos().z));
            entityTag.put("pos", posTag);
            ListTag blockPosTag = new ListTag();
            blockPosTag.add(IntTag.valueOf(entry.blockPos().getX()));
            blockPosTag.add(IntTag.valueOf(entry.blockPos().getY()));
            blockPosTag.add(IntTag.valueOf(entry.blockPos().getZ()));
            entityTag.put("blockPos", blockPosTag);
            entityTag.put("nbt", entry.nbt().copy());
            entitiesTag.add(entityTag);
        }
        tag.put("entities", entitiesTag);
        return tag;
    }

    /** 规范 NBT 未压缩字节的 SHA-256 十六进制,即蓝图内容哈希。 */
    public static String hash(CompoundTag canonicalTag) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            NbtIo.write(canonicalTag, new DataOutputStream(bytes));
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes.toByteArray()));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to serialize canonical snapshot", exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
