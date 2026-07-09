package dev.dubhe.anvilcraft.item.utility;

import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.DiskData;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class DiskItem extends Item {
    private static final String MESSAGE_PREFIX = "message.anvilcraft.disk.";
    private static final Component TOOLTIP_STORE = Component.translatable("tooltip.anvilcraft.item.disk.store")
        .withStyle(ChatFormatting.GRAY);
    private static final Component MESSAGE_STORED = message("data_stored");
    private static final Component MESSAGE_CLEARED = message("data_cleared");
    private static final Component MESSAGE_APPLIED = message("data_applied");
    private static final Component MESSAGE_INCOMPATIBLE = messageFailed("data_incompatible");

    public DiskItem(Properties properties) {
        super(properties);
    }

    /// 磁盘中是否存储有数据
    public static boolean hasDataStored(ItemStack stack) {
        return stack.has(ModComponents.DISK_DATA);
    }

    public static CompoundTag getData(ItemStack stack) {
        return stack.getOrDefault(ModComponents.DISK_DATA, new DiskData(new CompoundTag())).tag();
    }

    public static void deleteData(ItemStack stack) {
        stack.remove(ModComponents.DISK_DATA);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return hasDataStored(stack);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(
        ItemStack stack,
        TooltipContext context,
        TooltipDisplay display,
        Consumer<Component> builder,
        TooltipFlag tooltipFlag
    ) {
        if (!hasDataStored(stack)) builder.accept(TOOLTIP_STORE);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.PASS;
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown()) return InteractionResult.FAIL;
        BlockPos clickedPos = context.getClickedPos();
        if (!level.getBlockState(clickedPos).hasBlockEntity()) return InteractionResult.PASS;
        BlockEntity blockEntity = level.getBlockEntity(clickedPos);
        if (!(blockEntity instanceof IDiskCloneable diskCloneable)) return InteractionResult.PASS;
        ItemStack stack = context.getItemInHand();
        if (hasDataStored(stack)) {
            CompoundTag tag = stack.getOrDefault(ModComponents.DISK_DATA, new DiskData(new CompoundTag())).tag();
            if (!isCompatible(tag, blockEntity, diskCloneable)) {
                player.sendOverlayMessage(MESSAGE_INCOMPATIBLE);
                return InteractionResult.FAIL;
            }
            ValueInput input = TagValueInput.create(
                new ProblemReporter.ScopedCollector(log),
                level.registryAccess(),
                tag
            );
            diskCloneable.applyDiskData(input);
            player.sendOverlayMessage(MESSAGE_APPLIED);
        } else {
            TagValueOutput output = TagValueOutput.createWithContext(new ProblemReporter.ScopedCollector(log), level.registryAccess());
            output.store("StoredFrom", BuiltInRegistries.BLOCK_ENTITY_TYPE.byNameCodec(), blockEntity.getType());
            diskCloneable.storeDiskData(output);
            CompoundTag tag = output.buildResult();
            saveCompatibleGroups(tag, diskCloneable.getDiskCompatibleGroups());
            stack.set(ModComponents.DISK_DATA, new DiskData(tag));
            player.sendOverlayMessage(MESSAGE_STORED);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(
        Level level,
        Player player,
        InteractionHand usedHand
    ) {
        if (!level.isClientSide() && player.isShiftKeyDown()) {
            ItemStack itemStack = player.getItemInHand(usedHand);
            if (hasDataStored(itemStack)) {
                deleteData(itemStack);
                player.sendOverlayMessage(MESSAGE_CLEARED);
                return InteractionResult.SUCCESS;
            }
        }
        return super.use(level, player, usedHand);
    }

    private static Component message(String suffix) {
        return Component.translatable(MESSAGE_PREFIX + suffix);
    }

    @SuppressWarnings("SameParameterValue")
    private static Component messageFailed(String suffix) {
        return Component.translatable(MESSAGE_PREFIX + suffix)
            .withStyle(ChatFormatting.RED);
    }

    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack offhand = player.getOffhandItem();
        if (!(offhand.getItem() instanceof DiskItem) || !hasDataStored(offhand)) return;
        BlockPos pos = event.getPos();
        Level level = (Level) event.getLevel();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof IDiskCloneable diskCloneable)) return;
        CompoundTag tag = getData(offhand);
        if (!isCompatible(tag, blockEntity, diskCloneable)) {
            player.sendOverlayMessage(MESSAGE_INCOMPATIBLE);
            return;
        }
        ValueInput input = TagValueInput.create(
            new ProblemReporter.ScopedCollector(log),
            level.registryAccess(),
            tag
        );
        diskCloneable.applyDiskData(input);
        player.sendOverlayMessage(MESSAGE_APPLIED);
    }

    private static void saveCompatibleGroups(CompoundTag tag, List<String> groups) {
        CompoundTag groupsTag = new CompoundTag();
        groupsTag.putInt("Size", groups.size());
        for (int i = 0; i < groups.size(); i++) {
            groupsTag.putString("Group" + i, groups.get(i));
        }
        tag.put("CompatibleGroups", groupsTag);
    }

    private static List<String> loadCompatibleGroups(CompoundTag tag) {
        if (!tag.contains("CompatibleGroups")) return List.of();
        CompoundTag groupsTag = tag.getCompoundOrEmpty("CompatibleGroups");
        int size = groupsTag.getIntOr("Size", -1);
        if (size >= 0) {
            List<String> groups = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                String group = groupsTag.getStringOr("Group" + i, "");
                if (!group.isEmpty()) groups.add(group);
            }
            return groups;
        }
        return groupsTag.keySet().stream()
            .sorted()
            .map(key -> groupsTag.getStringOr(key, ""))
            .filter(group -> !group.isEmpty())
            .toList();
    }

    private static boolean isCompatible(CompoundTag tag, BlockEntity blockEntity, IDiskCloneable diskCloneable) {
        String storedFrom = tag.getStringOr("StoredFrom", "");
        String targetType = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()).toString();
        if (storedFrom.equals(targetType)) return true;

        List<String> storedGroups = loadCompatibleGroups(tag);
        if (storedGroups.isEmpty()) return false;
        List<String> targetGroups = diskCloneable.getDiskCompatibleGroups();
        return storedGroups.stream().anyMatch(targetGroups::contains);
    }
}
