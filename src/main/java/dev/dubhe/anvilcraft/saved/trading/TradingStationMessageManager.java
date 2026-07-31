package dev.dubhe.anvilcraft.saved.trading;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.TradingStationBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.saved.BetterSavedData;
import dev.dubhe.anvilcraft.saved.datafixers.DataFixers;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class TradingStationMessageManager extends BetterSavedData {
    private static final Identifier FIXER_ID = AnvilCraft.of("tsmm_fixers");
    private static final Codec<TradingStationMessageManager> CODEC = MapCodec
        .unit(TradingStationMessageManager::new)
        .codec();
    private static final SavedDataType<TradingStationMessageManager> TYPE = new SavedDataType<>(
        AnvilCraft.of("trading_station_messages"),
        TradingStationMessageManager::new,
        TradingStationMessageManager.CODEC
    );
    private static final TradingStationMessageManager CLIENT_COPY = new TradingStationMessageManager();
    private final List<ITradingStationMessage> messages = new ArrayList<>();
    private final List<BlockPos> playerBroke = new ArrayList<>();

    public static TradingStationMessageManager get() {
        return BetterSavedData.get(TradingStationMessageManager.TYPE, TradingStationMessageManager.CLIENT_COPY);
    }

    public void onPlayerBreak(ServerLevel level, BlockPos pos, Player breaker) {
        this.playerBroke.add(pos);
        MinecraftServer server = level.getServer();
        Optional<TradingStationBlockEntity> beOp = level.getBlockEntity(pos, ModBlockEntities.TRADING_STATION.get());
        if (beOp.isEmpty()) return;
        TradingStationBlockEntity be = beOp.get();
        UUID owner = be.getOwner();
        if (owner == null || be.isOwner(breaker)) return;
        TradingStationPlayerBreakMessage message = new TradingStationPlayerBreakMessage(
            owner,
            breaker.getGameProfile().id(),
            level.dimension(),
            pos,
            new Date().getTime()
        );
        Component realTime = message.getRealTimeMessage(id -> TradingStationMessageUtil.findPlayerName(server, id));
        server.sendSystemMessage(realTime);
        PlayerList players = server.getPlayerList();
        for (ServerPlayer player : players.getPlayers()) {
            player.sendSystemMessage(realTime);
        }
        if (players.getPlayer(owner) != null) return;
        this.messages.add(message);
    }

    public void onNonPlayerBreak(ServerLevel level, BlockPos pos) {
        for (Iterator<BlockPos> iterator = this.playerBroke.iterator(); iterator.hasNext(); ) {
            BlockPos playerBroke = iterator.next();
            if (!pos.equals(playerBroke)) continue;
            iterator.remove();
            return;
        }
        MinecraftServer server = level.getServer();
        Optional<TradingStationBlockEntity> beOp = level.getBlockEntity(pos, ModBlockEntities.TRADING_STATION.get());
        if (beOp.isEmpty()) return;
        TradingStationBlockEntity be = beOp.get();
        UUID owner = be.getOwner();
        if (owner == null) return;
        PlayerList players = server.getPlayerList();
        TradingStationNonPlayerBreakMessage message = new TradingStationNonPlayerBreakMessage(
            owner,
            level.dimension(),
            pos,
            new Date().getTime(),
            Lists.transform(players.getPlayers(), sp -> sp.getGameProfile().id()),
            Optional.ofNullable(level.getNearestPlayer(TargetingConditions.DEFAULT, pos.getX(), pos.getY(), pos.getZ()))
                .map(p -> p.getGameProfile().id())
                .orElse(null)
        );
        Component realTime = message.getRealTimeMessage(id -> TradingStationMessageUtil.findPlayerName(server, id));
        server.sendSystemMessage(realTime);
        for (ServerPlayer player : players.getPlayers()) {
            player.sendSystemMessage(realTime);
        }
        if (players.getPlayer(owner) != null) return;
        this.messages.add(message);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        // 事件从PlayerList发出，而它有一个server变量
        // 这说明服务器已经初始化，所以我们可以放心的拿到服务器实例
        MinecraftServer server = Objects.requireNonNull(ServerLifecycleHooks.getCurrentServer());
        TradingStationMessageManager manager = TradingStationMessageManager.get();
        Player player = event.getEntity();
        UUID playerId = player.getGameProfile().id();
        for (Iterator<ITradingStationMessage> iterator = manager.messages.iterator(); iterator.hasNext(); ) {
            ITradingStationMessage message = iterator.next();
            if (!message.owner().equals(playerId)) continue;
            player.sendSystemMessage(message.getOwnerMessage(id -> TradingStationMessageUtil.findPlayerName(server, id)));
            iterator.remove();
        }
    }

    @Override
    protected void registerDataFixers() {
        DataFixers.registerFixer(TradingStationMessageManager.FIXER_ID);
    }

    @Override
    protected @Nullable Packet<? extends CustomPacketPayload> createPacket(RegistryAccess registryAccess) {
        return null;
    }
}
