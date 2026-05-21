package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

@Getter
@Setter
public class TeslaTowerRenderState extends BlockEntityRenderState {
    @Nullable
    private Vec3 start;
    @Nullable
    private Vec3 end;
    @Nullable
    private Vec3 camera;
}
