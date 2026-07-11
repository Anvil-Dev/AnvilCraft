package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 保存极端天体快照的奇点晶体。存有数据时显示附魔光效，在空中按住 Shift 右键可清除数据。
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
    public InteractionResult use(Level level, Player player, InteractionHand usedHand) {
        if (!level.isClientSide() && player.isShiftKeyDown()) {
            ItemStack itemStack = player.getItemInHand(usedHand);
            if (hasDataStored(itemStack)) {
                deleteData(itemStack);
                player.sendSystemMessage(
                    Component.translatable("message.anvilcraft.disk.data_cleared"));
                return InteractionResult.SUCCESS;
            }
        }
        return super.use(level, player, usedHand);
    }
}
