package dev.dubhe.anvilcraft.rpc;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import dev.anvilcraft.lib.v2.rpc.RemoteCallable;
import lombok.experimental.UtilityClass;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

@UtilityClass
public class BundleLikeServerStub {
    private static final Table<UUID, ResourceLocation, Boolean> INVERTED =
        Tables.synchronizedTable(HashBasedTable.create());

    @RemoteCallable
    public static void updateInverted(UUID id, ResourceLocation location, boolean inverted) {
        BundleLikeServerStub.INVERTED.put(id, location, inverted);
    }

    public static boolean isInvertedAction(UUID id, ResourceLocation location) {
        return Boolean.TRUE.equals(BundleLikeServerStub.INVERTED.get(id, location));
    }

    /** 玩家退出时清理其反色动作状态，避免静态表永久残留。 */
    public static void clear(UUID id) {
        BundleLikeServerStub.INVERTED.row(id).clear();
    }
}
