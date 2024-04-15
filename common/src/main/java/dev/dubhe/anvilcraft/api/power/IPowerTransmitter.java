package dev.dubhe.anvilcraft.api.power;

import dev.dubhe.anvilcraft.AnvilCraft;
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
        return Shapes.box(-range,  -range,  -range, range, range, range);
    }

    @Override
    default @NotNull PowerComponentType getComponentType() {
        return PowerComponentType.TRANSMITTER;
    }
}
