package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorRegistry;
import dev.dubhe.anvilcraft.block.entity.megastructure.AcceleratorHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.ColliderHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.DysonSphereHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.EcoStationHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.ExcavatorHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.ExtractorHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.GiantExtractorHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.IMegastructureHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.MagnetarCoilHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.MatterDecompressorHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.PenroseSphereHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.TempleHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.WormholeStabilizerHandler;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class CfaMegastructureManager {
    private int activeMegastructureIndex = -1;

    private final Map<String, IMegastructureHandler> handlers = new LinkedHashMap<>();
    @Getter
    private final AcceleratorHandler acceleratorHandler;

    public CfaMegastructureManager() {
        registerHandler(new ExcavatorHandler());
        registerHandler(new ExtractorHandler());
        registerHandler(new GiantExtractorHandler());
        registerHandler(new ColliderHandler());
        registerHandler(new DysonSphereHandler("dyson_sphere_small"));
        registerHandler(new DysonSphereHandler("dyson_sphere_large"));
        registerHandler(new MagnetarCoilHandler());
        registerHandler(new PenroseSphereHandler());
        registerHandler(new MatterDecompressorHandler());
        registerHandler(new WormholeStabilizerHandler());
        registerHandler(new EcoStationHandler());
        registerHandler(new TempleHandler());
        this.acceleratorHandler = new AcceleratorHandler();
    }

    private void registerHandler(IMegastructureHandler handler) {
        handlers.put(handler.name(), handler);
    }

    public int getActiveIndex() {
        return activeMegastructureIndex;
    }

    @Nullable
    public IMegastructureHandler getActiveHandler(CelestialForgingAnvilBlockEntity be) {
        CelestialRefactorOption option = getActiveOption(be);
        if (option == null) return null;
        return handlers.get(option.megastructure());
    }

    @Nullable
    public CelestialRefactorOption getActiveOption(CelestialForgingAnvilBlockEntity be) {
        if (activeMegastructureIndex < 0 || be.getCelestialBodyData() == null) return null;
        var options = CelestialRefactorRegistry.getOptions(
            be.getCelestialBodyData(),
            be.isAmplify(),
            be.getPlanetaryResourceSet()
        );
        if (activeMegastructureIndex >= options.size()) return null;
        return options.get(activeMegastructureIndex);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends IMegastructureHandler> T findHandler(Class<T> type) {
        for (var handler : handlers.values()) {
            if (type.isInstance(handler)) return (T) handler;
        }
        return null;
    }

    public WormholeStabilizerHandler getWormholeHandler() {
        return findHandler(WormholeStabilizerHandler.class);
    }

    // === Tick ===

    public void serverTick(CelestialForgingAnvilBlockEntity be) {
        if (activeMegastructureIndex >= 0) {
            IMegastructureHandler active = getActiveHandler(be);
            if (active != null) {
                active.serverTick(be);
            }
            syncLaserRequirements(be);
        }
        if (acceleratorHandler.isActive()) {
            acceleratorHandler.serverTick(be);
        }
    }

    // === Laser requirements ===

    public void syncLaserRequirements(CelestialForgingAnvilBlockEntity be) {
        IMegastructureHandler active = getActiveHandler(be);
        if (active == null) {
            clearAllLaserRequirements(be);
            return;
        }
        var lasers = CfaInterfaceScanner.findLaserInterfaces(be.getLevel(), be.getBlockPos());
        String name = active.name();
        if ("planet_excavator".equals(name)) {
            for (var laser : lasers) {
                laser.setLaserRequirement(16, false);
            }
        } else if ("matter_decompressor".equals(name)) {
            for (var laser : lasers) {
                laser.setLaserRequirement(1, true);
            }
        } else {
            clearAllLaserRequirements(be);
        }
    }

    private void clearAllLaserRequirements(CelestialForgingAnvilBlockEntity be) {
        var lasers = CfaInterfaceScanner.findLaserInterfaces(be.getLevel(), be.getBlockPos());
        for (var laser : lasers) {
            laser.setLaserRequirement(0, false);
        }
    }

    // === Build / Clear ===

    public void buildMegastructure(int optionIndex, CelestialForgingAnvilBlockEntity be) {
        var body = be.getCelestialBodyData();
        if (body == null) return;
        var options = be.getClientVisibleOptions();
        if (optionIndex < 0 || optionIndex >= options.size()) return;
        CelestialRefactorOption option = options.get(optionIndex);
        if ("stellar_evolution_accelerator".equals(option.megastructure())) {
            acceleratorHandler.onBuild(be);
            return;
        }
        if (activeMegastructureIndex >= 0) return;
        this.activeMegastructureIndex = optionIndex;
        IMegastructureHandler handler = handlers.get(option.megastructure());
        if (handler != null) {
            handler.onBuild(be);
        }
    }

    public void clearMegastructure(CelestialForgingAnvilBlockEntity be) {
        if (activeMegastructureIndex >= 0) {
            IMegastructureHandler handler = getActiveHandler(be);
            if (handler != null) {
                handler.onClear(be);
            }
        }
        this.activeMegastructureIndex = -1;
        clearAllLaserRequirements(be);
    }

    public void clearAllMegastructures(CelestialForgingAnvilBlockEntity be) {
        acceleratorHandler.onClear(be);
        clearMegastructure(be);
    }

    // === NBT ===

    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("activeMegastructure", activeMegastructureIndex);
        for (var handler : handlers.values()) {
            handler.saveAdditional(tag, registries);
        }
        acceleratorHandler.saveAdditional(tag, registries);
    }

    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        this.activeMegastructureIndex = tag.contains("activeMegastructure")
            ? tag.getInt("activeMegastructure") : -1;
        for (var handler : handlers.values()) {
            handler.loadAdditional(tag, registries);
        }
        acceleratorHandler.loadAdditional(tag, registries);
    }

    public void writeUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("activeMegastructure", activeMegastructureIndex);
        for (var handler : handlers.values()) {
            handler.writeUpdateTag(tag, registries);
        }
        acceleratorHandler.writeUpdateTag(tag, registries);
    }

    public void readUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        this.activeMegastructureIndex = tag.contains("activeMegastructure")
            ? tag.getInt("activeMegastructure") : -1;
        for (var handler : handlers.values()) {
            handler.readUpdateTag(tag, registries);
        }
        acceleratorHandler.readUpdateTag(tag, registries);
    }

    // === Power ===

    public int getInputPower(CelestialForgingAnvilBlockEntity be) {
        IMegastructureHandler handler = getActiveHandler(be);
        return handler != null ? handler.getInputPower(be) : 0;
    }

    public int getOutputPower(CelestialForgingAnvilBlockEntity be) {
        IMegastructureHandler handler = getActiveHandler(be);
        return handler != null ? handler.getOutputPower(be) : 0;
    }

    public boolean isInfinitePower(CelestialForgingAnvilBlockEntity be) {
        if (!be.isAcceleratorActive() || be.getAcceleratorStage() != 1 || !be.isAmplifierPresent()) {
            return false;
        }
        CelestialRefactorOption option = getActiveOption(be);
        return option != null && option.megastructure().contains("dyson_sphere");
    }

    public PowerComponentType getComponentType(CelestialForgingAnvilBlockEntity be) {
        IMegastructureHandler handler = getActiveHandler(be);
        if (handler != null) {
            return handler.getComponentType();
        }
        return PowerComponentType.CONSUMER;
    }

    public void gridTick(CelestialForgingAnvilBlockEntity be) {
        IMegastructureHandler handler = getActiveHandler(be);
        if (handler != null) {
            handler.gridTick(be);
        }
        acceleratorHandler.gridTick(be);
    }
}
