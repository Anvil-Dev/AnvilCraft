package dev.dubhe.anvilcraft.fabric;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.event.fabric.ModFabricEventsListener;
import dev.dubhe.anvilcraft.init.fabric.ModRecipeTypesFabric;
import dev.dubhe.anvilcraft.init.fabric.ModVillagers;
import dev.dubhe.anvilcraft.util.AnvilCraftCustomInfo;
import dev.dubhe.anvilcraft.util.SystemOutToLog4jDebug;
import dev.dubhe.anvilcraft.util.fabric.ModCustomTrades;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AnvilCraftFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        AnvilCraft.init();
        ModVillagers.register();
        ModCustomTrades.registerCustomTrades();
        ModRecipeTypesFabric.register();
        ModFabricEventsListener.init();
        // System.out debug init (only fabric)
        System.setOut(new SystemOutToLog4jDebug("STDOUT", System.out));
        System.setErr(new SystemOutToLog4jDebug("STDERR", System.err));

        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            CustomValue anvilcraft = mod.getMetadata().getCustomValue("anvilcraft");
            if (anvilcraft == null || anvilcraft.getType() != CustomValue.CvType.OBJECT) continue;
            CustomValue.CvObject object = anvilcraft.getAsObject();
            if (object.containsKey("integrations")) {
                CustomValue integrations = object.get("integrations");
                if (integrations.getType() == CustomValue.CvType.OBJECT) {
                    AnvilCraftFabric.loadIntegrations(integrations.getAsObject());
                }
            }
        }
    }

    private static void loadIntegrations(@NotNull CustomValue.CvObject integrations) {
        for (Map.Entry<String, CustomValue> entry : integrations) {
            String modid = entry.getKey();
            CustomValue value = entry.getValue();
            List<String> classes = Collections.synchronizedList(new ArrayList<>());
            if (value.getType() == CustomValue.CvType.ARRAY) {
                for (CustomValue listValue : value.getAsArray()) {
                    if (listValue.getType() != CustomValue.CvType.STRING) continue;
                    classes.add(listValue.getAsString());
                }
            } else if (value.getType() == CustomValue.CvType.STRING) {
                classes.add(value.getAsString());
            }
            AnvilCraftCustomInfo.INTEGRATIONS.put(modid, classes);
        }
        AnvilCraftCustomInfo.apply();
    }
}