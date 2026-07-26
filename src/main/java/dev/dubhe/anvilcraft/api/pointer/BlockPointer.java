package dev.dubhe.anvilcraft.api.pointer;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.piston.IMoveableEntityBlock;
import dev.anvilcraft.lib.v2.util.Util;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@Getter
public class BlockPointer implements ITargetPointer {
    private final Type type;
    private final BlockPos pos;
    private final BlockState state;

    public BlockPointer(Type type, BlockPos pos, BlockState state) {
        this.type = type;
        this.pos = pos;
        this.state = state;
    }

    @Override
    public boolean isStillValid(Level level) {
        if (this.state.isAir()) {
            return false;
        }
        BlockState current = level.getBlockState(this.pos);
        if (!current.is(this.state.getBlock())) {
            return false;
        }
        for (Property<?> property : this.state.getValues().keySet()) {
            if (current.getOptionalValue(property).filter(value -> value.equals(this.state.getValue(property))).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean matches(BlockState requiredState) {
        return this.state.equals(requiredState);
    }

    @Override
    public Either<ItemStack, BlockState> getDisplayedBlock() {
        return Either.right(this.state);
    }

    @Override
    public boolean applyToPos(ServerLevel level, BlockPos pos) {
        if (!this.isStillValid(level)) {
            return false;
        }

        BlockEntity entity = null;
        if (this.state.getBlock() instanceof IMoveableEntityBlock) {
            entity = level.getBlockEntity(this.pos);
            if (entity != null) {
                level.removeBlockEntity(this.pos);
            }
        }
        level.removeBlock(this.pos, true);

        BlockState state = this.state;
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)) {
            state = state.setValue(BlockStateProperties.WATERLOGGED, Boolean.FALSE);
        }
        level.setBlock(pos, state, 67);
        if (state.getBlock() instanceof IMoveableEntityBlock block && entity != null) {
            entity.worldPosition = pos;
            entity.clearRemoved();
            level.removeBlockEntity(pos);
            level.setBlockEntity(entity);
            block.notifyMoved(level, pos, state, entity);
        }
        level.neighborChanged(pos, state.getBlock(), pos);
        return true;
    }

    public static class Type implements ITargetPointer.Type<BlockPointer> {
        public static final Codec<Type> CODEC = ModRegistries.TARGET_POINTER_TYPE_REGISTRY.byNameCodec().flatXmap(
            raw -> raw instanceof Type type
                   ? DataResult.success(type)
                   : DataResult.error(() -> "Cannot cast %s to BlockPointer.Type".formatted(raw.getClass().getSimpleName())),
            DataResult::success
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, Type> STREAM_CODEC = ByteBufCodecs.registry(
            ModRegistries.TARGET_POINTER_TYPE_KEY
        ).map(Util::cast, Function.identity());
        public static final MapCodec<BlockPointer> POINTER_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Type.CODEC
                .fieldOf("type")
                .forGetter(BlockPointer::getType),
            BlockPos.CODEC
                .fieldOf("pos")
                .forGetter(BlockPointer::getPos),
            BlockState.CODEC
                .fieldOf("state")
                .forGetter(BlockPointer::getState)
        ).apply(inst, BlockPointer::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, BlockPointer> POINTER_STREAM_CODEC = StreamCodec.composite(
            Type.STREAM_CODEC,
            BlockPointer::getType,
            BlockPos.STREAM_CODEC,
            BlockPointer::getPos,
            StreamCodecUtil.BLOCK_STATE,
            BlockPointer::getState,
            BlockPointer::new
        );

        @Override
        public MapCodec<BlockPointer> codec() {
            return Type.POINTER_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BlockPointer> streamCodec() {
            return Type.POINTER_STREAM_CODEC;
        }

        @Override
        public @Nullable BlockPointer point(
            Level level,
            BlockPos pos,
            Direction facing,
            @Nullable BlockState requiredState
        ) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                return null;
            }
            if (requiredState != null && !state.equals(requiredState)) {
                return null;
            }
            if (!PistonBaseBlock.isPushable(state, level, pos, facing.getOpposite(), false, facing.getOpposite())) {
                return null;
            }
            return new BlockPointer(this, pos, state);
        }
    }
}
