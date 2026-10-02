package dev.dubhe.anvilcraft.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BlueprintCapture {
    private BlueprintCapture() {
    }

    @Nullable
    public static CompoundTag captureEntity(Entity entity) {
        CompoundTag tag = saveEntity(entity);
        if (tag == null) return null;
        tag.remove("Passengers");
        if (entity.getVehicle() != null) tag.store("anvilcraft:vehicle", UUIDUtil.CODEC, entity.getVehicle().getUUID());
        return tag;
    }

    static @Nullable CompoundTag saveEntity(Entity entity) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.level().registryAccess());
        if (entity.saveAsPassenger(output)) return output.buildResult();
        if (!(entity instanceof LeashFenceKnotEntity)) return null;
        output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.level().registryAccess());
        entity.saveWithoutId(output);
        CompoundTag tag = output.buildResult();
        tag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
        return tag;
    }

    public static BlueprintNormalizer.Result capture(ServerLevel level, AABB area) {
        BlockPos origin = BlockPos.containing(area.minX, area.minY, area.minZ);
        BlockPos end = BlockPos.containing(area.maxX - 1, area.maxY - 1, area.maxZ - 1);
        Map<BlockPos, StructureSnapshot.BlockEntry> blocks = new LinkedHashMap<>();
        List<BlockState> palette = new ArrayList<>();
        for (BlockPos cursor : BlockPos.betweenClosed(origin, end)) {
            BlockPos pos = cursor.immutable();
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || !BlueprintMultiblocks.shouldRecord(state)) continue;
            BlueprintMultiblocks.forEachPart(pos, state, (part, generated) -> {
                BlockState actual = level.getBlockState(part);
                if (actual.isAir()) actual = generated;
                int index = palette.indexOf(actual);
                if (index < 0) {
                    index = palette.size();
                    palette.add(actual);
                }
                var entity = level.getBlockEntity(part);
                blocks.put(part, new StructureSnapshot.BlockEntry(part.subtract(origin), index,
                    Optional.ofNullable(entity).map(value -> value.saveWithFullMetadata(level.registryAccess()))));
            });
        }
        List<StructureSnapshot.EntityEntry> entities = new ArrayList<>();
        for (Entity entity : level.getEntities((Entity) null, area, entity -> !(entity instanceof Player))) {
            CompoundTag tag = captureEntity(entity);
            if (tag == null) continue;
            Vec3 pos = entity.position().subtract(Vec3.atLowerCornerOf(origin));
            BlockPos block = entity instanceof Painting painting ? painting.getPos().subtract(origin) : BlockPos.containing(pos);
            entities.add(new StructureSnapshot.EntityEntry(pos, block, tag));
        }
        List<BlueprintTicks.Entry> ticks = BlueprintTicks.capture(level, BoundingBox.fromCorners(origin, end)).stream()
            .map(tick -> tick.at(tick.pos().subtract(origin))).toList();
        StructureSnapshot raw = new StructureSnapshot(new Vec3i(end.getX() - origin.getX() + 1,
            end.getY() - origin.getY() + 1, end.getZ() - origin.getZ() + 1), palette, new ArrayList<>(blocks.values()),
            entities, ticks, level.getGameTime());
        return BlueprintNormalizer.normalize(ScannerDiskNormalizer.normalize(raw, Direction.NORTH, false));
    }
}
