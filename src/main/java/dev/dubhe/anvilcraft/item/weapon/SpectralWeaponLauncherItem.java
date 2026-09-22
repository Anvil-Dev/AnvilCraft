package dev.dubhe.anvilcraft.item.weapon;

import dev.dubhe.anvilcraft.api.item.ICapacitorChargeable;
import dev.dubhe.anvilcraft.api.item.IFullCapacitor;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;
import dev.dubhe.anvilcraft.item.tool.SpectralSlingshotItem;
import dev.dubhe.anvilcraft.util.ColorUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public class SpectralWeaponLauncherItem extends SpectralSlingshotItem implements ICapacitorChargeable {
    public static final int SHOOT_CONSUME = 1_600_000;
    public static final int EXHAUSTED_MODEL = 1;
    private static final int FULL_BAR_COLOR = 0xFF5454FF;
    private static final int BAR_COLOR = 0x7087FFFF;
    public static final int MAX_ENERGY = 640_000_000;

    public SpectralWeaponLauncherItem(Properties properties) {
        super(
            properties
                .component(ModComponents.STORED_ENERGY, new StoredEnergy(SpectralWeaponLauncherItem.MAX_ENERGY))
                .component(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.EMPTY)
        );
    }

    // 第一人称的手持动画、装填弹药的额外渲染等特殊代码在SpectralWeaponLauncherRenderer等类中
    @Override
    public boolean unableToUse(ItemStack stack) {
        return stack.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value() < SpectralWeaponLauncherItem.SHOOT_CONSUME;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        updateExhaustedModel(stack);
        if (this.unableToUse(stack)) {
            EnergyWeaponItem.showInsufficientPower(player);
            return InteractionResult.FAIL;
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
        if (!(level instanceof ServerLevel)) return;
        if (this.unableToUse(weapon)) {
            updateExhaustedModel(weapon);
            if (shooter instanceof Player player) EnergyWeaponItem.showInsufficientPower(player);
            return;
        }
        if (weapon.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY).isEmpty()) return;
        super.performShooting(level, shooter, hand, weapon, velocity, inaccuracy, target);
        if (shooter.hasInfiniteMaterials()) return;
        int newEnergy = weapon.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value()
            - SpectralWeaponLauncherItem.SHOOT_CONSUME;
        weapon.set(ModComponents.STORED_ENERGY, new StoredEnergy(newEnergy));
        updateExhaustedModel(weapon);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        updateExhaustedModel(stack);
        super.inventoryTick(stack, level, entity, slot);
    }

    private static void updateExhaustedModel(ItemStack stack) {
        CustomModelData model = stack.getOrDefault(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.EMPTY);
        CustomModelData expected = stack.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value() < SHOOT_CONSUME
            ? new CustomModelData(java.util.List.of((float) EXHAUSTED_MODEL), java.util.List.of(), java.util.List.of(), java.util.List.of())
            : CustomModelData.EMPTY;
        if (!model.equals(expected)) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, expected);
        }
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
        int energy = stack.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value();
        return energy <= 0 ? 0 : Math.max(1, Math.round(Math.clamp((float) energy / MAX_ENERGY, 0, 1) * 13));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float energy = stack.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value();
        return ColorUtil.lerpColor(energy / SpectralWeaponLauncherItem.MAX_ENERGY, BAR_COLOR, FULL_BAR_COLOR);
    }

    @Override
    public void onCharged(ItemStack stack, IFullCapacitor capacitor, ItemStack capacitorStack) {
        stack.set(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.EMPTY);
    }

}
