package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Singularity crystal item that stores extreme celestial body snapshots.
 * Shows enchantment glint when data is stored.
 * Shift-right-click in air clears stored data like a disk.
 */
public class SingularityCrystalItem extends BlockItem {

    private static final String SNAPSHOT_KEY = "celestialSnapshot";

    public SingularityCrystalItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static boolean hasDataStored(ItemStack stack) {
        return CelestialForgingAnvilBlockEntity.loadSnapshotFromStack(stack) != null;
    }

    public static void deleteData(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        var tag = customData.copyTag();
        tag.remove(SNAPSHOT_KEY);
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return hasDataStored(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (!level.isClientSide && player.isShiftKeyDown()) {
            ItemStack itemStack = player.getItemInHand(usedHand);
            if (hasDataStored(itemStack)) {
                deleteData(itemStack);
                player.displayClientMessage(
                    Component.translatable("message.anvilcraft.disk.data_cleared"), true);
                return InteractionResultHolder.success(itemStack);
            }
        }
        return super.use(level, player, usedHand);
    }
}
