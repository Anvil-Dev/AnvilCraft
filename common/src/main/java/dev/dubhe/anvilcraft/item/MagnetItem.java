package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.AnvilCraft;
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
    public @NotNull InteractionResultHolder<ItemStack> use(
            @NotNull Level level,
            @NotNull Player player,
            @NotNull InteractionHand usedHand
    ) {
        ItemStack item = player.getItemInHand(usedHand);
        double radius = AnvilCraft.config.magnetItemAttractsRadius;
        AABB aabb = new AABB(player.position().add(-radius, -radius, -radius), player.position()
                .add(radius, radius, radius));
        level.getEntities(EntityTypeTest.forClass(ItemEntity.class), aabb, Entity::isAlive)
                .forEach(e -> e.moveTo(player.position()));
        item.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(usedHand));
        return InteractionResultHolder.sidedSuccess(item, level.isClientSide());
    }
}
