package dev.dubhe.anvilcraft.api.pointer;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.block.BlockPlacementRules;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.init.registry.ModRegistries;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import dev.dubhe.anvilcraft.util.BlockPlacementUtil;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;

@Getter
public class BlockItemHandlerPointer implements ITargetPointer {
    private final Type type;
    private final BlockPos pos;
    private final Direction dir;
    private final int slot;
    private final ItemStack stack;

    public BlockItemHandlerPointer(Type type, BlockPos pos, Direction dir, int slot, ItemStack stack) {
        this.type = type;
        this.pos = pos;
        this.dir = dir;
        this.slot = slot;
        this.stack = stack.copy();
    }

    private @Nullable IItemHandler resolveHandler(Level level) {
        return ItemHandlerUtil.getSourceItemHandler(this.pos, this.dir, level);
    }

    @Override
    public boolean isStillValid(Level level) {
        IItemHandler handler = this.resolveHandler(level);
        return handler != null && this.matchesStoredStack(handler);
    }

    private boolean matchesStoredStack(IItemHandler handler) {
        return this.slot >= 0
            && this.slot < handler.getSlots()
            && ItemStack.isSameItemSameComponents(handler.getStackInSlot(this.slot), this.stack);
    }

    @Override
    public boolean matches(Level level, BlockState requiredState) {
        return BlockPlacementRules.getPlacementItemCount(level.registryAccess(), requiredState, this.stack) > 0;
    }

    @Override
    public Either<ItemStack, BlockState> getDisplayedBlock() {
        return Either.left(this.stack.copyWithCount(1));
    }

    @Override
    public boolean applyToPos(ServerLevel level, BlockPos pos) {
        return this.placeToPos(level, pos, null);
    }

    @Override
    public boolean applyToPos(ServerLevel level, BlockPos pos, BlockState requiredState) {
        if (!this.matches(level, requiredState)) {
            return false;
        }
        return this.placeToPos(level, pos, requiredState);
    }

    private boolean placeToPos(ServerLevel level, BlockPos pos, @Nullable BlockState requiredState) {
        IItemHandler handler = this.resolveHandler(level);
        if (handler == null || !this.matchesStoredStack(handler)) {
            return false;
        }

        int requiredCount = requiredState == null
            ? 1
            : BlockPlacementRules.getPlacementItemCount(level.registryAccess(), requiredState, this.stack);
        if (requiredCount < 0) {
            return false;
        }
        if (this.extractMatchingItems(handler, requiredCount, true) != requiredCount) {
            return false;
        }

        ItemStack stack = this.stack.copyWithCount(requiredCount);
        int initialCount = stack.getCount();
        ItemStack result = BlockPlacementUtil.placeBlock(level, pos, stack, requiredState);
        int consumed = initialCount - result.getCount();
        if (consumed <= 0) {
            return false;
        }

        if (this.extractMatchingItems(handler, requiredCount, false) != requiredCount) {
            return false;
        }
        // 放置后返还物品由 PlacementItem 定义（如细雪桶放置后返还空桶）
        BlockState returnState = requiredState != null
            ? requiredState
            : Block.byItem(this.stack.getItem()).defaultBlockState();
        ItemStack returnItem = BlockPlacementRules.getReturnItem(level.registryAccess(), returnState);
        if (!returnItem.isEmpty()) {
            ItemStack returnStack = returnItem.copyWithCount(consumed);
            for (int i = 0; i < handler.getSlots(); i++) {
                returnStack = handler.insertItem(i, returnStack, false);
                if (returnStack.isEmpty()) break;
            }
        }
        return true;
    }

    private int extractMatchingItems(IItemHandler handler, int amount, boolean simulate) {
        int extractedCount = 0;
        for (int slot = 0; slot < handler.getSlots() && extractedCount < amount; slot++) {
            ItemStack inSlot = handler.getStackInSlot(slot);
            if (!ItemStack.isSameItemSameComponents(inSlot, this.stack)) {
                continue;
            }
            ItemStack extracted = handler.extractItem(slot, amount - extractedCount, simulate);
            if (ItemStack.isSameItemSameComponents(extracted, this.stack)) {
                extractedCount += extracted.getCount();
            }
        }
        return extractedCount;
    }

    public static class Type implements ITargetPointer.Type<BlockItemHandlerPointer> {
        public static final Codec<Type> CODEC = ModRegistries.TARGET_POINTER_TYPE.byNameCodec().flatXmap(
            raw -> raw instanceof Type type
                   ? DataResult.success(type)
                   : DataResult.error(() -> "Cannot cast %s to BlockItemHandlerPointer.Type".formatted(raw.getClass().getSimpleName())),
            DataResult::success
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, Type> STREAM_CODEC = ByteBufCodecs.registry(
            ModRegistryKeys.TARGET_POINTER_TYPE
        ).map(Util::cast, Function.identity());
        public static final MapCodec<BlockItemHandlerPointer> POINTER_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Type.CODEC
                .fieldOf("type")
                .forGetter(BlockItemHandlerPointer::getType),
            BlockPos.CODEC
                .fieldOf("pos")
                .forGetter(BlockItemHandlerPointer::getPos),
            Direction.CODEC
                .fieldOf("dir")
                .forGetter(BlockItemHandlerPointer::getDir),
            Codec.INT
                .fieldOf("slot")
                .forGetter(BlockItemHandlerPointer::getSlot),
            ItemStack.OPTIONAL_CODEC
                .fieldOf("stack")
                .forGetter(BlockItemHandlerPointer::getStack)
        ).apply(inst, BlockItemHandlerPointer::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, BlockItemHandlerPointer> POINTER_STREAM_CODEC = StreamCodec.composite(
            Type.STREAM_CODEC,
            BlockItemHandlerPointer::getType,
            BlockPos.STREAM_CODEC,
            BlockItemHandlerPointer::getPos,
            Direction.STREAM_CODEC,
            BlockItemHandlerPointer::getDir,
            ByteBufCodecs.VAR_INT,
            BlockItemHandlerPointer::getSlot,
            ItemStack.OPTIONAL_STREAM_CODEC,
            BlockItemHandlerPointer::getStack,
            BlockItemHandlerPointer::new
        );

        private final @Nullable Predicate<ItemStack> filter;

        public Type(Predicate<ItemStack> filter) {
            this.filter = filter;
        }

        public Type() {
            this.filter = null;
        }

        @Override
        public MapCodec<BlockItemHandlerPointer> codec() {
            return Type.POINTER_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BlockItemHandlerPointer> streamCodec() {
            return Type.POINTER_STREAM_CODEC;
        }

        @Override
        public @Nullable BlockItemHandlerPointer point(
            Level level,
            BlockPos pos,
            Direction facing,
            @Nullable BlockState requiredState
        ) {
            IItemHandler handler = ItemHandlerUtil.getSourceItemHandler(pos, facing, level);
            if (handler == null) {
                return null;
            }
            Predicate<ItemStack> filter = this.filter == null ? stack -> true : this.filter;
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack inSlot = handler.getStackInSlot(i);
                if (!filter.test(inSlot)) {
                    continue;
                }
                BlockItemHandlerPointer pointer = new BlockItemHandlerPointer(this, pos, facing, i, inSlot);
                if (requiredState == null) {
                    return pointer;
                }
                int requiredCount = BlockPlacementRules.getPlacementItemCount(
                    level.registryAccess(),
                    requiredState,
                    inSlot
                );
                if (requiredCount > 0
                    && pointer.matches(level, requiredState)
                    && pointer.extractMatchingItems(handler, requiredCount, true) == requiredCount) {
                    return pointer;
                }
            }
            return null;
        }
    }
}
