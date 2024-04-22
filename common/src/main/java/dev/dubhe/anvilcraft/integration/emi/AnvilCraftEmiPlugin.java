package dev.dubhe.anvilcraft.integration.emi;

import dev.dubhe.anvilcraft.integration.emi.stack.BlockStateEmiStack;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

@EmiEntrypoint
public class AnvilCraftEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        for (Block block : BuiltInRegistries.BLOCK) {
            EmiStack stack = new BlockStateEmiStack(block.defaultBlockState());
            registry.addEmiStack(stack);
        }
    }
}
