package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorRegistry;
import dev.dubhe.anvilcraft.block.entity.celestial.Megastructure;
import dev.dubhe.anvilcraft.block.entity.megastructure.AcceleratorHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.IMegastructureHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.WormholeStabilizerHandler;
import dev.dubhe.anvilcraft.init.ModMegastructures;
import dev.dubhe.anvilcraft.init.registry.ModRegistries;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class CfaMegastructureManager {
    private @Nullable Identifier activeMegastructureId;
    private int legacyActiveMegastructureIndex = -1;
    private @Nullable String legacyActiveMegastructureName;
    private int legacyActiveMegastructureRing = -1;

    private final Map<Identifier, IMegastructureHandler> handlers = new LinkedHashMap<>();
    @Getter
    private final AcceleratorHandler acceleratorHandler;

    public CfaMegastructureManager() {
        for (Megastructure megastructure : ModRegistries.MEGASTRUCTURE) {
            IMegastructureHandler previous = this.handlers.put(megastructure.id(), megastructure.createHandler());
            if (previous != null) {
                throw new IllegalStateException("Duplicate megastructure handler: " + megastructure.id());
            }
        }
        IMegastructureHandler accelerator = this.handlers.get(ModMegastructures.STELLAR_EVOLUTION_ACCELERATOR.getId());
        if (!(accelerator instanceof AcceleratorHandler handler)) {
            throw new IllegalStateException("Stellar evolution accelerator handler was not registered");
        }
        this.acceleratorHandler = handler;
    }

    public boolean hasActiveMegastructure() {
        return this.activeMegastructureId != null
            || this.legacyActiveMegastructureName != null
            || this.legacyActiveMegastructureIndex >= 0;
    }

    public @Nullable Identifier getActiveId(CelestialForgingAnvilBlockEntity be) {
        this.resolveLegacyIdentity(be);
        return this.activeMegastructureId;
    }

    @Nullable
    public IMegastructureHandler getActiveHandler(CelestialForgingAnvilBlockEntity be) {
        Identifier id = this.getActiveId(be);
        return id == null ? null : this.handlers.get(id);
    }

    @Nullable
    public CelestialRefactorOption getActiveOption(CelestialForgingAnvilBlockEntity be) {
        Identifier id = this.getActiveId(be);
        if (id == null) return null;
        return CelestialRefactorRegistry.getOption(
            id,
            be.getCelestialBodyData(),
            be.isAmplify(),
            be.getPlanetaryResourceSet()
        );
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends IMegastructureHandler> T findHandler(Class<T> type) {
        for (IMegastructureHandler handler : this.handlers.values()) {
            if (type.isInstance(handler)) return (T) handler;
        }
        return null;
    }

    public WormholeStabilizerHandler getWormholeHandler() {
        return Objects.requireNonNull(
            this.findHandler(WormholeStabilizerHandler.class), "Wormhole handler was not registered"
        );
    }

    public void serverTick(CelestialForgingAnvilBlockEntity be) {
        if (this.hasActiveMegastructure()) {
            IMegastructureHandler active = this.getActiveHandler(be);
            if (active != null) {
                active.serverTick(be);
            }
            this.syncLaserRequirements(be);
        }
        for (Megastructure megastructure : ModRegistries.MEGASTRUCTURE) {
            if (!megastructure.auxiliary()) continue;
            IMegastructureHandler handler = this.handlers.get(megastructure.id());
            if (handler != null) {
                handler.serverTick(be);
            }
        }
    }

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

    public void buildMegastructure(CelestialRefactorOption option, CelestialForgingAnvilBlockEntity be) {
        IMegastructureHandler handler = this.handlers.get(option.id());
        if (option.auxiliary()) {
            if (handler != null) {
                handler.onBuild(be);
            }
            return;
        }
        if (this.hasActiveMegastructure()) return;
        this.activeMegastructureId = option.id();
        this.clearLegacyIdentity();
        if (handler != null) {
            handler.onBuild(be);
        }
    }

    public void clearMegastructure(CelestialForgingAnvilBlockEntity be) {
        if (this.hasActiveMegastructure()) {
            IMegastructureHandler handler = this.getActiveHandler(be);
            if (handler != null) {
                handler.onClear(be);
            }
        }
        this.activeMegastructureId = null;
        this.clearLegacyIdentity();
        this.clearAllLaserRequirements(be);
    }

    public void clearAllMegastructures(CelestialForgingAnvilBlockEntity be) {
        for (Megastructure megastructure : ModRegistries.MEGASTRUCTURE) {
            if (!megastructure.auxiliary()) continue;
            IMegastructureHandler handler = this.handlers.get(megastructure.id());
            if (handler != null) {
                handler.onClear(be);
            }
        }
        this.clearMegastructure(be);
    }

    public void saveAdditional(ValueOutput output) {
        if (this.activeMegastructureId != null) {
            output.putString("activeMegastructureId", this.activeMegastructureId.toString());
        }
        for (IMegastructureHandler handler : this.handlers.values()) {
            handler.saveAdditional(output);
        }
    }

    public void loadAdditional(ValueInput input) {
        this.loadActiveIdentity(
            input.getStringOr("activeMegastructureId", ""),
            input.getStringOr("activeMegastructureName", ""),
            input.getIntOr("activeMegastructureRing", -1),
            input.getIntOr("activeMegastructure", -1)
        );
        for (IMegastructureHandler handler : this.handlers.values()) {
            handler.loadAdditional(input);
        }
    }

    public void writeUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        if (this.activeMegastructureId != null) {
            tag.putString("activeMegastructureId", this.activeMegastructureId.toString());
        }
        for (IMegastructureHandler handler : this.handlers.values()) {
            handler.writeUpdateTag(tag, registries);
        }
    }

    public void readUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        this.loadActiveIdentity(
            tag.getStringOr("activeMegastructureId", ""),
            tag.getStringOr("activeMegastructureName", ""),
            tag.getIntOr("activeMegastructureRing", -1),
            tag.getIntOr("activeMegastructure", -1)
        );
        for (IMegastructureHandler handler : this.handlers.values()) {
            handler.readUpdateTag(tag, registries);
        }
    }

    public int getInputPower(CelestialForgingAnvilBlockEntity be) {
        IMegastructureHandler handler = this.getActiveHandler(be);
        return handler != null ? handler.getInputPower(be) : 0;
    }

    public int getOutputPower(CelestialForgingAnvilBlockEntity be) {
        IMegastructureHandler handler = this.getActiveHandler(be);
        return handler != null ? handler.getOutputPower(be) : 0;
    }

    /** Returns whether a Dyson sphere is supplying unlimited power during stellar acceleration. */
    public boolean isInfinitePower(CelestialForgingAnvilBlockEntity be) {
        if (!be.isAcceleratorActive() || be.getAcceleratorStage() != 1 || !be.isAmplifierPresent()) {
            return false;
        }
        Identifier id = this.getActiveId(be);
        return ModMegastructures.DYSON_SPHERE_SMALL.getId().equals(id)
            || ModMegastructures.DYSON_SPHERE_LARGE.getId().equals(id);
    }

    public PowerComponentType getComponentType(CelestialForgingAnvilBlockEntity be) {
        IMegastructureHandler handler = this.getActiveHandler(be);
        if (handler != null) return handler.getComponentType();
        return PowerComponentType.CONSUMER;
    }

    public void gridTick(CelestialForgingAnvilBlockEntity be) {
        IMegastructureHandler handler = this.getActiveHandler(be);
        if (handler != null) handler.gridTick(be);
        for (Megastructure megastructure : ModRegistries.MEGASTRUCTURE) {
            if (!megastructure.auxiliary()) continue;
            IMegastructureHandler auxiliary = this.handlers.get(megastructure.id());
            if (auxiliary != null) auxiliary.gridTick(be);
        }
    }

    private void resolveLegacyIdentity(CelestialForgingAnvilBlockEntity be) {
        if (this.activeMegastructureId != null) return;
        if (this.legacyActiveMegastructureName != null) {
            this.activeMegastructureId = CelestialRefactorRegistry.findLegacyId(
                this.legacyActiveMegastructureName,
                this.legacyActiveMegastructureRing,
                be.getCelestialBodyData(),
                be.isAmplify(),
                be.getPlanetaryResourceSet()
            );
        }
        if (this.activeMegastructureId == null && this.legacyActiveMegastructureIndex >= 0) {
            var options = CelestialRefactorRegistry.getOptions(
                be.getCelestialBodyData(),
                be.isAmplify(),
                be.getPlanetaryResourceSet()
            );
            if (this.legacyActiveMegastructureIndex < options.size()) {
                this.activeMegastructureId = options.get(this.legacyActiveMegastructureIndex).id();
            }
        }
        if (this.activeMegastructureId != null) {
            this.clearLegacyIdentity();
        }
    }

    private void loadActiveIdentity(String id, String legacyName, int legacyRing, int legacyIndex) {
        this.activeMegastructureId = id.isEmpty() ? null : Identifier.tryParse(id);
        if (this.activeMegastructureId != null) {
            this.clearLegacyIdentity();
            return;
        }
        this.legacyActiveMegastructureName = legacyName.isEmpty() ? null : legacyName;
        this.legacyActiveMegastructureRing = legacyRing;
        this.legacyActiveMegastructureIndex = legacyIndex;
    }

    private void clearLegacyIdentity() {
        this.legacyActiveMegastructureName = null;
        this.legacyActiveMegastructureRing = -1;
        this.legacyActiveMegastructureIndex = -1;
    }
}
