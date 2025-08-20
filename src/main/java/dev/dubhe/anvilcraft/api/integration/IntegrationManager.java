package dev.dubhe.anvilcraft.api.integration;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import lombok.extern.slf4j.Slf4j;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
import net.neoforged.fml.loading.modscan.ModAnnotation;
import net.neoforged.fml.loading.progress.ProgressMeter;
import net.neoforged.fml.loading.progress.StartupNotificationManager;
import net.neoforged.neoforgespi.language.ModFileScanData;

import java.lang.annotation.ElementType;
import java.util.List;
import java.util.Optional;

@Slf4j
public class IntegrationManager {
    private final Multimap<String, IntegrationInstance> instances = MultimapBuilder.hashKeys().hashSetValues().build();

    public static final String INTEGRATION_NAME = "L" + Integration.class.getName().replace(".", "/") + ";";

    @SuppressWarnings("UnstableApiUsage")
    public void compileContent() {
        ProgressMeter meter = StartupNotificationManager.addProgressBar("Load Integrations", LoadingModList.get().getModFiles().size());
        for (ModFileInfo modFile : LoadingModList.get().getModFiles()) {
            meter.increment();
            ModFileScanData scanData = modFile.getFile().getScanResult();
            for (ModFileScanData.AnnotationData annotation : scanData.getAnnotations()) {
                if (annotation.annotationType().getDescriptor().equals(INTEGRATION_NAME) && annotation.targetType() == ElementType.TYPE) {
                    String modid = (String) annotation.annotationData().get("value");
                    String version = (String) annotation.annotationData().get("version");
                    //noinspection unchecked
                    List<ModAnnotation.EnumHolder> typeHolders = ((List<ModAnnotation.EnumHolder>) annotation.annotationData().get("type"));
                    if (version == null) version = "*";
                    List<IntegrationType> type = List.of(IntegrationType.DEDICATED_SERVER, IntegrationType.CLIENT);
                    if (typeHolders != null) {
                        type = typeHolders.stream().map(
                            holder -> switch (holder.value()) {
                                case "DEDICATED_SERVER" -> IntegrationType.DEDICATED_SERVER;
                                case "CLIENT" -> IntegrationType.CLIENT;
                                case "DATA" -> IntegrationType.DATA;
                                default -> throw new IllegalArgumentException("Unknown integration type: " + holder.value());
                            }
                        ).toList();
                    }
                    log.info("Considering integration {} for {id:{}, version:{}}", annotation.memberName(), modid, version);
                    IntegrationInstance instance = new IntegrationInstance(
                        modid,
                        ModVersionRange.of(version),
                        annotation.memberName(),
                        type
                    );
                    this.instances.put(modid, instance);
                }
            }
        }
        StartupNotificationManager.popBar(meter);
    }

    @SuppressWarnings("DataFlowIssue")
    public void load(String modid, ModInfo info) {
        for (IntegrationInstance instance : instances.get(modid)) {
            if (FMLLoader.getDist().isDedicatedServer() && !instance.containsType(IntegrationType.DEDICATED_SERVER)) return;
            if (!instance.is(info)) continue;
            instance.newInstance();
            log.info("Loading integration {} for {}.", instance.instance(), modid);
            instance.invoke();
        }
    }

    @SuppressWarnings("DataFlowIssue")
    public void loadClient(String modid, ModInfo info) {
        for (IntegrationInstance instance : instances.get(modid)) {
            if (!instance.is(info)) continue;
            instance.newInstance();
            log.info("Loading client integration {} for {}.", instance.instance(), modid);
            instance.invokeClient();
        }
    }

    @SuppressWarnings("DataFlowIssue")
    public void loadData(String modid, ModInfo info) {
        for (IntegrationInstance instance : instances.get(modid)) {
            if (!instance.is(info)) continue;
            instance.newInstance();
            log.info("Loading data integration {} for {}.", instance.instance(), modid);
            instance.invokeData();
        }
    }

    public void loadAllIntegrations() {
        for (String key : instances.keySet()) {
            Optional<ModInfo> info = LoadingModList.get().getMods().stream().filter(it -> it.getModId().equals(key)).findFirst();
            info.ifPresent(modInfo -> load(key, modInfo));
        }
    }

    public void loadAllClientIntegrations() {
        for (String key : instances.keySet()) {
            Optional<ModInfo> info = LoadingModList.get().getMods().stream().filter(it -> it.getModId().equals(key)).findFirst();
            info.ifPresent(modInfo -> loadClient(key, modInfo));
        }
    }

    public void loadAllDataIntegrations() {
        for (String key : instances.keySet()) {
            Optional<ModInfo> info = LoadingModList.get().getMods().stream().filter(it -> it.getModId().equals(key)).findFirst();
            info.ifPresent(modInfo -> loadData(key, modInfo));
        }
    }
}
