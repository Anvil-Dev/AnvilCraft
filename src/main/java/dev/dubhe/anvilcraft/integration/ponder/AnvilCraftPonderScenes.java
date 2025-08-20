package dev.dubhe.anvilcraft.integration.ponder;

import dev.dubhe.anvilcraft.integration.ponder.scene.AnvilScene;
import dev.dubhe.anvilcraft.integration.ponder.scene.redstone.BlockComparatorScene;
import dev.dubhe.anvilcraft.integration.ponder.scene.redstone.MagnetScene;
import dev.dubhe.anvilcraft.integration.ponder.scene.SpaceOvercompressorScene;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class AnvilCraftPonderScenes {
    public static void register(@NotNull PonderSceneRegistrationHelper<ResourceLocation> helper) {
        AnvilScene.register(helper);
        MagnetScene.register(helper);
        SpaceOvercompressorScene.register(helper);
        BlockComparatorScene.register(helper);
    }
}
