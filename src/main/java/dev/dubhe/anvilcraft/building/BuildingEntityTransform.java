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
        CompoundTag tag = EntityBuildAdapters.withWorldPos(entry.nbt(), world);
        BlockPos block = placement.worldOf(entry.blockPos());
        if (MagnetizedNodeBuildAdapter.isNode(tag)) {
            tag.put("BlockPos", NbtUtils.writeBlockPos(block));
            var state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("BlockState"));
            state = placement.stateOf(state);
            tag.put("BlockState", NbtUtils.writeBlockState(state));
            double height = state.getCollisionShape(EmptyBlockGetter.INSTANCE, block).max(Direction.Axis.Y, 0.5, 0.5);
            tag = EntityBuildAdapters.withWorldPos(tag, Vec3.atBottomCenterOf(block).add(0, height, 0));
        }
        if (tag.contains("TileX")) {
            tag.putInt("TileX", block.getX());
            tag.putInt("TileY", block.getY());
            tag.putInt("TileZ", block.getZ());
        }
        if (tag.contains("Facing")) {
            Direction facing = Direction.from3DDataValue(tag.getByte("Facing"));
            tag.putByte("Facing", (byte) placement.rotation().rotate(placement.mirror().mirror(facing)).get3DDataValue());
        }
        ListTag motion = tag.getList("Motion", Tag.TAG_DOUBLE);
        if (motion.size() == 3) {
            Vec3 vector = new Vec3(motion.getDouble(0), motion.getDouble(1), motion.getDouble(2));
            Vec3 rotatedMotion = placement.localOf(vector).subtract(placement.localOf(Vec3.ZERO));
            ListTag transformedMotion = new ListTag();
            transformedMotion.add(DoubleTag.valueOf(rotatedMotion.x));
            transformedMotion.add(DoubleTag.valueOf(rotatedMotion.y));
            transformedMotion.add(DoubleTag.valueOf(rotatedMotion.z));
            tag.put("Motion", transformedMotion);
        }
        if (DynamicBuildingEntities.Outlet.isOutlet(tag)) {
            var old = NbtUtils.readBlockPos(tag, "CauldronPos");
            var target = NbtUtils.readBlockPos(tag, "TargetPos");
            if (old.isPresent() && target.isPresent()) {
                tag.put("TargetPos", NbtUtils.writeBlockPos(placement.worldOf(entry.blockPos().offset(target.get().subtract(old.get())))));
            }
            tag.put("CauldronPos", NbtUtils.writeBlockPos(block));
            Direction side = Direction.from3DDataValue(tag.getInt("AttachedDirection"));
            side = placement.rotation().rotate(placement.mirror().mirror(side));
            tag.putInt("AttachedDirection", side.get3DDataValue());
            var state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("CauldronState"));
            tag.put("CauldronState", NbtUtils.writeBlockState(placement.stateOf(state)));
        }
        if (tag.contains("BlockState") && !MagnetizedNodeBuildAdapter.isNode(tag)) {
            var state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("BlockState"));
            tag.put("BlockState", NbtUtils.writeBlockState(placement.stateOf(state)));
        }
        if ("anvilcraft:animate_ascending_block".equals(tag.getString("id"))) {
            for (String key : List.of("Start", "End")) {
                var relative = NbtUtils.readBlockPos(tag, "Relative" + key);
                if (relative.isPresent()) {
                    tag.put(key + "Pos", NbtUtils.writeBlockPos(placement.worldOf(entry.blockPos().offset(relative.get()))));
                }
            }
        }
        if (tag.contains("SlidingBlocks", Tag.TAG_COMPOUND)) {
            var start = NbtUtils.readBlockPos(entry.nbt(), "StartPos");
            ListTag oldPos = entry.nbt().getList("Pos", Tag.TAG_DOUBLE);
            BlockPos sourcePos = oldPos.size() == 3
                ? BlockPos.containing(oldPos.getDouble(0), oldPos.getDouble(1), oldPos.getDouble(2)) : entry.blockPos();
            BlockPos delta = NbtUtils.readBlockPos(tag, "RelativeStart").orElse(start.orElse(sourcePos).subtract(sourcePos));
            if (Math.abs(delta.getX()) > 64 || Math.abs(delta.getY()) > 64 || Math.abs(delta.getZ()) > 64) delta = BlockPos.ZERO;
            BlockPos localStart = entry.blockPos().offset(delta);
            tag.put("RelativeStart", NbtUtils.writeBlockPos(placement.localOf(delta)));
            tag.put("StartPos", NbtUtils.writeBlockPos(placement.worldOf(localStart)));
            Direction direction = Direction.byName(tag.getString("MovingDirection"));
            if (direction != null) {
                tag.putString("MovingDirection", placement.rotation().rotate(placement.mirror().mirror(direction)).getSerializedName());
            }
            for (Tag value : tag.getCompound("SlidingBlocks").getList("blocks", Tag.TAG_COMPOUND)) {
                CompoundTag part = (CompoundTag) value;
                BlockPos offset = NbtUtils.readBlockPos(part, "offset").orElseThrow();
                BlockPos rotated = placement.localOf(offset);
                part.put("offset", NbtUtils.writeBlockPos(rotated));
                var state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), part.getCompound("state"));
                part.put("state", NbtUtils.writeBlockState(placement.stateOf(state)));
                CompoundTag data = part.getCompound("entityData");
                BlockPos worldPart = BlockPos.containing(world).offset(rotated);
                if (!data.isEmpty()) {
                    data.putInt("x", worldPart.getX());
                    data.putInt("y", worldPart.getY());
                    data.putInt("z", worldPart.getZ());
                }
            }
        }
        ListTag angles = tag.getList("Rotation", 5);
        float yaw = angles.isEmpty() ? 0 : angles.getFloat(0);
        yaw = switch (placement.mirror()) {
            case FRONT_BACK -> -yaw;
            case LEFT_RIGHT -> 180 - yaw;
            case NONE -> yaw;
        };
        ListTag rotated = new ListTag();
        rotated.add(FloatTag.valueOf(yaw - placement.yawDegrees()));
        rotated.add(FloatTag.valueOf(angles.size() > 1 ? angles.getFloat(1) : 0));
        tag.put("Rotation", rotated);
        tag.remove("Passengers");
        return tag;
    }

    static CompoundTag sanitize(Entity entity, CompoundTag tag) {
        CompoundTag safe = tag.copy();
        safe.remove("UUID");
        safe.remove("Passengers");
        return safe;
    }
}
