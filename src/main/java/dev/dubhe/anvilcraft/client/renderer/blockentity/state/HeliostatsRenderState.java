package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class HeliostatsRenderState extends BlockEntityRenderState {
    private BlockModelRenderState head;
    private final List<Quaternionf> rotation = new ArrayList<>();

    public void addRotation(Quaternionf rotation) {
        this.rotation.add(rotation);
    }
}
