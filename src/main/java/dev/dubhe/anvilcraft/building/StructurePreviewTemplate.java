package dev.dubhe.anvilcraft.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 将扫描仪预览坐标转换为手册共用的原版结构模板，保留方块实体的完整配置。 */
public record StructurePreviewTemplate(StructureTemplate template, BlockPos minimum) {
    public static StructurePreviewTemplate create(
        @Nullable StructureSnapshot snapshot, BlueprintPlacement placement, AABB bounds,
        @Nullable BlockPos scannerPos, @Nullable BlockState scannerState, HolderLookup.Provider registries
    ) {
        BlockPos minimum = BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ);
        Vec3i size = new Vec3i((int) Math.ceil(bounds.maxX) - minimum.getX(),
            (int) Math.ceil(bounds.maxY) - minimum.getY(), (int) Math.ceil(bounds.maxZ) - minimum.getZ());
        BlueprintPlacement local = new BlueprintPlacement(placement.anchor().subtract(minimum), placement.rotation(), placement.mirror());
        var palette = new ArrayList<BlockState>();
        var blocks = new ArrayList<StructureSnapshot.BlockEntry>();
        var entities = new ArrayList<StructureSnapshot.EntityEntry>();
        if (snapshot != null) {
            palette.addAll(snapshot.palette().stream().map(local::stateOf).toList());
            BlockPos sourceOrigin = BlueprintBlockConfiguration.sourceOrigin(snapshot);
            for (var entry : snapshot.blocks()) {
                BlockPos pos = local.worldOf(entry.pos());
                var data = entry.nbt().map(tag -> {
                    var copy = tag.copy();
                    BlueprintBlockConfiguration.transform(copy, local, sourceOrigin);
                    copy.putInt("x", pos.getX());
                    copy.putInt("y", pos.getY());
                    copy.putInt("z", pos.getZ());
                    return copy;
                });
                blocks.add(new StructureSnapshot.BlockEntry(pos, entry.stateIndex(), data));
            }
            for (var entry : snapshot.entities()) {
                Vec3 pos = local.localOf(entry.pos(), entry.blockPos()).add(Vec3.atLowerCornerOf(local.anchor()));
                entities.add(new StructureSnapshot.EntityEntry(pos, local.worldOf(entry.blockPos()),
                    BuildingEntityTransform.transform(entry, local)));
            }
        }
        if (scannerPos != null && scannerState != null) {
            BlockPos localScanner = scannerPos.subtract(minimum);
            if (blocks.stream().noneMatch(entry -> entry.pos().equals(localScanner))) {
                blocks.add(new StructureSnapshot.BlockEntry(localScanner, palette.size(), Optional.empty()));
                palette.add(scannerState);
            }
        }
        var structure = StructureSnapshotCodec.canonicalize(new StructureSnapshot(size, palette, blocks, entities, List.of(), 0));
        StructureTemplate template = new StructureTemplate();
        template.load(registries.lookupOrThrow(Registries.BLOCK), StructureSnapshotCodec.write(structure));
        return new StructurePreviewTemplate(template, minimum);
    }
}
