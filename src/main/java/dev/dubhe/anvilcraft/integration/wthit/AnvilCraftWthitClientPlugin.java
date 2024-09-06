package dev.dubhe.anvilcraft.integration.wthit;

import dev.dubhe.anvilcraft.integration.wthit.provider.PowerBlockProvider;
import mcp.mobius.waila.api.IClientRegistrar;
import mcp.mobius.waila.api.IWailaClientPlugin;
import net.minecraft.world.level.block.Block;

public class AnvilCraftWthitClientPlugin implements IWailaClientPlugin {
    @Override
    public void register(IClientRegistrar registrar) {
        registrar.body(PowerBlockProvider.INSTANCE, Block.class);
    }
}
