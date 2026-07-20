package dev.dubhe.anvilcraft.rpc;

import dev.anvilcraft.lib.v2.rpc.CallableParam;
import dev.anvilcraft.lib.v2.rpc.IRemoteCallableValidator;
import dev.anvilcraft.lib.v2.rpc.RemoteCallable;
import dev.dubhe.anvilcraft.saved.setting.PlayerSetting;
import dev.dubhe.anvilcraft.saved.setting.PlayerSettings;
import dev.dubhe.anvilcraft.saved.setting.mode.NbtDisplayMode;
import dev.dubhe.anvilcraft.saved.setting.mode.OrderMode;
import dev.dubhe.anvilcraft.saved.setting.mode.SearchMode;
import dev.dubhe.anvilcraft.saved.setting.mode.SortMode;
import dev.dubhe.anvilcraft.saved.storage.category.ICategory;
import dev.dubhe.anvilcraft.saved.storage.category.store.CategoryEntry;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public final class SettingServerStub {
    @RemoteCallable(validator = OwnSettingValidator.class)
    public static PlayerSetting get(UUID playerId) {
        return PlayerSettings.getSetting(playerId);
    }

    @RemoteCallable(validator = OwnSettingValidator.class)
    public static void list(UUID playerId, ICategory category) {
        PlayerSettings.getSetting(playerId).list(category);
        PlayerSettings.get().setDirty();
    }

    @CallableParam(clazz = CategoryEntry.class, field = "STREAM_CODEC")
    @RemoteCallable(validator = OwnSettingValidator.class)
    public static CategoryEntry unlist(UUID playerId, int index) {
        CategoryEntry entry = PlayerSettings.getSetting(playerId).unlist(index);
        PlayerSettings.get().setDirty();
        return entry;
    }

    @RemoteCallable(validator = OwnSettingValidator.class)
    public static void pinToTop(UUID playerId, int index) {
        PlayerSettings.getSetting(playerId).pinToTop(index);
        PlayerSettings.get().setDirty();
    }

    @RemoteCallable(validator = OwnSettingValidator.class)
    public static void addCustom(UUID playerId, ICategory category) {
        PlayerSettings.getSetting(playerId).addCustom(category);
        PlayerSettings.get().setDirty();
    }

    @CallableParam(clazz = CategoryEntry.class, field = "LIST_STREAM_CODEC")
    @RemoteCallable(validator = OwnSettingValidator.class)
    public static List<CategoryEntry> update(
        UUID playerId,
        @CallableParam(clazz = CategoryEntry.class, field = "LIST_STREAM_CODEC") List<CategoryEntry> categories
    ) {
        List<CategoryEntry> listed = PlayerSettings.getSetting(playerId).listed();
        listed.clear();
        listed.addAll(categories);
        PlayerSettings.get().setDirty();
        return listed;
    }

    @RemoteCallable(validator = OwnSettingValidator.class)
    public static void update(UUID playerId, String content) {
        PlayerSettings.getSetting(playerId).storage().setSearchContent(content);
        PlayerSettings.get().setDirty();
    }

    @RemoteCallable(validator = OwnSettingValidator.class)
    public static void update(UUID playerId, SearchMode mode) {
        PlayerSettings.getSetting(playerId).storage().setSearch(mode);
        PlayerSettings.get().setDirty();
    }

    @RemoteCallable(validator = OwnSettingValidator.class)
    public static void update(UUID playerId, SortMode mode) {
        PlayerSettings.getSetting(playerId).storage().setSort(mode);
        PlayerSettings.get().setDirty();
    }

    @RemoteCallable(validator = OwnSettingValidator.class)
    public static void update(UUID playerId, OrderMode mode) {
        PlayerSettings.getSetting(playerId).storage().setOrder(mode);
        PlayerSettings.get().setDirty();
    }

    @RemoteCallable(validator = OwnSettingValidator.class)
    public static void update(UUID playerId, NbtDisplayMode mode) {
        PlayerSettings.getSetting(playerId).storage().setNbtDisplay(mode);
        PlayerSettings.get().setDirty();
    }

    public static final class OwnSettingValidator implements IRemoteCallableValidator {
        @Override
        public boolean validate(@NonNull IPayloadContext ctx, @NonNull Method method, Object @NonNull [] args) {
            return ctx.player() instanceof ServerPlayer player
                && args.length > 0
                && args[0] instanceof UUID playerId
                && player.getGameProfile().id().equals(playerId);
        }
    }

    private SettingServerStub() {
    }
}
