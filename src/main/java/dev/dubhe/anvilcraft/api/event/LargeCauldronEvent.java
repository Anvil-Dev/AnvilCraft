package dev.dubhe.anvilcraft.api.event;

import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 大型炼药锅扩展点。服务器事件仅在服务器线程发布；{@link UseItem} 可能在两侧调用。
 */
@Getter
public class LargeCauldronEvent extends Event {
    private final Level level;
    private final BlockPos pos;

    public LargeCauldronEvent(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    @Getter
    public static class ServerTick extends LargeCauldronEvent {
        private final ServerLevel serverLevel;
        private final LargeCauldronBlockEntity cauldron;

        public ServerTick(ServerLevel level, LargeCauldronBlockEntity cauldron) {
            super(level, cauldron.getBlockPos());
            this.serverLevel = level;
            this.cauldron = cauldron;
        }
    }

    @Getter
    public static class EntityInside extends LargeCauldronEvent {
        private final BlockState state;
        private final Entity entity;

        public EntityInside(Level level, BlockPos pos, BlockState state, Entity entity) {
            super(level, pos);
            this.state = state;
            this.entity = entity;
        }
    }

    @Getter
    @Setter
    public static class UseItem extends LargeCauldronEvent implements ICancellableEvent {
        private final BlockState state;
        private final Player player;
        private final InteractionHand hand;
        private final BlockHitResult hit;
        private final ItemStack stack;
        private ItemInteractionResult result = ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        public UseItem(
            Level level,
            BlockPos pos,
            BlockState state,
            Player player,
            InteractionHand hand,
            BlockHitResult hit,
            ItemStack stack
        ) {
            super(level, pos);
            this.state = state;
            this.player = player;
            this.hand = hand;
            this.hit = hit;
            this.stack = stack;
        }
    }

    @Getter
    @Setter
    public static class GiantAnvilImpact extends LargeCauldronEvent {
        private final AnvilEvent.OnLand landing;
        private final LargeCauldronBlockEntity cauldron;
        private BlockState landedAnvilState;

        public GiantAnvilImpact(
            Level level,
            AnvilEvent.OnLand landing,
            LargeCauldronBlockEntity cauldron,
            BlockState landedAnvilState
        ) {
            super(level, landing.getPos());
            this.landing = landing;
            this.cauldron = cauldron;
            this.landedAnvilState = landedAnvilState;
        }
    }

    @Getter
    @Setter
    public static class FluidDamage extends LargeCauldronEvent {
        private final LargeCauldronBlockEntity cauldron;
        private final FluidStack fluid;
        private float damage;

        public FluidDamage(LargeCauldronBlockEntity cauldron, FluidStack fluid, float damage) {
            super(cauldron.getLevel(), cauldron.getBlockPos());
            this.cauldron = cauldron;
            this.fluid = fluid;
            this.damage = damage;
        }
    }

    @Getter
    @Setter
    public static class MixingOutput extends LargeCauldronEvent {
        private final LargeCauldronBlockEntity cauldron;
        private ItemStack result;

        public MixingOutput(LargeCauldronBlockEntity cauldron, ItemStack result) {
            super(cauldron.getLevel(), cauldron.getBlockPos());
            this.cauldron = cauldron;
            this.result = result;
        }
    }
}
