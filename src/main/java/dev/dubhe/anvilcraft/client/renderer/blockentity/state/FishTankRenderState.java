package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import com.mojang.datafixers.util.Pair;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class FishTankRenderState extends FluidHandlerRenderState {
    private final List<Pair<ItemStack, ItemClusterRenderState>> stacks = new ArrayList<>();
    private boolean ignited;
    private BlockModelRenderState fire;
    private long seed;
}
