package dev.dubhe.anvilcraft.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class BlueprintLeashes {
    private BlueprintLeashes() {
    }

    static Optional<BlockPos> fence(CompoundTag tag) {
        Optional<BlockPos> current = tag.read(Leashable.LEASH_TAG, BlockPos.CODEC);
        if (current.isPresent()) return current;
        CompoundTag legacy = tag.getCompoundOrEmpty("Leash");
        return (legacy.get("X") instanceof NumericTag)
            ? Optional.of(new BlockPos(legacy.getIntOr("X", 0), legacy.getIntOr("Y", 0), legacy.getIntOr("Z", 0))) : Optional.empty();
    }

    static boolean hasLeash(CompoundTag tag) {
        return fence(tag).isPresent() || tag.getCompoundOrEmpty(Leashable.LEASH_TAG).read("UUID", UUIDUtil.CODEC).isPresent()
            || tag.getCompoundOrEmpty("Leash").read("UUID", UUIDUtil.CODEC).isPresent();
    }

    static Optional<BlockPos> attachment(CompoundTag tag) {
        if (!"minecraft:leash_knot".equals(tag.getStringOr("id", ""))) return fence(tag);
        var pos = BlueprintNbt.list(tag, "Pos", Tag.TAG_DOUBLE);
        return pos.size() == 3 ? Optional.of(BlockPos.containing(pos.getDoubleOr(0, 0.0), pos.getDoubleOr(1, 0.0), pos.getDoubleOr(2, 0.0)))
            : Optional.empty();
    }

    static void transform(CompoundTag tag, BlockPos origin, BlueprintPlacement placement) {
        Optional<BlockPos> fence = fence(tag);
        if (fence.isPresent()) {
            tag.store(Leashable.LEASH_TAG, BlockPos.CODEC, placement.worldOf(fence.get().subtract(origin)));
            tag.remove("Leash");
        } else if (tag.getCompoundOrEmpty("Leash").read("UUID", UUIDUtil.CODEC).isPresent()) {
            tag.put(Leashable.LEASH_TAG, tag.getCompoundOrEmpty("Leash").copy());
            tag.remove("Leash");
        }
    }

    static boolean save(Entity entity, CompoundTag tag) {
        CompoundTag saved = BlueprintCapture.saveEntity(entity);
        if (saved == null) return false;
        tag.merge(saved);
        return true;
    }

    @Nullable
    static Entity create(CompoundTag tag, Level level) {
        if (!"minecraft:leash_knot".equals(tag.getStringOr("id", ""))) {
            CompoundTag normalized = EntityBuildAdapters.normalizeType(tag);
            if (normalized.contains("Leash") && !normalized.contains(Leashable.LEASH_TAG)) {
                transform(normalized, BlockPos.ZERO,
                    new BlueprintPlacement(BlockPos.ZERO, net.minecraft.world.level.block.Rotation.NONE,
                        net.minecraft.world.level.block.Mirror.NONE));
            }
            return EntityType.create(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), normalized),
                level, EntitySpawnReason.LOAD).orElse(null);
        }
        var position = BlueprintNbt.list(tag, "Pos", Tag.TAG_DOUBLE);
        if (position.size() != 3) return null;
        BlockPos pos = BlockPos.containing(position.getDoubleOr(0, 0.0), position.getDoubleOr(1, 0.0), position.getDoubleOr(2, 0.0));
        LeashFenceKnotEntity knot = new LeashFenceKnotEntity(level, pos);
        knot.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
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
