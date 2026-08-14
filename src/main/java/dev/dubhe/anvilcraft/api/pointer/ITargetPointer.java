package dev.dubhe.anvilcraft.api.pointer;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.util.ISerializer;
import dev.dubhe.anvilcraft.init.registry.ModRegistries;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public interface ITargetPointer {
    Codec<ITargetPointer> CODEC = ModRegistries.TARGET_POINTER_TYPE.byNameCodec()
        .dispatch(ITargetPointer::getType, Type::codec);
    StreamCodec<RegistryFriendlyByteBuf, ITargetPointer> STREAM_CODEC = ByteBufCodecs.registry(ModRegistryKeys.TARGET_POINTER_TYPE)
        .dispatch(ITargetPointer::getType, Type::streamCodec);

    boolean isStillValid(Level level);

    boolean applyToPos(ServerLevel level, BlockPos pos);

    default boolean applyToPos(ServerLevel level, BlockPos pos, BlockState state) {
        if (!this.matches(level, state) || !this.applyToPos(level, pos)) {
            return false;
        }
        return level.getBlockState(pos).is(state.getBlock());
    }

    default boolean matches(Level level, BlockState state) {
        return true;
    }

    default @Nullable Either<ItemStack, BlockState> getDisplayedBlock() {
        return null;
    }

    Type<? extends ITargetPointer> getType();

    interface Type<T extends ITargetPointer> extends ISerializer<T> {
        @Nullable
        T point(Level level, BlockPos pos, Direction facing, @Nullable BlockState requiredState);
    }
}
