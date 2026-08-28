package dev.dubhe.anvilcraft.item.armor;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.DynamicPowerComponent;
import dev.dubhe.anvilcraft.api.power.IDynamicPowerComponentHolder;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.tooltip.providers.IItemTooltipProvider;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.IInventoryCarriedAware;
import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;
import dev.dubhe.anvilcraft.network.IonoCraftBackpackFlyingPacket;
import dev.dubhe.anvilcraft.util.ColorUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.HashedStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

public class IonoCraftBackpackItem extends Item implements IInventoryCarriedAware, IItemTooltipProvider {
    public static final int MAX_ENERGY = 120000000;
    public static final int FLIGHT_CONSUMPTION = 5000;

    public static final DynamicPowerComponent.PowerConsumption CONSUMPTION_64 = new DynamicPowerComponent.PowerConsumption(64);
    public static final DynamicPowerComponent.PowerConsumption CONSUMPTION_128 = new DynamicPowerComponent.PowerConsumption(128);
    public static final DynamicPowerComponent.PowerConsumption CONSUMPTION_256 = new DynamicPowerComponent.PowerConsumption(256);
    public static final DynamicPowerComponent.PowerConsumption CONSUMPTION_512 = new DynamicPowerComponent.PowerConsumption(512);

    public static final Identifier TEXTURE = AnvilCraft.of("textures/entity/equipment/ionocraft_backpack.png");
    public static final Identifier TEXTURE_OFF = AnvilCraft.of("textures/entity/equipment/ionocraft_backpack_off.png");

    public static final Identifier CREATIVE_FLIGHT_ID = AnvilCraft.of("creative_flight");
    public static final AttributeModifier CREATIVE_FLIGHT = new AttributeModifier(
        IonoCraftBackpackItem.CREATIVE_FLIGHT_ID,
        1,
        AttributeModifier.Operation.ADD_VALUE
    );

    private static final Set<Function<Player, ItemStack>> STACK_PROVIDERS = new HashSet<>();
    /** 追踪玩家背包飞行状态，用于在状态变化时同步到其他客户端 */
    private static final Map<UUID, Boolean> FLYING_TRACKER = new HashMap<>();

    /** 玩家退出时清理飞行追踪器，防止内存泄漏 */
    public static void onPlayerLoggedOut(UUID uuid) {
        IonoCraftBackpackItem.FLYING_TRACKER.remove(uuid);
    }

    public IonoCraftBackpackItem(Properties properties) {
        super(
            properties
                .repairable(ModItemTags.TIN_INGOTS)
                .component(ModComponents.STORED_ENERGY, new StoredEnergy(IonoCraftBackpackItem.MAX_ENERGY))
        );
        IonoCraftBackpackItem.addStackProvider(player -> player.getItemBySlot(EquipmentSlot.CHEST));
    }

    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlot armorType, LivingEntity entity) {
        return armorType == EquipmentSlot.CHEST;
    }

    public static int getEnergyStored(ItemStack stack) {
        return stack.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value();
    }

    public static void setEnergyStored(ItemStack stack, int energy) {
        stack.set(
            ModComponents.STORED_ENERGY,
            new StoredEnergy(Math.clamp(energy, 0, IonoCraftBackpackItem.MAX_ENERGY))
        );
    }

    public static void addEnergy(ItemStack stack, int amount) {
        int current = IonoCraftBackpackItem.getEnergyStored(stack);
        IonoCraftBackpackItem.setEnergyStored(stack, current + amount);
    }
    
    public static int getFlightTime(ItemStack stack) {
        return getEnergyStored(stack) / FLIGHT_CONSUMPTION;
    }

    public static boolean canModify(ItemStack stack, DynamicPowerComponent component) {
        return stack.is(ModItems.IONOCRAFT_BACKPACK)
            && component.getPowerGrid() != null
            && component.getPowerGrid().isWorking();
    }

    public static void addStackProvider(Function<Player, ItemStack> provider) {
        IonoCraftBackpackItem.STACK_PROVIDERS.add(provider);
    }

    public static ItemStack getByPlayer(Player player) {
        for (Function<Player, ItemStack> provider : IonoCraftBackpackItem.STACK_PROVIDERS) {
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
        ItemStack equipped = IonoCraftBackpackItem.getByPlayer(player);
        if (equipped.isEmpty()) {
            powerComponent.getPowerConsumptions().remove(IonoCraftBackpackItem.CONSUMPTION_64);
            powerComponent.getPowerConsumptions().remove(IonoCraftBackpackItem.CONSUMPTION_128);
            powerComponent.getPowerConsumptions().remove(IonoCraftBackpackItem.CONSUMPTION_256);
            powerComponent.getPowerConsumptions().remove(IonoCraftBackpackItem.CONSUMPTION_512);
            if (instance.hasModifier(IonoCraftBackpackItem.CREATIVE_FLIGHT_ID)) {
                instance.removeModifier(IonoCraftBackpackItem.CREATIVE_FLIGHT);
            }
            return;
        } else if (IonoCraftBackpackItem.getEnergyStored(equipped) >= IonoCraftBackpackItem.MAX_ENERGY
                   && !player.getAbilities().flying) {
            powerComponent.getPowerConsumptions().remove(IonoCraftBackpackItem.CONSUMPTION_64);
            powerComponent.getPowerConsumptions().remove(IonoCraftBackpackItem.CONSUMPTION_128);
            powerComponent.getPowerConsumptions().remove(IonoCraftBackpackItem.CONSUMPTION_256);
            powerComponent.getPowerConsumptions().remove(IonoCraftBackpackItem.CONSUMPTION_512);
            return;
        }

        if (powerComponent.getPowerGrid() == null) return;

        PowerGrid powerGrid = powerComponent.getPowerGrid();
        if (powerGrid.isWorking()) {
            boolean hasConsumption = powerComponent.getPowerConsumptions()
                .contains(IonoCraftBackpackItem.CONSUMPTION_64)
                                     || powerComponent.getPowerConsumptions()
                                         .contains(IonoCraftBackpackItem.CONSUMPTION_128)
                                     || powerComponent.getPowerConsumptions()
                                         .contains(IonoCraftBackpackItem.CONSUMPTION_256)
                                     || powerComponent.getPowerConsumptions()
                                         .contains(IonoCraftBackpackItem.CONSUMPTION_512);

            if (!hasConsumption) {
                AtomicInteger playerCount = new AtomicInteger(0);
                powerGrid.getDynamicComponents().forEach(component -> {
                    if (component.getOwner() instanceof ServerPlayer) {
                        playerCount.incrementAndGet();
                    }
                });
                int remaining = powerGrid.getRemaining() / playerCount.get();
                if (remaining >= 512) {
                    powerComponent.getPowerConsumptions().add(IonoCraftBackpackItem.CONSUMPTION_512);
                } else if (remaining >= 256) {
                    powerComponent.getPowerConsumptions().add(IonoCraftBackpackItem.CONSUMPTION_256);
                } else if (remaining >= 128) {
                    powerComponent.getPowerConsumptions().add(IonoCraftBackpackItem.CONSUMPTION_128);
                } else if (remaining >= 64) {
                    powerComponent.getPowerConsumptions().add(IonoCraftBackpackItem.CONSUMPTION_64);
                }
            }
        } else {
            powerComponent.getPowerConsumptions().remove(IonoCraftBackpackItem.CONSUMPTION_64);
            powerComponent.getPowerConsumptions().remove(IonoCraftBackpackItem.CONSUMPTION_128);
            powerComponent.getPowerConsumptions().remove(IonoCraftBackpackItem.CONSUMPTION_256);
            powerComponent.getPowerConsumptions().remove(IonoCraftBackpackItem.CONSUMPTION_512);
        }
    }

    public static void refreshFlight(ServerPlayer player) {
        ItemStack equipped = IonoCraftBackpackItem.getByPlayer(player);
        AttributeInstance instance = player.getAttributes().getInstance(NeoForgeMod.CREATIVE_FLIGHT);
        if (instance == null) return;
        int energy = IonoCraftBackpackItem.getEnergyStored(equipped);
        if (energy > 0) {
            if (!instance.hasModifier(IonoCraftBackpackItem.CREATIVE_FLIGHT_ID)) {
                instance.addTransientModifier(IonoCraftBackpackItem.CREATIVE_FLIGHT);
            }
        } else {
            if (instance.hasModifier(IonoCraftBackpackItem.CREATIVE_FLIGHT_ID)) {
                instance.removeModifier(IonoCraftBackpackItem.CREATIVE_FLIGHT);
            }
        }
    }

    public static void playerTick(ServerPlayer player) {
        final IDynamicPowerComponentHolder holder = IDynamicPowerComponentHolder.of(player);

        IonoCraftBackpackItem.refreshPower(player);
        IonoCraftBackpackItem.refreshFlight(player);

        ItemStack backpack = IonoCraftBackpackItem.getByPlayer(player);
        boolean nowFlying = !backpack.isEmpty()
            && player.getAbilities().flying
            && !player.isCreative()
            && !player.isSpectator();

        // 飞行状态变化时同步到周边客户端
        Boolean prevFlying = IonoCraftBackpackItem.FLYING_TRACKER.put(player.getUUID(), nowFlying);
        if (prevFlying == null || prevFlying != nowFlying) {
            PacketDistributor.sendToPlayersTrackingEntity(
                player,
                new IonoCraftBackpackFlyingPacket(player.getId(), nowFlying)
            );
        }

        if (backpack.isEmpty()) return;

        if (player.getAbilities().flying && !player.isCreative() && !player.isSpectator()) {
            int energy = IonoCraftBackpackItem.getEnergyStored(backpack);
            if (energy > 0) {
                IonoCraftBackpackItem.setEnergyStored(
                    backpack,
                    energy - IonoCraftBackpackItem.FLIGHT_CONSUMPTION
                );
            } else {
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
        IonoCraftBackpackItem.capacitorTick(holder, backpack);
    }

    private static void capacitorTick(IDynamicPowerComponentHolder holder, ItemStack backpack) {
        if (!(holder instanceof ServerPlayer player)) return;
        int energy = IonoCraftBackpackItem.getEnergyStored(backpack);
        if (energy >= IonoCraftBackpackItem.MAX_ENERGY) return; // 能量已满，不消耗电容器
        Inventory inventory = player.getInventory();

        int capacitorSlot = inventory.findSlotMatchingItem(ModItems.CAPACITOR.asStack());
        if (capacitorSlot >= 0) {
            inventory.removeItem(capacitorSlot, 1);
            inventory.placeItemBackInInventory(ModItems.CAPACITOR_EMPTY.asStack());
            IonoCraftBackpackItem.addEnergy(backpack, 8_000_000);
            return;
        }

        int superSlot = inventory.findSlotMatchingItem(ModItems.SUPER_CAPACITOR.asStack());
        if (superSlot >= 0) {
            inventory.removeItem(superSlot, 1);
            inventory.placeItemBackInInventory(ModItems.SUPER_CAPACITOR_EMPTY.asStack());
            IonoCraftBackpackItem.addEnergy(backpack, 160_000_000);
        }
    }

    @Override
    public void onCarriedUpdate(HashedStack stack, ServerPlayer serverPlayer) {
        AttributeInstance instance = serverPlayer.getAttributes().getInstance(NeoForgeMod.CREATIVE_FLIGHT);
        if (instance != null && instance.hasModifier(IonoCraftBackpackItem.CREATIVE_FLIGHT_ID)) {
            instance.removeModifier(IonoCraftBackpackItem.CREATIVE_FLIGHT);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int energy = IonoCraftBackpackItem.getEnergyStored(stack);
        return Math.round(energy * 13.0f / IonoCraftBackpackItem.MAX_ENERGY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float ratio = (float) IonoCraftBackpackItem.getEnergyStored(stack) / IonoCraftBackpackItem.MAX_ENERGY;
        return ColorUtil.lerpColor(ratio, 0x7087FFFF, 0xFF5454FF);
    }

    @Override
    public void appendItemTooltip(
        ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        int energy = IonoCraftBackpackItem.getEnergyStored(stack);
        int totalSeconds = energy / IonoCraftBackpackItem.FLIGHT_CONSUMPTION / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        builder.accept(Component.translatable(
            "item.anvilcraft.ionocraft_backpack.flight_time",
            Component.literal(String.valueOf(minutes)).withStyle(ChatFormatting.GOLD),
            Component.literal(String.valueOf(seconds)).withStyle(ChatFormatting.GOLD)
        ).withStyle(ChatFormatting.GRAY));
    }
}
