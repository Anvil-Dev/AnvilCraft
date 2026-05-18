package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.phys.Vec3;

@Getter
@Setter
public class TeslaTowerRenderState extends BlockEntityRenderState {
    private Vec3 start;
    private Vec3 end;
    private Vec3 camera;
}
