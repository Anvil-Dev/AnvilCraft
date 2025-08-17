package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.recipe.anvil.InWorldRecipeManager;
import dev.dubhe.anvilcraft.util.mixin.recipe.InWorldRecipeManagerInjector;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MinecraftServer.class)
abstract class MinecraftServerMixin implements InWorldRecipeManagerInjector {
    @Unique
    private final InWorldRecipeManager anvilcraft$inWorldRecipeManager = new InWorldRecipeManager();

    @Override
    public InWorldRecipeManager anvilcraft$getInWorldRecipeManager() {
        return this.anvilcraft$inWorldRecipeManager;
    }
}
