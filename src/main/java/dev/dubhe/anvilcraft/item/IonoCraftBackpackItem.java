package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.DynamicPowerComponent;
import dev.dubhe.anvilcraft.api.power.IDynamicPowerComponentHolder;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.init.ModItemProperties;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class IonoCraftBackpackItem extends ArmorItem implements IInventoryCarriedAware {
    public static final DynamicPowerComponent.PowerConsumption CONSUMPTION = new DynamicPowerComponent.PowerConsumption(64);

    public static final ResourceLocation TEXTURE = AnvilCraft.of("textures/entity/equipment/ionocraft_backpack.png");
    public static final ResourceLocation TEXTURE_OFF = AnvilCraft.of("textures/entity/equipment/ionocraft_backpack_off.png");

    public static final ResourceLocation CREATIVE_FLIGHT_ID = AnvilCraft.of("creative_flight");
    public static final AttributeModifier CREATIVE_FLIGHT = new AttributeModifier(
        CREATIVE_FLIGHT_ID,
        1,
        AttributeModifier.Operation.ADD_VALUE
    );

    private static final Set<Function<Player, ItemStack>> STACK_PROVIDERS = new HashSet<>();

    public IonoCraftBackpackItem(Properties properties) {
        super(
            ArmorMaterials.IRON,
            Type.CHESTPLATE,
            properties.component(ModComponents.FLIGHT_TIME, 0)
        );
        DispenserBlock.registerBehavior(this, ArmorItem.DISPENSE_ITEM_BEHAVIOR);
        addStackProvider(player -> player.getItemBySlot(EquipmentSlot.CHEST));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 15;
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.TITANIUM_INGOT);
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
    public Holder<SoundEvent> getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_IRON;
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.CHEST;
    }

    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlot armorType, LivingEntity entity) {
        return armorType == EquipmentSlot.CHEST;
    }

    @Override
    public @Nullable ResourceLocation getArmorTexture(
        ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        if (getFlightTime(stack) > 0) {
            return TEXTURE;
        }
        return TEXTURE_OFF;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable(
            "item.anvilcraft.ionocraft_backpack.flight_time",
            Component.literal(String.valueOf(getFlightTime(stack) / 20)).withStyle(ChatFormatting.GOLD)
        ));
    }

    public static int getFlightTime(ItemStack stack) {
        return stack.getOrDefault(ModComponents.FLIGHT_TIME, 0);
    }

    public static void addFlightTime(ItemStack stack, int time) {
        stack.set(ModComponents.FLIGHT_TIME, Math.clamp(getFlightTime(stack) + time, 0, AnvilCraft.config.ionoCraftBackpackMaxFlightTime));
    }

    public static boolean canModify(ItemStack stack, DynamicPowerComponent component) {
        return stack.is(ModItems.IONOCRAFT_BACKPACK)
            && component.getPowerGrid() != null
            && component.getPowerGrid().isWorking()
            && component.getPowerConsumptions().contains(CONSUMPTION);
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

        DynamicPowerComponent powerComponent = holder.anvilCraft$getPowerComponent();
        ItemStack equipped = getByPlayer(player);
        if (equipped.isEmpty()) {
            powerComponent.getPowerConsumptions().remove(CONSUMPTION);
            if (instance.hasModifier(CREATIVE_FLIGHT_ID)) {
                instance.removeModifier(CREATIVE_FLIGHT);
            }
            return;
        } else if (getFlightTime(equipped) >= AnvilCraft.config.ionoCraftBackpackMaxFlightTime && !player.getAbilities().flying) {
            powerComponent.getPowerConsumptions().remove(CONSUMPTION);
            return;
        }

        if (powerComponent.getPowerGrid() == null) return;
        if (powerComponent.getPowerGrid().getRemaining() >= 64) {
            powerComponent.getPowerConsumptions().add(CONSUMPTION);
        } else if (powerComponent.getPowerConsumptions().contains(CONSUMPTION) && !powerComponent.getPowerGrid().isWorking()) {
            powerComponent.getPowerConsumptions().remove(CONSUMPTION);
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
        IDynamicPowerComponentHolder holder = IDynamicPowerComponentHolder.of(player);

        refreshPower(player);
        refreshFlight(player);

        ItemStack backpack = getByPlayer(player);
        if (backpack.isEmpty()) return;

        AtomicInteger flightTime = new AtomicInteger();

        if (player.getAbilities().flying) {
            flightTime.decrementAndGet();
        }
        capacitorTick(holder, backpack, flightTime);

        addFlightTime(backpack, flightTime.get());
    }

    private static void capacitorTick(IDynamicPowerComponentHolder holder, ItemStack backpack, AtomicInteger flightTime) {
        if (getFlightTime(backpack) > AnvilCraft.config.ionoCraftBackpackMaxFlightTime / 2) return;

        if (!(holder instanceof ServerPlayer player)) return;
        Inventory inventory = player.getInventory();
        int slot = inventory.findSlotMatchingItem(ModItems.CAPACITOR.asStack());
        if (slot < 0) return;

        inventory.removeItem(slot, 1);
        inventory.placeItemBackInInventory(ModItems.CAPACITOR_EMPTY.asStack());
        flightTime.addAndGet(AnvilCraft.config.ionoCraftBackpackMaxFlightTime / 2);
    }

    @Override
    public void onCarriedUpdate(ItemStack itemStack, ServerPlayer serverPlayer) {
        AttributeInstance instance = serverPlayer.getAttributes().getInstance(NeoForgeMod.CREATIVE_FLIGHT);
        if (instance != null && instance.hasModifier(CREATIVE_FLIGHT_ID)) {
            instance.removeModifier(CREATIVE_FLIGHT);
        }
    }
}
