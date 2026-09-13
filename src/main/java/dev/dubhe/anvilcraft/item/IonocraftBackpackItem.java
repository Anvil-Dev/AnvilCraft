package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.DynamicPowerComponent;
import dev.dubhe.anvilcraft.api.power.IDynamicPowerComponentHolder;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.init.ModDataAttachments;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.network.IonocraftBackpackFlyingPacket;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;
import javax.annotation.Nullable;

public class IonocraftBackpackItem extends ArmorItem implements IInventoryCarriedAware {
    public static final DynamicPowerComponent.PowerConsumption FLIGHT_POWER = new DynamicPowerComponent.PowerConsumption(8);
    private static final ResourceLocation SLOW_FALLING_ID = AnvilCraft.of("ionocraft_backpack_slow_falling");
    private static final AttributeModifier SLOW_FALLING = new AttributeModifier(
        SLOW_FALLING_ID, -0.875, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    );

    public static final ResourceLocation TEXTURE = AnvilCraft.of("textures/entity/equipment/ionocraft_backpack.png");
    public static final ResourceLocation TEXTURE_OFF = AnvilCraft.of("textures/entity/equipment/ionocraft_backpack_off.png");

    public static final ResourceLocation CREATIVE_FLIGHT_ID = AnvilCraft.of("creative_flight");
    public static final AttributeModifier CREATIVE_FLIGHT = new AttributeModifier(
        CREATIVE_FLIGHT_ID,
        1,
        AttributeModifier.Operation.ADD_VALUE
    );

    private static final Set<Function<Player, ItemStack>> STACK_PROVIDERS = new HashSet<>();
    /** 追踪玩家背包飞行状态，用于在状态变化时同步到其他客户端 */
    private static final Map<ServerPlayer, Boolean> FLYING_TRACKER = new WeakHashMap<>();

    public IonocraftBackpackItem(Properties properties) {
        super(ArmorMaterials.IRON, Type.CHESTPLATE, properties);
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
        return repair.is(ModItems.TIN_INGOT);
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
        return entity instanceof LivingEntity living && hasGridFlight(living) ? TEXTURE : TEXTURE_OFF;
    }

    public static boolean hasGridFlight(LivingEntity entity) {
        AttributeInstance instance = entity.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        return instance != null && instance.hasModifier(CREATIVE_FLIGHT_ID);
    }

    public static boolean isSlowFalling(Player player) {
        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        return gravity != null && gravity.hasModifier(SLOW_FALLING_ID);
    }

    public static void applySlowFalling(Player player) {
        if (!isSlowFalling(player) || player.onGround() || player.isCreative() || player.isSpectator()) return;
        player.fallDistance = 0;
        if (player.getDeltaMovement().y < -0.5) {
            player.setDeltaMovement(player.getDeltaMovement().multiply(1, 0, 1).add(0, -0.5, 0));
        }
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
        DynamicPowerComponent component = IDynamicPowerComponentHolder.of(player).anvilcraft$getPowerComponent();
        if (!getByPlayer(player).isEmpty() && player.isAlive() && !player.isCreative() && !player.isSpectator()
            && component.getPowerGrid() != null) {
            component.getPowerConsumptions().add(FLIGHT_POWER);
        } else {
            component.getPowerConsumptions().remove(FLIGHT_POWER);
        }
    }

    public static void refreshFlight(ServerPlayer player) {
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        if (flight == null || gravity == null) return;

        boolean equipped = !getByPlayer(player).isEmpty() && player.isAlive() && !player.isCreative() && !player.isSpectator();
        DynamicPowerComponent component = IDynamicPowerComponentHolder.of(player).anvilcraft$getPowerComponent();
        PowerGrid grid = component.getPowerGrid();
        boolean powered = equipped && grid != null && grid.isWorking() && grid.getGenerate() >= FLIGHT_POWER.amount()
            && component.getPowerConsumptions().contains(FLIGHT_POWER);
        boolean hadFlight = flight.hasModifier(CREATIVE_FLIGHT_ID);
        boolean wasFalling = gravity.hasModifier(SLOW_FALLING_ID);
        boolean hadDescent = player.getData(ModDataAttachments.IONOCRAFT_DESCENT_AVAILABLE);
        boolean startDescent = hadFlight && player.getAbilities().flying && grid == null;
        boolean descentAvailable = equipped && !player.onGround() && grid == null && (hadDescent || startDescent);
        if (hadDescent != descentAvailable) {
            player.setData(ModDataAttachments.IONOCRAFT_DESCENT_AVAILABLE, descentAvailable);
        }
        boolean falling = descentAvailable && (startDescent || wasFalling);

        if (powered && !hadFlight) {
            flight.addTransientModifier(CREATIVE_FLIGHT);
        } else if (!powered && hadFlight) {
            flight.removeModifier(CREATIVE_FLIGHT_ID);
        }
        if (falling && !wasFalling) {
            gravity.addTransientModifier(SLOW_FALLING);
        } else if (!falling && wasFalling) {
            gravity.removeModifier(SLOW_FALLING_ID);
        }

        // 触地优先结束本次缓降，避免同一刻重新入网时自动起飞。
        boolean resumeFlight = powered && hadDescent && !player.onGround();
        if ((hadFlight || powered || hadDescent) && !player.isCreative() && !player.isSpectator()) {
            boolean mayFly = flight.getValue() > 0;
            boolean flying = mayFly && (player.getAbilities().flying || resumeFlight);
            if (player.getAbilities().mayfly != mayFly || player.getAbilities().flying != flying) {
                player.getAbilities().mayfly = mayFly;
                player.getAbilities().flying = flying;
                player.onUpdateAbilities();
            }
        }
        applySlowFalling(player);
    }

    public static void toggleSlowFalling(ServerPlayer player) {
        refreshFlight(player);
        if (!player.getData(ModDataAttachments.IONOCRAFT_DESCENT_AVAILABLE)) return;
        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        if (gravity == null) return;
        if (gravity.hasModifier(SLOW_FALLING_ID)) {
            gravity.removeModifier(SLOW_FALLING_ID);
        } else {
            gravity.addTransientModifier(SLOW_FALLING);
            applySlowFalling(player);
        }
    }

    public static void playerTick(ServerPlayer player) {
        refreshPower(player);
        refreshFlight(player);

        ItemStack backpack = getByPlayer(player);
        boolean nowFlying = !backpack.isEmpty()
            && player.getAbilities().flying
            && !player.isCreative()
            && !player.isSpectator();

        // 飞行状态变化时同步到周边客户端
        Boolean prevFlying = FLYING_TRACKER.put(player, nowFlying);
        if (prevFlying == null || prevFlying != nowFlying) {
            PacketDistributor.sendToPlayersTrackingEntity(
                player,
                new IonocraftBackpackFlyingPacket(player.getId(), nowFlying)
            );
        }
    }

    @Override
    public void onCarriedUpdate(ItemStack itemStack, ServerPlayer serverPlayer) {
        refreshPower(serverPlayer);
        refreshFlight(serverPlayer);
    }
}
