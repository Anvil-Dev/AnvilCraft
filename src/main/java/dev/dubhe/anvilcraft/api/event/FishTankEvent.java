package dev.dubhe.anvilcraft.api.event;

import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 鱼缸扩展点。{@link ServerTick} 在本体熔岩翻新逻辑之前发布，因此非熔岩内容也可被处理。
 */
@Getter
public class FishTankEvent extends Event {
    private final Level level;
    private final BlockPos pos;
    private final FishTankBlockEntity tank;

    public FishTankEvent(Level level, BlockPos pos, FishTankBlockEntity tank) {
        this.level = level;
        this.pos = pos;
        this.tank = tank;
    }

    @Getter
    public static class ServerTick extends FishTankEvent {
        private final ServerLevel serverLevel;

        public ServerTick(ServerLevel level, BlockPos pos, FishTankBlockEntity tank) {
            super(level, pos, tank);
            this.serverLevel = level;
        }
    }

    @Getter
    public static class EntityInside extends FishTankEvent {
        private final BlockState state;
        private final Entity entity;

        public EntityInside(Level level, BlockPos pos, BlockState state, FishTankBlockEntity tank, Entity entity) {
            super(level, pos, tank);
            this.state = state;
            this.entity = entity;
        }
    }

    @Getter
    @Setter
    public static class FluidDamage extends FishTankEvent {
        private final FluidStack fluid;
        private float damage;

        public FluidDamage(Level level, BlockPos pos, FishTankBlockEntity tank, FluidStack fluid, float damage) {
            super(level, pos, tank);
            this.fluid = fluid;
            this.damage = damage;
        }
    }
}
