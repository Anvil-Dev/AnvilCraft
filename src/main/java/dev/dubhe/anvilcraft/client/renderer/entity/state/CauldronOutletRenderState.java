package dev.dubhe.anvilcraft.client.renderer.entity.state;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CauldronOutletRenderState extends EntityRenderState {
    private double dx;
    private double dy;
    private double dz;
    private final List<Quaternionf> rotation = new ArrayList<>();

    public void addRotation(Quaternionf rotation) {
        this.rotation.add(rotation);
    }
}
