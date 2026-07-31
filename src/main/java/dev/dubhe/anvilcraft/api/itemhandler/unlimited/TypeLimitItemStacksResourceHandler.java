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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntUnaryOperator;

public class TypeLimitItemStacksResourceHandler extends UnlimitedItemStacksResourceHandler {
    public static final String TYPE_LIMIT_KEY = "type_limit";
    public static final String SPACE_SIZE_KEY = SpaceSizeItemStacksResourceHandler.SPACE_SIZE_KEY;
    public static final MapCodec<TypeLimitItemStacksResourceHandler> CODEC = CodecUtil.mapCodec(
        UnlimitedItemStacksResourceHandler.STACKS_CODEC
            .fieldOf(UnlimitedItemStacksResourceHandler.STACKS_KEY)
            .forGetter(TypeLimitItemStacksResourceHandler::copyToList),
        Codec.INT
            .fieldOf(TypeLimitItemStacksResourceHandler.TYPE_LIMIT_KEY)
            .forGetter(TypeLimitItemStacksResourceHandler::getTypeLimit),
        Codec.INT
            .fieldOf(TypeLimitItemStacksResourceHandler.SPACE_SIZE_KEY)
            .forGetter(TypeLimitItemStacksResourceHandler::getSpaceSize),
        TypeLimitItemStacksResourceHandler::new
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, TypeLimitItemStacksResourceHandler> STREAM_CODEC =
        StreamCodec.composite(
            UnlimitedItemStacksResourceHandler.STACKS_STREAM_CODEC,
            TypeLimitItemStacksResourceHandler::copyToList,
            ByteBufCodecs.VAR_INT,
            TypeLimitItemStacksResourceHandler::getTypeLimit,
            ByteBufCodecs.VAR_INT,
            TypeLimitItemStacksResourceHandler::getSpaceSize,
            TypeLimitItemStacksResourceHandler::new
        );

    @Getter
    private final int typeLimit;
    @Getter
    private int spaceSize;

    public TypeLimitItemStacksResourceHandler(int spaceSize) {
        this(Integer.MAX_VALUE, spaceSize);
    }

    public TypeLimitItemStacksResourceHandler(int typeLimit, int spaceSize) {
        super(0);
        this.typeLimit = TypeLimitItemStacksResourceHandler.checkTypeLimit(typeLimit);
        this.spaceSize = TypeLimitItemStacksResourceHandler.checkSpaceSize(spaceSize);
    }

    private TypeLimitItemStacksResourceHandler(
        NonNullList<UnlimitedItemStack> stacks,
        int typeLimit,
        int spaceSize
    ) {
        super(TypeLimitItemStacksResourceHandler.trim(typeLimit, spaceSize, stacks));
        this.typeLimit = TypeLimitItemStacksResourceHandler.checkTypeLimit(typeLimit);
        this.spaceSize = TypeLimitItemStacksResourceHandler.checkSpaceSize(spaceSize);
    }

    private static int checkTypeLimit(int typeLimit) {
        if (typeLimit < 0) {
            throw new IllegalArgumentException("Type limit cannot be negative: " + typeLimit);
        }
        return typeLimit;
    }

    private static int checkSpaceSize(int spaceSize) {
        if (spaceSize < 0) {
            throw new IllegalArgumentException("Space size cannot be negative: " + spaceSize);
        }
        return spaceSize;
    }

    @Override
    protected int getCapacity(int index, ItemResource resource) {
        return TypeLimitItemStacksResourceHandler.computeCount(resource, this.spaceSize);
    }

    @Override
    public int insert(ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0 || TypeLimitItemStacksResourceHandler.computeCount(resource, this.spaceSize) < 1) {
            return 0;
        }

        int matchingIndex = this.findMatchingSlot(resource);
        if (matchingIndex >= 0) {
            return super.insert(matchingIndex, resource, amount, transaction);
        }

        int newIndex = this.findNewSlot();
        if (newIndex < 0) {
            return 0;
        }
        if (newIndex == this.size()) {
            NonNullList<UnlimitedItemStack> expanded = new NonNullList<>(
                new ArrayList<>(this.copyToList()),
                UnlimitedItemStack.EMPTY
            );
            expanded.add(UnlimitedItemStack.EMPTY);
            this.setStacks(expanded);
        }
        return super.insert(newIndex, resource, amount, transaction);
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, this.size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        UnlimitedItemStack stack = this.stacks.get(index);
        int matchingIndex = this.findMatchingSlot(resource);
        if (stack.isEmpty() && matchingIndex >= 0 && matchingIndex != index) {
            return 0;
        }
        return super.insert(index, resource, amount, transaction);
    }

    @Override
    public void set(int index, ItemResource resource, int amount) {
        Objects.checkIndex(index, this.size());
        TransferPreconditions.checkNonNegative(amount);
        if (resource.isEmpty() && amount > 0) {
            throw new IllegalArgumentException("Resource is empty but the amount is positive: " + amount);
        }
        if (!resource.isEmpty() && amount > 0) {
            int matchingIndex = this.findMatchingSlot(resource);
            if (matchingIndex >= 0 && matchingIndex != index) {
                throw new IllegalArgumentException("The resource already occupies another slot");
            }
            if (amount > TypeLimitItemStacksResourceHandler.computeCount(resource, this.spaceSize)) {
                throw new IllegalArgumentException("Stack does not fit in the space available to this item type");
            }
        }
        super.set(index, resource, amount);
    }

    public void addSpaceSize(IntUnaryOperator adder) {
        int newSpaceSize = adder.applyAsInt(this.spaceSize);
        if (newSpaceSize >= this.spaceSize) {
            this.spaceSize = TypeLimitItemStacksResourceHandler.checkSpaceSize(newSpaceSize);
        }
    }

    public int getSpace() {
        int space = 0;
        for (UnlimitedItemStack stack : this.stacks) {
            space = (int) Math.min(
                Integer.MAX_VALUE,
                (long) space + TypeLimitItemStacksResourceHandler.computeSpace(stack, stack.getCount())
            );
        }
        return space;
    }

    @Override
    public double getFullness() {
        if (this.typeLimit == 0 || this.spaceSize == 0) {
            return 0;
        }
        return this.getSpace() / ((double) this.typeLimit * this.spaceSize);
    }

    @Override
    public void sync(UnlimitedItemStacksResourceHandler items) {
        if (items instanceof TypeLimitItemStacksResourceHandler typeHandler) {
            this.spaceSize = Math.max(this.spaceSize, typeHandler.spaceSize);
        }
        this.setStacks(TypeLimitItemStacksResourceHandler.trim(this.typeLimit, this.spaceSize, items.copyToList()));
    }

    @Override
    public void serialize(ValueOutput output) {
        output.store(
            UnlimitedItemStacksResourceHandler.STACKS_KEY,
            UnlimitedItemStacksResourceHandler.STACKS_CODEC,
            UnlimitedItemStacksResourceHandler.trim(this.copyToList())
        );
        output.putInt(TypeLimitItemStacksResourceHandler.TYPE_LIMIT_KEY, this.typeLimit);
        output.putInt(TypeLimitItemStacksResourceHandler.SPACE_SIZE_KEY, this.spaceSize);
    }

    @Override
    public void deserialize(ValueInput input) {
        input.getInt(TypeLimitItemStacksResourceHandler.SPACE_SIZE_KEY)
            .ifPresent(size -> this.spaceSize = Math.max(this.spaceSize, TypeLimitItemStacksResourceHandler.checkSpaceSize(size)));
        input.read(UnlimitedItemStacksResourceHandler.STACKS_KEY, UnlimitedItemStacksResourceHandler.STACKS_CODEC)
            .ifPresent(stacks -> this.setStacks(
                TypeLimitItemStacksResourceHandler.trim(this.typeLimit, this.spaceSize, stacks)
            ));
    }

    protected int computeEmptySize(ItemResource resource) {
        int matchingIndex = this.findMatchingSlot(resource);
        int occupied = matchingIndex < 0
            ? 0
            : TypeLimitItemStacksResourceHandler.computeSpace(
                resource,
                (int) this.getAmountAsLong(matchingIndex)
            );
        return TypeLimitItemStacksResourceHandler.computeCount(resource, this.spaceSize - occupied);
    }

    protected int computeEmptySize(ItemInstance instance) {
        return TypeLimitItemStacksResourceHandler.computeCount(instance, this.spaceSize);
    }

    protected int findNewSlot() {
        for (int index = 0; index < this.size(); index++) {
            if (this.stacks.get(index).isEmpty()) {
                return index;
            }
        }
        return this.size() < this.typeLimit ? this.size() : -1;
    }

    private int findMatchingSlot(ItemResource resource) {
        for (int index = 0; index < this.size(); index++) {
            if (this.stacks.get(index).isSameItemSameComponents(resource)) {
                return index;
            }
        }
        return -1;
    }

    private static int findMatchingSlot(List<UnlimitedItemStack> stacks, UnlimitedItemStack target) {
        for (int index = 0; index < stacks.size(); index++) {
            if (stacks.get(index).isSameItemSameComponents(target)) {
                return index;
            }
        }
        return -1;
    }

    private static NonNullList<UnlimitedItemStack> trim(
        int typeLimit,
        int spaceSize,
        List<UnlimitedItemStack> stacks
    ) {
        TypeLimitItemStacksResourceHandler.checkTypeLimit(typeLimit);
        TypeLimitItemStacksResourceHandler.checkSpaceSize(spaceSize);
        NonNullList<UnlimitedItemStack> result = new NonNullList<>(new ArrayList<>(), UnlimitedItemStack.EMPTY);
        for (UnlimitedItemStack input : stacks) {
            if (input.isEmpty()) {
                continue;
            }

            int existingIndex = TypeLimitItemStacksResourceHandler.findMatchingSlot(result, input);
            if (existingIndex < 0 && result.size() >= typeLimit) {
                continue;
            }

            int capacity = TypeLimitItemStacksResourceHandler.computeCount(input, spaceSize);
            if (existingIndex >= 0) {
                UnlimitedItemStack existing = result.get(existingIndex);
                long mergedCount = (long) existing.getCount() + input.getCount();
                existing.setCount((int) Math.min(capacity, mergedCount));
            } else {
                UnlimitedItemStack accepted = input.copy();
                accepted.setCount(Math.min(capacity, input.getCount()));
                if (!accepted.isEmpty()) {
                    result.add(accepted);
                }
            }
        }
        return result;
    }

    public static int computeSpace(ItemResource resource, int count) {
        return SpaceSizeItemStacksResourceHandler.computeSpace(resource, count);
    }

    public static int computeSpace(ItemInstance instance, int count) {
        return SpaceSizeItemStacksResourceHandler.computeSpace(instance, count);
    }

    public static int computeCount(ItemResource resource, int space) {
        return SpaceSizeItemStacksResourceHandler.computeCount(resource, space);
    }

    public static int computeCount(ItemInstance instance, int space) {
        return SpaceSizeItemStacksResourceHandler.computeCount(instance, space);
    }
}
