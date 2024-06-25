package dev.dubhe.anvilcraft.integration.create;

import com.simibubi.create.content.fluids.tank.BoilerHeaters;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.util.Integration;

@SuppressWarnings("unused")
public class BoilerIntegration implements Integration {
    @Override
    public void apply() {
        BoilerHeaters.registerHeaterProvider(((level, pos, state) -> {
            if (state.is(ModBlocks.HEATER.get())) {
                return (level1, blockPos, blockState) -> {
                    try {
                        return blockState.getValue(HeaterBlock.OVERLOAD) ? -1 : 1;
                    } catch (Exception exception) {
                        AnvilCraft.LOGGER.error(exception.getMessage(), exception);
                        return -1;
                    }
                };
            }
            return null;
        }));
    }
}
