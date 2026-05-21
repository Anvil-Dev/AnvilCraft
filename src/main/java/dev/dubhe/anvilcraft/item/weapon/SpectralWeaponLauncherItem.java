package dev.dubhe.anvilcraft.item.weapon;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;
import dev.dubhe.anvilcraft.item.tool.SpectralSlingshotItem;
import dev.dubhe.anvilcraft.util.ColorUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class SpectralWeaponLauncherItem extends SpectralSlingshotItem {
    public static final int SHOOT_CONSUME = 800;
    private static final int FULL_BAR_COLOR = 0xFF5454FF;
    private static final int BAR_COLOR = 0x7087FFFF;
    public static final int MAX_ENERGY = 320000; // 320MJ

    public SpectralWeaponLauncherItem(Properties properties) {
        super(
            properties
                .component(ModComponents.STORED_ENERGY, new StoredEnergy(SpectralWeaponLauncherItem.MAX_ENERGY))
        );
    }

    @Override
    public boolean unableToUse(ItemStack stack) {
        return stack.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value() < SpectralWeaponLauncherItem.SHOOT_CONSUME;
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
        int newEnergy = weapon.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY)
                            .value() - SpectralWeaponLauncherItem.SHOOT_CONSUME;
        weapon.set(ModComponents.STORED_ENERGY, new StoredEnergy(newEnergy));
    }

    public static void playerTick(ServerPlayer player) {
        ItemStack launcher = player.getMainHandItem();
        if (launcher.isEmpty() || !launcher.is(ModItems.SPECTRAL_WEAPON_LAUNCHER)) launcher = player.getOffhandItem();
        if (launcher.isEmpty() || !launcher.is(ModItems.SPECTRAL_WEAPON_LAUNCHER)) return;

        int energy = launcher.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value();
        while (energy <= 240000) { // 240MJ
            Inventory inventory = player.getInventory();
            int slot = inventory.findSlotMatchingItem(ModItems.SUPER_CAPACITOR.asStack());
            if (slot < 0) break;

            if (!player.hasInfiniteMaterials()) {
                inventory.removeItem(slot, 1);
                inventory.placeItemBackInInventory(ModItems.SUPER_CAPACITOR_EMPTY.asStack());
            }
            energy += 80000; // 80MJ
        }
        if (energy == launcher.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value()) return;
        launcher.set(ModComponents.STORED_ENERGY, new StoredEnergy(energy));
    }

    @Override
    protected double getDamageAmplification() {
        return 1.0;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int energy = stack.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value();
        return Math.clamp(energy / SpectralWeaponLauncherItem.MAX_ENERGY, 0, 1) * 13;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float energy = stack.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value();
        return ColorUtil.lerpColor(energy / SpectralWeaponLauncherItem.MAX_ENERGY, BAR_COLOR, FULL_BAR_COLOR);
    }
}
