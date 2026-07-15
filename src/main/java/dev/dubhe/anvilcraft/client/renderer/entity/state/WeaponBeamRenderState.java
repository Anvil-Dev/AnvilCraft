package dev.dubhe.anvilcraft.client.renderer.entity.state;

import dev.dubhe.anvilcraft.client.renderer.blockentity.state.LaserRenderState;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.Vec3;

@Getter
@Setter
public class WeaponBeamRenderState extends EntityRenderState {
    private boolean visible;
    private int style;
    private Vec3 origin = Vec3.ZERO;
    private Vec3 originOffset = Vec3.ZERO;
    private Vec3 endOffset = Vec3.ZERO;
    private LaserRenderState laser;
    private boolean compensateViewBob;
}
