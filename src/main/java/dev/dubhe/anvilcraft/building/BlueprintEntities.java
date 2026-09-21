package dev.dubhe.anvilcraft.building;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
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

    static void relocateMemories(CompoundTag data, StructureSnapshot.EntityEntry entry,
                                 StructureSnapshot snapshot, BlueprintPlacement placement, String dimension) {
        BlockPos origin = BlueprintBlockConfiguration.sourceOrigin(snapshot);
        if (origin == null) {
            ListTag saved = entry.nbt().getList("Pos", Tag.TAG_DOUBLE);
            origin = saved.size() == 3 ? new BlockPos(
                (int) Math.round(saved.getDouble(0) - entry.pos().x),
                (int) Math.round(saved.getDouble(1) - entry.pos().y),
                (int) Math.round(saved.getDouble(2) - entry.pos().z)) : BlockPos.ZERO;
        }
        CompoundTag memories = data.getCompound("Brain").getCompound("memories");
        for (String key : List.copyOf(memories.getAllKeys())) {
            CompoundTag value = memories.getCompound(key).getCompound("value");
            var pos = NbtUtils.readBlockPos(value, "pos");
            if (pos.isEmpty()) continue;
            BlockPos local = pos.get().subtract(origin);
            if (local.getX() < 0 || local.getY() < 0 || local.getZ() < 0 || local.getX() >= snapshot.size().getX()
                || local.getY() >= snapshot.size().getY() || local.getZ() >= snapshot.size().getZ()) {
                memories.remove(key);
            } else {
                value.put("pos", NbtUtils.writeBlockPos(placement.worldOf(local)));
                value.putString("dimension", dimension);
            }
        }
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
        if (!tag.hasUUID("UUID")) {
            tag.putUUID("UUID", UUID.nameUUIDFromBytes((path + entry.pos() + tag).getBytes(StandardCharsets.UTF_8)));
        }
        CompoundTag existing = seen.putIfAbsent(tag.getUUID("UUID"), tag);
        if (existing != null) {
            if (tag.hasUUID(VEHICLE)) existing.putUUID(VEHICLE, tag.getUUID(VEHICLE));
            return;
        }
        ListTag passengers = tag.getList("Passengers", Tag.TAG_COMPOUND);
        tag.remove("Passengers");
        result.add(new StructureSnapshot.EntityEntry(entry.pos(), entry.blockPos(), tag));
        ListTag savedPos = entry.nbt().getList("Pos", Tag.TAG_DOUBLE);
        Vec3 offset = savedPos.size() == 3
            ? entry.pos().subtract(new Vec3(savedPos.getDouble(0), savedPos.getDouble(1), savedPos.getDouble(2))) : Vec3.ZERO;
        for (int i = 0; i < passengers.size(); i++) {
            CompoundTag passenger = passengers.getCompound(i).copy();
            ListTag pos = passenger.getList("Pos", Tag.TAG_DOUBLE);
            Vec3 local = pos.size() == 3
                ? new Vec3(pos.getDouble(0), pos.getDouble(1), pos.getDouble(2)).add(offset) : entry.pos();
            passenger.putUUID(VEHICLE, tag.getUUID("UUID"));
            flatten(new StructureSnapshot.EntityEntry(local, BlockPos.containing(local), passenger),
                result, seen, path + "/" + i, depth + 1);
        }
    }

    static Map<UUID, UUID> identities(StructureSnapshot snapshot, BlueprintPlacement placement) {
        Map<UUID, UUID> ids = new HashMap<>();
        for (var entry : snapshot.entities()) {
            if (!entry.nbt().hasUUID("UUID")) continue;
            UUID old = entry.nbt().getUUID("UUID");
            ids.put(old, UUID.nameUUIDFromBytes((old + ":" + placement.anchor().toShortString()).getBytes(StandardCharsets.UTF_8)));
        }
        return ids;
    }

    static void scope(CompoundTag target, CompoundTag original, Map<UUID, UUID> ids) {
        remap(target, ids);
        if (original.hasUUID("UUID")) target.putUUID(SOURCE, ids.get(original.getUUID("UUID")));
        if (original.hasUUID(VEHICLE)) {
            UUID vehicle = ids.get(original.getUUID(VEHICLE));
            if (vehicle != null) target.putUUID(VEHICLE, vehicle);
        }
    }

    static void link(List<Map.Entry<Entity, CompoundTag>> spawned) {
        Map<UUID, UUID> ids = new HashMap<>();
        Map<UUID, Entity> entities = new HashMap<>();
        for (var pair : spawned) {
            if (!pair.getValue().hasUUID(SOURCE)) continue;
            UUID source = pair.getValue().getUUID(SOURCE);
            ids.put(source, pair.getKey().getUUID());
            entities.put(source, pair.getKey());
        }
        for (var pair : spawned) {
            Entity entity = pair.getKey();
            CompoundTag saved = new CompoundTag();
            entity.saveAsPassenger(saved);
            remap(saved, ids);
            entity.load(saved);
            CompoundTag original = pair.getValue();
            if (!original.hasUUID(VEHICLE)) continue;
            Entity vehicle = entities.get(original.getUUID(VEHICLE));
            if (vehicle != null && vehicle != entity) entity.startRiding(vehicle, true);
        }
    }

    private static void remap(CompoundTag tag, Map<UUID, UUID> ids) {
        for (String key : List.copyOf(tag.getAllKeys())) {
            Tag value = tag.get(key);
            if (value instanceof IntArrayTag array && array.getAsIntArray().length == 4) {
                UUID target = ids.get(NbtUtils.loadUUID(array));
                if (target != null) tag.putUUID(key, target);
            } else if (value instanceof CompoundTag compound) {
                remap(compound, ids);
            } else if (value instanceof ListTag list) {
                for (Tag element : list) {
                    if (element instanceof CompoundTag compound) remap(compound, ids);
                }
            }
        }
    }
}
