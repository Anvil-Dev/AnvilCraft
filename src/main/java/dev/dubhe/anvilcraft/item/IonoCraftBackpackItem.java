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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class IonoCraftBackpackItem extends ArmorItem {
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
    @SuppressWarnings({"removal"})
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        ItemProperties.register(
            this,
            AnvilCraft.of("flight_time"),
            ModItemProperties.FLIGHT_TIME
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return this.swapWithEquipmentSlot(this, level, player, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide) return;
        if (!(entity instanceof ServerPlayer serverPlayer)) return;
        IDynamicPowerComponentHolder holder = IDynamicPowerComponentHolder.of(serverPlayer);
        DynamicPowerComponent powerComponent = holder.anvilCraft$getPowerComponent();
        if (getByPlayer(serverPlayer) != stack) {
            powerComponent.getPowerConsumptions().remove(CONSUMPTION);
            AttributeInstance instance = serverPlayer.getAttributes().getInstance(NeoForgeMod.CREATIVE_FLIGHT);
            if (instance.hasModifier(CREATIVE_FLIGHT_ID)) {
                instance.removeModifier(CREATIVE_FLIGHT);
            }
            return;
        }
        if (powerComponent.getPowerGrid() != null) {
            powerComponent.getPowerConsumptions().add(CONSUMPTION);
        } else {
            powerComponent.getPowerConsumptions().remove(CONSUMPTION);
        }
        if (getFlightTime(stack) > 0) {
            AttributeInstance instance = serverPlayer.getAttributes().getInstance(NeoForgeMod.CREATIVE_FLIGHT);
            if (!instance.hasModifier(CREATIVE_FLIGHT_ID)) {
                instance.addTransientModifier(CREATIVE_FLIGHT);
            }
        } else {
            AttributeInstance instance = serverPlayer.getAttributes().getInstance(NeoForgeMod.CREATIVE_FLIGHT);
            if (instance.hasModifier(CREATIVE_FLIGHT_ID)) {
                instance.removeModifier(CREATIVE_FLIGHT);
            }
        }
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
    public @Nullable ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
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

    public static void setFlightTime(ItemStack stack, int time) {
        stack.set(ModComponents.FLIGHT_TIME, Math.clamp(time, 0, AnvilCraft.config.ionoCraftBackpackMaxFlightTime));
    }

    public static void addStackProvider(Function<Player, ItemStack> provider) {
        STACK_PROVIDERS.add(provider);
    }

    public static ItemStack getByPlayer(Player player) {
        for (var provider : STACK_PROVIDERS) {
            var stack = provider.apply(player);
            if (!stack.isEmpty()) return stack;
        }
        return ItemStack.EMPTY;
    }

    public static void flightTick(ServerPlayer player) {
        if (player.isCreative()) return;
        if (player instanceof IDynamicPowerComponentHolder holder && player.getAbilities().flying) {
            ItemStack itemStack = getByPlayer(player);
            if (itemStack.is(ModItems.IONOCRAFT_BACKPACK)) {
                int flightTime = IonoCraftBackpackItem.getFlightTime(itemStack);
                flightTime--;
                if (!(holder.anvilCraft$getPowerComponent().getPowerGrid() != null && holder.anvilCraft$getPowerComponent().getPowerGrid().isWorking())) {
                    if (flightTime <= AnvilCraft.config.ionoCraftBackpackMaxFlightTime / 2) {
                        Inventory inventory = player.getInventory();
                        int slot = inventory.findSlotMatchingItem(ModItems.CAPACITOR.asStack());
                        if (slot != -1) {
                            inventory.removeItem(slot, 1);
                            inventory.placeItemBackInInventory(ModItems.CAPACITOR_EMPTY.asStack());
                            flightTime = flightTime + AnvilCraft.config.ionoCraftBackpackMaxFlightTime / 2;
                        }
                    }
                }
                IonoCraftBackpackItem.setFlightTime(itemStack, flightTime);
            }
        }
    }
}
