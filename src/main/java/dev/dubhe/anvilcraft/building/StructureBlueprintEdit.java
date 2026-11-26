package dev.dubhe.anvilcraft.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

/** 编辑蓝图快照，不修改扫描来源世界中的方块。 */
public final class StructureBlueprintEdit {
    private StructureBlueprintEdit() {
    }

    /** 标记物品只轮换名为 facing 的方向属性，保留其他状态和物品组件。 */
    public static boolean cycleFacing(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item)) {
            return false;
        }
        var properties = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        BlockState state = properties.apply(item.getBlock().defaultBlockState());
        Property<?> facing = state.getBlock().getStateDefinition().getProperty("facing");
        if (facing == null || !(state.getValue(facing) instanceof Direction)) {
            return false;
        }
        cycleFacing(stack, state, facing, properties);
        return true;
    }

    private static <T extends Comparable<T>> void cycleFacing(
        ItemStack stack, BlockState state, Property<T> facing, BlockItemStateProperties properties
    ) {
        stack.set(DataComponents.BLOCK_STATE, properties.with(facing, state.cycle(facing).getValue(facing)));
    }

    /** 单个替换按坐标定位，批量替换只匹配完整方块状态。 */
    public static StructureSnapshot replace(StructureSnapshot snapshot, BlockPos selected, BlockState replacement, boolean all) {
        var source = snapshot.blocks().stream().filter(entry -> entry.pos().equals(selected)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Selected blueprint block is unavailable"));
        BlockState original = snapshot.stateOf(source);
        if (original == replacement) {
            return snapshot;
        }
        var palette = new ArrayList<>(snapshot.palette());
        int replacementIndex = palette.indexOf(replacement);
        if (replacementIndex < 0) {
            replacementIndex = palette.size();
            palette.add(replacement);
        }
        var blocks = new ArrayList<StructureSnapshot.BlockEntry>();
        var changed = new HashSet<BlockPos>();
        for (var entry : snapshot.blocks()) {
            if (entry.pos().equals(selected) || all && snapshot.stateOf(entry) == original) {
                BlockState oldPart = entry.replacedPart().orElse(snapshot.stateOf(entry));
                Optional<BlockState> replacedPart = replacement != oldPart && BlueprintMultiblocks.hasParts(oldPart)
                    ? Optional.of(oldPart) : Optional.empty();
                blocks.add(new StructureSnapshot.BlockEntry(entry.pos(), replacementIndex, Optional.empty(), replacedPart));
                changed.add(entry.pos());
            } else {
                blocks.add(entry);
            }
        }
        return StructureSnapshotCodec.canonicalize(new StructureSnapshot(snapshot.size(), palette, blocks, snapshot.entities(),
            snapshot.ticks().stream().filter(tick -> !changed.contains(tick.pos())).toList(), snapshot.capturedAt()));
    }

    /** 宽度围绕扫描仪变化，深度向前延伸，高度从扫描仪所在的底面或顶面变化。 */
    public static StructureSnapshot resize(StructureSnapshot snapshot, Vec3i size, boolean upsideDown) {
        return resize(snapshot, snapshot.size(), size, BlockPos.ZERO, upsideDown);
    }

    /** 扩展结构的偏移以原扫描选区为基准，裁剪后转为新选区内的局部坐标。 */
    public static StructureSnapshot resize(
        StructureSnapshot snapshot, Vec3i previousSize, Vec3i size, BlockPos previewOffset, boolean upsideDown
    ) {
        if (size.getX() < 1 || size.getY() < 1 || size.getZ() < 1
            || size.getX() > StructureSnapshotCodec.MAX_AXIS || size.getY() > StructureSnapshotCodec.MAX_AXIS
            || size.getZ() > StructureSnapshotCodec.MAX_AXIS) {
            throw new IllegalArgumentException("Blueprint size must be between 1 and 16 on each axis");
        }
        BlockPos shift = previewOffset.offset(size.getX() / 2 - previousSize.getX() / 2,
            upsideDown ? size.getY() - previousSize.getY() : 0, 0);
        return transform(snapshot, new BlueprintPlacement(shift, Rotation.NONE, Mirror.NONE), size, true);
    }

    /** 将扫描结果转为预览中朝北的坐标，后续编辑使用与导入蓝图相同的坐标系。 */
    public static StructureSnapshot rotate(StructureSnapshot snapshot, Rotation rotation) {
        BlueprintPlacement origin = new BlueprintPlacement(BlockPos.ZERO, rotation, Mirror.NONE);
        var bounds = origin.bounds(snapshot.size());
        BlockPos anchor = new BlockPos(-bounds.minX(), -bounds.minY(), -bounds.minZ());
        return transform(snapshot, new BlueprintPlacement(anchor, rotation, Mirror.NONE),
            new Vec3i(bounds.getXSpan(), bounds.getYSpan(), bounds.getZSpan()), false);
    }

    private static StructureSnapshot transform(StructureSnapshot snapshot, BlueprintPlacement placement, Vec3i size, boolean crop) {
        var sourceOrigin = BlueprintBlockConfiguration.sourceOrigin(snapshot);
        var blocks = new ArrayList<StructureSnapshot.BlockEntry>();
        for (var entry : snapshot.blocks()) {
            BlockPos pos = placement.worldOf(entry.pos());
            if (crop && !contains(size, pos)) {
                continue;
            }
            var nbt = entry.nbt().map(tag -> {
                var copy = tag.copy();
                BlueprintBlockConfiguration.transform(copy, placement, sourceOrigin);
                copy.putInt("x", pos.getX());
                copy.putInt("y", pos.getY());
                copy.putInt("z", pos.getZ());
                return copy;
            });
            blocks.add(new StructureSnapshot.BlockEntry(pos, entry.stateIndex(), nbt, entry.replacedPart().map(placement::stateOf)));
        }
        var entities = new ArrayList<StructureSnapshot.EntityEntry>();
        for (var entry : snapshot.entities()) {
            Vec3 pos = placement.localOf(entry.pos(), entry.blockPos()).add(Vec3.atLowerCornerOf(placement.anchor()));
            BlockPos blockPos = placement.worldOf(entry.blockPos());
            if (!crop || contains(size, blockPos)) {
                entities.add(new StructureSnapshot.EntityEntry(pos, blockPos, BuildingEntityTransform.transform(entry, placement)));
            }
        }
        return StructureSnapshotCodec.canonicalize(new StructureSnapshot(size,
            snapshot.palette().stream().map(placement::stateOf).toList(), blocks, entities,
            snapshot.ticks().stream().map(tick -> tick.at(placement.worldOf(tick.pos())))
                .filter(tick -> !crop || contains(size, tick.pos())).toList(), snapshot.capturedAt()));
    }

    private static boolean contains(Vec3i size, BlockPos pos) {
        return pos.getX() >= 0 && pos.getX() < size.getX() && pos.getY() >= 0 && pos.getY() < size.getY()
            && pos.getZ() >= 0 && pos.getZ() < size.getZ();
    }
}
