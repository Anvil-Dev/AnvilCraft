package dev.dubhe.anvilcraft.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class BuildingEntityTransform {
    private BuildingEntityTransform() {
    }

    public static CompoundTag transform(StructureSnapshot.EntityEntry entry, BlueprintPlacement placement) {
        Vec3 world = placement.localOf(entry.pos(), entry.blockPos()).add(
            placement.anchor().getX(), placement.anchor().getY(), placement.anchor().getZ());
        CompoundTag tag = EntityBuildAdapters.withWorldPos(entry.nbt(), world);
        BlockPos block = placement.worldOf(entry.blockPos());
        if (tag.contains("TileX")) {
            tag.putInt("TileX", block.getX());
            tag.putInt("TileY", block.getY());
            tag.putInt("TileZ", block.getZ());
        }
        if (tag.contains("Facing")) {
            Direction facing = Direction.from3DDataValue(tag.getByte("Facing"));
            tag.putByte("Facing", (byte) placement.rotation().rotate(placement.mirror().mirror(facing)).get3DDataValue());
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
        List<String> keys = new ArrayList<>(List.of("id", "Pos", "Rotation", "CustomName", "CustomNameVisible"));
        if (entity instanceof Boat) keys.add("Type");
        if (entity instanceof AbstractMinecart) {
            keys.addAll(List.of("CustomDisplayTile", "DisplayState", "DisplayOffset"));
            if (entity.getType() == EntityType.COMMAND_BLOCK_MINECART) keys.add("Command");
        }
        if (entity instanceof ArmorStand) keys.addAll(List.of("Pose", "Small", "ShowArms", "NoBasePlate", "Invisible", "NoGravity"));
        if (entity instanceof ItemFrame || entity instanceof Painting) {
            keys.addAll(List.of("Facing", "facing", "TileX", "TileY", "TileZ", "ItemRotation", "Invisible", "variant"));
        }
        CompoundTag safe = new CompoundTag();
        for (String key : keys) {
            Tag value = tag.get(key);
            if (value != null) safe.put(key, value.copy());
        }
        return safe;
    }
}
