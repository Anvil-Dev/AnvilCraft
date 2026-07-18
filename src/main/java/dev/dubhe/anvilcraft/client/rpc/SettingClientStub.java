package dev.dubhe.anvilcraft.client.rpc;

import dev.anvilcraft.lib.v2.rpc.RPC;
import dev.anvilcraft.lib.v2.rpc.RpcTarget;
import dev.dubhe.anvilcraft.network.PlayerSettingsSyncPacket;
import dev.dubhe.anvilcraft.rpc.SettingServerStub;
import dev.dubhe.anvilcraft.saved.setting.PlayerSetting;
import dev.dubhe.anvilcraft.saved.setting.PlayerSettings;
import dev.dubhe.anvilcraft.saved.setting.StorageSetting;
import dev.dubhe.anvilcraft.saved.setting.mode.NbtDisplayMode;
import dev.dubhe.anvilcraft.saved.setting.mode.OrderMode;
import dev.dubhe.anvilcraft.saved.setting.mode.SearchMode;
import dev.dubhe.anvilcraft.saved.setting.mode.SortMode;
import dev.dubhe.anvilcraft.saved.storage.category.ICategory;
import dev.dubhe.anvilcraft.saved.storage.category.store.CategoryEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SettingClientStub {
    private static CompletableFuture<PlayerSetting> pendingLoad;
    private static UUID pendingPlayerId;
    private static PlayerSetting cachedSetting;
    private static UUID cachedPlayerId;
    private static PlayerSetting fallbackSetting;
    private static UUID fallbackPlayerId;

    public static CompletableFuture<PlayerSetting> load() {
        UUID playerId = SettingClientStub.playerId();
        PlayerSetting setting = SettingClientStub.cachedSetting(playerId);
        if (setting != null) {
            return CompletableFuture.completedFuture(setting);
        }
        synchronized (SettingClientStub.class) {
            if (SettingClientStub.pendingLoad != null && playerId.equals(SettingClientStub.pendingPlayerId)) {
                return SettingClientStub.pendingLoad;
            }
            SettingClientStub.pendingPlayerId = playerId;
            SettingClientStub.pendingLoad = RPC.invoke(RpcTarget.server(), SettingServerStub::get, playerId)
                .thenApply(loaded -> {
                    PlayerSettings.get().getSettings().put(playerId, loaded);
                    SettingClientStub.cache(playerId, loaded);
                    SettingClientStub.clearFallback(playerId);
                    return loaded;
                })
                .whenComplete((_, _) -> SettingClientStub.clearPendingLoad(playerId));
            return SettingClientStub.pendingLoad;
        }
    }

    public static PlayerSetting setting() {
        UUID playerId = SettingClientStub.playerId();
        PlayerSetting setting = SettingClientStub.cachedSetting(playerId);
        if (setting != null) {
            return setting;
        }
        synchronized (SettingClientStub.class) {
            if (SettingClientStub.fallbackSetting == null || !playerId.equals(SettingClientStub.fallbackPlayerId)) {
                SettingClientStub.fallbackPlayerId = playerId;
                SettingClientStub.fallbackSetting = new PlayerSetting();
            }
            return SettingClientStub.fallbackSetting;
        }
    }

    public static List<CategoryEntry> listed() {
        return SettingClientStub.setting().listed();
    }

    public static List<ICategory> custom() {
        return SettingClientStub.setting().custom();
    }

    public static StorageSetting storage() {
        return SettingClientStub.setting().storage();
    }

    public static PlayerSetting copy() {
        return SettingClientStub.copy(SettingClientStub.setting());
    }

    private static PlayerSetting copy(PlayerSetting setting) {
        List<CategoryEntry> listed = setting.listed().stream()
            .map(entry -> new CategoryEntry(entry.getCategory(), entry.getMode()))
            .toList();
        StorageSetting storage = setting.storage();
        return new PlayerSetting(
            new ArrayList<>(listed),
            new ArrayList<>(setting.custom()),
            new StorageSetting("", storage.getSearch(), storage.getSort(), storage.getOrder(), storage.getNbtDisplay())
        );
    }

    public static void commit(PlayerSetting setting) {
        PlayerSetting committed = SettingClientStub.copy(setting);
        PlayerSetting cached = SettingClientStub.setting();
        cached.listed().clear();
        cached.listed().addAll(committed.listed());
        cached.custom().clear();
        cached.custom().addAll(committed.custom());
        cached.storage().setSearchContent(committed.storage().getSearchContent());
        cached.storage().setSearch(committed.storage().getSearch());
        cached.storage().setSort(committed.storage().getSort());
        cached.storage().setOrder(committed.storage().getOrder());
        cached.storage().setNbtDisplay(committed.storage().getNbtDisplay());
        ClientPacketDistributor.sendToServer(new PlayerSettingsSyncPacket(committed));
    }

    public static void list(ICategory category) {
        RPC.call(RpcTarget.server(), SettingServerStub::list, SettingClientStub.playerId(), category);
    }

    public static CompletableFuture<CategoryEntry> unlist(int index) {
        return RPC.invoke(RpcTarget.server(), SettingServerStub::unlist, SettingClientStub.playerId(), index);
    }

    public static void pinToTop(int index) {
        RPC.call(RpcTarget.server(), SettingServerStub::pinToTop, SettingClientStub.playerId(), index);
    }

    public static void addCustom(ICategory category) {
        RPC.call(RpcTarget.server(), SettingServerStub::addCustom, SettingClientStub.playerId(), category);
    }

    public static void update(List<CategoryEntry> categories) {
        List<CategoryEntry> listed = SettingClientStub.setting().listed();
        listed.clear();
        listed.addAll(categories);
        RPC.call(RpcTarget.server(), SettingServerStub::update, SettingClientStub.playerId(), categories);
    }

    public static void update(String content) {
        SettingClientStub.setting().storage().setSearchContent(content);
        RPC.call(RpcTarget.server(), SettingServerStub::update, SettingClientStub.playerId(), content);
    }

    public static void update(SearchMode mode) {
        SettingClientStub.setting().storage().setSearch(mode);
        RPC.call(RpcTarget.server(), SettingServerStub::update, SettingClientStub.playerId(), mode);
    }

    public static void update(SortMode mode) {
        SettingClientStub.setting().storage().setSort(mode);
        RPC.call(RpcTarget.server(), SettingServerStub::update, SettingClientStub.playerId(), mode);
    }

    public static void update(OrderMode mode) {
        SettingClientStub.setting().storage().setOrder(mode);
        RPC.call(RpcTarget.server(), SettingServerStub::update, SettingClientStub.playerId(), mode);
    }

    public static void update(NbtDisplayMode mode) {
        SettingClientStub.setting().storage().setNbtDisplay(mode);
        RPC.call(RpcTarget.server(), SettingServerStub::update, SettingClientStub.playerId(), mode);
    }

    private static UUID playerId() {
        return SettingClientStub.player().getGameProfile().id();
    }

    private static synchronized void clearPendingLoad(UUID playerId) {
        if (playerId.equals(SettingClientStub.pendingPlayerId)) {
            SettingClientStub.pendingLoad = null;
            SettingClientStub.pendingPlayerId = null;
        }
    }

    private static synchronized void clearFallback(UUID playerId) {
        if (playerId.equals(SettingClientStub.fallbackPlayerId)) {
            SettingClientStub.fallbackSetting = null;
            SettingClientStub.fallbackPlayerId = null;
        }
    }

    private static synchronized PlayerSetting cachedSetting(UUID playerId) {
        if (playerId.equals(SettingClientStub.cachedPlayerId)) {
            return SettingClientStub.cachedSetting;
        }
        PlayerSetting setting = PlayerSettings.get().getSettings().get(playerId);
        if (setting != null) {
            SettingClientStub.cache(playerId, setting);
        }
        return setting;
    }

    private static void cache(UUID playerId, PlayerSetting setting) {
        SettingClientStub.cachedPlayerId = playerId;
        SettingClientStub.cachedSetting = setting;
    }

    private static Player player() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            throw new IllegalStateException("Cannot call setting RPC without a client player");
        }
        return player;
    }

    private SettingClientStub() {
    }
}
