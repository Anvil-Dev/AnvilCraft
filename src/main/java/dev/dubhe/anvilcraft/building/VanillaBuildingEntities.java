package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.entity.FluidTankMinecartEntity;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/** 施工实体适配注册:船/矿车/盔甲架/画/展示框、刷怪蛋。 */
public final class VanillaBuildingEntities {
    private VanillaBuildingEntities() {
    }

    public static void register() {
        EntityBuildAdapters.register(new VehicleAdapter());
        EntityBuildAdapters.register(new ArmorStandAdapter());
        EntityBuildAdapters.register(new HangingAdapter());

    }

    private static CompoundTag saveSanitized(Entity entity) {
        CompoundTag tag = new CompoundTag();
        entity.save(tag);
        tag.remove("UUID");
        return tag;
    }

    private static void extractContainer(Container container, List<EntityBuildAdapter.SlotStack> contents) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            contents.add(new EntityBuildAdapter.SlotStack(slot, stack.copy()));
            container.setItem(slot, ItemStack.EMPTY);
        }
    }

    private static void extractHandler(IItemHandler handler, List<EntityBuildAdapter.SlotStack> contents) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            contents.add(new EntityBuildAdapter.SlotStack(slot, stack.copy()));
            if (handler instanceof IItemHandlerModifiable modifiable) {
                modifiable.setStackInSlot(slot, ItemStack.EMPTY);
            } else {
                handler.extractItem(slot, stack.getCount(), false);
            }
        }
    }

    private static final class VehicleAdapter implements EntityBuildAdapter {
        @Override
        public boolean matches(Entity entity, CompoundTag nbt) {
            return entity instanceof Boat || entity instanceof AbstractMinecart;
        }

        @Override
        public boolean matches(EntityType<?> type, CompoundTag nbt) {
            if (type == EntityType.BOAT
                || type == EntityType.CHEST_BOAT
                || type == EntityType.MINECART
                || type == EntityType.CHEST_MINECART
                || type == EntityType.HOPPER_MINECART
                || type == EntityType.FURNACE_MINECART
                || type == EntityType.TNT_MINECART
                || type == EntityType.COMMAND_BLOCK_MINECART) {
                return true;
            }
            Class<? extends Entity> base = type.getBaseClass();
            return Boat.class.isAssignableFrom(base) || AbstractMinecart.class.isAssignableFrom(base);
        }

        @Override
        public Planned plan(ServerLevel level, StructureSnapshot.EntityEntry entry, CompoundTag transformedNbt) {
            EntityType<?> type = EntityType.by(transformedNbt).orElse(null);
            Entity entity = EntityType.create(transformedNbt, level).orElse(null);
            List<SlotStack> contents = new ArrayList<>();
            ItemStack material = ItemStack.EMPTY;
            if (entity instanceof Container container) {
                extractContainer(container, contents);
            }
            if (entity instanceof Boat boat) {
                Item drop = boat.getDropItem();
                material = drop == null || drop == Items.AIR ? new ItemStack(Items.OAK_BOAT) : new ItemStack(drop);
            } else if (entity instanceof AbstractMinecart cart) {
                material = cart.getPickResult();
            }
            if (material == null || material.isEmpty()) {
                material = vehicleItem(type);
            }
            if (material.isEmpty()) {
                return Planned.skip();
            }
            CompoundTag sanitized = entity == null ? transformedNbt.copy() : saveSanitized(entity);
            sanitized.putString("id", transformedNbt.getString("id"));
            List<FluidBuildAdapter.TankFluid> fluids = new ArrayList<>();
            if (entity != null) {
                IFluidHandler handler = fluidHandlerOf(entity);
                if (handler != null) {
                    for (int tank = 0; tank < handler.getTanks(); tank++) {
                        if (!handler.getFluidInTank(tank).isEmpty()) {
                            fluids.add(new FluidBuildAdapter.TankFluid(tank, handler.getFluidInTank(tank).copy()));
                        }
                    }
                }
                if (entity instanceof FluidTankMinecartEntity) {
                    material = ModItems.FLUID_TANK_MINECART.asStack();
                }
            }
            return new Planned(material, ItemStack.EMPTY, sanitized, List.copyOf(contents), fluids, false);
        }
    }

    private static final class ArmorStandAdapter implements EntityBuildAdapter {
        @Override
        public boolean matches(EntityType<?> type, CompoundTag nbt) {
            return type == EntityType.ARMOR_STAND;
        }

        @Override
        public Planned plan(ServerLevel level, StructureSnapshot.EntityEntry entry, CompoundTag transformedNbt) {
            Entity entity = EntityType.create(transformedNbt, level).orElse(null);
            if (!(entity instanceof ArmorStand stand)) {
                return Planned.skip();
            }
            List<SlotStack> contents = new ArrayList<>();
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = stand.getItemBySlot(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                contents.add(new SlotStack(slot.ordinal(), stack.copy()));
                stand.setItemSlot(slot, ItemStack.EMPTY);
            }
            return new Planned(
                new ItemStack(Items.ARMOR_STAND),
                ItemStack.EMPTY,
                saveSanitized(stand),
                List.copyOf(contents),
                List.of(),
                false
            );
        }

        @Override
        public void insertContents(Entity entity, List<SlotStack> contents, HolderLookup.Provider registries) {
            if (!(entity instanceof ArmorStand stand)) {
                return;
            }
            EquipmentSlot[] slots = EquipmentSlot.values();
            for (SlotStack content : contents) {
                if (content.slot() >= 0 && content.slot() < slots.length) {
                    stand.setItemSlot(slots[content.slot()], content.stack().copy());
                }
            }
        }
    }

    private static final class HangingAdapter implements EntityBuildAdapter {
        @Override
        public boolean matches(EntityType<?> type, CompoundTag nbt) {
            return type == EntityType.ITEM_FRAME
                || type == EntityType.GLOW_ITEM_FRAME
                || type == EntityType.PAINTING;
        }

        @Override
        public Planned plan(ServerLevel level, StructureSnapshot.EntityEntry entry, CompoundTag transformedNbt) {
            Entity entity = EntityType.create(transformedNbt, level).orElse(null);
            if (entity == null) {
                return Planned.skip();
            }
            List<SlotStack> contents = new ArrayList<>();
            ItemStack material;
            if (entity instanceof ItemFrame frame) {
                material = new ItemStack(frame.getType() == EntityType.GLOW_ITEM_FRAME
                    ? Items.GLOW_ITEM_FRAME
                    : Items.ITEM_FRAME);
                ItemStack displayed = frame.getItem();
                if (!displayed.isEmpty()) {
                    contents.add(new SlotStack(0, displayed.copy()));
                    frame.setItem(ItemStack.EMPTY);
                }
            } else if (entity instanceof Painting) {
                material = new ItemStack(Items.PAINTING);
            } else {
                return Planned.skip();
            }
            return new Planned(material, ItemStack.EMPTY, saveSanitized(entity), List.copyOf(contents), List.of(), false);
        }

        @Override
        public void insertContents(Entity entity, List<SlotStack> contents, HolderLookup.Provider registries) {
            if (!(entity instanceof ItemFrame frame) || contents.isEmpty()) {
                return;
            }
            frame.setItem(contents.getFirst().stack().copy(), false);
        }
    }

    private static ItemStack vehicleItem(@Nullable EntityType<?> type) {
        if (type == EntityType.HOPPER_MINECART) {
            return new ItemStack(Items.HOPPER_MINECART);
        }
        if (type == EntityType.CHEST_MINECART) {
            return new ItemStack(Items.CHEST_MINECART);
        }
        if (type == EntityType.FURNACE_MINECART) {
            return new ItemStack(Items.FURNACE_MINECART);
        }
        if (type == EntityType.TNT_MINECART) {
            return new ItemStack(Items.TNT_MINECART);
        }
        if (type == EntityType.COMMAND_BLOCK_MINECART) {
            return new ItemStack(Items.COMMAND_BLOCK_MINECART);
        }
        if (type == EntityType.MINECART) {
            return new ItemStack(Items.MINECART);
        }
        if (type == EntityType.BOAT) {
            return new ItemStack(Items.OAK_BOAT);
        }
        if (type == EntityType.CHEST_BOAT) {
            return new ItemStack(Items.OAK_CHEST_BOAT);
        }
        return ItemStack.EMPTY;
    }

    @Nullable
    private static IFluidHandler fluidHandlerOf(Entity entity) {
        if (entity instanceof IFluidHandlerHolder holder) {
            return holder.getFluidHandler();
        }
        return entity.getCapability(Capabilities.FluidHandler.ENTITY, null);
    }
}
