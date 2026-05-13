package dev.dubhe.anvilcraft.item.armor;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.DynamicPowerComponent;
import dev.dubhe.anvilcraft.api.power.IDynamicPowerComponentHolder;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItemProperties;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.IInventoryCarriedAware;
import dev.dubhe.anvilcraft.item.property.component.FlightTime;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

public class IonoCraftBackpackItem extends Item implements IInventoryCarriedAware {
    public static final DynamicPowerComponent.PowerConsumption CONSUMPTION_64 = new DynamicPowerComponent.PowerConsumption(64);
    public static final DynamicPowerComponent.PowerConsumption CONSUMPTION_128 = new DynamicPowerComponent.PowerConsumption(128);
    public static final DynamicPowerComponent.PowerConsumption CONSUMPTION_256 = new DynamicPowerComponent.PowerConsumption(256);
    public static final DynamicPowerComponent.PowerConsumption CONSUMPTION_512 = new DynamicPowerComponent.PowerConsumption(512);

    public static final Identifier TEXTURE = AnvilCraft.of("textures/entity/equipment/ionocraft_backpack.png");
    public static final Identifier TEXTURE_OFF = AnvilCraft.of("textures/entity/equipment/ionocraft_backpack_off.png");

    public static final Identifier CREATIVE_FLIGHT_ID = AnvilCraft.of("creative_flight");
    public static final AttributeModifier CREATIVE_FLIGHT = new AttributeModifier(
        CREATIVE_FLIGHT_ID,
        1,
        AttributeModifier.Operation.ADD_VALUE
    );

    private static final Set<Function<Player, ItemStack>> STACK_PROVIDERS = new HashSet<>();

    public IonoCraftBackpackItem(Properties properties) {
        super(
            properties
                .repairable(ModItems.TIN_INGOT.asItem())
                .component(ModComponents.FLIGHT_TIME, FlightTime.EMPTY)
        );
        addStackProvider(player -> player.getItemBySlot(EquipmentSlot.CHEST));
    }

    @Override
    @SuppressWarnings({"removal"})
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        ItemProperties.register(
            this,
            AnvilCraft.of("flight_time"),
            ModItemProperties.FLIGHT_TIME
        );
    }

    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlot armorType, LivingEntity entity) {
        return armorType == EquipmentSlot.CHEST;
    }

    @Override
    public @Nullable Identifier getArmorTexture(
        ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        if (getFlightTime(stack) > 0) {
            return TEXTURE;
        }
        return TEXTURE_OFF;
    }

    public static int getFlightTime(ItemStack stack) {
        return stack.getOrDefault(ModComponents.FLIGHT_TIME, FlightTime.EMPTY).value();
    }

    public static void addFlightTime(ItemStack stack, int time) {
        stack.set(
            ModComponents.FLIGHT_TIME,
            new FlightTime(Math.clamp(getFlightTime(stack) + time, 0, AnvilCraft.CONFIG.ionoCraftBackpackMaxFlightTime))
        );
    }

    public static boolean canModify(ItemStack stack, DynamicPowerComponent component) {
        return stack.is(ModItems.IONOCRAFT_BACKPACK)
            && component.getPowerGrid() != null
            && component.getPowerGrid().isWorking();
    }

    public static void addStackProvider(Function<Player, ItemStack> provider) {
        STACK_PROVIDERS.add(provider);
    }

    public static ItemStack getByPlayer(Player player) {
        for (Function<Player, ItemStack> provider : STACK_PROVIDERS) {
            ItemStack stack = provider.apply(player);
            if (stack.is(ModItems.IONOCRAFT_BACKPACK)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static void refreshPower(ServerPlayer player) {
        IDynamicPowerComponentHolder holder = IDynamicPowerComponentHolder.of(player);

        AttributeInstance instance = player.getAttributes().getInstance(NeoForgeMod.CREATIVE_FLIGHT);
        if (instance == null) return;

        DynamicPowerComponent powerComponent = holder.anvilcraft$getPowerComponent();
        ItemStack equipped = getByPlayer(player);
        if (equipped.isEmpty()) {
            powerComponent.getPowerConsumptions().remove(CONSUMPTION_64);
            powerComponent.getPowerConsumptions().remove(CONSUMPTION_128);
            powerComponent.getPowerConsumptions().remove(CONSUMPTION_256);
            powerComponent.getPowerConsumptions().remove(CONSUMPTION_512);
            if (instance.hasModifier(CREATIVE_FLIGHT_ID)) {
                instance.removeModifier(CREATIVE_FLIGHT);
            }
            return;
        } else if (getFlightTime(equipped) >= AnvilCraft.CONFIG.ionoCraftBackpackMaxFlightTime && !player.getAbilities().flying) {
            powerComponent.getPowerConsumptions().remove(CONSUMPTION_64);
            powerComponent.getPowerConsumptions().remove(CONSUMPTION_128);
            powerComponent.getPowerConsumptions().remove(CONSUMPTION_256);
            powerComponent.getPowerConsumptions().remove(CONSUMPTION_512);
            return;
        }

        if (powerComponent.getPowerGrid() == null) return;

        PowerGrid powerGrid = powerComponent.getPowerGrid();
        if (powerGrid.isWorking()) {
            boolean hasConsumption = powerComponent.getPowerConsumptions().contains(CONSUMPTION_64)
                                  || powerComponent.getPowerConsumptions().contains(CONSUMPTION_128)
                                  || powerComponent.getPowerConsumptions().contains(CONSUMPTION_256)
                                  || powerComponent.getPowerConsumptions().contains(CONSUMPTION_512);

            if (!hasConsumption) {
                AtomicInteger playerCount = new AtomicInteger(0);
                powerGrid.getDynamicComponents().forEach(component -> {
                    if (component.getOwner() instanceof ServerPlayer) {
                        playerCount.incrementAndGet();
                    }
                });
                int remaining = powerGrid.getRemaining() / playerCount.get();
                if (remaining >= 512) {
                    powerComponent.getPowerConsumptions().add(CONSUMPTION_512);
                } else if (remaining >= 256) {
                    powerComponent.getPowerConsumptions().add(CONSUMPTION_256);
                } else if (remaining >= 128) {
                    powerComponent.getPowerConsumptions().add(CONSUMPTION_128);
                } else if (remaining >= 64) {
                    powerComponent.getPowerConsumptions().add(CONSUMPTION_64);
                }
            }
        } else {
            powerComponent.getPowerConsumptions().remove(CONSUMPTION_64);
            powerComponent.getPowerConsumptions().remove(CONSUMPTION_128);
            powerComponent.getPowerConsumptions().remove(CONSUMPTION_256);
            powerComponent.getPowerConsumptions().remove(CONSUMPTION_512);
        }
    }

    public static void refreshFlight(ServerPlayer player) {
        ItemStack equipped = getByPlayer(player);
        AttributeInstance instance = player.getAttributes().getInstance(NeoForgeMod.CREATIVE_FLIGHT);
        if (instance == null) return;
        int flightTime = getFlightTime(equipped);
        if (flightTime > 0) {
            if (!instance.hasModifier(CREATIVE_FLIGHT_ID)) {
                instance.addTransientModifier(CREATIVE_FLIGHT);
            }
        } else {
            if (instance.hasModifier(CREATIVE_FLIGHT_ID)) {
                instance.removeModifier(CREATIVE_FLIGHT);
            }
        }
    }

    public static void playerTick(ServerPlayer player) {
        final IDynamicPowerComponentHolder holder = IDynamicPowerComponentHolder.of(player);

        refreshPower(player);
        refreshFlight(player);

        ItemStack backpack = getByPlayer(player);
        if (backpack.isEmpty()) return;

        AtomicInteger flightTime = new AtomicInteger();

        if (player.getAbilities().flying && !player.isCreative() && !player.isSpectator()) {
            flightTime.decrementAndGet();
        }
        capacitorTick(holder, backpack, flightTime);

        addFlightTime(backpack, flightTime.get());
    }

    private static void capacitorTick(IDynamicPowerComponentHolder holder, ItemStack backpack, AtomicInteger flightTime) {
        int currentFlightTime = getFlightTime(backpack);
        if (currentFlightTime <= AnvilCraft.CONFIG.ionoCraftBackpackMaxFlightTime * 0.9333) {
            if (!(holder instanceof ServerPlayer player)) return;
            Inventory inventory = player.getInventory();
            int slot = inventory.findSlotMatchingItem(ModItems.CAPACITOR.asStack());
            if (slot >= 0) {
                inventory.removeItem(slot, 1);
                inventory.placeItemBackInInventory(ModItems.CAPACITOR_EMPTY.asStack());
                flightTime.addAndGet(AnvilCraft.CONFIG.ionoCraftBackpackMaxFlightTime / 15);
                return;
            }
        }
        if (currentFlightTime <= AnvilCraft.CONFIG.ionoCraftBackpackMaxFlightTime / 15) {
            if (!(holder instanceof ServerPlayer player)) return;
            Inventory inventory = player.getInventory();
            int slot = inventory.findSlotMatchingItem(ModItems.SUPER_CAPACITOR.asStack());
            if (slot >= 0) {
                inventory.removeItem(slot, 1);
                inventory.placeItemBackInInventory(ModItems.SUPER_CAPACITOR_EMPTY.asStack());
                flightTime.addAndGet(AnvilCraft.CONFIG.ionoCraftBackpackMaxFlightTime - currentFlightTime);
            }
        }
    }

    @Override
    public void onCarriedUpdate(ItemStack itemStack, ServerPlayer serverPlayer) {
        AttributeInstance instance = serverPlayer.getAttributes().getInstance(NeoForgeMod.CREATIVE_FLIGHT);
        if (instance != null && instance.hasModifier(CREATIVE_FLIGHT_ID)) {
            instance.removeModifier(CREATIVE_FLIGHT);
        }
    }
}
