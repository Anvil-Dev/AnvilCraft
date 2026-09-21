package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;

import java.util.List;

final class MagnetizedNodeBuildAdapter implements EntityBuildAdapter {
    private static final String ID = "anvilcraft:magnetized_node";

    static boolean isNode(CompoundTag tag) {
        return ID.equals(tag.getString("id"));
    }

    static BlockPos support(StructureSnapshot snapshot, StructureSnapshot.EntityEntry entry) {
        String block = entry.nbt().getCompound("BlockState").getString("Name");
        BlockPos column = BlockPos.containing(entry.pos());
        for (var candidate : snapshot.blocks()) {
            BlockPos pos = candidate.pos();
            if (pos.getX() != column.getX() || pos.getZ() != column.getZ()) continue;
            var state = snapshot.stateOf(candidate);
            if (!BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(block)) continue;
            double height = state.getCollisionShape(EmptyBlockGetter.INSTANCE, pos).max(Direction.Axis.Y, 0.5, 0.5);
            if (Math.abs(pos.getY() + height - entry.pos().y) < 1.0E-5) return pos;
        }
        throw new IllegalArgumentException("Missing magnetized node support at " + entry.pos());
    }

    @Override
    public boolean matches(EntityType<?> type, CompoundTag nbt) {
        return isNode(nbt);
    }

    @Override
    public ItemStack requiredTool() {
        return ModItems.MAGNET.asStack();
    }

    @Override
    public Planned plan(ServerLevel level, StructureSnapshot.EntityEntry entry, CompoundTag transformedNbt) {
        if (NbtUtils.readBlockPos(transformedNbt, "BlockPos").isEmpty()) return Planned.skip();
        return new Planned(ItemStack.EMPTY, ItemStack.EMPTY, transformedNbt.copy(), List.of(), List.of(), false);
    }
}
