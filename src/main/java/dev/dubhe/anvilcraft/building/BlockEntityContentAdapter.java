package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * 从结构方块实体 NBT 拆出真实物品槽,配置字段留在剥离后的 NBT 里供提交恢复。
 * 战利品表箱子不能在规划时生成物品,记为无法映射。
 */
public final class BlockEntityContentAdapter {
    public record SlotStack(int slot, ItemStack stack) {
    }

    public record Extracted(CompoundTag config, List<SlotStack> contents, boolean unmapped) {
        public static Extracted empty() {
            return new Extracted(new CompoundTag(), List.of(), false);
        }
    }

    private BlockEntityContentAdapter() {
    }

    public static Extracted extract(BlockState state, @Nullable CompoundTag nbt, HolderLookup.Provider registries) {
        if (nbt == null || nbt.isEmpty()) {
            return Extracted.empty();
        }
        return extract(BlockEntity.loadStatic(BlockPos.ZERO, state, nbt, registries), nbt, registries, null);
    }

    static Extracted extract(BlockEntity entity, HolderLookup.Provider registries, @Nullable Level level) {
        return extract(entity, entity.saveWithFullMetadata(registries), registries, level);
    }

    private static Extracted extract(
        @Nullable BlockEntity loaded, CompoundTag nbt, HolderLookup.Provider registries, @Nullable Level level
    ) {
        if (nbt.contains("LootTable", Tag.TAG_STRING) && !nbt.getString("LootTable").isEmpty()) {
            return new Extracted(stripResources(nbt), List.of(), true);
        }
        List<SlotStack> contents = new ArrayList<>();
        if (loaded instanceof LecternBlockEntity lectern) {
            ItemStack book = lectern.getBook();
            if (!book.isEmpty() && !book.is(Items.WRITTEN_BOOK) && !book.is(Items.WRITABLE_BOOK)) {
                throw new IllegalArgumentException("Blueprint lectern contains an invalid book");
            }
            if (!book.isEmpty()) contents.add(new SlotStack(0, book.copy()));
            CompoundTag config = stripResources(nbt);
            config.remove("Book");
            config.remove("Page");
            return new Extracted(config, List.copyOf(contents), false);
        }
        if (loaded instanceof JukeboxBlockEntity jukebox) {
            ItemStack record = jukebox.getTheItem();
            if (!record.isEmpty()) contents.add(new SlotStack(0, record.copy()));
            CompoundTag config = stripResources(nbt);
            config.remove("RecordItem");
            config.remove("ticks_since_song_started");
            return new Extracted(config, List.copyOf(contents), false);
        }
        if (loaded instanceof Container container) {
            extractContainer(container, contents);
            var components = loaded.collectComponents();
            if (components.has(DataComponents.CONTAINER)) {
                loaded.applyComponents(DataComponentMap.builder().addAll(components)
                    .set(DataComponents.CONTAINER, ItemContainerContents.EMPTY).build(), DataComponentPatch.EMPTY);
            }
            return new Extracted(configOf(loaded, registries), List.copyOf(contents), false);
        }
        IItemHandler handler = itemHandlerOf(loaded, level);
        if (handler != null) {
            extractHandler(handler, contents);
            CompoundTag config = configOf(loaded, registries);
            stripSerializedHandlerItems(config);
            return new Extracted(config, List.copyOf(contents), false);
        }
        parseVanillaItems(nbt, registries, contents);
        return new Extracted(stripResources(nbt), List.copyOf(contents), false);
    }

    public static void insert(
        BlockEntity blockEntity,
        List<SlotStack> contents,
        HolderLookup.Provider registries
    ) {
        if (contents.isEmpty()) {
            return;
        }
        if (blockEntity instanceof LecternBlockEntity lectern) {
            lectern.setBook(contents.getFirst().stack().copy());
            return;
        }
        if (blockEntity instanceof Container container) {
            for (SlotStack content : contents) {
                if (content.slot() >= 0 && content.slot() < container.getContainerSize()) {
                    container.setItem(content.slot(), content.stack().copy());
                }
            }
            container.setChanged();
            return;
        }
        IItemHandler handler = itemHandlerOf(blockEntity, blockEntity.getLevel());
        if (handler instanceof IItemHandlerModifiable modifiable) {
            for (SlotStack content : contents) {
                if (content.slot() >= 0 && content.slot() < modifiable.getSlots()) {
                    modifiable.setStackInSlot(content.slot(), content.stack().copy());
                }
            }
            blockEntity.setChanged();
            return;
        }
        CompoundTag tag = blockEntity.saveWithFullMetadata(registries);
        ListTag items = tag.contains("Items", Tag.TAG_LIST)
            ? tag.getList("Items", Tag.TAG_COMPOUND)
            : new ListTag();
        for (SlotStack content : contents) {
            CompoundTag itemTag = new CompoundTag();
            itemTag.putByte("Slot", (byte) content.slot());
            items.add(content.stack().save(registries, itemTag));
        }
        tag.put("Items", items);
        blockEntity.loadWithComponents(tag, registries);
        blockEntity.setChanged();
    }

    private static void parseVanillaItems(
        CompoundTag nbt,
        HolderLookup.Provider registries,
        List<SlotStack> contents
    ) {
        if (!nbt.contains("Items", Tag.TAG_LIST)) {
            return;
        }
        ListTag items = nbt.getList("Items", Tag.TAG_COMPOUND);
        NonNullList<ItemStack> stacks = NonNullList.withSize(Math.max(27, items.size()), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(nbt, stacks, registries);
        for (int slot = 0; slot < stacks.size(); slot++) {
            ItemStack stack = stacks.get(slot);
            if (!stack.isEmpty()) {
                contents.add(new SlotStack(slot, stack.copy()));
            }
        }
    }

    private static void extractContainer(Container container, List<SlotStack> contents) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) continue;
            contents.add(new SlotStack(slot, stack.copy()));
        }
    }

    private static void extractHandler(IItemHandler handler, List<SlotStack> contents) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            contents.add(new SlotStack(slot, stack.copy()));
        }
    }

    @Nullable
    private static IItemHandler itemHandlerOf(@Nullable BlockEntity blockEntity, @Nullable Level level) {
        // 外部仓储通过组件引用全局数据，不能在规划时读取或清空实际仓储。
        if (blockEntity instanceof StorageBlockEntity) return null;
        if (blockEntity instanceof IItemHandlerHolder holder) {
            return holder.getItemHandler();
        }
        if (blockEntity instanceof IItemHandler handler) return handler;
        if (blockEntity == null || level == null) return null;
        return level.getCapability(Capabilities.ItemHandler.BLOCK, blockEntity.getBlockPos(),
            blockEntity.getBlockState(), blockEntity, null);
    }

    private static CompoundTag configOf(BlockEntity blockEntity, HolderLookup.Provider registries) {
        return stripResources(blockEntity.saveWithFullMetadata(registries));
    }

    private static CompoundTag stripResources(CompoundTag nbt) {
        CompoundTag config = nbt.copy();
        config.remove("Items");
        config.remove("LootTable");
        config.remove("LootTableSeed");
        config.remove("x");
        config.remove("y");
        config.remove("z");
        return config;
    }

    private static void stripSerializedHandlerItems(CompoundTag config) {
        stripSerializedHandlerItems((Tag) config);
    }

    private static void stripSerializedHandlerItems(Tag tag) {
        if (tag instanceof CompoundTag compound) {
            compound.remove("SlotItem");
            compound.remove("Items");
            if (compound.contains("components", Tag.TAG_COMPOUND)) {
                compound.getCompound("components").remove("minecraft:container");
            }
            for (String key : List.copyOf(compound.getAllKeys())) {
                Tag child = compound.get(key);
                if (child != null) {
                    stripSerializedHandlerItems(child);
                }
            }
            return;
        }
        if (tag instanceof ListTag list) {
            for (Tag child : list) {
                stripSerializedHandlerItems(child);
            }
        }
    }
}
