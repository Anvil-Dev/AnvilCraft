package dev.dubhe.anvilcraft.block.item;

import dev.dubhe.anvilcraft.block.ShulkerContainerBlock;
import dev.dubhe.anvilcraft.block.entity.ShulkerContainerBlockEntity;
import dev.dubhe.anvilcraft.block.state.OpenedCube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.ContainerStorageRef;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class ShulkerContainerBlockItem extends FlexibleMultiPartBlockItem<OpenedCube3x3PartHalf, BooleanProperty, Boolean> {
    public ShulkerContainerBlockItem(ShulkerContainerBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, @Nullable Player player, ItemStack stack, BlockState state) {
        if (!level.isClientSide) {
            Util.ifAllPresent(
                ShulkerContainerBlockItem.getStorageId(stack),
                () -> level.getBlockEntity(pos, ModBlockEntities.SHULKER_CONTAINER.get()),
                ShulkerContainerBlockItem::setStorageId
            );
        }
        return super.updateCustomBlockEntityTag(pos, level, player, stack, state);
    }

    private static Optional<UUID> getStorageId(ItemStack stack) {
        return stack.getOrDefault(ModComponents.CONTAINER_STORAGE, ContainerStorageRef.EMPTY).id();
    }

    public static void setStorageId(UUID storageId, ShulkerContainerBlockEntity entity) {
        entity.setStorageId(storageId);
    }
}
