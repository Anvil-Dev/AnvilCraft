package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.api.item.ICapacitorChargeable;
import dev.dubhe.anvilcraft.api.item.IFullCapacitor;
import dev.dubhe.anvilcraft.api.power.DynamicPowerComponent;
import dev.dubhe.anvilcraft.api.power.IDynamicPowerComponentHolder;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class WeatherproofChestplateItem extends IonocraftBackpackItem implements ICapacitorChargeable {
    public static final int MAX_ENERGY = 160_000_000;
    public static final int FLIGHT_CONSUMPTION = 5_000;
    private static final int GRID_ENERGY_PER_KW = 24 * FLIGHT_CONSUMPTION / 64;
    private static final DynamicPowerComponent.PowerConsumption[] CHARGING_POWER = {
        new DynamicPowerComponent.PowerConsumption(64), new DynamicPowerComponent.PowerConsumption(128),
        new DynamicPowerComponent.PowerConsumption(256), new DynamicPowerComponent.PowerConsumption(512)
    };

    public WeatherproofChestplateItem(Properties properties) {
        super(properties.component(ModComponents.STORED_ENERGY, 0), true);
    }

    public ItemStack creativeStack() {
        ItemStack stack = this.getDefaultInstance();
        stack.set(ModComponents.STORED_ENERGY, MAX_ENERGY);
        return stack;
    }

    public static int getEnergyStored(ItemStack stack) {
        return Math.clamp(stack.getOrDefault(ModComponents.STORED_ENERGY, 0), 0, MAX_ENERGY);
    }

    public static boolean canFly(ItemStack stack) {
        return getEnergyStored(stack) >= FLIGHT_CONSUMPTION;
    }

    @Override
    public boolean canAccept(ItemStack stack, IFullCapacitor capacitor, ItemStack capacitorStack, boolean force) {
        return force || getEnergyStored(stack) <= FLIGHT_CONSUMPTION;
    }

    @Override
    public boolean charge(ItemStack stack, IFullCapacitor capacitor, ItemStack capacitorStack) {
        return this.canAccept(stack, capacitor, capacitorStack, false) && this.chargeForce(stack, capacitor, capacitorStack);
    }

    public static void clearGridDemand(DynamicPowerComponent component) {
        for (DynamicPowerComponent.PowerConsumption demand : CHARGING_POWER) {
            component.getPowerConsumptions().remove(demand);
        }
    }

    public static int refreshGridDemand(ServerPlayer player, long available) {
        DynamicPowerComponent component = IDynamicPowerComponentHolder.of(player).anvilcraft$getPowerComponent();
        ItemStack stack = getByPlayer(player);
        PowerGrid grid = component.getPowerGrid();
        if (!(stack.getItem() instanceof WeatherproofChestplateItem) || grid == null || !player.isAlive()
            || player.isCreative() || player.isSpectator() || getEnergyStored(stack) >= MAX_ENERGY) return 0;
        for (int index = CHARGING_POWER.length - 1; index >= 0; index--) {
            if (available < CHARGING_POWER[index].amount()) continue;
            component.getPowerConsumptions().add(CHARGING_POWER[index]);
            return CHARGING_POWER[index].amount();
        }
        return 0;
    }

    private static void chargeFromGrid(ServerPlayer player, ItemStack stack) {
        if (player.level().getGameTime() % PowerGrid.GRID_TICK != 0) return;
        DynamicPowerComponent component = IDynamicPowerComponentHolder.of(player).anvilcraft$getPowerComponent();
        PowerGrid grid = component.getPowerGrid();
        if (grid == null || !grid.isWorking()) return;
        for (DynamicPowerComponent.PowerConsumption demand : CHARGING_POWER) {
            if (!component.getPowerConsumptions().contains(demand) || grid.getGenerate() < demand.amount()) continue;
            int energy = Math.min(MAX_ENERGY, getEnergyStored(stack) + demand.amount() * GRID_ENERGY_PER_KW);
            stack.set(ModComponents.STORED_ENERGY, energy);
            break;
        }
    }

    public static void tickEnergy(ServerPlayer player) {
        ItemStack stack = getByPlayer(player);
        if (!(stack.getItem() instanceof WeatherproofChestplateItem armor) || !player.isAlive()
            || player.isCreative() || player.isSpectator()) return;
        if (player.getAbilities().flying) {
            stack.set(ModComponents.STORED_ENERGY, Math.max(0, getEnergyStored(stack) - FLIGHT_CONSUMPTION));
        }
        chargeFromGrid(player, stack);
        if (getEnergyStored(stack) > FLIGHT_CONSUMPTION) return;
        for (ItemStack source : PocketInventory.carriedItems(player)) {
            if (source.isEmpty() || !(source.getItem() instanceof IFullCapacitor capacitor)) continue;
            if (!armor.charge(stack, capacitor, source.copy())) continue;
            ItemStack empty = capacitor.getEmpty(source);
            armor.onCharged(stack, capacitor, source.copy());
            source.shrink(1);
            player.getInventory().placeItemBackInInventory(empty);
            break;
        }
    }
}
