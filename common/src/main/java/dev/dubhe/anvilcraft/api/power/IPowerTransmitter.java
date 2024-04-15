package dev.dubhe.anvilcraft.api.power;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

/**
 * 电力中继器
 */
public interface IPowerTransmitter extends IPowerComponent {
    @Override
    default VoxelShape getRange() {
        int range = AnvilCraft.config.powerTransmitterRange;
        Vec3 vec3 = getPos().getCenter();
        return Shapes.box(
            vec3.x - range, vec3.y - range, vec3.z - range,
            vec3.x + range, vec3.y + range, vec3.z + range
        );
    }

    @Override
    default @NotNull PowerComponentType getComponentType() {
        return PowerComponentType.TRANSMITTER;
    }
}
