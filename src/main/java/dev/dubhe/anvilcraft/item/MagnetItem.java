package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.item.UseMagnetEvent;
import dev.dubhe.anvilcraft.api.item.IChargerChargeable;
import dev.dubhe.anvilcraft.init.ModItems;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModLoader;

import org.jetbrains.annotations.NotNull;

public class MagnetItem extends Item implements IChargerChargeable {
    public MagnetItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
            @NotNull Level level, @NotNull Player player, @NotNull InteractionHand usedHand) {
        ItemStack item = player.getItemInHand(usedHand);
        double radius = AnvilCraft.config.magnetItemAttractsRadius;
        UseMagnetEvent event = new UseMagnetEvent(level, player, radius);
        ModLoader.postEvent(event);
        if (event.isCanceled()) return InteractionResultHolder.pass(item);
        radius = event.getAttractRadius();
        AABB aabb = new AABB(
                player.position().add(-radius, -radius, -radius),
                player.position().add(radius, radius, radius));
        level.getEntities(EntityTypeTest.forClass(ItemEntity.class), aabb, Entity::isAlive)
                .forEach(e -> e.moveTo(player.position()));
        item.hurtAndBreak(1, player, LivingEntity.getSlotForHand(usedHand));
        return InteractionResultHolder.sidedSuccess(item, level.isClientSide());
    }

    @Override
    public ItemStack charge(ItemStack input) {
        return ModItems.MAGNET.asStack(1);
    }
}
