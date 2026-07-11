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
import java.util.Objects;

public class CfaMegastructureManager {
    private int activeMegastructureIndex = -1;
    private @Nullable String activeMegastructureName;
    private int activeMegastructureRing = -1;

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
        this.registerHandler(new PenroseSphereHandler());
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
        if (this.activeMegastructureName == null) {
            this.getActiveOption(be);
        }
        return this.activeMegastructureName == null ? null : this.handlers.get(this.activeMegastructureName);
    }

    @Nullable
    public CelestialRefactorOption getActiveOption(CelestialForgingAnvilBlockEntity be) {
        if (this.activeMegastructureIndex < 0) return null;
        if (this.activeMegastructureName != null) {
            for (CelestialRefactorOption option : CelestialRefactorRegistry.getOptionsForRing(0, 6)) {
                if (option.megastructure().equals(this.activeMegastructureName)
                    && option.ring() == this.activeMegastructureRing) {
                    return option;
                }
            }
        }
        if (be.getCelestialBodyData() == null) return null;
        var options = CelestialRefactorRegistry.getOptions(
            be.getCelestialBodyData(),
            be.isAmplify(),
            be.getPlanetaryResourceSet()
        );
        if (this.activeMegastructureIndex >= options.size()) return null;
        CelestialRefactorOption option = options.get(this.activeMegastructureIndex);
        this.activeMegastructureName = option.megastructure();
        this.activeMegastructureRing = option.ring();
        return option;
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
        return Objects.requireNonNull(
            this.findHandler(WormholeStabilizerHandler.class), "Wormhole handler was not registered"
        );
    }

    // ==================== 巨构刻逻辑 ====================

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

    // ==================== 激光需求 ====================

    public void syncLaserRequirements(CelestialForgingAnvilBlockEntity be) {
        IMegastructureHandler active = this.getActiveHandler(be);
        if (active == null) {
            this.clearAllLaserRequirements(be);
            return;
        }
        var lasers = CfaInterfaceScanner.findLaserInterfaces(be.getLevel(), be.getBlockPos());
        IMegastructureHandler.LaserRequirement requirement = active.getLaserRequirement();
        for (var laser : lasers) {
            laser.setLaserRequirement(requirement.level(), requirement.gamma());
        }
    }

    private void clearAllLaserRequirements(CelestialForgingAnvilBlockEntity be) {
        var lasers = CfaInterfaceScanner.findLaserInterfaces(be.getLevel(), be.getBlockPos());
        for (var laser : lasers) {
            laser.setLaserRequirement(0, false);
        }
    }

    // ==================== 建造与清除 ====================

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
        this.activeMegastructureName = option.megastructure();
        this.activeMegastructureRing = option.ring();
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
        this.activeMegastructureName = null;
        this.activeMegastructureRing = -1;
        this.clearAllLaserRequirements(be);
    }

    public void clearAllMegastructures(CelestialForgingAnvilBlockEntity be) {
        this.acceleratorHandler.onClear(be);
        this.clearMegastructure(be);
    }

    // ==================== 磁盘持久化 ====================

    public void saveAdditional(ValueOutput output) {
        output.putInt("activeMegastructure", this.activeMegastructureIndex);
        this.saveActiveIdentity(output);
        for (var handler : this.handlers.values()) {
            handler.saveAdditional(output);
        }
        this.acceleratorHandler.saveAdditional(output);
    }

    public void loadAdditional(ValueInput input) {
        this.activeMegastructureIndex = input.getIntOr("activeMegastructure", -1);
        this.loadActiveIdentity(input);
        for (var handler : this.handlers.values()) {
            handler.loadAdditional(input);
        }
        this.acceleratorHandler.loadAdditional(input);
    }

    // ==================== 基于 CompoundTag 的网络同步 ====================

    public void writeUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("activeMegastructure", this.activeMegastructureIndex);
        if (this.activeMegastructureName != null) {
            tag.putString("activeMegastructureName", this.activeMegastructureName);
            tag.putInt("activeMegastructureRing", this.activeMegastructureRing);
        }
        for (var handler : this.handlers.values()) {
            handler.writeUpdateTag(tag, registries);
        }
        this.acceleratorHandler.writeUpdateTag(tag, registries);
    }

    public void readUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        this.activeMegastructureIndex = tag.getIntOr("activeMegastructure", -1);
        String activeName = tag.getStringOr("activeMegastructureName", "");
        this.activeMegastructureName = activeName.isEmpty() ? null : activeName;
        this.activeMegastructureRing = tag.getIntOr("activeMegastructureRing", -1);
        for (var handler : this.handlers.values()) {
            handler.readUpdateTag(tag, registries);
        }
        this.acceleratorHandler.readUpdateTag(tag, registries);
    }

    // ==================== 电力 ====================

    public int getInputPower(CelestialForgingAnvilBlockEntity be) {
        IMegastructureHandler handler = this.getActiveHandler(be);
        return handler != null ? handler.getInputPower(be) : 0;
    }

    public int getOutputPower(CelestialForgingAnvilBlockEntity be) {
        IMegastructureHandler handler = this.getActiveHandler(be);
        return handler != null ? handler.getOutputPower(be) : 0;
    }

    /** 戴森球在恒星演化第一阶段是否正在提供无限电力。 */
    public boolean isInfinitePower(CelestialForgingAnvilBlockEntity be) {
        if (!be.isAcceleratorActive() || be.getAcceleratorStage() != 1 || !be.isAmplifierPresent()) {
            return false;
        }
        CelestialRefactorOption option = this.getActiveOption(be);
        return option != null && option.megastructure().contains("dyson_sphere");
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

    private void saveActiveIdentity(ValueOutput output) {
        if (this.activeMegastructureName == null) return;
        output.putString("activeMegastructureName", this.activeMegastructureName);
        output.putInt("activeMegastructureRing", this.activeMegastructureRing);
    }

    private void loadActiveIdentity(ValueInput input) {
        String activeName = input.getStringOr("activeMegastructureName", "");
        this.activeMegastructureName = activeName.isEmpty() ? null : activeName;
        this.activeMegastructureRing = input.getIntOr("activeMegastructureRing", -1);
    }
}
