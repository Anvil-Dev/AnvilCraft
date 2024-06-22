package dev.dubhe.anvilcraft.integration.create;

import com.simibubi.create.content.fluids.tank.BoilerHeaters;
import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;

public class BoilerIntegration implements Integration {

    @Override
    public void apply() {
        BoilerHeaters.registerHeaterProvider(((level, pos, state) -> {
            if (state.is(ModBlocks.HEATER.get())) {
                return (l, p, s) -> {
                    try {
                        boolean value = s.getValue(HeaterBlock.OVERLOAD);
                        return !value ? 1 : -1;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return -1;
                    }
                };
            }
            return null;
        }));
    }
}
