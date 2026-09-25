package dev.dubhe.anvilcraft.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Litematica {@code .litematic} 到原版结构 NBT 的转换器。支持多区域、负尺寸轴、
 * 紧凑跨 long 的位压缩调色板、方块实体与实体;输出与原版模板一致的稠密方块列表
 * (含空气),之后统一走数据修复与规范解析,与其他来源产生同一规范快照。
 * 区域重叠、无法映射的扩展字段等情况按设计要求显式报告,不静默丢弃成方块缺失。
 */
public final class LitematicaImporter {
    /** 转换阶段就拒绝的总体积上限,与规范解析的方块条目上限一致。 */
    private static final long MAX_VOLUME = StructureSnapshotCodec.MAX_BLOCK_ENTRIES;
    /** 已知但不影响方块与实体重建的区域扩展字段。 */
    private static final Set<String> IGNORABLE_REGION_KEYS = Set.of(
        "Position",
        "Size",
        "BlockStatePalette",
        "BlockStates",
        "TileEntities",
        "Entities",
        "PendingBlockTicks",
        "PendingFluidTicks"
    );

    private LitematicaImporter() {
    }

    public static boolean isLitematicFile(String fileName) {
        return fileName.toLowerCase(Locale.ROOT).endsWith(".litematic");
    }

    /** 转换结果:原版结构语义的 NBT(含 DataVersion)与转换阶段警告。 */
    public record ConvertedStructure(CompoundTag structureTag, List<StructureSnapshotCodec.BlueprintWarning> warnings) {
    }

    public static ConvertedStructure convert(CompoundTag litematic) throws ConstructionBlueprintException {
        if (!(litematic.get("Regions") instanceof CompoundTag)) {
            throw new ConstructionBlueprintException("corrupt_litematic", "missing Regions");
        }
        CompoundTag regions = litematic.getCompoundOrEmpty("Regions");
        if (regions.isEmpty()) {
            throw new ConstructionBlueprintException("corrupt_litematic", "empty Regions");
        }
        List<StructureSnapshotCodec.BlueprintWarning> warnings = new ArrayList<>();

        List<ParsedRegion> parsedRegions = new ArrayList<>();
        for (String regionName : regions.keySet().stream().sorted().toList()) {
            parsedRegions.add(parseRegion(regionName, regions.getCompoundOrEmpty(regionName), warnings));
        }

        // 全体区域的包围盒并集决定蓝图尺寸,所有坐标平移到并集最小角。
        BlockPos min = parsedRegions.getFirst().min();
        BlockPos max = parsedRegions.getFirst().max();
        for (ParsedRegion region : parsedRegions) {
            min = new BlockPos(
                Math.min(min.getX(), region.min().getX()),
                Math.min(min.getY(), region.min().getY()),
                Math.min(min.getZ(), region.min().getZ())
            );
            max = new BlockPos(
                Math.max(max.getX(), region.max().getX()),
                Math.max(max.getY(), region.max().getY()),
                Math.max(max.getZ(), region.max().getZ())
            );
        }
        Vec3i size = new Vec3i(max.getX() - min.getX() + 1, max.getY() - min.getY() + 1, max.getZ() - min.getZ() + 1);
        long volume = (long) size.getX() * size.getY() * size.getZ();
        // 体积必须先为正再谈上限:损坏文件里 Integer.MIN_VALUE 之类的尺寸取绝对值仍是负数,
        // 只比上限会直接放行,随后按负长度分配数组当场崩掉
        if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0 || volume <= 0L) {
            throw new ConstructionBlueprintException(
                "corrupt_size",
                size.getX() + "x" + size.getY() + "x" + size.getZ()
            );
        }
        if (size.getX() > StructureSnapshotCodec.MAX_AXIS
            || size.getY() > StructureSnapshotCodec.MAX_AXIS
            || size.getZ() > StructureSnapshotCodec.MAX_AXIS
            || volume > MAX_VOLUME) {
            throw new ConstructionBlueprintException(
                "oversized",
                size.getX() + "x" + size.getY() + "x" + size.getZ()
            );
        }

        // 输出调色板:空气固定占第 0 位,区域调色板逐项映射。
        List<CompoundTag> paletteOut = new ArrayList<>();
        CompoundTag airState = new CompoundTag();
        airState.putString("Name", "minecraft:air");
        paletteOut.add(airState);

        int[] stateByPosition = new int[(int) volume];
        CompoundTag[] nbtByPosition = new CompoundTag[(int) volume];
        boolean[] covered = new boolean[(int) volume];
        int overlapCount = 0;

        ListTag entitiesOut = new ListTag();
        List<BlueprintTicks.Entry> ticks = new ArrayList<>();
        for (ParsedRegion region : parsedRegions) {
            int[] paletteMap = new int[region.palette().size()];
            for (int index = 0; index < region.palette().size(); index++) {
                paletteMap[index] = internState(paletteOut, region.palette().getCompoundOrEmpty(index));
            }
            overlapCount += region.blit(min, size, paletteMap, stateByPosition, nbtByPosition, covered);
            region.appendEntities(min, entitiesOut);
            for (boolean fluid : List.of(false, true)) {
                ListTag saved = BlueprintNbt.list(regions.getCompoundOrEmpty(region.name()),
                    fluid ? "PendingFluidTicks" : "PendingBlockTicks", Tag.TAG_COMPOUND);
                for (Tag value : saved) {
                    CompoundTag tick = (CompoundTag) value;
                    BlockPos pos = new BlockPos(tick.getIntOr("x", 0), tick.getIntOr("y", 0), tick.getIntOr("z", 0))
                        .offset(region.min()).subtract(min);
                    ticks.add(new BlueprintTicks.Entry(pos,
                        Identifier.parse(tick.getStringOr(fluid ? "Fluid" : "Block", "")),
                        (int) Math.clamp(tick.getLongOr("Time", 0L), 0, Integer.MAX_VALUE),
                        tick.getIntOr("Priority", 0), fluid, tick.getLongOr("SubTick", 0L)));
                }
            }
        }
        if (overlapCount > 0) {
            warnings.add(new StructureSnapshotCodec.BlueprintWarning(
                "overlapping_regions",
                String.valueOf(overlapCount)
            ));
        }

        CompoundTag out = new CompoundTag();
        out.putInt("DataVersion", litematic.getIntOr("MinecraftDataVersion", 0));
        ListTag sizeTag = new ListTag();
        sizeTag.add(IntTag.valueOf(size.getX()));
        sizeTag.add(IntTag.valueOf(size.getY()));
        sizeTag.add(IntTag.valueOf(size.getZ()));
        out.put("size", sizeTag);
        ListTag paletteTag = new ListTag();
        paletteTag.addAll(paletteOut);
        out.put("palette", paletteTag);

        ListTag blocksTag = new ListTag();
        int positionIndex = 0;
        for (int y = 0; y < size.getY(); y++) {
            for (int z = 0; z < size.getZ(); z++) {
                for (int x = 0; x < size.getX(); x++, positionIndex++) {
                    final CompoundTag blockTag = new CompoundTag();
                    ListTag posTag = new ListTag();
                    posTag.add(IntTag.valueOf(x));
                    posTag.add(IntTag.valueOf(y));
                    posTag.add(IntTag.valueOf(z));
                    blockTag.put("pos", posTag);
                    blockTag.putInt("state", stateByPosition[positionIndex]);
                    if (nbtByPosition[positionIndex] != null) {
                        blockTag.put("nbt", nbtByPosition[positionIndex]);
                    }
                    blocksTag.add(blockTag);
                }
            }
        }
        out.put("blocks", blocksTag);
        out.put("entities", entitiesOut);
        BlueprintTicks.write(out, ticks);
        return new ConvertedStructure(out, warnings);
    }

    /** 输出调色板去重:相同状态标签复用同一编号。 */
    private static int internState(List<CompoundTag> paletteOut, CompoundTag state) {
        for (int index = 0; index < paletteOut.size(); index++) {
            if (paletteOut.get(index).equals(state)) return index;
        }
        paletteOut.add(state.copy());
        return paletteOut.size() - 1;
    }

    private record ParsedRegion(
        String name,
        BlockPos origin,
        BlockPos min,
        BlockPos max,
        Vec3i extent,
        ListTag palette,
        long[] packedStates,
        int bitsPerEntry,
        ListTag tileEntities,
        ListTag entities
    ) {
        /** 把区域方块写入并集数组;返回与已覆盖位置重叠的数量。 */
        int blit(
            BlockPos unionMin,
            Vec3i unionSize,
            int[] paletteMap,
            int[] stateByPosition,
            CompoundTag[] nbtByPosition,
            boolean[] covered
        ) throws ConstructionBlueprintException {
            int overlaps = 0;
            long volume = (long) this.extent.getX() * this.extent.getY() * this.extent.getZ();
            for (long index = 0; index < volume; index++) {
                int localX = (int) (index % this.extent.getX());
                int localZ = (int) ((index / this.extent.getX()) % this.extent.getZ());
                int localY = (int) (index / ((long) this.extent.getX() * this.extent.getZ()));
                int paletteIndex = readPacked(this.packedStates, this.bitsPerEntry, index, this.name);
                if (paletteIndex >= paletteMap.length) {
                    throw new ConstructionBlueprintException(
                        "corrupt_litematic",
                        "state index " + paletteIndex + " outside palette in region " + this.name
                    );
                }
                int unionX = this.min.getX() - unionMin.getX() + localX;
                int unionY = this.min.getY() - unionMin.getY() + localY;
                int unionZ = this.min.getZ() - unionMin.getZ() + localZ;
                int unionIndex = (unionY * unionSize.getZ() + unionZ) * unionSize.getX() + unionX;
                if (covered[unionIndex]) overlaps++;
                covered[unionIndex] = true;
                stateByPosition[unionIndex] = paletteMap[paletteIndex];
                nbtByPosition[unionIndex] = null;
            }

            for (int index = 0; index < this.tileEntities.size(); index++) {
                CompoundTag tileEntity = this.tileEntities.getCompoundOrEmpty(index).copy();
                int localX = tileEntity.getIntOr("x", 0);
                int localY = tileEntity.getIntOr("y", 0);
                int localZ = tileEntity.getIntOr("z", 0);
                if (localX < 0 || localY < 0 || localZ < 0
                    || localX >= this.extent.getX() || localY >= this.extent.getY() || localZ >= this.extent.getZ()) {
                    throw new ConstructionBlueprintException(
                        "corrupt_litematic",
                        "tile entity outside region " + this.name
                    );
                }
                tileEntity.remove("x");
                tileEntity.remove("y");
                tileEntity.remove("z");
                int unionX = this.min.getX() - unionMin.getX() + localX;
                int unionY = this.min.getY() - unionMin.getY() + localY;
                int unionZ = this.min.getZ() - unionMin.getZ() + localZ;
                nbtByPosition[(unionY * unionSize.getZ() + unionZ) * unionSize.getX() + unionX] = tileEntity;
            }
            return overlaps;
        }

        /** 实体 Pos 相对区域 Position(选区角点,可为任意角),平移到并集局部坐标。 */
        void appendEntities(BlockPos unionMin, ListTag entitiesOut) {
            for (int index = 0; index < this.entities.size(); index++) {
                CompoundTag entity = this.entities.getCompoundOrEmpty(index).copy();
                ListTag posTag = BlueprintNbt.list(entity, "Pos", Tag.TAG_DOUBLE);
                if (posTag.size() != 3) continue;
                double x = posTag.getDoubleOr(0, 0.0) + this.origin.getX() - unionMin.getX();
                double y = posTag.getDoubleOr(1, 0.0) + this.origin.getY() - unionMin.getY();
                double z = posTag.getDoubleOr(2, 0.0) + this.origin.getZ() - unionMin.getZ();
                ListTag newPos = new ListTag();
                newPos.add(DoubleTag.valueOf(x));
                newPos.add(DoubleTag.valueOf(y));
                newPos.add(DoubleTag.valueOf(z));
                entity.put("Pos", newPos);

                final CompoundTag entry = new CompoundTag();
                ListTag entryPos = new ListTag();
                entryPos.add(DoubleTag.valueOf(x));
                entryPos.add(DoubleTag.valueOf(y));
                entryPos.add(DoubleTag.valueOf(z));
                entry.put("pos", entryPos);
                ListTag entryBlockPos = new ListTag();
                entryBlockPos.add(IntTag.valueOf((int) Math.floor(x)));
                entryBlockPos.add(IntTag.valueOf((int) Math.floor(y)));
                entryBlockPos.add(IntTag.valueOf((int) Math.floor(z)));
                entry.put("blockPos", entryBlockPos);
                entry.put("nbt", entity);
                entitiesOut.add(entry);
            }
        }
    }

    private static ParsedRegion parseRegion(
        String name,
        CompoundTag region,
        List<StructureSnapshotCodec.BlueprintWarning> warnings
    ) throws ConstructionBlueprintException {
        if (!(region.get("Position") instanceof CompoundTag) || !(region.get("Size") instanceof CompoundTag)) {
            throw new ConstructionBlueprintException("corrupt_litematic", "region " + name + " missing bounds");
        }
        BlockPos position = readVec(region.getCompoundOrEmpty("Position"));
        BlockPos rawSize = readVec(region.getCompoundOrEmpty("Size"));
        if (rawSize.getX() == 0 || rawSize.getY() == 0 || rawSize.getZ() == 0) {
            throw new ConstructionBlueprintException("corrupt_litematic", "region " + name + " has zero size");
        }
        // 负尺寸轴表示区域向负方向延伸,最小角是 Position 加上(尺寸+1)。
        BlockPos min = new BlockPos(
            position.getX() + Math.min(rawSize.getX() + 1, 0),
            position.getY() + Math.min(rawSize.getY() + 1, 0),
            position.getZ() + Math.min(rawSize.getZ() + 1, 0)
        );
        Vec3i extent = new Vec3i(Math.abs(rawSize.getX()), Math.abs(rawSize.getY()), Math.abs(rawSize.getZ()));
        // Math.abs(Integer.MIN_VALUE) 仍是负数,区域尺寸必须逐轴确认为正再算体积
        if (extent.getX() <= 0 || extent.getY() <= 0 || extent.getZ() <= 0) {
            throw new ConstructionBlueprintException(
                "corrupt_litematic",
                "region " + name + " has invalid size "
                    + rawSize.getX() + "x" + rawSize.getY() + "x" + rawSize.getZ()
            );
        }
        final BlockPos max = new BlockPos(
            min.getX() + extent.getX() - 1,
            min.getY() + extent.getY() - 1,
            min.getZ() + extent.getZ() - 1
        );
        long volume = (long) extent.getX() * extent.getY() * extent.getZ();
        if (volume > MAX_VOLUME) {
            throw new ConstructionBlueprintException("oversized", "region " + name + " volume " + volume);
        }

        ListTag palette = BlueprintNbt.list(region, "BlockStatePalette", Tag.TAG_COMPOUND);
        if (palette.isEmpty()) {
            throw new ConstructionBlueprintException("corrupt_litematic", "region " + name + " has empty palette");
        }
        if (!(region.get("BlockStates") instanceof LongArrayTag packed)) {
            throw new ConstructionBlueprintException("corrupt_litematic", "region " + name + " missing BlockStates");
        }
        int bitsPerEntry = Math.max(2, 32 - Integer.numberOfLeadingZeros(palette.size() - 1));
        long requiredBits = volume * bitsPerEntry;
        long availableBits = (long) packed.getAsLongArray().length * 64L;
        if (availableBits < requiredBits) {
            throw new ConstructionBlueprintException(
                "corrupt_litematic",
                "region " + name + " BlockStates too short: " + availableBits + " < " + requiredBits
            );
        }

        for (String key : region.keySet()) {
            if (!IGNORABLE_REGION_KEYS.contains(key)) {
                warnings.add(new StructureSnapshotCodec.BlueprintWarning("unmapped_fields", name + "." + key));
            }
        }

        return new ParsedRegion(
            name,
            position,
            min,
            max,
            extent,
            palette,
            packed.getAsLongArray(),
            bitsPerEntry,
            BlueprintNbt.list(region, "TileEntities", Tag.TAG_COMPOUND),
            BlueprintNbt.list(region, "Entities", Tag.TAG_COMPOUND)
        );
    }

    private static BlockPos readVec(CompoundTag tag) {
        return new BlockPos(tag.getIntOr("x", 0), tag.getIntOr("y", 0), tag.getIntOr("z", 0));
    }

    /** Litematica 的紧凑位压缩:条目跨越 long 边界连续存放,与原版分段存储不同。 */
    private static int readPacked(long[] longs, int bitsPerEntry, long index, String regionName)
        throws ConstructionBlueprintException {
        long startOffset = index * bitsPerEntry;
        int startIndex = (int) (startOffset >> 6);
        int endIndex = (int) ((startOffset + bitsPerEntry - 1) >> 6);
        int startBit = (int) (startOffset & 63);
        long mask = (1L << bitsPerEntry) - 1L;
        if (endIndex >= longs.length) {
            throw new ConstructionBlueprintException("corrupt_litematic", "BlockStates truncated in " + regionName);
        }
        long value = longs[startIndex] >>> startBit;
        if (startIndex != endIndex) {
            value |= longs[endIndex] << (64 - startBit);
        }
        return (int) (value & mask);
    }
}
