package dev.dubhe.anvilcraft.integration.curios.data;

import dev.anvilcraft.lib.v2.integration.Integration;
import dev.anvilcraft.lib.v2.integration.IntegrationHook;
import dev.anvilcraft.lib.v2.integration.IntegrationType;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@Integration(value = "curios", type = IntegrationType.CLIENT_DATA)
public class CuriosData {
    public void applyClientData() {
        GatherDataEvent event = IntegrationHook.getEvent();
        
        event.createProvider(ModCuriosProvider::new);
    }
}
