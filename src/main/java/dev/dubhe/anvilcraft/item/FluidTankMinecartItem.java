package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.api.tooltip.FluidTankItemTooltip;
import dev.dubhe.anvilcraft.entity.FluidTankMinecartEntity;
import dev.dubhe.anvilcraft.init.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Item used to place and dispense a {@link FluidTankMinecartEntity}. */
public class FluidTankMinecartItem extends Item {
    public FluidTankMinecartItem(Properties properties) {
        super(properties
            .stacksTo(1)
            .component(DataComponents.BLOCK_ENTITY_DATA, FluidTankMinecartItem.defaultBlockEntityData()));
        DispenserBlock.registerBehavior(this, new DispenseBehavior());
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        TooltipContext context,
        List<Component> tooltipComponents,
        TooltipFlag tooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        FluidTankItemTooltip.appendFixedTank(
            stack,
            context,
            tooltipComponents,
            FluidTankMinecartEntity.CAPACITY
        );
    }

    private static FluidTankMinecartEntity createMinecart(
        ServerLevel level,
        double x,
        double y,
        double z,
        ItemStack stack,
        net.minecraft.world.entity.player.Player player
    ) {
        FluidTankMinecartEntity cart = new FluidTankMinecartEntity(
            ModEntities.FLUID_TANK_MINECART.get(),
            level,
            x,
            y,
            z
        );
        EntityType.<FluidTankMinecartEntity>createDefaultStackConfig(level, stack, player).accept(cart);
        cart.loadTankFromItem(stack);
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
            RailShape shape = state.getBlock() instanceof BaseRailBlock rail
                ? rail.getRailDirection(state, level, pos, null)
                : RailShape.NORTH_SOUTH;
            double yoffset = shape.isAscending() ? 0.5D : 0.0D;
            FluidTankMinecartEntity cart = createMinecart(
                serverLevel,
                pos.getX() + 0.5D,
                pos.getY() + 0.0625D + yoffset,
                pos.getZ() + 0.5D,
                stack,
                context.getPlayer()
            );
            serverLevel.addFreshEntity(cart);
            serverLevel.gameEvent(
                GameEvent.ENTITY_PLACE,
                pos,
                GameEvent.Context.of(context.getPlayer(), serverLevel.getBlockState(pos.below()))
            );
        }
        stack.shrink(1);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static final class DispenseBehavior extends DefaultDispenseItemBehavior {
        private final DefaultDispenseItemBehavior fallback = new DefaultDispenseItemBehavior();

        @Override
        protected ItemStack execute(BlockSource source, ItemStack stack) {
            Direction direction = source.state().getValue(DispenserBlock.FACING);
            ServerLevel level = source.level();
            Vec3 center = source.center();
            double x = center.x + direction.getStepX() * 1.125D;
            double y = Math.floor(center.y) + direction.getStepY();
            double z = center.z + direction.getStepZ() * 1.125D;
            BlockPos pos = source.pos().relative(direction);
            BlockState state = level.getBlockState(pos);
            RailShape shape = state.getBlock() instanceof BaseRailBlock rail
                ? rail.getRailDirection(state, level, pos, null)
                : RailShape.NORTH_SOUTH;
            double yoffset;
            if (state.is(BlockTags.RAILS)) {
                yoffset = shape.isAscending() ? 0.6D : 0.1D;
            } else {
                if (!state.isAir() || !level.getBlockState(pos.below()).is(BlockTags.RAILS)) {
                    return this.fallback.dispense(source, stack);
                }
                BlockState below = level.getBlockState(pos.below());
                RailShape belowShape = below.getBlock() instanceof BaseRailBlock rail
                    ? rail.getRailDirection(below, level, pos.below(), null)
                    : RailShape.NORTH_SOUTH;
                yoffset = direction != Direction.DOWN && belowShape.isAscending() ? -0.4D : -0.9D;
            }

            FluidTankMinecartEntity cart = createMinecart(level, x, y + yoffset, z, stack, null);
            level.addFreshEntity(cart);
            stack.shrink(1);
            return stack;
        }

        @Override
        protected void playSound(BlockSource source) {
            source.level().levelEvent(1000, source.pos(), 0);
        }
    }

    private static CustomData defaultBlockEntityData() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "anvilcraft:fluid_tank");
        return CustomData.of(tag);
    }
}
