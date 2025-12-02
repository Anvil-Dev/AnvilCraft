package dev.dubhe.anvilcraft.integration.ponder;

import com.tterrag.registrate.providers.ProviderType;
import dev.anvilcraft.lib.integration.Integration;
import dev.anvilcraft.lib.integration.IntegrationType;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.ponder.data.PonderLangHandler;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.resources.ResourceLocation;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;

@SuppressWarnings("unused")
@Integration(value = "ponder", type = {IntegrationType.CLIENT, IntegrationType.DATA})
public class AnvilCraftPonderPlugin implements PonderPlugin {
    /**
     * Get the ModID of the mod that added this plugin
     *
     * @return the ModID of the mod that added this plugin
     */
    @Override
    public String getModId() {
        return AnvilCraft.MOD_ID;
    }

    /**
     * Register all the Ponder Scenes added by your Mod
     */
    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        AnvilCraftPonderScenes.register(helper);
    }

    /**
     * Register all the Ponder Tags added by your Mod
     */
    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        AnvilCraftPonderTags.register(helper);
    }

    public void apply() {
        PonderIndex.addPlugin(new AnvilCraftPonderPlugin());
        REGISTRATE.addDataGenerator(ProviderType.LANG, PonderLangHandler::init);
    }
}
