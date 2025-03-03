package dev.dubhe.anvilcraft.recipe.transform;

import dev.dubhe.anvilcraft.init.ModBlocks;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.mojang.serialization.Codec;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public enum TransformOptions implements StringRepresentable {
    KEEP_INVENTORY("keepInventory", 900) {
        @Override
        public void accept(Entity oldEntity, Entity newEntity) {
            if (newEntity instanceof LivingEntity n && oldEntity instanceof LivingEntity o) {
                for (EquipmentSlot value : EquipmentSlot.values()) {
                    n.setItemSlot(value, o.getItemBySlot(value));
                }
                for (InteractionHand value : InteractionHand.values()) {
                    n.setItemInHand(value, o.getItemInHand(value));
                }
            }
        }
    },
    REPLACE_ANVIL("replaceAnvil", 1000) {
        @Override
        public void accept(Entity oldEntity, Entity newEntity) {
            if (newEntity instanceof LivingEntity n && oldEntity instanceof LivingEntity o) {
                for (InteractionHand value : InteractionHand.values()) {
                    ItemStack itemStack = o.getItemInHand(value);
                    if (itemStack.is(Items.ANVIL)
                        || itemStack.is(Items.CHIPPED_ANVIL)
                        || itemStack.is(Items.DAMAGED_ANVIL)) {
                        if (newEntity instanceof Giant) {
                            o.setItemInHand(
                                value, ModBlocks.GIANT_ANVIL.asItem().getDefaultInstance());
                        }
                        if (n instanceof Mob mob) {
                            mob.setDropChance(
                                value == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND,
                                1.0f);
                        }
                        if (o instanceof Mob mob) {
                            mob.setDropChance(
                                value == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND,
                                1.0f);
                        }
                    }
                }
            }
        }
    };

    public static final Codec<TransformOptions> CODEC = StringRepresentable.fromEnum(TransformOptions::values);
    private final String name;

    @Getter
    private final int priority;

    TransformOptions(String name, int priority) {
        this.name = name;
        this.priority = priority;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }

    public abstract void accept(Entity oldEntity, Entity newEntity);
}
