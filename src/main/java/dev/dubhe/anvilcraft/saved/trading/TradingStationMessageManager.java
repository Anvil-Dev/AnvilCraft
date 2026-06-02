package dev.dubhe.anvilcraft.saved.trading;

import com.google.common.collect.Lists;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.TradingStationBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.saved.BetterSavedData;
import dev.dubhe.anvilcraft.saved.datafixers.DataFixers;
import dev.dubhe.anvilcraft.util.ComponentUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class TradingStationMessageManager extends BetterSavedData {
    private static final ResourceLocation FIXER_ID = AnvilCraft.of("tsmm_fixers");
    private final List<ITradingStationMessage> messages = new ArrayList<>();
    private final List<BlockPos> playerBroke = new ArrayList<>();

    public static TradingStationMessageManager get() {
        return BetterSavedData.get(
            "trading_station_messages",
            TradingStationMessageManager::new
        );
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
            breaker.getGameProfile().getId(),
            level.dimension(),
            pos,
            new Date().getTime()
        );
        server.sendSystemMessage(message.getRealTimeMessage(id -> ComponentUtil.findPlayerName(server.getProfileCache(), id)));
        if (server.getPlayerList().getPlayer(owner) != null) return;
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
        TradingStationNonPlayerBreakMessage message = new TradingStationNonPlayerBreakMessage(
            owner,
            level.dimension(),
            pos,
            new Date().getTime(),
            Lists.transform(server.getPlayerList().getPlayers(), sp -> sp.getGameProfile().getId()),
            Optional.ofNullable(level.getNearestPlayer(TargetingConditions.DEFAULT, pos.getX(), pos.getY(), pos.getZ()))
                .map(p -> p.getGameProfile().getId())
                .orElse(null)
        );
        server.sendSystemMessage(message.getRealTimeMessage(id -> ComponentUtil.findPlayerName(server.getProfileCache(), id)));
        if (server.getPlayerList().getPlayer(owner) != null) return;
        this.messages.add(message);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        // 事件从PlayerList发出，而它有一个server变量
        // 这说明服务器已经初始化，所以我们可以放心的拿到服务器实例
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        TradingStationMessageManager manager = TradingStationMessageManager.get();
        Player player = event.getEntity();
        UUID playerId = player.getGameProfile().getId();
        for (Iterator<ITradingStationMessage> iterator = manager.messages.iterator(); iterator.hasNext(); ) {
            ITradingStationMessage message = iterator.next();
            if (!message.owner().equals(playerId)) continue;
            player.sendSystemMessage(message.getOwnerMessage(id -> ComponentUtil.findPlayerName(server.getProfileCache(), id)));
            iterator.remove();
        }
    }

    @Override
    protected void registerDataFixers() {
        DataFixers.registerFixer(FIXER_ID);
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries) {

    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        return null;
    }
}
