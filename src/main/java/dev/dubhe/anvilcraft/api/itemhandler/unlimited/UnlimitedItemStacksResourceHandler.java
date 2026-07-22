package dev.dubhe.anvilcraft.api.itemhandler.unlimited;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.util.UnlimitedItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.ArrayList;
import java.util.List;

public class UnlimitedItemStacksResourceHandler extends StacksResourceHandler<UnlimitedItemStack, ItemResource> {
    public static final String STACKS_KEY = "stacks";
    public static final Codec<NonNullList<UnlimitedItemStack>> STACKS_CODEC = UnlimitedItemStack.OPTIONAL_CODEC
        .listOf()
        .xmap(UnlimitedItemStacksResourceHandler::constructStackList, UnlimitedItemStacksResourceHandler::trim);
    public static final MapCodec<UnlimitedItemStacksResourceHandler> CODEC = CodecUtil.mapCodec(
        UnlimitedItemStacksResourceHandler.STACKS_CODEC
            .fieldOf(UnlimitedItemStacksResourceHandler.STACKS_KEY)
            .forGetter(UnlimitedItemStacksResourceHandler::copyToList),
        UnlimitedItemStacksResourceHandler::new
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, NonNullList<UnlimitedItemStack>> STACKS_STREAM_CODEC =
        UnlimitedItemStack.STREAM_CODEC
            .apply(ByteBufCodecs.list())
            .map(UnlimitedItemStacksResourceHandler::constructStackList, UnlimitedItemStacksResourceHandler::trim);
    public static final StreamCodec<RegistryFriendlyByteBuf, UnlimitedItemStacksResourceHandler> STREAM_CODEC =
        UnlimitedItemStacksResourceHandler.STACKS_STREAM_CODEC
            .map(UnlimitedItemStacksResourceHandler::new, UnlimitedItemStacksResourceHandler::copyToList);

    public UnlimitedItemStacksResourceHandler(int size) {
        super(size, UnlimitedItemStack.EMPTY, UnlimitedItemStack.OPTIONAL_CODEC);
    }

    public UnlimitedItemStacksResourceHandler(NonNullList<UnlimitedItemStack> stacks) {
        super(stacks, UnlimitedItemStack.EMPTY, UnlimitedItemStack.OPTIONAL_CODEC);
    }

    @Override
    protected ItemResource getResourceFrom(UnlimitedItemStack stack) {
        return ItemResource.of(stack.getStack());
    }

    @Override
    protected int getAmountFrom(UnlimitedItemStack stack) {
        return stack.getCount();
    }

    @Override
    protected UnlimitedItemStack getStackFrom(ItemResource resource, int amount) {
        return amount == 0 ? UnlimitedItemStack.EMPTY : new UnlimitedItemStack(resource, amount);
    }

    @Override
    protected UnlimitedItemStack copyOf(UnlimitedItemStack stack) {
        return stack.copy();
    }

    @Override
    protected int getCapacity(int index, ItemResource resource) {
        return Integer.MAX_VALUE;
    }

    @Override
    protected boolean matches(UnlimitedItemStack stack, ItemResource resource) {
        return stack.isSameItemSameComponents(resource);
    }

    public int getTypeCount() {
        int count = 0;
        for (UnlimitedItemStack stack : this.stacks) {
            if (!stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /**
     * This handler has no type limit. Subclasses that expose a finite type limit can override this method.
     */
    public int getTypeLimit() {
        return Integer.MAX_VALUE;
    }

    public double getFullness() {
        double fullness = 0.0;
        for (UnlimitedItemStack stack : this.stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            fullness += (double) stack.getCount() / stack.getMaxStackSize();
        }
        return fullness;
    }

    public void sync(UnlimitedItemStacksResourceHandler items) {
        NonNullList<UnlimitedItemStack> source = UnlimitedItemStacksResourceHandler.trim(items.copyToList());
        NonNullList<UnlimitedItemStack> synced = NonNullList.withSize(this.size(), UnlimitedItemStack.EMPTY);
        for (int index = 0; index < Math.min(source.size(), synced.size()); index++) {
            synced.set(index, source.get(index));
        }
        this.setStacks(synced);
    }

    protected static NonNullList<UnlimitedItemStack> constructStackList(List<UnlimitedItemStack> from) {
        NonNullList<UnlimitedItemStack> result = new NonNullList<>(new ArrayList<>(), UnlimitedItemStack.EMPTY);
        result.addAll(from);
        return result;
    }

    protected static NonNullList<UnlimitedItemStack> trim(List<UnlimitedItemStack> from) {
        NonNullList<UnlimitedItemStack> result = new NonNullList<>(new ArrayList<>(), UnlimitedItemStack.EMPTY);
        for (UnlimitedItemStack stack : from) {
            if (!stack.isEmpty()) {
                result.add(stack);
            }
        }
        return result;
    }
}
