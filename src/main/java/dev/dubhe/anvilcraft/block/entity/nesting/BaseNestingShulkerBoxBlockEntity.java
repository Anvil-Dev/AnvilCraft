package dev.dubhe.anvilcraft.block.entity.nesting;

import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.OverLimitItemHandler;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.OverLimitItemContainerContents;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;

public abstract class BaseNestingShulkerBoxBlockEntity extends BlockEntity implements IItemHandlerHolder, Nameable {
    @Getter
    private final OverLimitItemHandler items;
    @Nullable
    private Component name;

    public BaseNestingShulkerBoxBlockEntity(int baseLimit, BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.items = new OverLimitItemHandler(baseLimit, 1);
    }

    @Override
    public IItemHandler getItemHandler() {
        return this.items;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items.deserializeNBT(registries, tag);
        if (tag.contains("CustomName", Tag.OBJECT_HEADER)) {
            this.name = parseCustomNameSafe(tag.getString("CustomName"), registries);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.merge(this.items.serializeNBT(registries));
        if (this.name != null) {
            tag.putString("CustomName", Component.Serializer.toJson(this.name, registries));
        }
    }

    @Override
    public Component getName() {
        return this.name != null ? this.name : this.getDefaultName();
    }

    @Override
    public Component getDisplayName() {
        return this.getName();
    }

    @Override
    @Nullable
    public Component getCustomName() {
        return this.name;
    }

    protected abstract Component getDefaultName();

    @Override
    protected void applyImplicitComponents(DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        this.name = componentInput.get(DataComponents.CUSTOM_NAME);
        componentInput.getOrDefault(ModComponents.OVER_LIMIT_CONTAINER, OverLimitItemContainerContents.EMPTY)
            .copyInto(this.items);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (this.name != null) {
            components.set(DataComponents.CUSTOM_NAME, this.name);
        }
        components.set(ModComponents.OVER_LIMIT_CONTAINER, OverLimitItemContainerContents.fromItems(this.items));
    }
}