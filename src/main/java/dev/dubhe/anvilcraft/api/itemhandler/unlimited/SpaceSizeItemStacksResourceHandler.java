package dev.dubhe.anvilcraft.api.itemhandler.unlimited;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.util.UnlimitedItemStack;
import lombok.Getter;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemInstance;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

public class SpaceSizeItemStacksResourceHandler extends UnlimitedItemStacksResourceHandler {
    public static final String SPACE_SIZE_KEY = "space_size";
    public static final MapCodec<SpaceSizeItemStacksResourceHandler> CODEC = CodecUtil.mapCodec(
        UnlimitedItemStacksResourceHandler.STACKS_CODEC
            .fieldOf(UnlimitedItemStacksResourceHandler.STACKS_KEY)
            .forGetter(SpaceSizeItemStacksResourceHandler::copyToList),
        Codec.INT
            .fieldOf(SpaceSizeItemStacksResourceHandler.SPACE_SIZE_KEY)
            .forGetter(SpaceSizeItemStacksResourceHandler::getSpaceSize),
        SpaceSizeItemStacksResourceHandler::new
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SpaceSizeItemStacksResourceHandler> STREAM_CODEC =
        StreamCodec.composite(
            UnlimitedItemStacksResourceHandler.STACKS_STREAM_CODEC,
            SpaceSizeItemStacksResourceHandler::copyToList,
            ByteBufCodecs.VAR_INT,
            SpaceSizeItemStacksResourceHandler::getSpaceSize,
            SpaceSizeItemStacksResourceHandler::new
        );

    @Getter
    private int spaceSize;

    public SpaceSizeItemStacksResourceHandler(int spaceSize) {
        this(spaceSize, new NonNullList<>(new ArrayList<>(), UnlimitedItemStack.EMPTY));
    }

    public SpaceSizeItemStacksResourceHandler(int spaceSize, NonNullList<UnlimitedItemStack> stacks) {
        super(SpaceSizeItemStacksResourceHandler.trim(spaceSize, stacks));
        this.spaceSize = checkSpaceSize(spaceSize);
    }

    public SpaceSizeItemStacksResourceHandler(NonNullList<UnlimitedItemStack> stacks, int spaceSize) {
        this(spaceSize, stacks);
    }

    private static int checkSpaceSize(int spaceSize) {
        if (spaceSize < 0) {
            throw new IllegalArgumentException("Space size cannot be negative: " + spaceSize);
        }
        return spaceSize;
    }

    @Override
    protected int getCapacity(int index, ItemResource resource) {
        int currentAmount = (int) this.getAmountAsLong(index);
        int remainingSpace = Math.max(0, this.spaceSize - this.getSpace());
        long capacity = (long) currentAmount
                        + SpaceSizeItemStacksResourceHandler.computeCount(resource, remainingSpace);
        return (int) Math.min(Integer.MAX_VALUE, capacity);
    }

    @Override
    public int insert(ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        int inserted = super.insert(resource, amount, transaction);
        if (inserted > 0 || amount == 0) {
            return inserted;
        }

        int remainingSpace = this.spaceSize - this.getSpace();
        if (SpaceSizeItemStacksResourceHandler.computeCount(resource, remainingSpace) < 1) {
            return 0;
        }

        NonNullList<UnlimitedItemStack> expanded = new NonNullList<>(
            new ArrayList<>(this.copyToList()),
            UnlimitedItemStack.EMPTY
        );
        expanded.add(UnlimitedItemStack.EMPTY);
        this.setStacks(expanded);
        return super.insert(resource, amount, transaction);
    }

    @Override
    public void set(int index, ItemResource resource, int amount) {
        TransferPreconditions.checkNonNegative(amount);
        if (resource.isEmpty() && amount > 0) {
            throw new IllegalArgumentException("Resource is empty but the amount is positive: " + amount);
        }
        UnlimitedItemStack old = this.stacks.get(index);
        int oldSpace = SpaceSizeItemStacksResourceHandler.computeSpace(old, old.getCount());
        int newSpace = SpaceSizeItemStacksResourceHandler.computeSpace(resource, amount);
        if (this.getSpace() - oldSpace + newSpace > this.spaceSize) {
            throw new IllegalArgumentException("Stack does not fit in the available space");
        }
        super.set(index, resource, amount);
    }

    public void addSpaceSize(IntUnaryOperator adder) {
        int newSpaceSize = adder.applyAsInt(this.spaceSize);
        if (newSpaceSize >= this.spaceSize) {
            this.spaceSize = checkSpaceSize(newSpaceSize);
        }
    }

    public int getSpace() {
        int space = 0;
        for (UnlimitedItemStack stack : this.stacks) {
            space = (int) Math.min(
                Integer.MAX_VALUE,
                (long) space + SpaceSizeItemStacksResourceHandler.computeSpace(stack, stack.getCount())
            );
        }
        return space;
    }

    @Override
    public double getFullness() {
        return this.spaceSize == 0 ? 0 : (double) this.getSpace() / this.spaceSize;
    }

    @Override
    public void sync(UnlimitedItemStacksResourceHandler items) {
        if (!(items instanceof SpaceSizeItemStacksResourceHandler spaceHandler)) {
            super.sync(items);
            return;
        }
        this.spaceSize = spaceHandler.spaceSize;
        this.setStacks(SpaceSizeItemStacksResourceHandler.trim(this.spaceSize, items.copyToList()));
    }

    @Override
    public void serialize(net.minecraft.world.level.storage.ValueOutput output) {
        output.store(
            UnlimitedItemStacksResourceHandler.STACKS_KEY,
            UnlimitedItemStacksResourceHandler.STACKS_CODEC,
            UnlimitedItemStacksResourceHandler.trim(this.copyToList())
        );
        output.putInt(SpaceSizeItemStacksResourceHandler.SPACE_SIZE_KEY, this.spaceSize);
    }

    @Override
    public void deserialize(net.minecraft.world.level.storage.ValueInput input) {
        super.deserialize(input);
        input.getInt(SpaceSizeItemStacksResourceHandler.SPACE_SIZE_KEY)
            .ifPresent(size -> this.spaceSize = checkSpaceSize(size));
        this.setStacks(SpaceSizeItemStacksResourceHandler.trim(this.spaceSize, this.copyToList()));
    }

    private static NonNullList<UnlimitedItemStack> trim(int spaceSize, List<UnlimitedItemStack> stacks) {
        checkSpaceSize(spaceSize);
        NonNullList<UnlimitedItemStack> result = new NonNullList<>(new ArrayList<>(), UnlimitedItemStack.EMPTY);
        int usedSpace = 0;
        for (UnlimitedItemStack input : stacks) {
            if (input.isEmpty()) {
                continue;
            }

            int remainingSpace = spaceSize - usedSpace;
            int accepted = Math.min(
                input.getCount(),
                SpaceSizeItemStacksResourceHandler.computeCount(input, remainingSpace)
            );
            if (accepted <= 0) {
                continue;
            }

            UnlimitedItemStack acceptedStack = input.copy();
            acceptedStack.setCount(accepted);
            usedSpace += SpaceSizeItemStacksResourceHandler.computeSpace(acceptedStack, accepted);
            boolean merged = false;
            for (UnlimitedItemStack existing : result) {
                if (existing.isSameItemSameComponents(acceptedStack)) {
                    existing.setCount(existing.getCount() + accepted);
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                result.add(acceptedStack);
            }
        }
        return result;
    }

    public static int computeSpace(ItemResource resource, int count) {
        return computeSpace(resource.getMaxStackSize(), count);
    }

    public static int computeSpace(ItemInstance instance, int count) {
        return computeSpace(instance.getMaxStackSize(), count);
    }

    private static int computeSpace(int maxStackSize, int count) {
        long space = (long) Math.ceilDiv(64, maxStackSize) * count;
        return (int) Math.min(Integer.MAX_VALUE, space);
    }

    public static int computeCount(ItemResource resource, int space) {
        return computeCount(resource.getMaxStackSize(), space);
    }

    public static int computeCount(ItemInstance instance, int space) {
        return computeCount(instance.getMaxStackSize(), space);
    }

    private static int computeCount(int maxStackSize, int space) {
        return Math.floorDiv(space, Math.ceilDiv(64, maxStackSize));
    }
}
