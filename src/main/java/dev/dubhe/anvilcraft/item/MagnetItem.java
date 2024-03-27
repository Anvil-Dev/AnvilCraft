package dev.dubhe.anvilcraft.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

public class MagnetItem extends Item {
    public MagnetItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, InteractionHand usedHand) {
        ItemStack item = player.getItemInHand(usedHand);
        AABB aabb = new AABB(player.position().add(-2.5, -2.5, -2.5), player.position().add(2.5, 2.5, 2.5));
        level.getEntities(EntityTypeTest.forClass(ItemEntity.class), aabb, Entity::isAlive).forEach(e -> e.moveTo(player.position()));
        item.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(usedHand));
        return InteractionResultHolder.sidedSuccess(item, level.isClientSide());
    }
}
