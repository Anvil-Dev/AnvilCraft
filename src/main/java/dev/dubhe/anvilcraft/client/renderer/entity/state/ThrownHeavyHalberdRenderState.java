package dev.dubhe.anvilcraft.client.renderer.entity.state;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ThrownHeavyHalberdRenderState extends EntityRenderState {
    private final List<Quaternionf> rotation = new ArrayList<>();
    private Identifier texture;
    private boolean isFoil;
}
