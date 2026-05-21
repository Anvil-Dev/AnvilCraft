package dev.dubhe.anvilcraft.block.entity.nesting;

import dev.dubhe.anvilcraft.api.itemhandler.IItemResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.OverLimitItemHandler;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.OverLimitItemContainerContents;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public abstract class BaseNestingShulkerBoxBlockEntity extends BlockEntity implements IItemResourceHandlerHolder, Nameable {
    @Getter
    private final OverLimitItemHandler items;
    @Nullable
    private Component name;

    public BaseNestingShulkerBoxBlockEntity(int baseLimit, BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.items = new OverLimitItemHandler(baseLimit, 1);
    }

    @Override
    public ResourceHandler<ItemResource> getItemHandler() {
        return this.items;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items.deserialize(input);
        Optional<ValueInput> customName = input.child("custom_name");

        if (customName.isPresent()) {
            this.name = parseCustomNameSafe(input, "custom_name");
        }
        super.loadAdditional(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.items.serialize(output);
        super.saveAdditional(output);
        if (this.name != null) {
            output.storeNullable("custom_name", ComponentSerialization.CODEC, this.name);
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
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        this.name = components.get(DataComponents.CUSTOM_NAME);
        components.getOrDefault(ModComponents.OVER_LIMIT_CONTAINER, OverLimitItemContainerContents.EMPTY)
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
