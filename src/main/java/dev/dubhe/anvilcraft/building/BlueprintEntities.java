package dev.dubhe.anvilcraft.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class BlueprintEntities {
    static final String SOURCE = "anvilcraft:source_entity";
    static final String VEHICLE = "anvilcraft:vehicle";

    private BlueprintEntities() {
    }

    static List<StructureSnapshot.EntityEntry> flatten(List<StructureSnapshot.EntityEntry> entries) {
        List<StructureSnapshot.EntityEntry> result = new ArrayList<>();
        Map<UUID, CompoundTag> seen = new HashMap<>();
        for (int i = 0; i < entries.size(); i++) flatten(entries.get(i), result, seen, "entity:" + i, 0);
        return result;
    }

    private static void flatten(StructureSnapshot.EntityEntry entry, List<StructureSnapshot.EntityEntry> result,
                                Map<UUID, CompoundTag> seen, String path, int depth) {
        if (depth > 32 || result.size() >= StructureSnapshotCodec.MAX_ENTITY_ENTRIES) {
            throw new IllegalArgumentException("Too many blueprint passengers");
        }
        CompoundTag tag = entry.nbt().copy();
        if (tag.read("UUID", UUIDUtil.CODEC).isEmpty()) {
            tag.store("UUID", UUIDUtil.CODEC, UUID.nameUUIDFromBytes((path + entry.pos() + tag).getBytes(StandardCharsets.UTF_8)));
        }
        CompoundTag existing = seen.putIfAbsent(tag.read("UUID", UUIDUtil.CODEC).orElseThrow(), tag);
        if (existing != null) {
            tag.read(VEHICLE, UUIDUtil.CODEC).ifPresent(vehicle -> existing.store(VEHICLE, UUIDUtil.CODEC, vehicle));
            return;
        }
        ListTag passengers = BlueprintNbt.list(tag, "Passengers", Tag.TAG_COMPOUND);
        tag.remove("Passengers");
        result.add(new StructureSnapshot.EntityEntry(entry.pos(), entry.blockPos(), tag));
        ListTag savedPos = BlueprintNbt.list(entry.nbt(), "Pos", Tag.TAG_DOUBLE);
        Vec3 offset = savedPos.size() == 3
            ? entry.pos().subtract(new Vec3(savedPos.getDoubleOr(0, 0.0), savedPos.getDoubleOr(1, 0.0), savedPos.getDoubleOr(2, 0.0)))
            : Vec3.ZERO;
        for (int i = 0; i < passengers.size(); i++) {
            CompoundTag passenger = passengers.getCompoundOrEmpty(i).copy();
            ListTag pos = BlueprintNbt.list(passenger, "Pos", Tag.TAG_DOUBLE);
            Vec3 local = pos.size() == 3
                ? new Vec3(pos.getDoubleOr(0, 0.0), pos.getDoubleOr(1, 0.0), pos.getDoubleOr(2, 0.0)).add(offset) : entry.pos();
            passenger.store(VEHICLE, UUIDUtil.CODEC, tag.read("UUID", UUIDUtil.CODEC).orElseThrow());
            flatten(new StructureSnapshot.EntityEntry(local, BlockPos.containing(local), passenger),
                result, seen, path + "/" + i, depth + 1);
        }
    }

}
