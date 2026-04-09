package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.DiskData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public class DiskItem extends Item {

    private static final String TOOLTIP_PREFIX = "tooltip.anvilcraft.item.disk.";
    private static final String MESSAGE_PREFIX = "message.anvilcraft.disk.";
    private static final Component TOOLTIP_STORE = tooltip("store");
    private static final Component TOOLTIP_CLEAR = tooltip("clear");
    private static final Component MESSAGE_STORED = message("data_stored");
    private static final Component MESSAGE_CLEARED = message("data_cleared");
    private static final Component MESSAGE_APPLIED = message("data_applied");
    private static final Component MESSAGE_INCOMPATIBLE = messageFailed("data_incompatible");

    public DiskItem(Properties properties) {
        super(properties);
    }

    /**
     * 磁盘中是否存储有数据
     */
    public static boolean hasDataStored(ItemStack stack) {
        return stack.has(ModComponents.DISK_DATA);
    }

    public static CompoundTag getData(ItemStack stack) {
        return stack.getOrDefault(ModComponents.DISK_DATA, new DiskData(new CompoundTag()))
            .tag();
    }

    public static CompoundTag createData(ItemStack stack) {
        CompoundTag tag = new CompoundTag();
        stack.set(ModComponents.DISK_DATA, new DiskData(tag));
        return tag;
    }

    public static void deleteData(ItemStack stack) {
        stack.remove(ModComponents.DISK_DATA);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return hasDataStored(stack);
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        Item.TooltipContext context,
        List<Component> tooltipComponents,
        TooltipFlag isAdvanced) {
        super.appendHoverText(stack, context, tooltipComponents, isAdvanced);
        if (hasDataStored(stack)) {
            ResourceLocation storedFrom = ResourceLocation.parse(getData(stack).getString("StoredFrom"));
            String name = Component.translatable("block." + storedFrom.toLanguageKey())
                .getString();
            tooltipComponents.add(Component.translatable("item.anvilcraft.disk.stored_from", name)
                .withStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
            tooltipComponents.add(TOOLTIP_CLEAR);
        } else {
            tooltipComponents.add(TOOLTIP_STORE);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.PASS;
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown()) return InteractionResult.FAIL;
        BlockPos clickedPos = context.getClickedPos();
        if (!level.getBlockState(clickedPos).hasBlockEntity()) return InteractionResult.PASS;
        BlockEntity blockEntity = level.getBlockEntity(clickedPos);
        if (!(blockEntity instanceof IDiskCloneable diskCloneable)) return InteractionResult.PASS;
        ItemStack stack = context.getItemInHand();
        if (hasDataStored(stack)) {
            CompoundTag tag = getData(stack);
            if (!tag.getString("StoredFrom").equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()).toString())) {
                player.displayClientMessage(MESSAGE_INCOMPATIBLE, true);
                return InteractionResult.FAIL;
            }
            diskCloneable.applyDiskData(tag);
            player.displayClientMessage(MESSAGE_APPLIED, true);
        } else {
            CompoundTag tag = createData(stack);
            tag.putString(
                "StoredFrom",
                BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()).toString()
            );
            diskCloneable.storeDiskData(tag);
            player.displayClientMessage(MESSAGE_STORED, true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
        Level level, Player player, InteractionHand usedHand) {
        if (!level.isClientSide && player.isShiftKeyDown()) {
            ItemStack itemStack = player.getItemInHand(usedHand);
            if (hasDataStored(itemStack)) {
                deleteData(itemStack);
                player.displayClientMessage(MESSAGE_CLEARED, true);
                return InteractionResultHolder.success(itemStack);
            }
        }
        return super.use(level, player, usedHand);
    }

    private static Component tooltip(String suffix) {
        return Component.translatable(TOOLTIP_PREFIX + suffix)
            .withStyle(ChatFormatting.GRAY);
    }

    private static Component message(String suffix) {
        return Component.translatable(MESSAGE_PREFIX + suffix);
    }

    @SuppressWarnings("SameParameterValue")
    private static Component messageFailed(String suffix) {
        return Component.translatable(MESSAGE_PREFIX + suffix)
            .withStyle(ChatFormatting.RED);
    }
}
