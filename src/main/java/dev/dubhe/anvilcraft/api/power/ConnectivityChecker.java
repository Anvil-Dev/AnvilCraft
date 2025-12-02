package dev.dubhe.anvilcraft.api.power;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;

import java.util.List;

public abstract class ConnectivityChecker {
    private static final List<ConnectivityChecker> instances = ObjectLists.synchronize(new ObjectArrayList<>());

    public static void register(ConnectivityChecker instance) {
        instances.add(instance);
    }

    public static boolean check(PowerGrid powerGrid, IPowerComponent component) {
        for (ConnectivityChecker it : instances) {
            if (it.checkInRange(powerGrid, component)) {
                return true;
            }
        }
        return false;
    }

    abstract boolean checkInRange(PowerGrid powerGrid, IPowerComponent component);
}
