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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class CfaMegastructureManager {
    private int activeMegastructureIndex = -1;

    private final Map<String, IMegastructureHandler> handlers = new LinkedHashMap<>();
    @Getter
    private final AcceleratorHandler acceleratorHandler;

    public CfaMegastructureManager() {
        this.registerHandler(new ExcavatorHandler());
        this.registerHandler(new ExtractorHandler());
        this.registerHandler(new GiantExtractorHandler());
        this.registerHandler(new ColliderHandler());
        this.registerHandler(new DysonSphereHandler("dyson_sphere_small"));
        this.registerHandler(new DysonSphereHandler("dyson_sphere_large"));
        this.registerHandler(new MagnetarCoilHandler());
        this. registerHandler(new PenroseSphereHandler());
        this.registerHandler(new MatterDecompressorHandler());
        this.registerHandler(new WormholeStabilizerHandler());
        this.registerHandler(new EcoStationHandler());
        this.registerHandler(new TempleHandler());
        this.acceleratorHandler = new AcceleratorHandler();
    }

    private void registerHandler(IMegastructureHandler handler) {
        this.handlers.put(handler.name(), handler);
    }

    public int getActiveIndex() {
        return this.activeMegastructureIndex;
    }

    @Nullable
    public IMegastructureHandler getActiveHandler(CelestialForgingAnvilBlockEntity be) {
        CelestialRefactorOption option = this.getActiveOption(be);
        if (option == null) return null;
        return this.handlers.get(option.megastructure());
    }

    @Nullable
    public CelestialRefactorOption getActiveOption(CelestialForgingAnvilBlockEntity be) {
        if (this.activeMegastructureIndex < 0 || be.getCelestialBodyData() == null) return null;
        var options = CelestialRefactorRegistry.getOptions(
            be.getCelestialBodyData(),
            be.isAmplify(),
            be.getPlanetaryResourceSet()
        );
        if (this.activeMegastructureIndex >= options.size()) return null;
        return options.get(this.activeMegastructureIndex);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends IMegastructureHandler> T findHandler(Class<T> type) {
        for (var handler : this.handlers.values()) {
            if (type.isInstance(handler)) return (T) handler;
        }
        return null;
    }

    public WormholeStabilizerHandler getWormholeHandler() {
        return this.findHandler(WormholeStabilizerHandler.class);
    }

    // === Tick ===

    public void serverTick(CelestialForgingAnvilBlockEntity be) {
        if (this.activeMegastructureIndex >= 0) {
            IMegastructureHandler active = this.getActiveHandler(be);
            if (active != null) {
                active.serverTick(be);
            }
            this.syncLaserRequirements(be);
        }
        if (this.acceleratorHandler.isActive()) {
            this.acceleratorHandler.serverTick(be);
        }
    }

    // === Laser requirements ===

    public void syncLaserRequirements(CelestialForgingAnvilBlockEntity be) {
        IMegastructureHandler active = this.getActiveHandler(be);
        if (active == null) {
            this.clearAllLaserRequirements(be);
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
            this.clearAllLaserRequirements(be);
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
            this.acceleratorHandler.onBuild(be);
            return;
        }
        if (this.activeMegastructureIndex >= 0) return;
        this.activeMegastructureIndex = optionIndex;
        IMegastructureHandler handler = this.handlers.get(option.megastructure());
        if (handler != null) {
            handler.onBuild(be);
        }
    }

    public void clearMegastructure(CelestialForgingAnvilBlockEntity be) {
        if (this.activeMegastructureIndex >= 0) {
            IMegastructureHandler handler = this.getActiveHandler(be);
            if (handler != null) {
                handler.onClear(be);
            }
        }
        this.activeMegastructureIndex = -1;
        this.clearAllLaserRequirements(be);
    }

    public void clearAllMegastructures(CelestialForgingAnvilBlockEntity be) {
        this.acceleratorHandler.onClear(be);
        this.clearMegastructure(be);
    }

    // === Persistence: disk (ValueOutput/ValueInput) ===

    public void saveAdditional(ValueOutput output) {
        output.putInt("activeMegastructure", this.activeMegastructureIndex);
        for (var handler : this.handlers.values()) {
            handler.saveAdditional(output);
        }
        this.acceleratorHandler.saveAdditional(output);
    }

    public void loadAdditional(ValueInput input) {
        this.activeMegastructureIndex = input.getIntOr("activeMegastructure", -1);
        for (var handler : this.handlers.values()) {
            handler.loadAdditional(input);
        }
        this.acceleratorHandler.loadAdditional(input);
    }

    // === Network sync (still CompoundTag-based) ===

    public void writeUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("activeMegastructure", this.activeMegastructureIndex);
        for (var handler : this.handlers.values()) {
            handler.writeUpdateTag(tag, registries);
        }
        this.acceleratorHandler.writeUpdateTag(tag, registries);
    }

    public void readUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        this.activeMegastructureIndex = tag.getIntOr("activeMegastructure", -1);
        for (var handler : this.handlers.values()) {
            handler.readUpdateTag(tag, registries);
        }
        this.acceleratorHandler.readUpdateTag(tag, registries);
    }

    // === Power ===

    public int getInputPower(CelestialForgingAnvilBlockEntity be) {
        IMegastructureHandler handler = this.getActiveHandler(be);
        return handler != null ? handler.getInputPower(be) : 0;
    }

    public int getOutputPower(CelestialForgingAnvilBlockEntity be) {
        IMegastructureHandler handler = this.getActiveHandler(be);
        return handler != null ? handler.getOutputPower(be) : 0;
    }

    public PowerComponentType getComponentType(CelestialForgingAnvilBlockEntity be) {
        IMegastructureHandler handler = this.getActiveHandler(be);
        if (handler != null) return handler.getComponentType();
        return PowerComponentType.CONSUMER;
    }

    public void gridTick(CelestialForgingAnvilBlockEntity be) {
        IMegastructureHandler handler = this.getActiveHandler(be);
        if (handler != null) handler.gridTick(be);
        this.acceleratorHandler.gridTick(be);
    }
}
