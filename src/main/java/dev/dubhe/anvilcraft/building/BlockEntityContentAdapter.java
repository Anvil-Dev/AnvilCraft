package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.api.itemhandler.IItemResourceHandlerHolder;
import dev.dubhe.anvilcraft.block.entity.ProcessingTableBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ProblemReporter;
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
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
        if ((nbt.get("LootTable") instanceof StringTag) && !nbt.getStringOr("LootTable", "").isEmpty()) {
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
        ResourceHandler<ItemResource> handler = itemHandlerOf(loaded, level);
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
        ResourceHandler<ItemResource> handler = itemHandlerOf(blockEntity, blockEntity.getLevel());
        if (handler instanceof ItemStacksResourceHandler modifiable) {
            for (SlotStack content : contents) {
                if (content.slot() >= 0 && content.slot() < modifiable.size()) {
                    modifiable.set(content.slot(), ItemResource.of(content.stack()), content.stack().getCount());
                }
            }
            blockEntity.setChanged();
            return;
        }
        CompoundTag tag = blockEntity.saveWithFullMetadata(registries);
        ListTag items = (tag.get("Items") instanceof ListTag)
            ? BlueprintNbt.list(tag, "Items", Tag.TAG_COMPOUND)
            : new ListTag();
        for (SlotStack content : contents) {
            CompoundTag itemTag = new CompoundTag();
            itemTag.putByte("Slot", (byte) content.slot());
            itemTag.merge(BlueprintNbt.writeItem(registries, content.stack()));
            items.add(itemTag);
        }
        tag.put("Items", items);
        blockEntity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag));
        blockEntity.setChanged();
    }

    private static void parseVanillaItems(
        CompoundTag nbt,
        HolderLookup.Provider registries,
        List<SlotStack> contents
    ) {
        if (!(nbt.get("Items") instanceof ListTag)) {
            return;
        }
        ListTag items = BlueprintNbt.list(nbt, "Items", Tag.TAG_COMPOUND);
        NonNullList<ItemStack> stacks = NonNullList.withSize(Math.max(27, items.size()), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(TagValueInput.create(ProblemReporter.DISCARDING, registries, nbt), stacks);
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

    private static void extractHandler(ResourceHandler<ItemResource> handler, List<SlotStack> contents) {
        for (int slot = 0; slot < handler.size(); slot++) {
            ItemStack stack = handler.getResource(slot).toStack((int) handler.getAmountAsLong(slot));
            if (stack.isEmpty()) continue;
            contents.add(new SlotStack(slot, stack.copy()));
        }
    }

    @Nullable
    private static ResourceHandler<ItemResource> itemHandlerOf(@Nullable BlockEntity blockEntity, @Nullable Level level) {
        // 外部仓储通过组件引用全局数据，不能在规划时读取或清空实际仓储。
        if (blockEntity instanceof StorageBlockEntity) return null;
        if (blockEntity instanceof ProcessingTableBlockEntity table) return table.getInput();
        if (blockEntity instanceof IItemResourceHandlerHolder holder) {
            return holder.getItemHandler();
        }

        if (blockEntity == null || level == null) return null;
        return level.getCapability(Capabilities.Item.BLOCK, blockEntity.getBlockPos(),
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
            compound.remove("stacks");
            compound.remove("Items");
            if ((compound.get("components") instanceof CompoundTag)) {
                compound.getCompoundOrEmpty("components").remove("minecraft:container");
            }
            for (String key : List.copyOf(compound.keySet())) {
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
