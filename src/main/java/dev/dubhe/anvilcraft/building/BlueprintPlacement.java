package dev.dubhe.anvilcraft.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 蓝图放置参数与坐标变换。快照局部坐标以零点为枢轴按原版 {@link StructureTemplate}
 * 语义镜像和旋转,再平移到锚点;方块状态使用原版 mirror/rotate,保证与最终提交一致。
 */
public record BlueprintPlacement(BlockPos anchor, Rotation rotation, Mirror mirror) {
    public static Rotation facingPlayer(Direction source, Direction playerFacing) {
        Direction front = source.getAxis().isHorizontal() ? source : Direction.NORTH;
        for (Rotation candidate : Rotation.values()) {
            if (candidate.rotate(front) == playerFacing.getOpposite()) return candidate;
        }
        return Rotation.NONE;
    }

    /** 快照局部方块坐标经镜像与旋转后的局部坐标,不含锚点平移。 */
    @SuppressWarnings("deprecation")
    public BlockPos localOf(BlockPos snapshotLocal) {
        return StructureTemplate.transform(snapshotLocal, this.mirror, this.rotation, BlockPos.ZERO);
    }

    /**
     * 快照局部连续坐标经镜像与旋转后的局部坐标,不含锚点平移。
     * 先变换所在方块再变换格内小数,使矿车等实体仍落在对应方块格子里;
     * 原版 {@link StructureTemplate#transform(Vec3, Mirror, Rotation, BlockPos)} 绕原点直接变换会把格内偏移甩到邻格。
     */
    public Vec3 localOf(Vec3 snapshotLocal) {
        return this.localOf(snapshotLocal, BlockPos.containing(snapshotLocal));
    }

    /**
     * 以快照记录的方块格为格子原点变换格内小数,使矿车跟它所在的轨道格一起走。
     */
    public Vec3 localOf(Vec3 snapshotLocal, BlockPos snapshotBlock) {
        double fx = snapshotLocal.x - snapshotBlock.getX();
        double fy = snapshotLocal.y - snapshotBlock.getY();
        double fz = snapshotLocal.z - snapshotBlock.getZ();
        if (this.mirror == Mirror.FRONT_BACK) {
            fx = 1.0D - fx;
        } else if (this.mirror == Mirror.LEFT_RIGHT) {
            fz = 1.0D - fz;
        }
        double rx = fx;
        double rz = fz;
        switch (this.rotation) {
            case CLOCKWISE_90 -> {
                rx = 1.0D - fz;
                rz = fx;
            }
            case CLOCKWISE_180 -> {
                rx = 1.0D - fx;
                rz = 1.0D - fz;
            }
            case COUNTERCLOCKWISE_90 -> {
                rx = fz;
                rz = 1.0D - fx;
            }
            default -> {
            }
        }
        BlockPos transformed = this.localOf(snapshotBlock);
        return new Vec3(transformed.getX() + rx, transformed.getY() + fy, transformed.getZ() + rz);
    }

    /** 与 {@code Axis.YP.rotationDegrees} 同号的结构偏航,供投影姿态插值。 */
    public float yawDegrees() {
        return switch (this.rotation) {
            case NONE -> 0.0F;
            case CLOCKWISE_90 -> -90.0F;
            case CLOCKWISE_180 -> 180.0F;
            case COUNTERCLOCKWISE_90 -> 90.0F;
        };
    }

    public float mirrorX() {
        return this.mirror == Mirror.FRONT_BACK ? -1.0F : 1.0F;
    }

    public float mirrorZ() {
        return this.mirror == Mirror.LEFT_RIGHT ? -1.0F : 1.0F;
    }

    /** 快照局部方块坐标对应的世界坐标。 */
    public BlockPos worldOf(BlockPos local) {
        return this.localOf(local).offset(this.anchor);
    }

    /** 快照局部连续坐标对应的世界坐标,用于实体条目。 */
    public Vec3 worldOf(Vec3 local) {
        Vec3 transformed = this.localOf(local);
        return transformed.add(this.anchor.getX(), this.anchor.getY(), this.anchor.getZ());
    }

    /** 应用镜像与旋转后的方块状态。 */
    public BlockState stateOf(BlockState state) {
        return state.mirror(this.mirror).rotate(this.rotation);
    }

    /** 放置后整个蓝图的世界包围盒。 */
    public BoundingBox bounds(Vec3i size) {
        BlockPos first = this.worldOf(BlockPos.ZERO);
        BlockPos second = this.worldOf(new BlockPos(size.getX() - 1, size.getY() - 1, size.getZ() - 1));
        return BoundingBox.fromCorners(first, second);
    }

    public List<BlueprintPlacement> tile(StructureSnapshot snapshot, BlockPos last) {
        BoundingBox bounds = this.bounds(snapshot.size());
        int width = bounds.getXSpan();
        int height = bounds.getYSpan();
        int depth = bounds.getZSpan();
        long countX = Math.abs((long) last.getX() - this.anchor.getX()) / width + 1;
        long countY = Math.abs((long) last.getY() - this.anchor.getY()) / height + 1;
        long countZ = Math.abs((long) last.getZ() - this.anchor.getZ()) / depth + 1;
        if (countX > BuildingRodService.MAX_BLOCKS || countY > BuildingRodService.MAX_BLOCKS
            || countZ > BuildingRodService.MAX_BLOCKS) return List.of();
        long count = countX * countY * countZ;
        long entries = Math.max(1L, (long) snapshot.nonAirBlockCount() + snapshot.entities().size());
        if (count > 1 && count * entries > BuildingRodService.MAX_BLOCKS) return List.of();
        int signX = Integer.compare(last.getX(), this.anchor.getX());
        int signY = Integer.compare(last.getY(), this.anchor.getY());
        int signZ = Integer.compare(last.getZ(), this.anchor.getZ());
        List<BlueprintPlacement> placements = new ArrayList<>((int) count);
        for (int x = 0; x < countX; x++) {
            for (int y = 0; y < countY; y++) {
                for (int z = 0; z < countZ; z++) {
                    placements.add(new BlueprintPlacement(this.anchor.offset(
                        x * width * signX, y * height * signY, z * depth * signZ), this.rotation, this.mirror));
                }
            }
        }
        return List.copyOf(placements);
    }
}
