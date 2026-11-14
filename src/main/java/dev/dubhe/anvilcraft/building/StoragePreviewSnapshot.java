package dev.dubhe.anvilcraft.building;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.StoragePortManager;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.SpaceSizeItemStacksResourceHandler;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.TypeLimitItemStacksResourceHandler;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler.ResourceKey;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.init.storage.ModStorageTypes;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import dev.dubhe.anvilcraft.saved.storage.CraftingStorage;
import dev.dubhe.anvilcraft.saved.storage.IStorageType;
import dev.dubhe.anvilcraft.saved.storage.ShulkerContainerStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 结构 NBT 中的只读存储快照，物品按类型与组件合并，数量独立保存。 */
public record StoragePreviewSnapshot(
    List<Entry> items, List<StorageServerStub.FluidEntry> fluids, StorageServerStub.Capacity capacity,
    double fullness, CraftingStorage crafting, boolean craftingUnlocked
) {
    public static final String KEY = "anvilcraft:storage_preview";
    private static final Codec<StorageServerStub.Capacity> CAPACITY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.fieldOf("space").forGetter(StorageServerStub.Capacity::space),
        Codec.INT.fieldOf("space_size").forGetter(StorageServerStub.Capacity::spaceSize),
        Codec.INT.fieldOf("types").forGetter(StorageServerStub.Capacity::typeCount),
        Codec.INT.fieldOf("type_limit").forGetter(StorageServerStub.Capacity::typeLimit)
    ).apply(instance, StorageServerStub.Capacity::new));
    private static final Codec<StorageServerStub.FluidEntry> FLUID_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        FluidStack.CODEC.fieldOf("fluid").forGetter(StorageServerStub.FluidEntry::icon),
        Codec.intRange(0, Integer.MAX_VALUE).fieldOf("amount").forGetter(StorageServerStub.FluidEntry::amount)
    ).apply(instance, StorageServerStub.FluidEntry::new));
    public static final Codec<StoragePreviewSnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Entry.CODEC.listOf().fieldOf("items").forGetter(StoragePreviewSnapshot::items),
        FLUID_CODEC.listOf().optionalFieldOf("fluids", List.of()).forGetter(StoragePreviewSnapshot::fluids),
        CAPACITY_CODEC.fieldOf("capacity").forGetter(StoragePreviewSnapshot::capacity),
        Codec.doubleRange(0, 1).fieldOf("fullness").forGetter(StoragePreviewSnapshot::fullness),
        CraftingStorage.CODEC.codec().optionalFieldOf("crafting", CraftingStorage.EMPTY).forGetter(StoragePreviewSnapshot::crafting),
        Codec.BOOL.optionalFieldOf("crafting_unlocked", false).forGetter(StoragePreviewSnapshot::craftingUnlocked)
    ).apply(instance, StoragePreviewSnapshot::new));

    public StoragePreviewSnapshot {
        items = List.copyOf(items);
        fluids = fluids.stream().map(entry -> new StorageServerStub.FluidEntry(entry.icon().copy(), entry.amount())).toList();
        crafting = new CraftingStorage(crafting.stonecutterInput().copy(), crafting.craftingInput().stream().map(ItemStack::copy).toList(),
            crafting.stonecutterSelected(), crafting.lastOpened(), crafting.autoFill(), crafting.toStorage());
    }

    /** 一个物品定义只保存一次，数量可以超过普通物品堆叠上限。 */
    public record Entry(ItemStack icon, long count) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.fieldOf("item").forGetter(Entry::icon),
            Codec.LONG.validate(value -> value > 0 ? DataResult.success(value)
                : DataResult.error(() -> "Storage snapshot count must be positive")).fieldOf("count").forGetter(Entry::count)
        ).apply(instance, Entry::new));

        public Entry {
            icon = icon.copyWithCount(1);
        }
    }

    public static StoragePreviewSnapshot empty(IStorageType<?> type) {
        int space = type == ModStorageTypes.CRATE.get() ? 2048
            : type == ModStorageTypes.LARGE_CRATE.get() || type == ModStorageTypes.SHULKER_CONTAINER.get() ? 65536 : Integer.MAX_VALUE;
        int types = type == ModStorageTypes.SHULKER_CONTAINER.get() ? ShulkerContainerStorage.DEFAULT_TYPE_LIMIT : Integer.MAX_VALUE;
        return new StoragePreviewSnapshot(List.of(), List.of(), new StorageServerStub.Capacity(0, space, 0, types),
            0, CraftingStorage.EMPTY, false);
    }

    public static StoragePreviewSnapshot capture(BaseStorage<?> storage) {
        Map<ResourceKey, Long> contents = new LinkedHashMap<>();
        var available = storage.getAvailableItems();
        for (int slot = 0; slot < available.size(); slot++) {
            long amount = available.getAmountAsLong(slot);
            if (amount > 0) {
                contents.merge(ResourceKey.of(available.getStackInSlot(slot)), amount, Math::addExact);
            }
        }
        var items = storage.getItems();
        int used = items instanceof SpaceSizeItemStacksResourceHandler space ? space.getSpace() : 0;
        int size = items instanceof SpaceSizeItemStacksResourceHandler space ? space.getSpaceSize()
            : items instanceof TypeLimitItemStacksResourceHandler types ? types.getSpaceSize() : Integer.MAX_VALUE;
        var capacity = new StorageServerStub.Capacity(used, size, items.getTypeCount(), items.getTypeLimit());
        return new StoragePreviewSnapshot(contents.entrySet().stream().map(entry ->
            new Entry(entry.getKey().stack(), entry.getValue())).toList(), StoragePortManager.collect(storage.getId()),
            capacity, Math.clamp(items.getFullness(), 0, 1), storage.getCrafting(), storage.getRecipeBases() != null);
    }

    public static CompoundTag captureBlockEntity(BlockEntity entity) {
        var level = entity.getLevel();
        if (level == null) {
            throw new IllegalStateException("Cannot capture a detached block entity");
        }
        CompoundTag tag = entity.saveWithFullMetadata(level.registryAccess());
        if (entity instanceof StorageBlockEntity storage && level instanceof ServerLevel serverLevel) {
            if (entity.getBlockState().getBlock() instanceof AbstractMultiPartBlock<?> multi && !multi.isMainPart(entity.getBlockState())) {
                return tag;
            }
            var stores = serverLevel.getServer().overworld().getDataStorage().computeIfAbsent(Storages.TYPE, "storages");
            var snapshot = storage.getId() == null ? empty(storage.getStorageType())
                : stores.get(storage.getId()).map(StoragePreviewSnapshot::capture).orElseGet(() -> empty(storage.getStorageType()));
            tag.put(KEY, CODEC.encodeStart(level.registryAccess().createSerializationContext(NbtOps.INSTANCE), snapshot).getOrThrow());
        }
        return tag;
    }

    public static StoragePreviewSnapshot read(CompoundTag data, HolderLookup.Provider registries, IStorageType<?> type) {
        if (!data.contains(KEY)) {
            return empty(type);
        }
        return CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), data.get(KEY)).getOrThrow();
    }
}
