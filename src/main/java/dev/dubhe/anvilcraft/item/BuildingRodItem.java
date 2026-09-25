package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.item.ICapacitorChargeable;
import dev.dubhe.anvilcraft.api.item.IFullCapacitor;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import dev.dubhe.anvilcraft.item.ingredients.CapacitorItem;
import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;
import dev.dubhe.anvilcraft.item.utility.CrabClawItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import org.jspecify.annotations.Nullable;

public class BuildingRodItem extends Item implements ICapacitorChargeable {
    public static final int MAX_ENERGY = CapacitorItem.ENERGY;

    public BuildingRodItem(Properties properties) {
        super(properties.stacksTo(1).component(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY));
    }

    public ItemStack creativeStack() {
        ItemStack stack = this.getDefaultInstance();
        stack.set(ModComponents.STORED_ENERGY, new StoredEnergy(MAX_ENERGY));
        return stack;
    }

    public static boolean isPlacementMaterial(ItemStack stack) {
        return stack.is(ModItems.FILTER) || stack.getItem() instanceof BlockItem
            || stack.getItem() instanceof BucketItem && !(stack.getItem() instanceof MobBucketItem)
            && !FluidUtil.getFirstStackContained(stack).isEmpty();
    }

    public static boolean isHeld(Player player) {
        return player.getMainHandItem().is(ModItems.BUILDING_ROD) || player.getOffhandItem().is(ModItems.BUILDING_ROD);
    }

    public static ItemStack heldRod(Player player) {
        return player.getMainHandItem().is(ModItems.BUILDING_ROD) ? player.getMainHandItem() : player.getOffhandItem();
    }

    public static InteractionHand materialHand(Player player) {
        return player.getMainHandItem().is(ModItems.BUILDING_ROD) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    public static ItemStack material(Player player) {
        return player.getItemInHand(materialHand(player));
    }

    public static boolean isCarried(Player player) {
        if (isHeld(player)) return true;
        return PocketInventory.carriedItems(player).stream().anyMatch(stack -> stack.is(ModItems.BUILDING_ROD));
    }

    public static void updateReach(Player player) {
        boolean carried = isCarried(player);
        var block = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        var entity = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        double claw = CrabClawItem.RANGE_ATTRIBUTE_MODIFIER.amount();
        setReach(block, "building_rod_carried_reach", carried && block != null
            && !block.hasModifier(CrabClawItem.RANGE_ATTRIBUTE_MODIFIER.id()) ? claw : 0);
        setReach(entity, "building_rod_carried_reach", carried && entity != null
            && !entity.hasModifier(CrabClawItem.RANGE_ATTRIBUTE_MODIFIER.id()) ? claw : 0);
        ItemStack other = material(player);
        boolean building = isHeld(player) && (isPlacementMaterial(other) || other.is(ModItems.STRUCTURE_DISK));
        setReach(block, "building_rod_reach", building ? 15 - claw : 0);
        setReach(entity, "building_rod_reach", 0);
    }

    private static void setReach(@Nullable AttributeInstance attribute, String key, double amount) {
        if (attribute == null) return;
        var id = AnvilCraft.of(key);
        AttributeModifier previous = attribute.getModifier(id);
        if (previous != null && previous.amount() == amount) return;
        attribute.removeModifier(id);
        if (amount > 0) attribute.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
    }

    public static boolean ready(Player player, ItemStack rod) {
        recharge(player, rod);
        if (rod.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value() > 0) return true;
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.translatable("message.anvilcraft.building_rod.no_energy"), true);
        }
        return false;
    }

    public static void consume(Player player, ItemStack rod, int blocks) {
        if (player.isCreative()) return;
        rod.set(ModComponents.STORED_ENERGY, new StoredEnergy(
            Math.max(0, rod.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value() - blocks * 100)));
        recharge(player, rod);
    }

    private static void recharge(Player player, ItemStack rod) {
        if (rod.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value() != 0) return;
        for (ItemStack stack : PocketInventory.carriedItems(player)) {
            if (!(stack.getItem() instanceof IFullCapacitor capacitor)) continue;
            if (!((BuildingRodItem) rod.getItem()).charge(rod, capacitor, stack)) continue;
            ItemStack empty = capacitor.getEmpty(stack);
            stack.shrink(1);
            player.getInventory().placeItemBackInInventory(empty);
            return;
        }
    }

    @Override
    public boolean canBeCharged(ItemStack stack, EnergyHandler storage, IFullCapacitor capacitor, ItemStack capacitorStack) {
        return storage.getAmountAsLong() == 0;
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand) {
        return true;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * stack.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value() / MAX_ENERGY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x7087FF;
    }
}
