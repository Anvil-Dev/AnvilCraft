package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.block.LargeCakeBlock;
import dev.dubhe.anvilcraft.block.UseItemOnBlock;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.item.LargeCakeBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

/** 撤销实际收走和恢复的资源；只解析副本，不执行挖掘或修改现场容器。 */
final class BuildingUndoResources {
    final List<ItemStack> items = new ArrayList<>();
    final List<FluidStack> fluids = new ArrayList<>();
    final List<ItemStack> containers = new ArrayList<>();
    private final Set<BlockPos> cakes = new HashSet<>();

    void item(ItemStack stack) {
        if (stack.isEmpty()) return;
        for (ItemStack existing : this.items) {
            if (!ItemStack.isSameItemSameComponents(existing, stack)) continue;
            existing.grow(stack.getCount());
            return;
        }
        this.items.add(stack.copy());
    }

    void fluid(FluidStack stack) {
        if (stack.isEmpty()) return;
        for (FluidStack existing : this.fluids) {
            if (!FluidStack.isSameFluidSameComponents(existing, stack)) continue;
            existing.grow(stack.getAmount());
            return;
        }
        this.fluids.add(stack.copy());
    }

    void block(ServerLevel level, BlockPos pos, BlockState state, @Nullable CompoundTag data) {
        if (state.isAir() || state.is(Blocks.STRUCTURE_VOID) || BlueprintIgnition.isIgnition(state)) return;
        if (state.hasProperty(CakeBlock.BITES) && state.getValue(CakeBlock.BITES) != 0) {
            throw new IllegalArgumentException("Partially consumed cake has no lossless item form");
        }
        for (var property : state.getValues().entrySet()) {
            String name = property.getKey().getName();
            boolean storedResource = name.equals("age") || name.equals("honey_level") || name.equals("charges")
                || state.is(Blocks.COMPOSTER) && name.equals("level");
            if (storedResource && property.getValue() instanceof Integer value && value > 0) {
                throw new IllegalArgumentException("Harvestable block state has no lossless resource mapping");
            }
        }
        if (state.is(Blocks.MOVING_PISTON)) {
            if (data == null || !data.contains("blockState")) throw new IllegalArgumentException("Missing moving block");
            this.block(level, pos, NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), data.getCompound("blockState")), null);
            return;
        }
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)
            || state.is(Blocks.BUBBLE_COLUMN)) this.fluid(new FluidStack(Fluids.WATER, 1000));
        if (state.is(Blocks.BUBBLE_COLUMN)) return;
        if (FluidBuildAdapter.isLiquidBlock(state)) {
            this.fluid(FluidBuildAdapter.liquidOf(state));
            return;
        }
        if (FluidBuildAdapter.isFilledCauldron(state)) {
            this.item(FluidBuildAdapter.cauldronItem(state));
            this.fluid(FluidBuildAdapter.cauldronFluidOf(state));
            return;
        }
        if (state.is(Blocks.POWDER_SNOW)) {
            this.item(new ItemStack(Items.POWDER_SNOW_BUCKET));
            this.containers.add(new ItemStack(Items.BUCKET));
            return;
        }
        CompoundTag copy = data == null ? null : data.copy();
        if (copy != null) {
            BlockEntity probe = BlockEntity.loadStatic(pos, state, copy, level.registryAccess());
            if (probe == null) throw new IllegalArgumentException("Unknown block entity");
            if (!(probe instanceof StorageBlockEntity)) {
                if (BlockEntityContentAdapter.extract(state, copy.copy(), level.registryAccess()).unmapped()) {
                    throw new IllegalArgumentException("Unmapped container resources");
                }
                IFluidHandler handler = probe instanceof IFluidHandlerHolder holder ? holder.getFluidHandler()
                    : probe instanceof IFluidHandler fluidHandler ? fluidHandler : null;
                if (handler != null) {
                    for (int tank = 0; tank < handler.getTanks(); tank++) {
                        FluidStack fluid = handler.getFluidInTank(tank).copy();
                        if (fluid.isEmpty()) continue;
                        FluidStack drained = handler.drain(fluid.copy(), IFluidHandler.FluidAction.EXECUTE);
                        if (drained.getAmount() != fluid.getAmount() || !FluidStack.isSameFluidSameComponents(drained, fluid)) {
                            throw new IllegalArgumentException("Unmapped tank resources");
                        }
                        if (!handler.getFluidInTank(tank).isEmpty()) throw new IllegalArgumentException("Tank cannot be emptied");
                        this.fluid(drained);
                    }
                    copy = probe.saveWithFullMetadata(level.registryAccess());
                }
            }
        }
        BuildingBlockMaterial material = BuildingBlockMaterial.extract(state, copy, level);
        material.contents().forEach(content -> this.item(content.stack()));
        BlueprintBlockConfiguration.materials(material.config()).forEach(this::item);
        if (!BlueprintMultiblocks.shouldRecord(state)) return;
        if (state.getBlock() instanceof LargeCakeBlock && !this.cakes.add(LargeCakeBlockItem.origin(pos, state))) return;
        if (material.stack().isEmpty()) throw new IllegalArgumentException("Unknown block material");
        this.item(material.stack().copyWithCount(BuildingRodService.materialCount(state)));
        BlueprintSpecialBlocks.extra(state).forEach(this::item);
        this.item(UseItemOnBlock.materialFor(state));
        if (SignDecorationAdapter.isSign(state) && copy != null) {
            SignDecorationAdapter.extract(state, copy, level.registryAccess()).decorations()
                .forEach(decoration -> this.item(decoration.material()));
        }
    }

    void entity(ServerLevel level, CompoundTag data) {
        this.entity(level, data, ItemStack.EMPTY);
    }

    void entity(ServerLevel level, CompoundTag data, ItemStack supplied) {
        CompoundTag copy = data.copy();
        Entity probe = EntityBuildAdapters.create(copy, level).orElseThrow();
        if (probe instanceof Mob && !probe.isAlive()) return;
        if (EntityBuildAdapters.isTransient(probe)) throw new IllegalArgumentException("Transient entity resources");
        EntityBuildAdapter adapter = EntityBuildAdapters.find(probe, copy).orElseThrow();
        var entry = new StructureSnapshot.EntityEntry(probe.position(), probe.blockPosition(), copy);
        EntityBuildAdapter.Planned plan = adapter.plan(level, entry, copy);
        if (plan.unsupported()) throw new IllegalArgumentException("Unknown entity resources");
        if (probe instanceof Mob && plan.material().isEmpty()) throw new IllegalArgumentException("Missing creature material");
        ItemStack material = supplied.isEmpty() ? plan.material().copy() : supplied.copyWithCount(1);
        if (!material.isEmpty() && !(probe instanceof ItemEntity)) {
            if (probe.getCustomName() == null) material.remove(DataComponents.CUSTOM_NAME);
            else material.set(DataComponents.CUSTOM_NAME, probe.getCustomName());
            if (supplied.has(DataComponents.ENTITY_DATA)) {
                CompoundTag body = plan.entityNbt().copy();
                for (String key : List.of("UUID", "Passengers", "leash", "Leash")) body.remove(key);
                material.set(DataComponents.ENTITY_DATA, CustomData.of(body));
            }
        }
        this.item(material);
        plan.contents().forEach(content -> this.item(content.stack()));
        plan.fluids().forEach(tank -> this.fluid(tank.fluid()));
        if (probe instanceof Leashable && (copy.contains("leash") || copy.contains("Leash"))) {
            this.item(new ItemStack(Items.LEAD));
        }
    }

    static void cancel(BuildingUndoResources recovered, BuildingUndoResources required) {
        recovered.containers.forEach(required::item);
        required.containers.forEach(recovered::item);
        for (ItemStack stack : required.items) stack.shrink(BuildingRegionSnapshot.subtract(recovered.items, stack));
        recovered.items.removeIf(ItemStack::isEmpty);
        required.items.removeIf(ItemStack::isEmpty);
        for (FluidStack needed : required.fluids) {
            for (FluidStack available : recovered.fluids) {
                if (!FluidStack.isSameFluidSameComponents(needed, available)) continue;
                int amount = Math.min(needed.getAmount(), available.getAmount());
                needed.shrink(amount);
                available.shrink(amount);
            }
        }
        recovered.fluids.removeIf(FluidStack::isEmpty);
        required.fluids.removeIf(FluidStack::isEmpty);
    }
}
