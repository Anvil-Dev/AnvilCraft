package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.api.tooltip.FluidTankItemTooltip;
import dev.dubhe.anvilcraft.entity.FluidTankMinecartEntity;
import dev.dubhe.anvilcraft.init.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/// 放置与发射储罐矿车的物品
public class FluidTankMinecartItem extends Item {
    public FluidTankMinecartItem(Properties properties) {
        super(properties.stacksTo(1));
        DispenserBlock.registerBehavior(this, new DispenseBehavior());
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        TooltipContext context,
        TooltipDisplay display,
        Consumer<Component> builder,
        TooltipFlag tooltipFlag
    ) {
        super.appendHoverText(stack, context, display, builder, tooltipFlag);
        FluidTankItemTooltip.appendFixedTank(
            stack,
            context,
            builder,
            FluidTankMinecartEntity.CAPACITY
        );
    }

    private static @Nullable FluidTankMinecartEntity createMinecart(
        ServerLevel level,
        double x,
        double y,
        double z,
        ItemStack stack,
        @Nullable Player player
    ) {
        FluidTankMinecartEntity cart = AbstractMinecart.createMinecart(
            level,
            x,
            y,
            z,
            ModEntities.FLUID_TANK_MINECART.get(),
            EntitySpawnReason.DISPENSER,
            stack,
            player
        );
        if (cart != null) cart.loadTankFromItem(stack);
        return cart;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(BlockTags.RAILS)) return InteractionResult.FAIL;

        ItemStack stack = context.getItemInHand();
        if (level instanceof ServerLevel serverLevel) {
            double yoffset = railShape(state, serverLevel, pos).isSlope() ? 0.5D : 0.0D;
            FluidTankMinecartEntity cart = createMinecart(
                serverLevel,
                pos.getX() + 0.5D,
                pos.getY() + 0.0625D + yoffset,
                pos.getZ() + 0.5D,
                stack,
                context.getPlayer()
            );
            if (cart == null) return InteractionResult.FAIL;
            serverLevel.addFreshEntity(cart);
            serverLevel.gameEvent(
                GameEvent.ENTITY_PLACE,
                pos,
                GameEvent.Context.of(context.getPlayer(), serverLevel.getBlockState(pos.below()))
            );
        }
        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    private static RailShape railShape(BlockState state, Level level, BlockPos pos) {
        return state.getBlock() instanceof BaseRailBlock rail
            ? rail.getRailDirection(state, level, pos, null)
            : RailShape.NORTH_SOUTH;
    }

    private static final class DispenseBehavior extends DefaultDispenseItemBehavior {
        private final DefaultDispenseItemBehavior fallback = new DefaultDispenseItemBehavior();

        @Override
        public ItemStack execute(BlockSource source, ItemStack stack) {
            Direction direction = source.state().getValue(DispenserBlock.FACING);
            ServerLevel level = source.level();
            Vec3 center = source.center();
            double x = center.x + direction.getStepX() * 1.125D;
            double y = Math.floor(center.y) + direction.getStepY();
            double z = center.z + direction.getStepZ() * 1.125D;
            BlockPos pos = source.pos().relative(direction);
            BlockState state = level.getBlockState(pos);
            double yoffset;
            if (state.is(BlockTags.RAILS)) {
                yoffset = railShape(state, level, pos).isSlope() ? 0.6D : 0.1D;
            } else {
                if (!state.isAir()) return this.fallback.dispense(source, stack);
                BlockState below = level.getBlockState(pos.below());
                if (!below.is(BlockTags.RAILS)) return this.fallback.dispense(source, stack);
                yoffset = direction != Direction.DOWN && railShape(below, level, pos.below()).isSlope()
                    ? -0.4D
                    : -0.9D;
            }

            FluidTankMinecartEntity cart = createMinecart(level, x, y + yoffset, z, stack, null);
            if (cart == null) return stack;
            level.addFreshEntity(cart);
            stack.shrink(1);
            return stack;
        }

        @Override
        protected void playSound(BlockSource source) {
            source.level().levelEvent(1000, source.pos(), 0);
        }
    }
}
