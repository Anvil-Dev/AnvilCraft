package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

import java.util.List;

final class MagnetizedNodeBuildAdapter implements EntityBuildAdapter {
    static boolean isNode(CompoundTag tag) {
        return BuildingEntityTransform.isNode(tag);
    }

    static BlockPos support(StructureSnapshot snapshot, StructureSnapshot.EntityEntry entry) {
        return BuildingEntityTransform.nodeSupport(snapshot, entry);
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
        if (transformedNbt.read("block_pos", BlockPos.CODEC)
            .or(() -> transformedNbt.read("BlockPos", BlockPos.CODEC)).isEmpty()) return Planned.skip();
        return new Planned(ItemStack.EMPTY, ItemStack.EMPTY, transformedNbt.copy(), List.of(), List.of(), false);
    }
}
