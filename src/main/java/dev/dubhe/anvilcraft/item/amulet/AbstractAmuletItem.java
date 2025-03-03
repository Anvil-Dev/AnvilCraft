package dev.dubhe.anvilcraft.item.amulet;

import dev.dubhe.anvilcraft.init.ModDataAttachments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import static dev.dubhe.anvilcraft.init.ModDataAttachments.AMULET_COUNT;
import static dev.dubhe.anvilcraft.init.ModDataAttachments.AMULET_MAX;
import static dev.dubhe.anvilcraft.init.ModDataAttachments.DISCOUNT_RATE;
import static dev.dubhe.anvilcraft.init.ModDataAttachments.IMMUNE_TO_LIGHTNING;
import static dev.dubhe.anvilcraft.init.ModDataAttachments.NO_FALL_DAMAGE;
import static dev.dubhe.anvilcraft.init.ModDataAttachments.QUIETER;
import static dev.dubhe.anvilcraft.init.ModDataAttachments.SCARE_ENTITIES;
import static dev.dubhe.anvilcraft.init.ModDataAttachments.STEEL_HEAD;

public abstract class AbstractAmuletItem extends Item {

    public AbstractAmuletItem(Properties properties) {
        super(properties);
    }

    public static void resetWorkingAmuletData(@NotNull LivingEntity entity) {
        if (entity.hasData(AMULET_COUNT)) {
            entity.setData(AMULET_COUNT, 0);
        }
        if (entity.hasData(DISCOUNT_RATE)) {
            entity.setData(DISCOUNT_RATE, 0f);
        }
        if (entity.hasData(IMMUNE_TO_LIGHTNING)) {
            entity.setData(IMMUNE_TO_LIGHTNING, false);
        }
        if (entity.hasData(NO_FALL_DAMAGE)) {
            entity.setData(NO_FALL_DAMAGE, false);
        }
        if (entity.hasData(QUIETER)) {
            entity.setData(QUIETER, false);
        }
        if (entity.hasData(STEEL_HEAD)) {
            entity.setData(STEEL_HEAD, false);
        }
        if (entity.hasData(SCARE_ENTITIES)) {
            CompoundTag root = entity.getData(ModDataAttachments.SCARE_ENTITIES);
            root.putBoolean("skeletons", false);
            root.putBoolean("creepers", false);
            root.putBoolean("phantoms", false);
        }
    }

    abstract void UpdateAccessory(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected);

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!(stack.getItem() instanceof AbstractAmuletItem)) return;
        if (!(entity instanceof Player)) return;
        if (entity.getData(AMULET_COUNT) < entity.getData(AMULET_MAX)) {
            UpdateAccessory(stack, level, entity, slotId, isSelected);
            entity.setData(AMULET_COUNT, entity.getData(AMULET_COUNT) + 1);
        }
    }
}
