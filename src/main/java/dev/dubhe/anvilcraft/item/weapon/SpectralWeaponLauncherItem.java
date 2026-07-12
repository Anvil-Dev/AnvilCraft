package dev.dubhe.anvilcraft.item.weapon;

import dev.dubhe.anvilcraft.client.renderer.item.SpectralWeaponLauncherRenderer;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.SpectralSlingshotItem;
import dev.dubhe.anvilcraft.util.ColorUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class SpectralWeaponLauncherItem extends SpectralSlingshotItem {
    public static final int SHOOT_CONSUME = 1600000;
    public static final int EXHAUSTED_MODEL = 1;
    private static final int FULL_BAR_COLOR = 0xFF5454FF;
    private static final int BAR_COLOR = 0x7087FFFF;
    public static final int MAX_ENERGY = 640000000;

    public SpectralWeaponLauncherItem(Properties properties) {
        super(
            properties
                .component(ModComponents.STORED_ENERGY, SpectralWeaponLauncherItem.MAX_ENERGY)
                .component(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.DEFAULT)
        );
    }

    // 第一人称的手持动画、装填弹药的额外渲染等特殊代码在SpectralWeaponLauncherRenderer等类中
    @Override
    @OnlyIn(Dist.CLIENT)
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(SpectralWeaponLauncherRenderer.SpectralWeaponLauncherExtensions.of(SpectralWeaponLauncherRenderer.getInstance()));
    }

    @Override
    public boolean unableToUse(ItemStack stack) {
        return stack.getOrDefault(ModComponents.STORED_ENERGY, 0) < SpectralWeaponLauncherItem.SHOOT_CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        updateExhaustedModel(stack);
        if (unableToUse(stack)) {
            EnergyWeaponItem.showInsufficientPower(player);
            return InteractionResultHolder.fail(stack);
        }
        return super.use(level, player, hand);
    }

    @Override
    public void performShooting(
        Level level,
        LivingEntity shooter,
        InteractionHand hand,
        ItemStack weapon,
        float velocity,
        float inaccuracy,
        @Nullable LivingEntity target
    ) {
        super.performShooting(level, shooter, hand, weapon, velocity, inaccuracy, target);
        if (shooter.hasInfiniteMaterials()) return;
        int newEnergy = weapon.getOrDefault(ModComponents.STORED_ENERGY, 0) - SpectralWeaponLauncherItem.SHOOT_CONSUME;
        weapon.set(ModComponents.STORED_ENERGY, newEnergy);
        updateExhaustedModel(weapon);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        updateExhaustedModel(stack);
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    private static void updateExhaustedModel(ItemStack stack) {
        CustomModelData model = stack.getOrDefault(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.DEFAULT);
        CustomModelData expected = stack.getOrDefault(ModComponents.STORED_ENERGY, 0) < SHOOT_CONSUME
            ? new CustomModelData(EXHAUSTED_MODEL)
            : CustomModelData.DEFAULT;
        if (!model.equals(expected)) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, expected);
        }
    }

    public static void playerTick(ServerPlayer player) {
        ItemStack launcher = player.getMainHandItem();
        if (launcher.isEmpty() || !launcher.is(ModItems.SPECTRAL_WEAPON_LAUNCHER)) launcher = player.getOffhandItem();
        if (launcher.isEmpty() || !launcher.is(ModItems.SPECTRAL_WEAPON_LAUNCHER)) return;

        int energy = launcher.getOrDefault(ModComponents.STORED_ENERGY, 0);
        while (energy <= 480000) { // 480 kFE
            Inventory inventory = player.getInventory();
            int slot = inventory.findSlotMatchingItem(ModItems.SUPER_CAPACITOR.asStack());
            if (slot < 0) break;

            if (!player.hasInfiniteMaterials()) {
                inventory.removeItem(slot, 1);
                inventory.placeItemBackInInventory(ModItems.SUPER_CAPACITOR_EMPTY.asStack());
            }
            energy += 160000; // 160 kFE
        }
        if (energy != launcher.getOrDefault(ModComponents.STORED_ENERGY, 0)) {
            launcher.set(ModComponents.STORED_ENERGY, energy);
        }
        updateExhaustedModel(launcher);
    }

    @Override
    protected double getDamageAmplification() {
        return 1.0;
    }

    @Override
    protected SoundEvent getShootSound() {
        return SoundEvents.WIND_CHARGE_THROW;
    }

    @Override
    protected float getShootVolume() {
        return 0.7F;
    }

    @Override
    protected float getShootPitch() {
        return 0.9F;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int energy = stack.getOrDefault(ModComponents.STORED_ENERGY, 0);
        return Math.round(Math.clamp((float) energy / SpectralWeaponLauncherItem.MAX_ENERGY, 0, 1) * 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float energy = stack.getOrDefault(ModComponents.STORED_ENERGY, 0);
        return ColorUtil.lerpColor(energy / SpectralWeaponLauncherItem.MAX_ENERGY, BAR_COLOR, FULL_BAR_COLOR);
    }
}
