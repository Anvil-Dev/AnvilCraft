package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

import java.util.EnumMap;
import java.util.Map;

@Getter
@Setter
public class PipeCheckValveRenderState extends BlockEntityRenderState {
    private BlockModelRenderState arm;
    private Map<Direction, Direction> flows = new EnumMap<>(Direction.class);
}
