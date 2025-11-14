package dev.dubhe.anvilcraft.api.container;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.container.category.ICategory;
import dev.dubhe.anvilcraft.api.container.upgrade.Upgrades;
import dev.dubhe.anvilcraft.util.ListUtil;
import dev.dubhe.anvilcraft.util.stack.UnlimitedItemStack;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

@Getter
public class ContainerStorage {
    private static final Codec<List<UnlimitedItemStack>> ITEMS_CODEC = CompoundTag.CODEC.xmap(
        ContainerStorage::deserializeItems,
        ContainerStorage::serializeItems
    );
    private static final StreamCodec<RegistryFriendlyByteBuf, List<UnlimitedItemStack>> ITEMS_STREAM_CODEC = ByteBufCodecs.COMPOUND_TAG
        .<RegistryFriendlyByteBuf>cast()
        .map(
            ContainerStorage::deserializeItems,
            ContainerStorage::serializeItems
        );
    public static final MapCodec<ContainerStorage> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        UUIDUtil.CODEC
            .fieldOf("id")
            .forGetter(ContainerStorage::getId),
        ITEMS_CODEC
            .optionalFieldOf("items", new ArrayList<>())
            .forGetter(ContainerStorage::getItems),
        ICategory.CODEC.listOf()
            .fieldOf("categories")
            .forGetter(ContainerStorage::getCategories),
        Upgrades.CODEC
            .fieldOf("upgrades")
            .forGetter(ContainerStorage::getUpgrades)
    ).apply(ins, ContainerStorage::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ContainerStorage> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        ContainerStorage::getId,
        ITEMS_STREAM_CODEC,
        ContainerStorage::getItems,
        ICategory.STREAM_CODEC.apply(ByteBufCodecs.list()),
        ContainerStorage::getCategories,
        Upgrades.STREAM_CODEC,
        ContainerStorage::getUpgrades,
        ContainerStorage::new
    );
    private final UUID id;
    private final List<UnlimitedItemStack> items = new ArrayList<>();
    private final List<ICategory> categories = new ArrayList<>();
    private final Upgrades upgrades = new Upgrades();

    public ContainerStorage(UUID id) {
        this.id = id;
    }

    private ContainerStorage(UUID id, List<UnlimitedItemStack> items, List<ICategory> categories, Upgrades upgrades) {
        this.id = id;
        this.items.addAll(items);
        this.categories.addAll(categories);
        this.upgrades.sync(upgrades);
    }

    public boolean addItem(ItemStack stack) {
        return this.items.add(new UnlimitedItemStack(stack));
    }

    public UnlimitedItemStack getItem(int index) {
        return ListUtil.safelyGet(this.items, index).orElse(UnlimitedItemStack.EMPTY);
    }

    public ItemStack split(int index, int amount) {
        UnlimitedItemStack stack = this.getItem(index);
        if (stack.getCount() == amount) {
            this.items.remove(index);
            return stack.toStack();
        } else {
            return stack.split(amount);
        }
    }

    public int getMaxStackSize() {
        return Item.DEFAULT_MAX_STACK_SIZE * this.upgrades.getStackPower();
    }

    public int getMaxStackSize(ItemStack stack) {
        return stack.getMaxStackSize() * this.upgrades.getStackPower();
    }

    public boolean isFull(UnlimitedItemStack stack) {
        return stack.getStack().getMaxStackSize() * this.upgrades.getStackPower() >= stack.getCount();
    }

    public boolean isMaxItems() {
        return this.items.size() >= this.upgrades.getEntryLimit();
    }

    public IntSet getOrder(Predicate<UnlimitedItemStack> filter, Comparator<UnlimitedItemStack> sorter) {
        record StackPair(UnlimitedItemStack stack, int originalOrder) {
        }

        StackPair[] a = new StackPair[this.items.size()];
        for (int i = 0; i < this.items.size(); i++) {
            UnlimitedItemStack stack = this.items.get(i);
            if (!filter.test(stack)) continue;
            a[i] = new StackPair(stack, i);
        }
        Arrays.sort(a, Comparator.comparing(StackPair::stack, sorter));
        IntSet order = new IntOpenHashSet();
        for (StackPair pair : a) {
            order.add(pair.originalOrder);
        }
        return IntSets.unmodifiable(order);
    }

    public void sync(ContainerStorage storage) {
        this.items.clear();
        this.items.addAll(storage.items);
        this.categories.clear();
        this.categories.addAll(storage.categories);
        this.upgrades.sync(storage.upgrades);
    }

    public void applyCategory(ContainerStorage source) {
        this.categories.clear();
        this.categories.addAll(source.categories);
    }

    private static CompoundTag serializeItems(List<UnlimitedItemStack> items) {
        CompoundTag nbt = new CompoundTag();

        ListTag itemsNbt = new ListTag();
        stackLoop:
        for (UnlimitedItemStack stack : items) {
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getStack().getItem()).toString();
            for (Tag tag : itemsNbt) {
                CompoundTag compounded = (CompoundTag) tag;
                if (compounded.getString("id").equals(itemId)) {
                    CompoundTag dataNbt = new CompoundTag();
                    dataNbt.put("components", DataComponentPatch.CODEC.encodeStart(
                        NbtOps.INSTANCE,
                        stack.getStack().getComponentsPatch()
                    ).getOrThrow());
                    dataNbt.putInt("count", stack.getCount());
                    compounded.getList("data", Tag.TAG_COMPOUND).add(dataNbt);
                    continue stackLoop;
                }
            }
            CompoundTag itemNbt = new CompoundTag();
            itemNbt.putString("id", itemId);

            CompoundTag dataNbt = new CompoundTag();
            dataNbt.put("components", DataComponentPatch.CODEC.encodeStart(
                NbtOps.INSTANCE,
                stack.getStack().getComponentsPatch()
            ).getOrThrow());
            dataNbt.putInt("count", stack.getCount());
            itemNbt.put("data", dataNbt);

            itemsNbt.add(itemNbt);
        }
        nbt.put("Items", itemsNbt);

        return nbt;
    }

    private static List<UnlimitedItemStack> deserializeItems(CompoundTag nbt) {
        ListTag itemsNbt = nbt.getList("Items", Tag.TAG_COMPOUND);

        List<UnlimitedItemStack> items = new ArrayList<>();
        for (Tag itemNbt : itemsNbt) {
            CompoundTag compounded = (CompoundTag) itemNbt;
            Holder<Item> itemHolder = BuiltInRegistries.ITEM.getHolder(ResourceLocation.parse(compounded.getString("id"))).orElseThrow();
            for (Tag dataNbt : compounded.getList("data", Tag.TAG_COMPOUND)) {
                CompoundTag compoundedData = (CompoundTag) dataNbt;
                int count = compoundedData.getInt("count");
                DataComponentPatch components = DataComponentPatch.CODEC.decode(
                    NbtOps.INSTANCE,
                    compoundedData.get("components")
                ).getOrThrow().getFirst();
                items.add(new UnlimitedItemStack(itemHolder, count, components));
            }
        }

        return items;
    }
}
