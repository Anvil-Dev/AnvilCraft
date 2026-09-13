package dev.dubhe.anvilcraft.saved.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.UnlimitedItemStack;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import it.unimi.dsi.fastutil.ints.IntObjectBiConsumer;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.UUID;
import java.util.function.Function;

@Getter
public abstract class BaseStorage<T extends UnlimitedItemStacksResourceHandler> {
    public static final MapCodec<BaseStorage<?>> CODEC = StorageType.CODEC
        .dispatchMap(StorageType::find, StorageType::codec);
    public static final StreamCodec<RegistryFriendlyByteBuf, BaseStorage<?>> STREAM_CODEC = StorageType.STREAM_CODEC
        .<RegistryFriendlyByteBuf>cast()
        .dispatch(StorageType::find, StorageType::streamCodec);
    private final UUID id;
    private final T items = this.constructItemHandler(this::onContentsChanged);
    private CraftingStorage crafting = CraftingStorage.EMPTY;
    private boolean craftingUnlocked;

    protected BaseStorage(UUID id) {
        this.id = id;
    }

    public void setCrafting(CraftingStorage crafting) {
        this.crafting = crafting;
        Storages.get().setDirty();
    }

    public boolean unlockCrafting() {
        if (this.craftingUnlocked) return true;
        int workbench = -1;
        int stonecutter = -1;
        for (int slot = 0; slot < this.items.size(); slot++) {
            ItemResource resource = this.items.getResource(slot);
            if (resource.isEmpty() || this.items.getAmountAsLong(slot) <= 0) continue;
            if (resource.is(Tags.Items.PLAYER_WORKSTATIONS_CRAFTING_TABLES)) {
                workbench = slot;
            } else if (resource.is(ModItemTags.PLAYER_WORKSTATIONS_STONECUTTERS)) {
                stonecutter = slot;
            }
            if (workbench >= 0 && stonecutter >= 0) break;
        }
        if (workbench < 0 || stonecutter < 0) return false;
        try (Transaction transaction = Transaction.openRoot()) {
            if (this.items.extract(workbench, this.items.getResource(workbench), 1, transaction) != 1
                || this.items.extract(stonecutter, this.items.getResource(stonecutter), 1, transaction) != 1) return false;
            transaction.commit();
        }
        this.craftingUnlocked = true;
        Storages.get().setDirty();
        return true;
    }

    protected static <S extends BaseStorage<?>> MapCodec<S> withCrafting(MapCodec<S> body) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
            body.forGetter(Function.identity()),
            CraftingStorage.CODEC.codec().optionalFieldOf("crafting", CraftingStorage.EMPTY).forGetter(BaseStorage::getCrafting),
            Codec.BOOL.optionalFieldOf("crafting_unlocked", false).forGetter(BaseStorage::isCraftingUnlocked)
        ).apply(instance, BaseStorage::restoreCrafting));
    }

    protected static <S extends BaseStorage<?>> StreamCodec<RegistryFriendlyByteBuf, S> withCrafting(
        StreamCodec<RegistryFriendlyByteBuf, S> body
    ) {
        return StreamCodec.composite(
            body, Function.identity(),
            CraftingStorage.STREAM_CODEC, BaseStorage::getCrafting,
            ByteBufCodecs.BOOL, BaseStorage::isCraftingUnlocked,
            BaseStorage::restoreCrafting
        );
    }

    private static <S extends BaseStorage<?>> S restoreCrafting(S storage, CraftingStorage crafting, boolean unlocked) {
        BaseStorage<?> base = storage;
        base.crafting = crafting;
        base.craftingUnlocked = unlocked;
        return storage;
    }

    protected abstract T constructItemHandler(
        IntObjectBiConsumer<UnlimitedItemStack> onContentsChanged
    );

    protected void onContentsChanged(int index, UnlimitedItemStack original) {
        StorageServerStub.onContentsChanged(this.id);
        Storages.get().setDirty();
    }

    protected <S extends BaseStorage<?>> S sync(T items) {
        this.items.sync(items);
        return Util.cast(this);
    }
}
