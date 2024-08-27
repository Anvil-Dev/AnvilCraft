package dev.dubhe.anvilcraft.integration.wthit;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.wthit.provider.PowerBlockProvider;
import mcp.mobius.waila.api.ICommonRegistrar;
import mcp.mobius.waila.api.IWailaCommonPlugin;
import net.minecraft.world.level.block.entity.BlockEntity;

public class AnvilCraftWthitCommonPlugin implements IWailaCommonPlugin {
    @Override
    public void register(ICommonRegistrar registrar) {
        registrar.blockData(PowerBlockProvider.INSTANCE, BlockEntity.class);
        registrar.localConfig(AnvilCraft.of("power_provider"), true);
        registrar.localConfig(AnvilCraft.of("warning_percent"), 0.75);
    }
}
