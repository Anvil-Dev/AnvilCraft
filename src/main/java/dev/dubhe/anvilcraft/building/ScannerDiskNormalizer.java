package dev.dubhe.anvilcraft.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** 扫描器坐标使用预览朝向，方块状态使用世界朝向；先还原坐标再统一部署。 */
public final class ScannerDiskNormalizer {
    private ScannerDiskNormalizer() {
    }

    public static StructureSnapshot normalize(StructureSnapshot snapshot, Direction facing, boolean upsideDown) {
        Vec3i size = snapshot.size();
        int sx = size.getX();
        int sy = size.getY();
        int sz = size.getZ();
        Vec3i normalizedSize = facing.getAxis() == Direction.Axis.X
            ? new Vec3i(sz, sy, sx)
            : size;

        List<StructureSnapshot.BlockEntry> blocks = new ArrayList<>(snapshot.blocks().size());
        for (StructureSnapshot.BlockEntry entry : snapshot.blocks()) {
            BlockPos pos = entry.pos();
            int y = upsideDown ? sy - 1 - pos.getY() : pos.getY();
            BlockPos normalized = switch (facing) {
                case SOUTH -> new BlockPos(sx - 1 - pos.getX(), y, sz - 1 - pos.getZ());
                case WEST -> new BlockPos(pos.getZ(), y, sx - 1 - pos.getX());
                case EAST -> new BlockPos(sz - 1 - pos.getZ(), y, pos.getX());
                default -> new BlockPos(pos.getX(), y, pos.getZ());
            };
            blocks.add(new StructureSnapshot.BlockEntry(normalized, entry.stateIndex(), entry.nbt()));
        }

        StructureSnapshot normalizedBlocks = new StructureSnapshot(normalizedSize, snapshot.palette(), blocks, List.of());
        List<StructureSnapshot.EntityEntry> entities = new ArrayList<>(snapshot.entities().size());
        for (StructureSnapshot.EntityEntry entry : snapshot.entities()) {
            Vec3 pos = entry.pos();
            double y = upsideDown ? sy - pos.y : pos.y;
            Vec3 normalized = switch (facing) {
                case SOUTH -> new Vec3(sx - pos.x, y, sz - pos.z);
                case WEST -> new Vec3(pos.z, y, sx - pos.x);
                case EAST -> new Vec3(sz - pos.z, y, pos.x);
                default -> new Vec3(pos.x, y, pos.z);
            };
            BlockPos blockPos = DynamicBuildingEntities.Outlet.isOutlet(entry.nbt())
                ? DynamicBuildingEntities.Outlet.support(entry) : entry.blockPos();
            int blockY = upsideDown ? sy - 1 - blockPos.getY() : blockPos.getY();
            BlockPos normalizedBlockPos = switch (facing) {
                case SOUTH -> new BlockPos(sx - 1 - blockPos.getX(), blockY, sz - 1 - blockPos.getZ());
                case WEST -> new BlockPos(blockPos.getZ(), blockY, sx - 1 - blockPos.getX());
                case EAST -> new BlockPos(sz - 1 - blockPos.getZ(), blockY, blockPos.getX());
                default -> new BlockPos(blockPos.getX(), blockY, blockPos.getZ());
            };
            if (MagnetizedNodeBuildAdapter.isNode(entry.nbt())) {
                normalizedBlockPos = MagnetizedNodeBuildAdapter.support(normalizedBlocks,
                    new StructureSnapshot.EntityEntry(normalized, normalizedBlockPos, entry.nbt()));
            }
            entities.add(new StructureSnapshot.EntityEntry(normalized, normalizedBlockPos, entry.nbt()));
        }

        List<BlueprintTicks.Entry> ticks = snapshot.ticks().stream().map(tick -> {
            BlockPos pos = tick.pos();
            int y = upsideDown ? sy - 1 - pos.getY() : pos.getY();
            BlockPos normalized = switch (facing) {
                case SOUTH -> new BlockPos(sx - 1 - pos.getX(), y, sz - 1 - pos.getZ());
                case WEST -> new BlockPos(pos.getZ(), y, sx - 1 - pos.getX());
                case EAST -> new BlockPos(sz - 1 - pos.getZ(), y, pos.getX());
                default -> new BlockPos(pos.getX(), y, pos.getZ());
            };
            return tick.at(normalized);
        }).toList();
        return new StructureSnapshot(normalizedSize, snapshot.palette(), blocks, entities, ticks, snapshot.capturedAt());
    }
}
