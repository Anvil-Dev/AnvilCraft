package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.api.sliding.SlidingBlockSection;
import dev.dubhe.anvilcraft.block.UseItemOnBlock;
import dev.dubhe.anvilcraft.entity.AnimateAscendingBlockEntity;
import dev.dubhe.anvilcraft.entity.IonocraftEntity;
import dev.dubhe.anvilcraft.entity.SlidingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

final class DynamicBuildingEntities implements EntityBuildAdapter {
    static boolean requiresOperator(CompoundTag tag, ServerLevel level) {
        if (tag.contains("SlidingBlocks")) {
            var section = SlidingBlockSection.CODEC.parse(NbtOps.INSTANCE, tag.getCompound("SlidingBlocks")).getOrThrow();
            return section.blocks().stream().anyMatch(block ->
                BuildingBlockMaterial.extract(block.state(), block.beTag(), level).requiresOperator());
        }
        if (tag.contains("BlockState") && !MagnetizedNodeBuildAdapter.isNode(tag)) {
            var state = NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK),
                tag.getCompound("BlockState"));
            return BuildingBlockMaterial.extract(state, tag.getCompound("TileEntityData"), level).requiresOperator();
        }
        return false;
    }

    @Override
    public boolean matches(Entity entity, CompoundTag nbt) {
        return entity instanceof Mob || entity instanceof FallingBlockEntity || this.matches(entity.getType(), nbt);
    }

    @Override
    public boolean matches(EntityType<?> type, CompoundTag nbt) {
        return nbt.contains("BlockState") || nbt.contains("Health") || SpawnEggItem.byId(type) != null
            || nbt.contains("item") || type == EntityType.SNOWBALL || type == EntityType.EGG || type == EntityType.ENDER_PEARL
            || type == EntityType.POTION || type == EntityType.FIREBALL || type == EntityType.SMALL_FIREBALL
            || type == EntityType.FALLING_BLOCK || type == EntityType.TNT || type == EntityType.ITEM
            || type == EntityType.END_CRYSTAL || "anvilcraft:sliding_block".equals(nbt.getString("id"))
            || "anvilcraft:ionocraft".equals(nbt.getString("id"));
    }

    @Override
    public Planned plan(ServerLevel level, StructureSnapshot.EntityEntry entry, CompoundTag nbt) {
        Entity entity = EntityType.create(nbt, level).orElse(null);
        if (entity instanceof AbstractArrow arrow) {
            ItemStack material = arrow.getPickupItemStackOrigin().copyWithCount(1);
            return material.isEmpty() ? Planned.skip()
                : new Planned(material, ItemStack.EMPTY, nbt.copy(), List.of(), List.of(), false);
        }
        if (entity instanceof ThrowableItemProjectile projectile) {
            return new Planned(projectile.getItem().copyWithCount(1), ItemStack.EMPTY, nbt.copy(), List.of(), List.of(), false);
        }
        if (entity != null && (entity.getType() == EntityType.FIREBALL || entity.getType() == EntityType.SMALL_FIREBALL)) {
            return new Planned(new ItemStack(Items.FIRE_CHARGE), ItemStack.EMPTY, nbt.copy(), List.of(), List.of(), false);
        }
        if (entity instanceof AnimateAscendingBlockEntity) {
            return new Planned(ItemStack.EMPTY, ItemStack.EMPTY, nbt.copy(), List.of(), List.of(), false);
        }
        if (entity instanceof ItemEntity item) {
            return new Planned(item.getItem().copy(), ItemStack.EMPTY, nbt.copy(), List.of(), List.of(), false);
        }
        if (entity instanceof IonocraftEntity vehicle) {
            ItemStack material = vehicle.getPickResult();
            if (material == null || material.isEmpty()) return Planned.skip();
            return new Planned(material, ItemStack.EMPTY, nbt.copy(), List.of(), List.of(), false);
        }
        if (entity != null && entity.getType() == EntityType.END_CRYSTAL) {
            return new Planned(new ItemStack(Items.END_CRYSTAL), ItemStack.EMPTY, nbt.copy(), List.of(), List.of(), false);
        }
        if (entity instanceof SlidingBlockEntity) {
            var section = SlidingBlockSection.CODEC
                .parse(NbtOps.INSTANCE, nbt.getCompound("SlidingBlocks")).getOrThrow();
            List<SlotStack> materials = new ArrayList<>();
            for (var block : section.blocks()) {
                BuildingBlockMaterial material = BuildingBlockMaterial.extract(block.state(), block.beTag(), level);
                if (BlueprintMultiblocks.shouldRecord(block.state())) {
                    if (material.stack().isEmpty() && !BlueprintIgnition.isIgnition(block.state())) return Planned.skip();
                    if (!material.stack().isEmpty()) {
                        materials.add(new SlotStack(-1, material.stack().copyWithCount(BuildingRodService.materialCount(block.state()))));
                    }
                    for (ItemStack extra : BlueprintSpecialBlocks.extra(block.state())) materials.add(new SlotStack(-1, extra));
                    ItemStack upgrade = UseItemOnBlock.materialFor(block.state());
                    if (!upgrade.isEmpty()) materials.add(new SlotStack(-1, upgrade));
                }
                for (var content : material.contents()) materials.add(new SlotStack(-1, content.stack()));
            }
            return new Planned(ItemStack.EMPTY, ItemStack.EMPTY, nbt.copy(), materials, List.of(), false);
        }
        if (entity instanceof FallingBlockEntity falling) {
            BuildingBlockMaterial block = BuildingBlockMaterial.extract(falling.getBlockState(), nbt.getCompound("TileEntityData"), level);
            ItemStack material = block.stack();
            if (material.isEmpty()) return Planned.skip();
            CompoundTag saved = nbt.copy();
            // time=0 的原版下落实体会先删除起点方块；蓝图已将实体与方块分别恢复。
            saved.putInt("Time", Math.max(1, saved.getInt("Time")));
            List<SlotStack> costs = new ArrayList<>();
            for (var content : block.contents()) costs.add(new SlotStack(-1, content.stack()));
            ItemStack upgrade = UseItemOnBlock.materialFor(falling.getBlockState());
            if (!upgrade.isEmpty()) costs.add(new SlotStack(-1, upgrade));
            return new Planned(material, ItemStack.EMPTY, saved, costs, List.of(), false);
        }
        if (entity instanceof Mob mob) {
            List<SlotStack> contents = new ArrayList<>();
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = mob.getItemBySlot(slot);
                if (!stack.isEmpty()) contents.add(new SlotStack(100 + slot.ordinal(), stack.copy()));
                mob.setItemSlot(slot, ItemStack.EMPTY);
            }
            if (mob instanceof InventoryCarrier carrier) {
                Container inventory = carrier.getInventory();
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    ItemStack stack = inventory.getItem(i);
                    if (!stack.isEmpty()) contents.add(new SlotStack(i, stack.copy()));
                    inventory.setItem(i, ItemStack.EMPTY);
                }
            }
            CompoundTag saved = new CompoundTag();
            mob.save(saved);
            saved.remove("UUID");
            SpawnEggItem egg = SpawnEggItem.byId(mob.getType());
            return new Planned(egg == null ? ItemStack.EMPTY : new ItemStack(egg), ItemStack.EMPTY,
                saved, contents, List.of(), false);
        }
        if (entity != null && entity.getType() == EntityType.TNT) {
            return new Planned(new ItemStack(Items.TNT), ItemStack.EMPTY, nbt.copy(), List.of(), List.of(), false);
        }
        return Planned.skip();
    }

    @Override
    public @Nullable Entity spawn(ServerLevel level, BuildingEntityOp op) {
        Entity entity = EntityBuildAdapter.super.spawn(level, op);
        if (entity instanceof FallingBlockEntity falling) falling.setStartPos(falling.blockPosition());
        if (entity instanceof SlidingBlockEntity sliding && sliding.getMoveDirection() != null) {
            sliding.setMoveDirection(sliding.getMoveDirection());
        }
        return entity;
    }

    static final class Outlet implements EntityBuildAdapter {
        static boolean isOutlet(CompoundTag tag) {
            return "anvilcraft:cauldron_outlet".equals(tag.getString("id"));
        }

        static BlockPos support(StructureSnapshot.EntityEntry entry) {
            Direction direction = Direction.from3DDataValue(entry.nbt().getInt("AttachedDirection"));
            Vec3 inward = entry.pos().subtract(Vec3.atLowerCornerOf(direction.getNormal()).scale(0.51));
            return BlockPos.containing(inward);
        }

        @Override
        public boolean matches(EntityType<?> type, CompoundTag nbt) {
            return isOutlet(nbt);
        }

        @Override
        public boolean requiresHammer() {
            return true;
        }

        @Override
        public Planned plan(ServerLevel level, StructureSnapshot.EntityEntry entry, CompoundTag nbt) {
            return new Planned(ItemStack.EMPTY, ItemStack.EMPTY, nbt.copy(), List.of(), List.of(), false);
        }
    }
}
