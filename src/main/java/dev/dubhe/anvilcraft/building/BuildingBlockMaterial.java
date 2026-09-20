package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.block.placement.ProcessingTablePlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;
import javax.annotation.Nullable;

record BuildingBlockMaterial(
    ItemStack stack, CompoundTag config, List<BlockEntityContentAdapter.SlotStack> contents, boolean requiresOperator
) {
    @SuppressWarnings("deprecation")
    static BuildingBlockMaterial extract(BlockState state, @Nullable CompoundTag nbt, Level level) {
        final HolderLookup.Provider registries = level.registryAccess();
        ItemStack stack = ProcessingTablePlacement.baseMaterial(state);
        BlockEntity entity = createEntity(state);
        if (entity == null) return new BuildingBlockMaterial(stack, new CompoundTag(), List.of(), false);
        boolean restricted = entity.onlyOpCanSetNbt()
            && !(entity instanceof SignBlockEntity) && !(entity instanceof LecternBlockEntity);
        if (nbt == null || nbt.isEmpty()) return new BuildingBlockMaterial(stack, new CompoundTag(), List.of(), restricted);
        String id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType()).toString();
        if (nbt.contains("id") && !id.equals(nbt.getString("id"))) {
            throw new IllegalArgumentException("Blueprint block entity type does not match its block");
        }
        CompoundTag input = nbt.copy();
        if (entity instanceof SignBlockEntity) {
            input = SignDecorationAdapter.sanitize(state, input, registries);
        }
        final CompoundTag settings = BlueprintBlockConfiguration.take(entity, input, registries);
        input = entity.saveWithFullMetadata(registries).merge(input);
        try {
            entity.loadWithComponents(input, registries);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid blueprint block entity data", exception);
        }
        BlockEntityContentAdapter.Extracted extracted = BlockEntityContentAdapter.extract(entity, registries, level);
        BlockEntity empty = createEntity(state);
        if (empty == null) throw new IllegalArgumentException("Missing blueprint block entity");
        empty.loadWithComponents(extracted.config(), registries);
        entity = empty;
        if (entity instanceof IFluidHandlerHolder || entity instanceof IFluidHandler) {
            entity.saveToItem(stack, registries);
            var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
            if (data != null) {
                CompoundTag clean = data.copyTag();
                BlueprintBlockConfiguration.strip(entity, clean, registries);
                BlockEntity defaults = createEntity(state);
                if (defaults != null) {
                    CompoundTag emptyData = defaults.saveWithFullMetadata(registries);
                    removeDefaults(clean, emptyData);
                }
                BlockItem.setBlockEntityData(stack, entity.getType(), clean);
            }
            return new BuildingBlockMaterial(stack, settings, extracted.contents(), restricted);
        }
        stack.applyComponents(entity.collectComponents());
        if (!stack.getPrototype().has(DataComponents.CONTAINER)
            && stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).equals(ItemContainerContents.EMPTY)) {
            stack.remove(DataComponents.CONTAINER);
        }
        CompoundTag config = entity.saveWithFullMetadata(registries);
        entity.removeComponentsFromTag(config);
        config.remove("components");
        BlueprintBlockConfiguration.strip(entity, config, registries);
        config.merge(settings);
        if (entity instanceof AbstractFurnaceBlockEntity) {
            config.remove("RecipesUsed");
            config.remove("BurnTime");
            config.remove("CookTime");
            config.remove("CookTimeTotal");
        }
        return new BuildingBlockMaterial(stack, config, extracted.contents(), restricted);
    }

    @SuppressWarnings("deprecation")
    BuildingBlockMaterial withConfiguration(BlockState state, CompoundTag extracted, HolderLookup.Provider registries) {
        BlockEntity entity = createEntity(state);
        if (entity == null) return this;
        CompoundTag remaining = extracted.copy();
        final CompoundTag safe = BlueprintBlockConfiguration.take(entity, remaining, registries);
        BlueprintBlockConfiguration.strip(entity, remaining, registries);
        CompoundTag defaults = entity.saveWithFullMetadata(registries);
        entity.removeComponentsFromTag(defaults);
        for (String key : List.copyOf(remaining.getAllKeys())) {
            var value = remaining.get(key);
            if (value != null && value.equals(defaults.get(key))) remaining.remove(key);
        }
        remaining.remove("id");
        remaining.remove("x");
        remaining.remove("y");
        remaining.remove("z");
        ItemStack material = this.stack.copy();
        if (!remaining.isEmpty()) {
            // 未列为安全配置的数据必须由实际消耗的物品携带，降级放置不能从蓝图补回。
            BlockItem.setBlockEntityData(material, entity.getType(), remaining);
        }
        return new BuildingBlockMaterial(material, safe, this.contents, this.requiresOperator);
    }

    record Supplied(ItemStack stack, List<BlockEntityContentAdapter.SlotStack> contents) {
    }

    static Supplied separateContents(ItemStack supplied, Level level) {
        if (!(supplied.getItem() instanceof BlockItem item)) return new Supplied(supplied.copy(), List.of());
        BlockEntity entity = createEntity(item.getBlock().defaultBlockState());
        if (entity == null) return new Supplied(supplied.copy(), List.of());
        var data = supplied.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data != null) entity.loadWithComponents(data.copyTag(), level.registryAccess());
        entity.applyComponentsFromItemStack(supplied);
        var extracted = BlockEntityContentAdapter.extract(entity, level.registryAccess(), level);
        ItemStack empty = supplied.copy();
        if (empty.getPrototype().has(DataComponents.CONTAINER)) empty.set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        else empty.remove(DataComponents.CONTAINER);
        if (data != null) {
            CompoundTag clean = extracted.config().copy();
            BlueprintBlockConfiguration.strip(entity, clean, level.registryAccess());
            BlockEntity defaults = createEntity(item.getBlock().defaultBlockState());
            if (defaults != null) removeDefaults(clean, defaults.saveWithFullMetadata(level.registryAccess()));
            CompoundTag original = data.copyTag();
            for (String key : List.copyOf(clean.getAllKeys())) {
                if (!original.contains(key)) clean.remove(key);
            }
            BlockItem.setBlockEntityData(empty, entity.getType(), clean);
        }
        return new Supplied(empty, extracted.contents());
    }

    private static void removeDefaults(CompoundTag tag, CompoundTag defaults) {
        for (String key : List.copyOf(tag.getAllKeys())) {
            var value = tag.get(key);
            var defaultValue = defaults.get(key);
            if (value instanceof CompoundTag compound && defaultValue instanceof CompoundTag defaultCompound) {
                removeDefaults(compound, defaultCompound);
                if (compound.isEmpty()) tag.remove(key);
            } else if (value != null && value.equals(defaultValue)) {
                tag.remove(key);
            }
        }
    }

    @Nullable
    private static BlockEntity createEntity(BlockState state) {
        return state.getBlock() instanceof EntityBlock block ? block.newBlockEntity(BlockPos.ZERO, state) : null;
    }
}
