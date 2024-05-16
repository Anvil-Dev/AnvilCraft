package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.api.IDiskCloneable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DiskItem extends Item {

    public DiskItem(Properties properties) {
        super(properties);
    }

    /**
     * 磁盘中是否存储有数据
     */
    public static boolean hasDataStored(ItemStack stack) {
        return stack.getOrCreateTag().contains("DiskData")
                && stack.getOrCreateTag().get("DiskData") instanceof CompoundTag
                && !stack.getOrCreateTag().getCompound("DiskData").isEmpty();
    }

    public static CompoundTag getData(ItemStack stack) {
        return stack.getOrCreateTag().getCompound("DiskData");
    }


    public static CompoundTag createData(ItemStack stack) {
        CompoundTag tag = new CompoundTag();
        stack.getOrCreateTag().put("DiskData", tag);
        return tag;
    }

    public static void cancelData(ItemStack stack) {
        stack.getOrCreateTag().remove("DiskData");
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return hasDataStored(stack);
    }

    @Override
    public void appendHoverText(
            @NotNull ItemStack stack,
            @Nullable Level level,
            @NotNull List<Component> tooltipComponents,
            @NotNull TooltipFlag isAdvanced
    ) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        if (hasDataStored(stack)) {
            tooltipComponents.add(Component.translatable("item.anvilcraft.disk.has_enchantment"));
        }
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.PASS;
        if (!level.getBlockState(context.getClickedPos()).hasBlockEntity()) return InteractionResult.PASS;
        BlockEntity blockEntity = level.getBlockEntity(context.getClickedPos());
        if (context.getPlayer().isCrouching()) {
            if (hasDataStored(context.getItemInHand())) {
                cancelData(context.getItemInHand());
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.SUCCESS;
        }
        if (blockEntity instanceof IDiskCloneable diskCloneable) {
            ItemStack stack = context.getItemInHand();
            if (hasDataStored(stack)) {
                CompoundTag tag = getData(stack);
                if (!tag.getString("StoredFrom").equals(BuiltInRegistries.BLOCK_ENTITY_TYPE
                        .getKey(blockEntity.getType())
                        .toString())) return InteractionResult.PASS;
                diskCloneable.applyDiskData(tag);
                cancelData(stack);
            } else {
                CompoundTag tag = createData(stack);
                tag.putString(
                        "StoredFrom",
                        BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()).toString()
                );
                diskCloneable.storeDiskData(tag);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }
}
