package dev.dubhe.anvilcraft.saved.storage;

import com.mojang.datafixers.util.Pair;
import dev.anvilcraft.lib.v2.util.Util;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.event.StorageUnlockCraftingEvent;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;

@Getter
@Setter
public abstract class BaseStorage<T extends UnlimitedItemStacksResourceHandler> implements INBTSerializable<CompoundTag> {
    public static final String TYPE_KEY = "type";
    public static final String CRAFTING_KEY = "crafting";
    private final UUID id;
    private final T items = this.constructItemHandler(this::onContentsChanged);
    private CraftingStorage crafting = CraftingStorage.EMPTY;
    /**
     * 解锁合成功能时消耗的物品，按消耗顺序排列。
     *
     * <p>为 {@code null} 表示尚未解锁；空列表表示已解锁但没有消耗品记录：合并时已退还成实物的
     * 消耗品不再记录，以免破坏方块时重复退还。</p>
     */
    private @Nullable @Unmodifiable List<ItemStack> recipeBases;

    protected BaseStorage(UUID id) {
        this.id = id;
    }

    /**
     * 解锁合成功能：消耗储物内的一个玩家工作台与一个切石机，并记录消耗的物品。
     *
     * <p>扣除前发出 {@link StorageUnlockCraftingEvent}，其它模组可追加额外消耗槽位或取消事件。
     * 每个消耗槽位扣除一个物品，槽位下标非法或槽位为空时解锁失败且不消耗任何物品。</p>
     *
     * @return 解锁成功或此前已解锁时为 true
     */
    public boolean unlockCrafting() {
        if (this.recipeBases != null) {
            return true;
        }
        int workbench = -1;
        int stonecutter = -1;
        for (int i = 0; i < this.items.size(); i++) {
            ItemStack stack = this.items.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            if (stack.is(Tags.Items.PLAYER_WORKSTATIONS_CRAFTING_TABLES)) {
                workbench = i;
            } else if (stack.is(ModItemTags.PLAYER_WORKSTATIONS_STONECUTTERS)) {
                stonecutter = i;
            }
            if (workbench >= 0 && stonecutter >= 0) break;
        }
        if (workbench < 0 || stonecutter < 0) {
            return false;
        }
        StorageUnlockCraftingEvent event = new StorageUnlockCraftingEvent(this);
        event.addRecipeBaseSlot(workbench);
        event.addRecipeBaseSlot(stonecutter);
        if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
            return false;
        }
        List<Integer> slots = event.getRecipeBaseSlots();
        List<ItemStack> bases = new ArrayList<>(slots.size());
        for (int slot : slots) {
            if (slot < 0 || slot >= this.items.size()) {
                return false;
            }
            ItemStack stack = this.items.getStackInSlot(slot);
            if (stack.isEmpty()) {
                return false;
            }
            bases.add(stack.copyWithCount(1));
        }
        for (int slot : slots) {
            this.items.extractItem(slot, 1, false);
        }
        this.recipeBases = List.copyOf(bases);
        Storages.get().setDirty();
        return true;
    }

    protected abstract T constructItemHandler(
        BiConsumer<Integer, UnlimitedItemStack> onContentsChanged
    );

    protected void onContentsChanged(int index, UnlimitedItemStack original) {
        StorageServerStub.onContentsChanged(this.id);
        Storages.get().setDirty();
    }

    protected <S extends BaseStorage<?>> S sync(T items) {
        this.items.sync(items);
        return Util.cast(this);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("storage_id", this.id);
        IStorageType.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), this.getType())
            .ifSuccess(type -> tag.put(BaseStorage.TYPE_KEY, type));
        tag.put("items", this.items.serializeNBT(registries));
        if (this.recipeBases != null) {
            ListTag recipeBases = new ListTag();
            for (ItemStack base : this.recipeBases) {
                recipeBases.add(base.save(registries));
            }
            tag.put("recipe_bases", recipeBases);
        }
        CraftingStorage.CODEC.codec()
            .encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), this.crafting)
            .ifSuccess(tagValue -> tag.put(BaseStorage.CRAFTING_KEY, tagValue));
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.recipeBases = BaseStorage.readRecipeBases(provider, tag);
        if (tag.contains("items", Tag.TAG_COMPOUND)) {
            this.items.deserializeNBT(provider, tag.getCompound("items"));
        }
        if (tag.contains(BaseStorage.CRAFTING_KEY, Tag.TAG_COMPOUND)) {
            CraftingStorage.CODEC.codec()
                .parse(provider.createSerializationContext(NbtOps.INSTANCE), tag.get(BaseStorage.CRAFTING_KEY))
                .ifSuccess(crafting -> this.crafting = crafting);
        }
    }

    /**
     * 读取解锁合成功能所消耗的物品。
     *
     * <p>只写了布尔解锁标记的旧存档没有消耗品记录，按当时解锁固定消耗的工作台与切石机还原。</p>
     *
     * @return 消耗品记录；未解锁时为 null
     */
    private static @Nullable List<ItemStack> readRecipeBases(HolderLookup.Provider provider, CompoundTag tag) {
        if (tag.contains("recipe_bases", Tag.TAG_LIST)) {
            ListTag savedBases = tag.getList("recipe_bases", Tag.TAG_COMPOUND);
            List<ItemStack> bases = new ArrayList<>(savedBases.size());
            for (Tag savedBase : savedBases) {
                if (!(savedBase instanceof CompoundTag baseTag)) continue;
                ItemStack base = ItemStack.parseOptional(provider, baseTag);
                if (!base.isEmpty()) {
                    bases.add(base);
                }
            }
            return List.copyOf(bases);
        }
        if (!tag.getBoolean("crafting_unlocked")) {
            return null;
        }
        return List.of(new ItemStack(Items.CRAFTING_TABLE), new ItemStack(Items.STONECUTTER));
    }

    public abstract Holder<IStorageType<?>> getTypeHolder();

    public IStorageType<?> getType() {
        return this.getTypeHolder().value();
    }

    public static Map<UUID, BaseStorage<?>> loadFromNbt(String key, CompoundTag tag, HolderLookup.Provider registries) {
        Map<UUID, BaseStorage<?>> storages;
        if (tag.contains(key, Tag.TAG_COMPOUND)) {
            storages = BaseStorage.loadFromNbt(tag.getCompound(key), registries);
        } else {
            storages = new HashMap<>();
        }
        return storages;
    }

    public static Map<UUID, BaseStorage<?>> loadFromNbt(CompoundTag tag, HolderLookup.Provider registries) {
        Map<UUID, BaseStorage<?>> storages = new HashMap<>();
        for (String key : tag.getAllKeys()) {
            CompoundTag entryTag = tag.getCompound(key);
            if (!entryTag.contains(BaseStorage.TYPE_KEY, Tag.TAG_STRING)) continue;
            UUID id = UUID.fromString(key);
            BaseStorage<?> storage = BaseStorage.loadFromNbt(id, entryTag, registries);
            if (storage != null) {
                storages.put(id, storage);
            }
        }
        return storages;
    }

    public static @Nullable BaseStorage<?> loadFromNbt(UUID id, CompoundTag tag, HolderLookup.Provider registries) {
        return IStorageType.CODEC.decode(registries.createSerializationContext(NbtOps.INSTANCE), tag.get(BaseStorage.TYPE_KEY))
            .result()
            .map(Pair::getFirst)
            .map(type -> {
                BaseStorage<?> storage = type.newInstance(id);
                storage.deserializeNBT(registries, tag);
                return storage;
            })
            .orElse(null);
    }
}
