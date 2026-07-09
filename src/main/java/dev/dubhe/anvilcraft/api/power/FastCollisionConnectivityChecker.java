package dev.dubhe.anvilcraft.api.power;

import net.neoforged.neoforge.common.util.TriState;

public class FastCollisionConnectivityChecker extends ConnectivityChecker {

    @Override
    public TriState checkInRange(PowerGrid powerGrid, IPowerComponent component) {
        return powerGrid.collideFast(component.getShape()) ? TriState.TRUE : TriState.DEFAULT;
    }
}
