package dev.dubhe.anvilcraft.util.stack;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.util.Util;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

@Getter
@Setter
public class UnlimitedItemStack implements INBTSerializable<CompoundTag> {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final UnlimitedItemStack EMPTY = new UnlimitedItemStack(ItemStack.EMPTY, 0);
    public static final MapCodec<UnlimitedItemStack> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        ItemStack.ITEM_NON_AIR_CODEC
            .fieldOf("id")
            .forGetter(stack -> stack.getStack().getItemHolder()),
        Codec.INT
            .fieldOf("count")
            .forGetter(UnlimitedItemStack::getCount),
        DataComponentPatch.CODEC
            .optionalFieldOf("components", DataComponentPatch.EMPTY)
            .forGetter(stack -> stack.getStack().getComponentsPatch())
    ).apply(inst, UnlimitedItemStack::new));
    public static final Codec<UnlimitedItemStack> CODEC = Codec.lazyInitialized(MAP_CODEC::codec);
    public static final StreamCodec<RegistryFriendlyByteBuf, UnlimitedItemStack> OPTIONAL_STREAM_CODEC = new StreamCodec<>() {
        private static final StreamCodec<RegistryFriendlyByteBuf, Holder<Item>> ITEM_STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.ITEM);

        public UnlimitedItemStack decode(RegistryFriendlyByteBuf buf) {
            int count = buf.readVarInt();
            if (count <= 0) return UnlimitedItemStack.EMPTY;
            Holder<Item> holder = ITEM_STREAM_CODEC.decode(buf);
            DataComponentPatch components = DataComponentPatch.STREAM_CODEC.decode(buf);
            return new UnlimitedItemStack(holder, count, components);
        }

        public void encode(RegistryFriendlyByteBuf buf, UnlimitedItemStack stack) {
            if (stack.isEmpty()) {
                buf.writeVarInt(0);
            } else {
                buf.writeVarInt(stack.getCount());
                ITEM_STREAM_CODEC.encode(buf, stack.getStack().getItemHolder());
                DataComponentPatch.STREAM_CODEC.encode(buf, stack.getStack().getComponentsPatch());
            }
        }
    };
    public static final StreamCodec<RegistryFriendlyByteBuf, UnlimitedItemStack> STREAM_CODEC = new StreamCodec<>() {
        public UnlimitedItemStack decode(RegistryFriendlyByteBuf buf) {
            UnlimitedItemStack stack = UnlimitedItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            if (stack.isEmpty()) throw new DecoderException("Empty ItemStack not allowed");
            return stack;
        }

        public void encode(RegistryFriendlyByteBuf buf, UnlimitedItemStack stack) {
            if (stack.isEmpty()) throw new EncoderException("Empty ItemStack not allowed");
            UnlimitedItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
        }
    };
    private ItemStack stack;
    private int count;

    public UnlimitedItemStack(ItemStack stack, int count) {
        this.count = Math.max(count, 0);
        if (this.count == 0) {
            this.stack = ItemStack.EMPTY;
        } else {
            this.stack = stack.copyWithCount(1);
        }
    }

    public UnlimitedItemStack(Holder<Item> itemHolder, int count, DataComponentPatch components) {
        this(new ItemStack(itemHolder, 1, components), count);
    }

    public UnlimitedItemStack(ItemStack stack) {
        this(stack, stack.getCount());
    }

    public boolean isEmpty() {
        return this.stack.isEmpty() || this.count == 0;
    }

    public UnlimitedItemStack copy() {
        return new UnlimitedItemStack(this.stack, this.count);
    }

    public UnlimitedItemStack copyWithCount(int count) {
        return new UnlimitedItemStack(this.stack, count);
    }

    public ItemStack copyToStackWithCount(int count) {
        return this.copyWithCount(Math.min(this.count, this.stack.getMaxStackSize())).toStack();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return Util.cast(CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), this).getOrThrow());
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        UnlimitedItemStack.parse(provider, nbt).ifPresent(this::copyFrom);
    }

    public static Optional<UnlimitedItemStack> parse(HolderLookup.Provider provider, Tag tag) {
        return CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), tag)
            .resultOrPartial(info -> LOGGER.error("Tried to load invalid item: '{}'", info));
    }

    /**
     * 该方法<b>不会</b>设置数量。请使用 {@link UnlimitedItemStack#setCount(int)} 设置数量。
     *
     * @param stack 提供物品和数据组件的 {@link ItemStack}
     */
    public void setStack(ItemStack stack) {
        this.stack = stack.copyWithCount(1);
    }

    public void grow(int count) {
        this.setCount(this.count + count);
    }

    public void copyFrom(UnlimitedItemStack stack) {
        this.setStack(stack.getStack());
        this.setCount(stack.getCount());
    }

    public boolean isSameItemSameComponents(ItemStack stack) {
        return ItemStack.isSameItemSameComponents(this.toStack(), stack);
    }

    public boolean isSameItemSameComponents(UnlimitedItemStack stack) {
        return this.isSameItemSameComponents(stack.toStack());
    }

    public static boolean listMatches(List<UnlimitedItemStack> list, List<UnlimitedItemStack> other) {
        if (list.size() != other.size()) return false;
        for (int i = 0; i < list.size(); i++) {
            if (!list.get(i).isSameItemSameComponents(other.get(i))) return false;
        }
        return true;
    }

    public ItemStack toStack() {
        return this.stack.copyWithCount(this.count);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UnlimitedItemStack stack1)) return false;
        return this.isSameItemSameComponents(stack1)
               && this.getCount() == stack1.getCount();
    }

    @Override
    public int hashCode() {
        return ((this.stack.getItem().hashCode() + 31) * 31 + Integer.hashCode(this.count)) * 31 + this.stack.getComponents().hashCode();
    }
}
