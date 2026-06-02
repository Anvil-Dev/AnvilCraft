package dev.dubhe.anvilcraft.network.multiple;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.TradingStationBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class TradingStationPackets {
    private static <T extends IPacket> CustomPacketPayload.Type<T> of(String path) {
        return IPacket.type(AnvilCraft.of("trading_station_" + path));
    }

    public record SyncFilter(BlockPos pos, int slot, ItemStack stack) implements IServerboundPacket {
        public static final Type<SyncFilter> TYPE = TradingStationPackets.of("sync_filter");
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncFilter> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SyncFilter::pos,
            ByteBufCodecs.VAR_INT,
            SyncFilter::slot,
            ItemStack.OPTIONAL_STREAM_CODEC,
            SyncFilter::stack,
            SyncFilter::new
        );

        @Override
        public Type<SyncFilter> type() {
            return SyncFilter.TYPE;
        }

        @Override
        public void handleOnServer(Player player) {
            Level level = player.level();
            Optional<TradingStationBlockEntity> beOp = level.getBlockEntity(this.pos, ModBlockEntities.TRADING_STATION.get());
            if (beOp.isEmpty()) return;
            TradingStationBlockEntity be = beOp.get();
            be.getFilters().setItem(this.slot, this.stack);
            TradingStationBlockEntity.popoutInvalidItems(level, this.pos, be.getHandler());
            TradingStationBlockEntity.updateAndSend(be);
        }
    }

    public record SyncAllowing(
        BlockPos pos,
        boolean playerAllowed,
        boolean villagerAllowed,
        boolean inputAllowed,
        boolean outputAllowed
    ) implements IServerboundPacket {
        public static final Type<SyncAllowing> TYPE = TradingStationPackets.of("sync_allowing");
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncAllowing> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SyncAllowing::pos,
            ByteBufCodecs.BOOL,
            SyncAllowing::playerAllowed,
            ByteBufCodecs.BOOL,
            SyncAllowing::villagerAllowed,
            ByteBufCodecs.BOOL,
            SyncAllowing::inputAllowed,
            ByteBufCodecs.BOOL,
            SyncAllowing::outputAllowed,
            SyncAllowing::new
        );

        @Override
        public Type<SyncAllowing> type() {
            return SyncAllowing.TYPE;
        }

        @Override
        public void handleOnServer(Player player) {
            Level level = player.level();
            Optional<TradingStationBlockEntity> beOp = level.getBlockEntity(this.pos, ModBlockEntities.TRADING_STATION.get());
            if (beOp.isEmpty()) return;
            TradingStationBlockEntity be = beOp.get();
            be.setPlayerAllowed(this.playerAllowed);
            be.setVillagerAllowed(this.villagerAllowed);
            be.setInputAllowed(this.inputAllowed);
            be.setOutputAllowed(this.outputAllowed);
        }
    }
}
