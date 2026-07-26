package dev.dubhe.anvilcraft.api.pointer;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.util.ISerializer;
import dev.dubhe.anvilcraft.api.entity.fakeplayer.AnvilCraftFakePlayers;
import dev.dubhe.anvilcraft.api.item.IBlockItem;
import dev.dubhe.anvilcraft.init.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public interface ITargetPointer {
    Codec<ITargetPointer> CODEC = ModRegistries.TARGET_POINTER_TYPE_REGISTRY.byNameCodec()
        .dispatch(ITargetPointer::getType, Type::codec);
    StreamCodec<RegistryFriendlyByteBuf, ITargetPointer> STREAM_CODEC = ByteBufCodecs.registry(ModRegistries.TARGET_POINTER_TYPE_KEY)
        .dispatch(ITargetPointer::getType, Type::streamCodec);

    boolean isStillValid(Level level);

    boolean applyToPos(ServerLevel level, BlockPos pos);

    default boolean applyToPos(ServerLevel level, BlockPos pos, BlockState state) {
        if (!this.matches(state) || !this.applyToPos(level, pos)) {
            return false;
        }
        level.setBlock(pos, state, 3);
        return true;
    }

    default boolean matches(BlockState state) {
        return true;
    }

    default @Nullable Either<ItemStack, BlockState> getDisplayedBlock() {
        return null;
    }

    static ItemStack placeToPos(ServerLevel level, BlockPos pos, ItemStack stack) {
        IBlockItem item = null;
        if (stack.getItem() instanceof IBlockItem i) {
            item = i;
        } else if (stack.getItem() instanceof BlockItem b) {
            item = IBlockItem.wrap(b);
        }
        if (item == null) {
            return stack;
        }

        ServerPlayer player = AnvilCraftFakePlayers.getBlockPlacer().offerPlayer(level);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        item.place(level, pos, player, InteractionHand.MAIN_HAND);
        ItemStack result = player.getMainHandItem();
        AnvilCraftFakePlayers.getBlockPlacer().disable(player);
        return result;
    }

    Type<? extends ITargetPointer> getType();

    interface Type<T extends ITargetPointer> extends ISerializer<T> {
        @Nullable
        T point(Level level, BlockPos pos, Direction facing, @Nullable BlockState requiredState);
    }
}
