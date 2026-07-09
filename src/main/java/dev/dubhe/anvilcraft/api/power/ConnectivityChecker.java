package dev.dubhe.anvilcraft.api.power;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import net.neoforged.neoforge.common.util.TriState;

import java.util.List;

public abstract class ConnectivityChecker {
    private static final List<ConnectivityChecker> instances = ObjectLists.synchronize(new ObjectArrayList<>());

    public static void register(ConnectivityChecker instance) {
        instances.add(instance);
    }

    public static boolean check(PowerGrid powerGrid, IPowerComponent component) {
        boolean allDefault = true;
        for (ConnectivityChecker it : instances) {
            TriState triState = it.checkInRange(powerGrid, component);
            if (triState == TriState.FALSE) {
                return false;
            }
            if (triState != TriState.DEFAULT) {
                allDefault = false;
            }
        }
        return !allDefault;
    }

    /**
     * Checks whether a component should be connected to the power grid.
     *
     * @return FALSE = 拒绝加入电网，且优先级最高；DEFAULT = 弃权；TRUE = 允许加入电网，
     * 仅在没有 checker 返回 FALSE 时生效
     */
    public abstract TriState checkInRange(PowerGrid powerGrid, IPowerComponent component);
}
