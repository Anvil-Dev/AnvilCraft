package dev.dubhe.anvilcraft.building;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

final class BlueprintLeashes {
    private BlueprintLeashes() {
    }

    static Optional<BlockPos> fence(CompoundTag tag) {
        Optional<BlockPos> current = NbtUtils.readBlockPos(tag, Leashable.LEASH_TAG);
        if (current.isPresent()) return current;
        CompoundTag legacy = tag.getCompound("Leash");
        return legacy.contains("X", Tag.TAG_ANY_NUMERIC)
            ? Optional.of(new BlockPos(legacy.getInt("X"), legacy.getInt("Y"), legacy.getInt("Z"))) : Optional.empty();
    }

    static boolean hasLeash(CompoundTag tag) {
        return fence(tag).isPresent() || tag.getCompound(Leashable.LEASH_TAG).hasUUID("UUID")
            || tag.getCompound("Leash").hasUUID("UUID");
    }

    static Optional<BlockPos> attachment(CompoundTag tag) {
        if (!"minecraft:leash_knot".equals(tag.getString("id"))) return fence(tag);
        var pos = tag.getList("Pos", Tag.TAG_DOUBLE);
        return pos.size() == 3 ? Optional.of(BlockPos.containing(pos.getDouble(0), pos.getDouble(1), pos.getDouble(2)))
            : Optional.empty();
    }

    static void transform(CompoundTag tag, BlockPos origin, BlueprintPlacement placement) {
        Optional<BlockPos> fence = fence(tag);
        if (fence.isPresent()) {
            tag.put(Leashable.LEASH_TAG, NbtUtils.writeBlockPos(placement.worldOf(fence.get().subtract(origin))));
            tag.remove("Leash");
        } else if (tag.getCompound("Leash").hasUUID("UUID")) {
            tag.put(Leashable.LEASH_TAG, tag.getCompound("Leash").copy());
            tag.remove("Leash");
        }
    }

    static boolean save(Entity entity, CompoundTag tag) {
        if (entity.saveAsPassenger(tag)) return true;
        if (!(entity instanceof LeashFenceKnotEntity)) return false;
        entity.saveWithoutId(tag);
        tag.putString("id", EntityType.getKey(entity.getType()).toString());
        return true;
    }

    @Nullable
    static Entity create(CompoundTag tag, Level level) {
        if (!"minecraft:leash_knot".equals(tag.getString("id"))) return EntityType.create(tag, level).orElse(null);
        var position = tag.getList("Pos", Tag.TAG_DOUBLE);
        if (position.size() != 3) return null;
        BlockPos pos = BlockPos.containing(position.getDouble(0), position.getDouble(1), position.getDouble(2));
        LeashFenceKnotEntity knot = new LeashFenceKnotEntity(level, pos);
        knot.load(tag);
        knot.setPos(pos.getX(), pos.getY(), pos.getZ());
        return knot;
    }

    static List<Entity> link(List<Entity> entities) {
        List<Entity> created = new ArrayList<>();
        for (Entity entity : entities) {
            if (!(entity instanceof Leashable leashable) || !(entity.level() instanceof ServerLevel level)) continue;
            var data = leashable.getLeashData();
            if (data == null || data.delayedLeashInfo == null) continue;
            Entity holder = data.delayedLeashInfo.left().map(level::getEntity).orElse(null);
            Optional<BlockPos> pos = data.delayedLeashInfo.right();
            if (holder == null && pos.isPresent() && level.getBlockState(pos.get()).is(BlockTags.FENCES)) {
                boolean existing = !level.getEntities(EntityType.LEASH_KNOT, new AABB(pos.get()).inflate(1),
                    knot -> pos.get().equals(knot.getPos())).isEmpty();
                holder = LeashFenceKnotEntity.getOrCreateKnot(level, pos.get());
                if (!existing) created.add(holder);
            }
            if (holder != null) leashable.setLeashedTo(holder, true);
        }
        return created;
    }
}
