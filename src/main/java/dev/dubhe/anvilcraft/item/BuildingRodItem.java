package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.item.ICapacitorChargeable;
import dev.dubhe.anvilcraft.api.item.IFullCapacitor;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
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
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidUtil;

import javax.annotation.Nullable;

public class BuildingRodItem extends Item implements ICapacitorChargeable {
    public static final int MAX_ENERGY = CapacitorItem.ENERGY;

    public BuildingRodItem(Properties properties) {
        super(properties.stacksTo(1).component(ModComponents.STORED_ENERGY, 0));
    }

    public ItemStack creativeStack() {
        ItemStack stack = this.getDefaultInstance();
        stack.set(ModComponents.STORED_ENERGY, MAX_ENERGY);
        return stack;
    }

    public static boolean isPlacementMaterial(ItemStack stack) {
        return stack.is(ModItems.FILTER) || stack.getItem() instanceof BlockItem
            || stack.getItem() instanceof BucketItem && !(stack.getItem() instanceof MobBucketItem)
            && FluidUtil.getFluidContained(stack).isPresent();
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
        ItemStack other = material(player);
        boolean building = isHeld(player) && (isPlacementMaterial(other) || other.is(ModItems.STRUCTURE_DISK));
        setReach(player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE),
            building ? 15 - CrabClawItem.RANGE_ATTRIBUTE_MODIFIER.amount() : 0);
        setReach(player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE), 0);
    }

    private static void setReach(@Nullable AttributeInstance attribute, double amount) {
        if (attribute == null) return;
        var id = AnvilCraft.of("building_rod_reach");
        AttributeModifier previous = attribute.getModifier(id);
        if (previous != null && previous.amount() == amount) return;
        attribute.removeModifier(id);
        if (amount > 0) attribute.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
    }

    public static boolean ready(Player player, ItemStack rod) {
        recharge(player, rod);
        if (rod.getOrDefault(ModComponents.STORED_ENERGY, 0) > 0) return true;
        player.displayClientMessage(Component.translatable("message.anvilcraft.building_rod.no_energy"), true);
        return false;
    }

    public static void consume(Player player, ItemStack rod, int blocks) {
        if (player.isCreative()) return;
        rod.set(ModComponents.STORED_ENERGY, Math.max(0, rod.getOrDefault(ModComponents.STORED_ENERGY, 0) - blocks * 100));
        recharge(player, rod);
    }

    private static void recharge(Player player, ItemStack rod) {
        if (rod.getOrDefault(ModComponents.STORED_ENERGY, 0) != 0) return;
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
    public boolean canBeCharged(ItemStack stack, IEnergyStorage storage, IFullCapacitor capacitor, ItemStack capacitorStack) {
        return storage.getEnergyStored() == 0;
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
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * stack.getOrDefault(ModComponents.STORED_ENERGY, 0) / MAX_ENERGY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x7087FF;
    }
}
