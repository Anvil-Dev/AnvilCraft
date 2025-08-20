package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.UseMagnetEvent;
import dev.dubhe.anvilcraft.api.item.IChargerChargeable;
import dev.dubhe.anvilcraft.entity.MagnetizedNodeEntity;
import dev.dubhe.anvilcraft.init.ModEntities;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModLoader;
import org.jetbrains.annotations.NotNull;

public class MagnetItem extends Item implements IChargerChargeable {
    public MagnetItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
        @NotNull Level level,
        @NotNull Player player,
        @NotNull InteractionHand usedHand
    ) {
        if (player.isShiftKeyDown()) return InteractionResultHolder.pass(player.getItemInHand(usedHand));
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
        player.getCooldowns().addCooldown(this, 5);
        return InteractionResultHolder.sidedSuccess(item, level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null) return InteractionResult.PASS;
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        BlockPos pos = context.getClickedPos();
        BlockState blockState = level.getBlockState(pos);
        if (blockState.isAir()) return InteractionResult.PASS;
        double maxY = blockState.getCollisionShape(level, pos).max(Direction.Axis.Y, 0.5, 0.5);
        if (!blockState.getBlock().properties().hasCollision && maxY == 0) return InteractionResult.PASS;
        for (MagnetizedNodeEntity entity : level.getEntities(ModEntities.MAGNETIZED_NODE.get(), new AABB(pos).setMaxY(pos.getY() + 1.1), EntitySelector.NO_SPECTATORS)) {
            if (entity.blockPos.equals(pos)) return InteractionResult.PASS;
        }
        Vec3 nodePos = pos.getBottomCenter().add(0, maxY, 0);
        MagnetizedNodeEntity magnetizedNodeEntity = new MagnetizedNodeEntity(level, nodePos, pos);
        level.addFreshEntity(magnetizedNodeEntity);
        player.getCooldowns().addCooldown(this, 5);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(ModItems.MAGNET_INGOT);
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 1;
    }

    @Override
    public ItemStack charge(ItemStack input) {
        return ModItems.MAGNET.asStack(1);
    }
}
