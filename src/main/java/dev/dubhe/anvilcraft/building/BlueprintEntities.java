package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.item.block.HasMobBlockItem;
import dev.dubhe.anvilcraft.item.block.ResinBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class BlueprintEntities {
    private static final double MAX_SURVIVAL_SPEED = 15.9;
    static final String SOURCE = "anvilcraft:source_entity";
    static final String VEHICLE = "anvilcraft:vehicle";

    private BlueprintEntities() {
    }

    static EntityBuildAdapter.@Nullable Planned fromMaterial(ServerPlayer player, ItemStack material, EntityBuildAdapter.Planned plan) {
        Entity entity;
        if (material.getItem() instanceof ResinBlockItem) {
            entity = HasMobBlockItem.getMobFromItem(player.level(), material);
            if (entity != null && material.has(DataComponents.CUSTOM_NAME)) {
                entity.setCustomName(material.get(DataComponents.CUSTOM_NAME));
                if (entity instanceof Mob mob) mob.setPersistenceRequired();
            }
        } else if (material.getItem() instanceof SpawnEggItem) {
            EntityType<?> type = SpawnEggItem.getType(material);
            if (type == null) return null;
            entity = type.create(player.level(), EntitySpawnReason.SPAWN_ITEM_USE);
            if (entity instanceof Mob mob) {
                var pos = BlueprintNbt.list(plan.entityNbt(), "Pos", Tag.TAG_DOUBLE);
                mob.setPos(pos.getDoubleOr(0, 0.0), pos.getDoubleOr(1, 0.0), pos.getDoubleOr(2, 0.0));
                mob.finalizeSpawn(player.level(), player.level().getCurrentDifficultyAt(mob.blockPosition()),
                    EntitySpawnReason.SPAWN_ITEM_USE, null);
                if (mob.isSpawnCancelled()) return null;
            }
            if (entity != null) EntityType.createDefaultStackConfig(player.level(), material, player).accept(entity);
        } else {
            return null;
        }
        if (!(entity instanceof Mob) || entity.getType() != EntityBuildAdapters.typeOf(plan.entityNbt()).orElse(null)) return null;
        CompoundTag data = new CompoundTag();
        if (!BlueprintLeashes.save(entity, data)) return null;
        data.remove("UUID");
        data.remove("Passengers");
        data.remove("Leash");
        // 只从蓝图继承空间状态和已付费的连接，生物属性与物品完全来自实际材料。
        for (String key : List.of("Pos", "Rotation", "Motion", SOURCE, VEHICLE, "leash")) {
            data.remove(key);
            Tag value = plan.entityNbt().get(key);
            if (value != null) data.put(key, value.copy());
        }
        return new EntityBuildAdapter.Planned(plan.material(), plan.returned(), data, List.of(), List.of(), false);
    }

    private static Vec3 motion(CompoundTag data) {
        ListTag motion = BlueprintNbt.list(data, "Motion", Tag.TAG_DOUBLE);
        if (motion.size() != 3) return Vec3.ZERO;
        double x = motion.getDoubleOr(0, 0.0);
        double y = motion.getDoubleOr(1, 0.0);
        double z = motion.getDoubleOr(2, 0.0);
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z) ? new Vec3(x, y, z) : Vec3.ZERO;
    }

    static void limitMotion(CompoundTag data) {
        Vec3 velocity = motion(data);
        double largest = Math.max(Math.abs(velocity.x), Math.max(Math.abs(velocity.y), Math.abs(velocity.z)));
        if (largest > 0) {
            Vec3 scaled = new Vec3(velocity.x / largest, velocity.y / largest, velocity.z / largest);
            double length = scaled.length();
            if (largest > MAX_SURVIVAL_SPEED / length) velocity = scaled.scale(MAX_SURVIVAL_SPEED / length);
        }
        ListTag motion = new ListTag();
        motion.add(DoubleTag.valueOf(velocity.x));
        motion.add(DoubleTag.valueOf(velocity.y));
        motion.add(DoubleTag.valueOf(velocity.z));
        data.put("Motion", motion);
    }

    static void restoreMotion(Entity entity, CompoundTag data) {
        // Entity.load 会清零绝对值大于 10 的速度分量；蓝图已在材料分配阶段统一限速。
        entity.setDeltaMovement(motion(data));
    }

    static void relocateMemories(CompoundTag data, StructureSnapshot.EntityEntry entry,
                                 StructureSnapshot snapshot, BlueprintPlacement placement, String dimension) {
        BlockPos origin = BlueprintBlockConfiguration.sourceOrigin(snapshot);
        if (origin == null) {
            ListTag saved = BlueprintNbt.list(entry.nbt(), "Pos", Tag.TAG_DOUBLE);
            origin = saved.size() == 3 ? new BlockPos(
                (int) Math.round(saved.getDoubleOr(0, 0.0) - entry.pos().x),
                (int) Math.round(saved.getDoubleOr(1, 0.0) - entry.pos().y),
                (int) Math.round(saved.getDoubleOr(2, 0.0) - entry.pos().z)) : BlockPos.ZERO;
        }
        BlueprintLeashes.transform(data, origin, placement);
        CompoundTag memories = data.getCompoundOrEmpty("Brain").getCompoundOrEmpty("memories");
        for (String key : List.copyOf(memories.keySet())) {
            CompoundTag value = memories.getCompoundOrEmpty(key).getCompoundOrEmpty("value");
            var pos = value.read("pos", BlockPos.CODEC);
            if (pos.isEmpty()) continue;
            BlockPos local = pos.get().subtract(origin);
            if (local.getX() < 0 || local.getY() < 0 || local.getZ() < 0 || local.getX() >= snapshot.size().getX()
                || local.getY() >= snapshot.size().getY() || local.getZ() >= snapshot.size().getZ()) {
                memories.remove(key);
            } else {
                value.store("pos", BlockPos.CODEC, placement.worldOf(local));
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

    static Map<UUID, UUID> identities(StructureSnapshot snapshot, BlueprintPlacement placement) {
        Map<UUID, UUID> ids = new HashMap<>();
        for (var entry : snapshot.entities()) {
            if (!entry.nbt().read("UUID", UUIDUtil.CODEC).isPresent()) continue;
            UUID old = entry.nbt().read("UUID", UUIDUtil.CODEC).orElseThrow();
            ids.put(old, UUID.nameUUIDFromBytes((old + ":" + placement.anchor().toShortString()).getBytes(StandardCharsets.UTF_8)));
        }
        return ids;
    }

    static void scope(CompoundTag target, CompoundTag original, Map<UUID, UUID> ids) {
        remap(target, ids);
        original.read("UUID", UUIDUtil.CODEC).ifPresent(uuid -> target.store(SOURCE, UUIDUtil.CODEC, ids.get(uuid)));
        if (original.read(VEHICLE, UUIDUtil.CODEC).isPresent()) {
            UUID vehicle = ids.get(original.read(VEHICLE, UUIDUtil.CODEC).orElseThrow());
            if (vehicle != null) target.store(VEHICLE, UUIDUtil.CODEC, vehicle);
        }
    }

    static List<Entity> link(List<Map.Entry<Entity, CompoundTag>> spawned) {
        Map<UUID, UUID> ids = new HashMap<>();
        Map<UUID, Entity> entities = new HashMap<>();
        for (var pair : spawned) {
            if (!pair.getValue().read(SOURCE, UUIDUtil.CODEC).isPresent()) continue;
            UUID source = pair.getValue().read(SOURCE, UUIDUtil.CODEC).orElseThrow();
            ids.put(source, pair.getKey().getUUID());
            entities.put(source, pair.getKey());
        }
        for (var pair : spawned) {
            Entity entity = pair.getKey();
            CompoundTag saved = new CompoundTag();
            BlueprintLeashes.save(entity, saved);
            remap(saved, ids);
            entity.load(TagValueInput.create(ProblemReporter.DISCARDING, entity.registryAccess(), saved));
            restoreMotion(entity, saved);
            CompoundTag original = pair.getValue();
            if (!original.read(VEHICLE, UUIDUtil.CODEC).isPresent()) continue;
            Entity vehicle = entities.get(original.read(VEHICLE, UUIDUtil.CODEC).orElseThrow());
            if (vehicle != null && vehicle != entity) entity.startRiding(vehicle, true, true);
        }
        return BlueprintLeashes.link(spawned.stream().map(Map.Entry::getKey).toList());
    }

    private static void remap(CompoundTag tag, Map<UUID, UUID> ids) {
        for (String key : List.copyOf(tag.keySet())) {
            Tag value = tag.get(key);
            if (value instanceof IntArrayTag array && array.getAsIntArray().length == 4) {
                UUID target = ids.get(UUIDUtil.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, array).getOrThrow());
                if (target != null) tag.store(key, UUIDUtil.CODEC, target);
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
