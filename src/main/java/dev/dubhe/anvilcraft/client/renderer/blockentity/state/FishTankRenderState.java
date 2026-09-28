package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import com.mojang.datafixers.util.Pair;
import dev.dubhe.anvilcraft.api.rendering.BlockStateModelTessellateState;
import dev.dubhe.anvilcraft.client.renderer.blockentity.FishTankRenderHooks;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class FishTankRenderState extends FluidHandlerRenderState {
    private final List<Pair<ItemStack, ItemClusterRenderState>> stacks = new ArrayList<>();
    private final List<EntityRenderState> fishes = new ArrayList<>();
    private boolean ignited;
    private @Nullable BlockStateModelTessellateState fire;
    private long seed;
    private float ticks;
    private int itemTicks;
    private List<FishTankRenderHooks.AfterRender> afterRender = List.of();
}
