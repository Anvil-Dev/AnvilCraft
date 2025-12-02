package dev.dubhe.anvilcraft.api.power;

public class FastCollisionConnectivityChecker extends ConnectivityChecker {

    @Override
    boolean checkInRange(PowerGrid powerGrid, IPowerComponent component) {
        return powerGrid.collideFast(component.getShape());
    }
}
