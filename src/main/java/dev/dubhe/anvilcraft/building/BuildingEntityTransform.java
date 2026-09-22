package dev.dubhe.anvilcraft.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class BuildingEntityTransform {
    private BuildingEntityTransform() {
    }

    public static CompoundTag transform(StructureSnapshot.EntityEntry entry, BlueprintPlacement placement) {
        Vec3 world = placement.localOf(entry.pos(), entry.blockPos()).add(
            placement.anchor().getX(), placement.anchor().getY(), placement.anchor().getZ());
        CompoundTag tag = BuildingEntityTransform.withWorldPos(entry.nbt(), world);
        BlockPos block = placement.worldOf(entry.blockPos());
        if (BuildingEntityTransform.isNode(tag)) {
            BuildingEntityTransform.rename(tag, "BlockPos", "block_pos");
            BuildingEntityTransform.rename(tag, "BlockState", "block_state");
        }
        if ("anvilcraft:cauldron_outlet".equals(tag.getStringOr("id", ""))) {
            BuildingEntityTransform.rename(tag, "CauldronPos", "cauldron_pos");
            BuildingEntityTransform.rename(tag, "CauldronState", "cauldron_state");
            BuildingEntityTransform.rename(tag, "AttachedDirection", "attached_direction");
        }
        if (tag.contains("block_pos")) tag.store("block_pos", BlockPos.CODEC, block);
        if (BuildingEntityTransform.isNode(tag)) {
            tag.store("block_pos", BlockPos.CODEC, block);
            var state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK, tag.getCompoundOrEmpty("block_state"));
            state = placement.stateOf(state);
            tag.put("block_state", NbtUtils.writeBlockState(state));
            double height = state.getCollisionShape(EmptyBlockGetter.INSTANCE, block).max(Direction.Axis.Y, 0.5, 0.5);
            tag = BuildingEntityTransform.withWorldPos(tag, Vec3.atBottomCenterOf(block).add(0, height, 0));
        }
        if (tag.contains("TileX")) {
            tag.putInt("TileX", block.getX());
            tag.putInt("TileY", block.getY());
            tag.putInt("TileZ", block.getZ());
            tag.store("block_pos", BlockPos.CODEC, block);
        }
        if (tag.contains("Facing")) {
            Direction facing = Direction.from3DDataValue(tag.getByteOr("Facing", (byte) 0));
            tag.putByte("Facing", (byte) placement.rotation().rotate(placement.mirror().mirror(facing)).get3DDataValue());
        }
        ListTag motion = BlueprintNbt.list(tag, "Motion", Tag.TAG_DOUBLE);
        if (motion.size() == 3) {
            Vec3 vector = new Vec3(motion.getDoubleOr(0, 0.0), motion.getDoubleOr(1, 0.0), motion.getDoubleOr(2, 0.0));
            Vec3 rotatedMotion = placement.localOf(vector).subtract(placement.localOf(Vec3.ZERO));
            ListTag transformedMotion = new ListTag();
            transformedMotion.add(DoubleTag.valueOf(rotatedMotion.x));
            transformedMotion.add(DoubleTag.valueOf(rotatedMotion.y));
            transformedMotion.add(DoubleTag.valueOf(rotatedMotion.z));
            tag.put("Motion", transformedMotion);
        }
        if ("anvilcraft:cauldron_outlet".equals(tag.getStringOr("id", ""))) {
            var old = tag.read("cauldron_pos", BlockPos.CODEC);
            var target = tag.read("TargetPos", BlockPos.CODEC);
            if (old.isPresent() && target.isPresent()) {
                tag.store("TargetPos", BlockPos.CODEC, placement.worldOf(entry.blockPos().offset(target.get().subtract(old.get()))));
            }
            tag.store("cauldron_pos", BlockPos.CODEC, block);
            Direction side = Direction.from3DDataValue(tag.getIntOr("attached_direction", 0));
            side = placement.rotation().rotate(placement.mirror().mirror(side));
            tag.putInt("attached_direction", side.get3DDataValue());
            var state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK, tag.getCompoundOrEmpty("cauldron_state"));
            tag.put("cauldron_state", NbtUtils.writeBlockState(placement.stateOf(state)));
        }
        if (tag.contains("BlockState") && !BuildingEntityTransform.isNode(tag)) {
            var state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK, tag.getCompoundOrEmpty("BlockState"));
            tag.put("BlockState", NbtUtils.writeBlockState(placement.stateOf(state)));
        }
        if ("anvilcraft:animate_ascending_block".equals(tag.getStringOr("id", ""))) {
            for (String key : List.of("Start", "End")) {
                var relative = tag.read("Relative" + key, BlockPos.CODEC);
                if (relative.isPresent()) {
                    tag.store(key + "Pos", BlockPos.CODEC, placement.worldOf(entry.blockPos().offset(relative.get())));
                }
            }
        }
        if (tag.get("SlidingBlocks") instanceof CompoundTag) {
            var start = entry.nbt().read("StartPos", BlockPos.CODEC);
            ListTag oldPos = BlueprintNbt.list(entry.nbt(), "Pos", Tag.TAG_DOUBLE);
            BlockPos sourcePos = oldPos.size() == 3
                ? BlockPos.containing(oldPos.getDoubleOr(0, 0.0), oldPos.getDoubleOr(1, 0.0), oldPos.getDoubleOr(2, 0.0))
                : entry.blockPos();
            BlockPos delta = tag.read("RelativeStart", BlockPos.CODEC).orElse(start.orElse(sourcePos).subtract(sourcePos));
            if (Math.abs((long) delta.getX()) > 64 || Math.abs((long) delta.getY()) > 64 || Math.abs((long) delta.getZ()) > 64) {
                delta = BlockPos.ZERO;
            }
            BlockPos localStart = entry.blockPos().offset(delta);
            tag.store("RelativeStart", BlockPos.CODEC, placement.localOf(delta));
            tag.store("StartPos", BlockPos.CODEC, placement.worldOf(localStart));
            Direction direction = Direction.byName(tag.getStringOr("MovingDirection", ""));
            if (direction != null) {
                tag.putString("MovingDirection", placement.rotation().rotate(placement.mirror().mirror(direction)).getSerializedName());
            }
            for (Tag value : BlueprintNbt.list(tag.getCompoundOrEmpty("SlidingBlocks"), "blocks", Tag.TAG_COMPOUND)) {
                CompoundTag part = (CompoundTag) value;
                BlockPos offset = part.read("offset", BlockPos.CODEC).orElseThrow();
                BlockPos rotated = placement.localOf(offset);
                part.store("offset", BlockPos.CODEC, rotated);
                var state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK, part.getCompoundOrEmpty("state"));
                part.put("state", NbtUtils.writeBlockState(placement.stateOf(state)));
                BuildingEntityTransform.rename(part, "entityData", "entity_data");
                CompoundTag data = part.getCompoundOrEmpty("entity_data");
                BlockPos worldPart = BlockPos.containing(world).offset(rotated);
                if (!data.isEmpty()) {
                    data.putInt("x", worldPart.getX());
                    data.putInt("y", worldPart.getY());
                    data.putInt("z", worldPart.getZ());
                }
            }
        }
        ListTag angles = BlueprintNbt.list(tag, "Rotation", Tag.TAG_FLOAT);
        float yaw = angles.isEmpty() ? 0 : angles.getFloatOr(0, 0.0F);
        yaw = switch (placement.mirror()) {
            case FRONT_BACK -> -yaw;
            case LEFT_RIGHT -> 180 - yaw;
            case NONE -> yaw;
        };
        ListTag rotated = new ListTag();
        rotated.add(FloatTag.valueOf(yaw - placement.yawDegrees()));
        rotated.add(FloatTag.valueOf(angles.size() > 1 ? angles.getFloatOr(1, 0.0F) : 0));
        tag.put("Rotation", rotated);
        tag.remove("Passengers");
        return tag;
    }

    public static CompoundTag withWorldPos(CompoundTag nbt, Vec3 world) {
        CompoundTag copy = nbt.copy();
        copy.remove("UUID");
        copy.store("Pos", Vec3.CODEC, world);
        return copy;
    }

    static boolean isNode(CompoundTag tag) {
        return "anvilcraft:magnetized_node".equals(tag.getStringOr("id", ""));
    }

    private static void rename(CompoundTag tag, String legacy, String current) {
        if (!tag.contains(current) && tag.contains(legacy)) tag.put(current, tag.get(legacy).copy());
        tag.remove(legacy);
    }

    static CompoundTag sanitize(Entity entity, CompoundTag tag) {
        CompoundTag safe = tag.copy();
        safe.remove("UUID");
        safe.remove("Passengers");
        return safe;
    }
}
