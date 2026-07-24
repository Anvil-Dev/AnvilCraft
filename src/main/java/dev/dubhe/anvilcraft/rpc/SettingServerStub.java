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
import lombok.experimental.UtilityClass;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@UtilityClass
public class SettingServerStub {
    private static final ThreadLocal<HolderLookup.@Nullable Provider> REGISTRIES = new ThreadLocal<>();

    private static HolderLookup.Provider getAndClear() {
        HolderLookup.Provider registries = SettingServerStub.REGISTRIES.get();
        SettingServerStub.REGISTRIES.remove();
        return Objects.requireNonNull(registries);
    }

    @RemoteCallable(validator = OwnSettingValidator.class)
    public static PlayerSetting get(UUID playerId) {
        return PlayerSettings.getSetting(SettingServerStub.getAndClear(), playerId);
    }

    @RemoteCallable(validator = OwnSettingValidator.class)
    public static void list(UUID playerId, ICategory category) {
        PlayerSettings.getSetting(SettingServerStub.getAndClear(), playerId).list(category);
        PlayerSettings.get().setDirty();
    }

    @CallableParam(clazz = CategoryEntry.class, field = "STREAM_CODEC")
    @RemoteCallable(validator = OwnSettingValidator.class)
    public static CategoryEntry unlist(UUID playerId, int index) {
        CategoryEntry entry = PlayerSettings.getSetting(SettingServerStub.getAndClear(), playerId).unlist(index);
        PlayerSettings.get().setDirty();
        return entry;
    }

    @RemoteCallable(validator = OwnSettingValidator.class)
    public static void pinToTop(UUID playerId, int index) {
        PlayerSettings.getSetting(SettingServerStub.getAndClear(), playerId).pinToTop(index);
        PlayerSettings.get().setDirty();
    }

    @RemoteCallable(validator = OwnSettingValidator.class)
    public static void addCustom(UUID playerId, ICategory category) {
        PlayerSettings.getSetting(SettingServerStub.getAndClear(), playerId).addCustom(category);
        PlayerSettings.get().setDirty();
    }

    @CallableParam(clazz = CategoryEntry.class, field = "LIST_STREAM_CODEC")
    @RemoteCallable(validator = OwnSettingValidator.class)
    public static List<CategoryEntry> update(
        UUID playerId,
        @CallableParam(clazz = CategoryEntry.class, field = "LIST_STREAM_CODEC") List<CategoryEntry> categories
    ) {
        List<CategoryEntry> listed = PlayerSettings.getSetting(SettingServerStub.getAndClear(), playerId).listed();
        listed.clear();
        listed.addAll(categories);
        PlayerSettings.get().setDirty();
        return listed;
    }

    @RemoteCallable(validator = OwnSettingValidator.class)
    public static void update(UUID playerId, String content) {
        PlayerSettings.getSetting(SettingServerStub.getAndClear(), playerId).storage().setSearchContent(content);
        PlayerSettings.get().setDirty();
    }

    @RemoteCallable(validator = OwnSettingValidator.class)
    public static void update(UUID playerId, SearchMode mode) {
        PlayerSettings.getSetting(SettingServerStub.getAndClear(), playerId).storage().setSearch(mode);
        PlayerSettings.get().setDirty();
    }

    @RemoteCallable(validator = OwnSettingValidator.class)
    public static void update(UUID playerId, SortMode mode) {
        PlayerSettings.getSetting(SettingServerStub.getAndClear(), playerId).storage().setSort(mode);
        PlayerSettings.get().setDirty();
    }

    @RemoteCallable(validator = OwnSettingValidator.class)
    public static void update(UUID playerId, OrderMode mode) {
        PlayerSettings.getSetting(SettingServerStub.getAndClear(), playerId).storage().setOrder(mode);
        PlayerSettings.get().setDirty();
    }

    @RemoteCallable(validator = OwnSettingValidator.class)
    public static void update(UUID playerId, NbtDisplayMode mode) {
        PlayerSettings.getSetting(SettingServerStub.getAndClear(), playerId).storage().setNbtDisplay(mode);
        PlayerSettings.get().setDirty();
    }

    public static final class OwnSettingValidator implements IRemoteCallableValidator {
        @Override
        public boolean validate(IPayloadContext ctx, Method method, Object[] args) {
            boolean valid = this.isValid(ctx, args);
            if (valid) {
                SettingServerStub.REGISTRIES.set(ctx.player().registryAccess());
            }
            return valid;
        }

        private boolean isValid(IPayloadContext ctx, Object[] args) {
            return ctx.player() instanceof ServerPlayer player
                   && args.length > 0
                   && args[0] instanceof UUID playerId
                   && player.getGameProfile().id().equals(playerId);
        }
    }
}
