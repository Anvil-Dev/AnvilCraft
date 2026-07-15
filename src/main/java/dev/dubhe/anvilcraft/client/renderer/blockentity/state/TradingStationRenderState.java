package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.core.Direction;

import java.util.List;

@Getter
@Setter
public class TradingStationRenderState extends BlockEntityRenderState {
    private List<ItemClusterRenderState> items = List.of();
    private Direction facing = Direction.NORTH;
    private float rotation;
}
