package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import dev.dubhe.anvilcraft.block.entity.fluid.ControlValveBlockEntity;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

@Getter
@Setter
public class ControlValveRenderState extends BlockEntityRenderState {
    private @Nullable BlockModelRenderState handwheel;
    private Direction facing = Direction.NORTH;
    private Direction.Axis axis = Direction.Axis.Y;
    private int maxRate = ControlValveBlockEntity.MAX_RATE;
    private @Nullable FluidResource filterResource;
}
