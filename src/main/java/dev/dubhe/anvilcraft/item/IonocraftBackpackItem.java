package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.DynamicPowerComponent;
import dev.dubhe.anvilcraft.api.power.IDynamicPowerComponentHolder;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.init.ModDataAttachments;
import dev.dubhe.anvilcraft.network.IonocraftBackpackFlyingPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.WeakHashMap;

public class IonocraftBackpackItem extends EquipmentArmorItem implements IInventoryCarriedAware {
    public static final DynamicPowerComponent.PowerConsumption FLIGHT_POWER = new DynamicPowerComponent.PowerConsumption(8);
    private static final ResourceLocation SLOW_FALLING_ID = AnvilCraft.of("ionocraft_backpack_slow_falling");
    private static final AttributeModifier SLOW_FALLING = new AttributeModifier(
        SLOW_FALLING_ID, -0.875, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    );

    public static final ResourceLocation TEXTURE = AnvilCraft.of("textures/entity/equipment/spacesuit.png");
    public static final ResourceLocation TEXTURE_OFF = AnvilCraft.of("textures/entity/equipment/spacesuit_off.png");

    public static final ResourceLocation CREATIVE_FLIGHT_ID = AnvilCraft.of("creative_flight");
    public static final AttributeModifier CREATIVE_FLIGHT = new AttributeModifier(
        CREATIVE_FLIGHT_ID,
        1,
        AttributeModifier.Operation.ADD_VALUE
    );

    /** 追踪玩家背包飞行状态，用于在状态变化时同步到其他客户端 */
    private static final Map<ServerPlayer, Boolean> FLYING_TRACKER = new WeakHashMap<>();

    /** 追踪上一 tick 是否装备着背包，用于在装备状态变化时重同步物品栏槽位 */
    private static final Map<ServerPlayer, Boolean> EQUIPPED_TRACKER = new WeakHashMap<>();

    public IonocraftBackpackItem(Properties properties) {
        this(properties, false);
    }

    protected IonocraftBackpackItem(Properties properties, boolean weatherproof) {
        super(properties, Type.CHESTPLATE, weatherproof,
            weatherproof ? "weatherproof_spacesuit" : "ionocraft_backpack");
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
    public ResourceLocation getArmorTexture(
        ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        if (this.isWeatherproof()) {
            return WeatherproofChestplateItem.getEnergyStored(stack) > 0
                ? AnvilCraft.of("textures/entity/equipment/weatherproof_spacesuit.png")
                : AnvilCraft.of("textures/entity/equipment/weatherproof_spacesuit_off.png");
        }
        return entity.getData(ModDataAttachments.IN_POWER_GRID) ? TEXTURE : TEXTURE_OFF;
    }

    public static boolean hasGridFlight(LivingEntity entity) {
        AttributeInstance instance = entity.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        return instance != null && instance.hasModifier(CREATIVE_FLIGHT_ID);
    }

    public static boolean isSlowFalling(Player player) {
        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        return gravity != null && gravity.hasModifier(SLOW_FALLING_ID);
    }

    public static boolean protectsFromFalling(Player player) {
        if (player.isCreative() || player.isSpectator() || player.getAbilities().flying) return true;
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        return flight == null || !flight.hasModifier(CREATIVE_FLIGHT_ID)
            || flight.getBaseValue() > 0 || flight.getModifiers().size() > 1;
    }

    public static void applySlowFalling(Player player) {
        if (!isSlowFalling(player) || player.onGround() || player.isCreative() || player.isSpectator()) return;
        player.fallDistance = 0;
        if (player.getDeltaMovement().y < -0.5) {
            player.setDeltaMovement(player.getDeltaMovement().multiply(1, 0, 1).add(0, -0.5, 0));
        }
    }

    public static ItemStack getByPlayer(Player player) {
        ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
        return stack.getItem() instanceof IonocraftBackpackItem ? stack : ItemStack.EMPTY;
    }

    public static void refreshPower(ServerPlayer player) {
        WeatherproofChestplateItem.refreshGridDemand(player);
        DynamicPowerComponent component = IDynamicPowerComponentHolder.of(player).anvilcraft$getPowerComponent();
        if (!getByPlayer(player).isEmpty() && !(getByPlayer(player).getItem() instanceof WeatherproofChestplateItem)
            && player.isAlive() && !player.isCreative() && !player.isSpectator()
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
        boolean storedFlight = getByPlayer(player).getItem() instanceof WeatherproofChestplateItem;
        if (storedFlight) powered = equipped && WeatherproofChestplateItem.canFly(getByPlayer(player));
        boolean hadFlight = flight.hasModifier(CREATIVE_FLIGHT_ID);
        boolean wasFalling = gravity.hasModifier(SLOW_FALLING_ID);
        boolean hadDescent = player.getData(ModDataAttachments.IONOCRAFT_DESCENT_AVAILABLE);
        boolean startDescent = hadFlight && player.getAbilities().flying && (grid == null || storedFlight) && !powered;
        boolean descentAvailable = equipped && !player.onGround() && (grid == null || storedFlight)
            && !powered && (hadDescent || startDescent);
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
        WeatherproofChestplateItem.tickEnergy(player);
        refreshPower(player);
        refreshFlight(player);

        ItemStack backpack = getByPlayer(player);
        boolean equipped = !backpack.isEmpty();

        // 装备状态变化时整表重同步物品栏。背包的穿戴只走 setItemSlot / onEquipItem，
        // 不经过 inventoryMenu 的槽位变更广播，客户端本端 Inventory 会残留上一次的槽位内容，
        // 表现为护腿等槽位出现幻影物品（仅客户端渲染，任意槽位点击后即被服务端校正回传覆盖）。
        Boolean prevEquipped = EQUIPPED_TRACKER.put(player, equipped);
        if (prevEquipped != null && prevEquipped != equipped) {
            player.inventoryMenu.sendAllDataToRemote();
        }

        boolean nowFlying = equipped
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
