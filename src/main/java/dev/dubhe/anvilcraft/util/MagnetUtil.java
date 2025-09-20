package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.UseMagnetEvent;
import dev.dubhe.anvilcraft.block.MagnetBlock;
import dev.dubhe.anvilcraft.entity.MagnetizedNodeEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.ExperienceOrb;
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

public abstract class MagnetUtil {
    public static boolean hasMagnetism(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos.above());
        return (state.is(ModBlockTags.MAGNET) || state.getBlock() instanceof MagnetBlock) && state.hasProperty(MagnetBlock.LIT) && !state.getValue(
            MagnetBlock.LIT);
    }

    public static InteractionResult placeMagnetizedNode(Item item, UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null) return InteractionResult.PASS;
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        BlockPos pos = context.getClickedPos();
        BlockState blockState = level.getBlockState(pos);
        if (blockState.isAir()) return InteractionResult.PASS;
        double maxY = blockState.getCollisionShape(level, pos).max(Direction.Axis.Y, 0.5, 0.5);
        if (!blockState.getBlock().properties().hasCollision && maxY == 0) return InteractionResult.PASS;
        for (MagnetizedNodeEntity entity : level.getEntities(
            ModEntities.MAGNETIZED_NODE.get(),
            new AABB(pos).setMaxY(pos.getY() + 1.1),
            EntitySelector.NO_SPECTATORS
        )) {
            if (entity.blockPos.equals(pos)) {
                entity.discard();
                player.getCooldowns().addCooldown(item, 5);
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }
        Vec3 nodePos = pos.getBottomCenter().add(0, maxY, 0);
        MagnetizedNodeEntity magnetizedNodeEntity = new MagnetizedNodeEntity(level, nodePos, pos);
        level.addFreshEntity(magnetizedNodeEntity);
        player.getCooldowns().addCooldown(item, 5);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    public static InteractionResultHolder<ItemStack> magnetizeItems(
        Item item,
        Level level,
        Player player,
        InteractionHand usedHand
    ) {
        if (player.isShiftKeyDown()) return InteractionResultHolder.pass(player.getItemInHand(usedHand));
        ItemStack itemStack = player.getItemInHand(usedHand);
        double radius = AnvilCraft.CONFIG.magnetItemAttractsRadius;
        UseMagnetEvent event = new UseMagnetEvent(level, player, radius);
        ModLoader.postEvent(event);
        if (event.isCanceled()) return InteractionResultHolder.pass(itemStack);
        radius = event.getAttractRadius();
        AABB aabb = new AABB(player.position().add(-radius, -radius, -radius), player.position().add(radius, radius, radius));
        level.getEntities(EntityTypeTest.forClass(ItemEntity.class), aabb, Entity::isAlive).forEach(e -> e.moveTo(player.position()));
        int totalXp = level.getEntities(EntityTypeTest.forClass(ExperienceOrb.class), aabb, Entity::isAlive).stream().mapToInt(e -> {
            CompoundTag c = new CompoundTag(); // AT (make e.count public) not working for idea, idk why. use CompoundTag yet.
            e.addAdditionalSaveData(c);
            e.discard();
            return Math.max(c.getShort("Value") * c.getInt("Count"), 1);
        }).sum();
        if (totalXp > 0 && level instanceof ServerLevel serverLevel) ExperienceOrb.award(serverLevel, player.position(), totalXp);
        itemStack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(usedHand));
        player.getCooldowns().addCooldown(item, 5);
        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }
}
