package dev.dubhe.anvilcraft.api.power;

import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

@Getter
public class DynamicPowerComponent {
    private final Entity owner;
    @Getter
    @Setter
    private PowerGrid powerGrid;
    @Getter
    private final Set<PowerConsumption> powerConsumptions = Sets.newConcurrentHashSet();
    private final Supplier<AABB> aabbSupplier;

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

    public void switchTo(@Nullable PowerGrid powerGrid) {
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
            ((IDynamicPowerComponentHolder) owner).anvilcraft$gridTick();
        }
    }

    public MutableComponent getCommandDiscription() {
        double x = this.owner.getX();
        double y = this.owner.getY();
        double z = this.owner.getZ();
        return Component.translatable("command.anvilcraft.powergrid.info.dynamic_consumer",
            this.owner.getName(), formatDouble(x), formatDouble(y), formatDouble(z), this.getPowerConsumption())
            .withStyle(ChatFormatting.YELLOW);
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    public record PowerConsumption(int amount) {
    }
}
