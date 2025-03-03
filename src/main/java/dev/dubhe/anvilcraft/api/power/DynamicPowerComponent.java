package dev.dubhe.anvilcraft.api.power;

import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.Set;
import java.util.function.Supplier;

@Getter
public class DynamicPowerComponent {
    private final Entity owner;
    @Getter
    private final Set<PowerConsumption> powerConsumptions = Sets.newConcurrentHashSet();
    private final Supplier<AABB> aabbSupplier;
    @Getter
    @Setter
    private PowerGrid powerGrid;

    public DynamicPowerComponent(Entity owner, Supplier<AABB> aabbSupplier) {
        this.owner = owner;
        this.aabbSupplier = aabbSupplier;
    }

    public int getPowerConsumption() {
        int amount = 0;
        for (PowerConsumption powerConsumption : powerConsumptions) {
            amount += powerConsumption.amount;
        }
        return amount;
    }

    public void switchTo(PowerGrid powerGrid) {
        if (this.powerGrid == powerGrid) return;
        if (this.powerGrid != null) {
            this.powerGrid.notifyLeaving(this);
        }
        this.powerGrid = powerGrid;
        if (this.powerGrid != null) {
            this.powerGrid.notifyEntering(this);
        }
    }

    public AABB boundingBox() {
        return aabbSupplier.get();
    }

    public void gridTick() {
        if (owner instanceof IDynamicPowerComponentHolder) {
            ((IDynamicPowerComponentHolder) owner).anvilCraft$gridTick();
        }
    }

    public record PowerConsumption(int amount) {
    }
}
