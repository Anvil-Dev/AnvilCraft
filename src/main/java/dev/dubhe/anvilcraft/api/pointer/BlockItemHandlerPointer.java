package dev.dubhe.anvilcraft.api.pointer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.init.ModRegistries;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
    private @Nullable IItemHandler handler;

    public BlockItemHandlerPointer(Type type, BlockPos pos, Direction dir, int slot, ItemStack stack) {
        this.type = type;
        this.pos = pos;
        this.dir = dir;
        this.slot = slot;
        this.stack = stack;
    }

    @Nullable
    public IItemHandler getHandler(Level level) {
        if (this.handler == null) {
            IItemHandler handler = ItemHandlerUtil.getSourceItemHandler(this.pos, this.dir, level);
            if (handler == null) {
                return null;
            }
            this.handler = handler;
        }
        return this.handler;
    }

    @Override
    public boolean isStillValid(Level level) {
        IItemHandler handler = this.getHandler(level);
        if (handler == null) {
            return false;
        }
        return ItemStack.isSameItemSameComponents(handler.getStackInSlot(slot), this.stack);
    }

    @Override
    public boolean matches(BlockState requiredState) {
        return this.stack.getItem() instanceof net.minecraft.world.item.BlockItem item
            && item.getBlock() == requiredState.getBlock();
    }

    @Override
    public boolean applyToPos(ServerLevel level, BlockPos pos) {
        IItemHandler handler = this.getHandler(level);
        if (handler == null || !this.isStillValid(level)) {
            return false;
        }

        ItemStack stack = handler.extractItem(this.slot, this.stack.getCount(), true);
        if (!ItemStack.isSameItemSameComponents(stack, this.stack)) {
            return false;
        }

        int initialCount = stack.getCount();
        ItemStack result = ITargetPointer.placeToPos(level, pos, stack);
        int consumed = initialCount - result.getCount();
        if (consumed <= 0) {
            return false;
        }

        handler.extractItem(this.slot, consumed, false);
        return true;
    }

    public static class Type implements ITargetPointer.Type<BlockItemHandlerPointer> {
        public static final Codec<Type> CODEC = ModRegistries.TARGET_POINTER_TYPE_REGISTRY.byNameCodec().flatXmap(
            raw -> raw instanceof Type type
                   ? DataResult.success(type)
                   : DataResult.error(() -> "Cannot cast %s to BlockItemHandlerPointer.Type".formatted(raw.getClass().getSimpleName())),
            DataResult::success
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, Type> STREAM_CODEC = ByteBufCodecs.registry(
            ModRegistries.TARGET_POINTER_TYPE_KEY
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
                if (requiredState == null || pointer.matches(requiredState)) {
                    return pointer;
                }
            }
            return null;
        }
    }
}
