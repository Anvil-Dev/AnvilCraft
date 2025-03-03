package dev.dubhe.anvilcraft.api.integration;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import lombok.extern.slf4j.Slf4j;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.fml.loading.progress.ProgressMeter;
import net.neoforged.fml.loading.progress.StartupNotificationManager;
import net.neoforged.neoforgespi.language.ModFileScanData;

import java.lang.annotation.ElementType;

@Slf4j
public class IntegrationManager {

    public static final String INTEGRATION_NAME = "L" + Integration.class.getName().replace(".", "/") + ";";
    private final Multimap<String, IntegrationInstance> instances = MultimapBuilder.hashKeys().hashSetValues().build();

    public void compileContent() {
        ProgressMeter meter = StartupNotificationManager.addProgressBar("Load Integrations", LoadingModList.get().getModFiles().size());
        for (ModFileInfo modFile : LoadingModList.get().getModFiles()) {
            meter.increment();
            ModFileScanData scanData = modFile.getFile().getScanResult();
            for (ModFileScanData.AnnotationData annotation : scanData.getAnnotations()) {
                if (annotation.annotationType().getDescriptor().equals(INTEGRATION_NAME) && annotation.targetType() == ElementType.TYPE) {
                    String modid = (String) annotation.annotationData().get("value");
                    log.info("Considering integration {} for {}", annotation.memberName(), modid);
                    IntegrationInstance instance = new IntegrationInstance(
                        modid,
                        annotation.memberName()
                    );
                    this.instances.put(modid, instance);
                }
            }
        }
        StartupNotificationManager.popBar(meter);
    }

    public void load(String modid) {
        for (IntegrationInstance instance : instances.get(modid)) {
            instance.newInstance();
            log.info("Loading integration {} for {}.", instance.instance(), modid);
            instance.invoke();
        }
    }

    public void loadClient(String modid) {
        for (IntegrationInstance instance : instances.get(modid)) {
            instance.newInstance();
            log.info("Loading client integration {} for {}.", instance.instance(), modid);
            instance.invokeClient();
        }
    }

    public void loadAllIntegrations() {
        for (String key : instances.keys()) {
            if (LoadingModList.get().getMods().stream().anyMatch(it -> it.getModId().equals(key))) {
                load(key);
            }
        }
    }

    public void loadAllClientIntegrations() {
        for (String key : instances.keys()) {
            if (LoadingModList.get().getMods().stream().anyMatch(it -> it.getModId().equals(key))) {
                loadClient(key);
            }
        }
    }
}
