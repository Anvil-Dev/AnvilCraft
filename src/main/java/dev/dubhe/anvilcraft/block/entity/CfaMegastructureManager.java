package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorRegistry;
import dev.dubhe.anvilcraft.block.entity.celestial.Megastructure;
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
import dev.dubhe.anvilcraft.init.registry.ModRegistries;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

/** Owns the runtime state and handlers for one Celestial Forging Anvil. */
public class CfaMegastructureManager {
    private @Nullable ResourceLocation activeMegastructureId;
    /** Old index/name fields are kept so existing worlds and add-ons continue to load. */
    private int legacyActiveMegastructureIndex = -1;
    private @Nullable String legacyActiveMegastructureName;
    private int legacyActiveMegastructureRing = -1;

    private final Map<ResourceLocation, IMegastructureHandler> handlers = new LinkedHashMap<>();
    @Getter
    private final AcceleratorHandler acceleratorHandler;

    public CfaMegastructureManager() {
        for (Megastructure definition : ModRegistries.MEGASTRUCTURE) {
            IMegastructureHandler previous = this.handlers.put(definition.id(), definition.createHandler());
            if (previous != null) {
                throw new IllegalStateException("Duplicate megastructure handler: " + definition.id());
            }
        }
        // A block entity can be constructed by a data-fix/test while the
        // deferred registry is only partially filled. Fill every missing
        // built-in handler without replacing handlers already created from the
        // registry (or their runtime state).
        this.registerLegacyHandlers();
        ResourceLocation acceleratorId = AnvilCraft.of("stellar_evolution_accelerator");
        IMegastructureHandler accelerator = this.handlers.get(acceleratorId);
        if (!(accelerator instanceof AcceleratorHandler handler)) {
            throw new IllegalStateException(
                "Stellar evolution accelerator handler was not registered for " + acceleratorId
            );
        }
        this.acceleratorHandler = handler;
    }

    private void registerLegacyHandlers() {
        this.registerLegacy(new ExcavatorHandler());
        this.registerLegacy(new ExtractorHandler());
        this.registerLegacy(new GiantExtractorHandler());
        this.registerLegacy(new ColliderHandler());
        this.registerLegacy(new DysonSphereHandler("dyson_sphere_small"));
        this.registerLegacy(new DysonSphereHandler("dyson_sphere_large"));
        this.registerLegacy(new DysonSphereHandler("dyson_sphere_brown_dwarf"));
        this.registerLegacy(new MagnetarCoilHandler());
        this.registerLegacy(new PenroseSphereHandler());
        this.registerLegacy(new MatterDecompressorHandler());
        this.registerLegacy(new WormholeStabilizerHandler());
        this.registerLegacy(new EcoStationHandler());
        this.registerLegacy(new TempleHandler());
        this.registerLegacy(new AcceleratorHandler());
    }

    private void registerLegacy(IMegastructureHandler handler) {
        ResourceLocation id = AnvilCraft.of(handler.name());
        this.handlers.putIfAbsent(id, handler);
    }

    /**
     * Adds definitions that became visible after this manager was constructed.
     *
     * <p>Block entities can be instantiated by tests or data fixes before a
     * deferred registry has finished filling.  Keep already-created handlers
     * (including the legacy fallback instances) so their runtime state is not
     * replaced, while still making later add-on definitions usable.</p>
     */
    private void synchronizeRegistryHandlers() {
        for (Megastructure definition : ModRegistries.MEGASTRUCTURE) {
            if (!this.handlers.containsKey(definition.id())) {
                this.handlers.put(definition.id(), definition.createHandler());
            }
        }
    }

    /** Returns the legacy option index when known, or -1 when no primary is active. */
    public int getActiveIndex() {
        // A new-ID save may not have an index yet; callers use this value as
        // an active-state flag while the exact index is resolved from the
        // current definition list on the next option lookup.
        return this.legacyActiveMegastructureIndex >= 0 || this.activeMegastructureId != null
            ? Math.max(this.legacyActiveMegastructureIndex, 0)
            : -1;
    }

    public boolean hasActiveMegastructure() {
        return this.activeMegastructureId != null
            || this.legacyActiveMegastructureName != null
            || this.legacyActiveMegastructureIndex >= 0;
    }

    /** Resolves the stable ID, migrating old name/index state lazily. */
    public @Nullable ResourceLocation getActiveId(CelestialForgingAnvilBlockEntity be) {
        this.resolveLegacyIdentity(be);
        return this.activeMegastructureId;
    }

    @Nullable
    public IMegastructureHandler getActiveHandler(CelestialForgingAnvilBlockEntity be) {
        this.synchronizeRegistryHandlers();
        ResourceLocation id = this.getActiveId(be);
        return id == null ? null : this.handlers.get(id);
    }

    @Nullable
    public CelestialRefactorOption getActiveOption(CelestialForgingAnvilBlockEntity be) {
        ResourceLocation id = this.getActiveId(be);
        if (id == null) return null;
        CelestialRefactorOption option = CelestialRefactorRegistry.getOption(
            id,
            be.getCelestialBodyData(),
            be.isAmplify(),
            be.getPlanetaryResourceSet()
        );
        if (option != null) {
            this.updateActiveIndex(be, option.id());
        }
        return option;
    }

    private void updateActiveIndex(CelestialForgingAnvilBlockEntity be, ResourceLocation id) {
        List<CelestialRefactorOption> options = CelestialRefactorRegistry.getOptions(
            be.getCelestialBodyData(),
            be.isAmplify(),
            be.getPlanetaryResourceSet()
        );
        for (int index = 0; index < options.size(); index++) {
            if (id.equals(options.get(index).id())) {
                this.legacyActiveMegastructureIndex = index;
                return;
            }
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends IMegastructureHandler> T findHandler(Class<T> type) {
        this.synchronizeRegistryHandlers();
        for (IMegastructureHandler handler : this.handlers.values()) {
            if (type.isInstance(handler)) return (T) handler;
        }
        return null;
    }

    public WormholeStabilizerHandler getWormholeHandler() {
        return Objects.requireNonNull(
            this.findHandler(WormholeStabilizerHandler.class),
            "Wormhole stabilizer handler is not registered"
        );
    }

    // === Tick ===

    public void serverTick(CelestialForgingAnvilBlockEntity be) {
        IMegastructureHandler active = this.getActiveHandler(be);
        if (active != null) {
            active.serverTick(be);
        }
        if (this.hasActiveMegastructure()) this.syncLaserRequirements(be);
        boolean acceleratorTicked = false;
        for (Megastructure definition : ModRegistries.MEGASTRUCTURE) {
            if (!definition.auxiliary()) continue;
            IMegastructureHandler handler = this.handlers.get(definition.id());
            if (handler != null && handler != active && handler.isAuxiliaryActive(be)) {
                handler.serverTick(be);
                acceleratorTicked |= handler == this.acceleratorHandler;
            }
        }
        if (!acceleratorTicked
            && this.acceleratorHandler != active
            && this.acceleratorHandler.isAuxiliaryActive(be)) {
            this.acceleratorHandler.serverTick(be);
        }
    }

    // === Laser requirements ===

    public void syncLaserRequirements(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null) return;
        IMegastructureHandler active = this.getActiveHandler(be);
        if (active == null) {
            this.clearAllLaserRequirements(be);
            return;
        }
        IMegastructureHandler.LaserRequirement requirement = active.getLaserRequirement();
        var lasers = CfaInterfaceScanner.findLaserInterfaces(be.getLevel(), be.getBlockPos());
        for (var laser : lasers) {
            laser.setLaserRequirement(requirement.level(), requirement.gamma());
        }
    }

    private void clearAllLaserRequirements(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null) return;
        var lasers = CfaInterfaceScanner.findLaserInterfaces(be.getLevel(), be.getBlockPos());
        for (var laser : lasers) {
            laser.setLaserRequirement(0, false);
        }
    }

    // === Build / Clear ===

    /**
     * Validates a server-side build request before the CFA consumes its material.
     * The option list is resolved again so a stale client index cannot select a
     * definition that is no longer available for the current celestial body.
     */
    public boolean canBuild(CelestialRefactorOption option, CelestialForgingAnvilBlockEntity be) {
        this.synchronizeRegistryHandlers();
        if (!be.isLocked() || be.isSearching() || be.isAcceleratorActive() || be.getCelestialBodyData() == null) {
            return false;
        }
        boolean currentOption = be.getClientVisibleOptions().stream()
            .anyMatch(candidate -> candidate.id().equals(option.id()) && candidate.ring() == option.ring());
        if (!currentOption) return false;
        IMegastructureHandler handler = this.handlers.get(option.id());
        if (handler == null) return false;
        if (option.auxiliary()) {
            return !handler.isAuxiliaryActive(be) && !this.isRingOccupied(option.ring(), be);
        }
        return !this.hasActiveMegastructure()
            && this.getActiveAuxiliaryOptionForRing(be, option.ring()) == null;
    }

    private boolean isRingOccupied(int ring, CelestialForgingAnvilBlockEntity be) {
        CelestialRefactorOption primary = this.getActiveOption(be);
        return (primary != null && primary.ring() == ring)
            || this.getActiveAuxiliaryOptionForRing(be, ring) != null;
    }

    public void buildMegastructure(int optionIndex, CelestialForgingAnvilBlockEntity be) {
        if (be.getCelestialBodyData() == null) return;
        List<CelestialRefactorOption> options = be.getClientVisibleOptions();
        if (optionIndex < 0 || optionIndex >= options.size()) return;
        this.buildMegastructure(options.get(optionIndex), optionIndex, be);
    }

    /** Builds a resolved option; retained as an overload for 26.1-style callers. */
    public void buildMegastructure(CelestialRefactorOption option, CelestialForgingAnvilBlockEntity be) {
        List<CelestialRefactorOption> options = be.getClientVisibleOptions();
        this.buildMegastructure(option, options.indexOf(option), be);
    }

    private void buildMegastructure(
        CelestialRefactorOption option,
        int optionIndex,
        CelestialForgingAnvilBlockEntity be
    ) {
        this.synchronizeRegistryHandlers();
        if (optionIndex < 0) return;
        IMegastructureHandler handler = this.handlers.get(option.id());
        if (option.auxiliary()) {
            if (handler != null
                && !handler.isAuxiliaryActive(be)
                && !this.isRingOccupied(option.ring(), be)) {
                handler.onBuild(be);
            }
            return;
        }
        if (this.hasActiveMegastructure()
            || this.getActiveAuxiliaryOptionForRing(be, option.ring()) != null) {
            return;
        }
        this.activeMegastructureId = option.id();
        this.legacyActiveMegastructureIndex = Math.max(optionIndex, 0);
        this.legacyActiveMegastructureName = null;
        this.legacyActiveMegastructureRing = -1;
        if (handler != null) handler.onBuild(be);
    }

    public void clearMegastructure(CelestialForgingAnvilBlockEntity be) {
        IMegastructureHandler handler = this.getActiveHandler(be);
        if (handler != null) handler.onClear(be);
        this.activeMegastructureId = null;
        this.clearLegacyIdentity();
        this.clearAllLaserRequirements(be);
    }

    public void clearAllMegastructures(CelestialForgingAnvilBlockEntity be) {
        this.clearAuxiliaryMegastructures(be);
        this.clearMegastructure(be);
    }

    /**
     * 爆发事件期间清除其它巨构，但保留恒星演化加速器本身的状态机。
     * 直接调用 clearAllMegastructures 会触发加速器 onClear，导致抛射物时间线被截断。
     */
    public void clearOtherMegastructures(CelestialForgingAnvilBlockEntity be) {
        IMegastructureHandler active = this.getActiveHandler(be);
        if (active != null && active != this.acceleratorHandler) {
            active.onClear(be);
            this.activeMegastructureId = null;
            this.clearLegacyIdentity();
        } else if (active == null && this.hasActiveMegastructure()) {
            this.activeMegastructureId = null;
            this.clearLegacyIdentity();
        }
        this.synchronizeRegistryHandlers();
        for (Megastructure definition : ModRegistries.MEGASTRUCTURE) {
            if (!definition.auxiliary()) continue;
            IMegastructureHandler handler = this.handlers.get(definition.id());
            if (handler != null && handler != this.acceleratorHandler && handler.isAuxiliaryActive(be)) {
                handler.onClear(be);
            }
        }
        this.clearAllLaserRequirements(be);
    }

    public void clearAuxiliaryMegastructures(CelestialForgingAnvilBlockEntity be) {
        this.synchronizeRegistryHandlers();
        boolean acceleratorCleared = false;
        for (Megastructure definition : ModRegistries.MEGASTRUCTURE) {
            if (!definition.auxiliary()) continue;
            IMegastructureHandler handler = this.handlers.get(definition.id());
            if (handler != null && handler.isAuxiliaryActive(be)) {
                handler.onClear(be);
                acceleratorCleared |= handler == this.acceleratorHandler;
            }
        }
        if (!acceleratorCleared && this.acceleratorHandler.isAuxiliaryActive(be)) {
            this.acceleratorHandler.onClear(be);
        }
    }

    public boolean hasActiveAuxiliary(CelestialForgingAnvilBlockEntity be) {
        this.synchronizeRegistryHandlers();
        for (Megastructure definition : ModRegistries.MEGASTRUCTURE) {
            if (!definition.auxiliary()) continue;
            IMegastructureHandler handler = this.handlers.get(definition.id());
            if (handler != null && handler.isAuxiliaryActive(be)) return true;
        }
        return this.acceleratorHandler.isAuxiliaryActive(be);
    }

    public @Nullable CelestialRefactorOption getActiveAuxiliaryOptionForRing(
        CelestialForgingAnvilBlockEntity be,
        int ring
    ) {
        this.synchronizeRegistryHandlers();
        boolean acceleratorVisited = false;
        for (Megastructure definition : ModRegistries.MEGASTRUCTURE) {
            if (!definition.auxiliary()) continue;
            IMegastructureHandler handler = this.handlers.get(definition.id());
            if (handler == null || !handler.isAuxiliaryActive(be)) continue;
            acceleratorVisited |= handler == this.acceleratorHandler;
            CelestialRefactorOption option = CelestialRefactorRegistry.getOption(
                definition.id(),
                be.getCelestialBodyData(),
                be.isAmplify(),
                be.getPlanetaryResourceSet()
            );
            if (option != null && option.ring() == ring) return option;
        }
        if (!acceleratorVisited && this.acceleratorHandler.isAuxiliaryActive(be)) {
            CelestialRefactorOption option = CelestialRefactorRegistry.getOption(
                AnvilCraft.of("stellar_evolution_accelerator"),
                be.getCelestialBodyData(),
                be.isAmplify(),
                be.getPlanetaryResourceSet()
            );
            if (option != null && option.ring() == ring) return option;
        }
        return null;
    }

    /** Releases world-scoped runtime state while retaining data needed after a chunk reload. */
    public void unload(CelestialForgingAnvilBlockEntity be) {
        this.synchronizeRegistryHandlers();
        for (IMegastructureHandler handler : this.handlers.values()) {
            handler.onUnload(be);
        }
    }

    // === NBT ===

    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        this.synchronizeRegistryHandlers();
        if (this.activeMegastructureId != null) {
            tag.putString("activeMegastructureId", this.activeMegastructureId.toString());
        }
        tag.putInt("activeMegastructure", this.legacyActiveMegastructureIndex);
        if (this.legacyActiveMegastructureName != null) {
            tag.putString("activeMegastructureName", this.legacyActiveMegastructureName);
            tag.putInt("activeMegastructureRing", this.legacyActiveMegastructureRing);
        }
        for (IMegastructureHandler handler : this.handlers.values()) {
            handler.saveAdditional(tag, registries);
        }
    }

    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        this.synchronizeRegistryHandlers();
        this.loadActiveIdentity(
            tag.contains("activeMegastructureId") ? tag.getString("activeMegastructureId") : "",
            tag.contains("activeMegastructureName") ? tag.getString("activeMegastructureName") : "",
            tag.contains("activeMegastructureRing") ? tag.getInt("activeMegastructureRing") : -1,
            tag.contains("activeMegastructure") ? tag.getInt("activeMegastructure") : -1
        );
        for (IMegastructureHandler handler : this.handlers.values()) {
            handler.loadAdditional(tag, registries);
        }
    }

    public void writeUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        this.synchronizeRegistryHandlers();
        if (this.activeMegastructureId != null) {
            tag.putString("activeMegastructureId", this.activeMegastructureId.toString());
        }
        tag.putInt("activeMegastructure", this.legacyActiveMegastructureIndex);
        for (IMegastructureHandler handler : this.handlers.values()) {
            handler.writeUpdateTag(tag, registries);
        }
    }

    public void readUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        this.synchronizeRegistryHandlers();
        this.loadActiveIdentity(
            tag.contains("activeMegastructureId") ? tag.getString("activeMegastructureId") : "",
            tag.contains("activeMegastructureName") ? tag.getString("activeMegastructureName") : "",
            tag.contains("activeMegastructureRing") ? tag.getInt("activeMegastructureRing") : -1,
            tag.contains("activeMegastructure") ? tag.getInt("activeMegastructure") : -1
        );
        for (IMegastructureHandler handler : this.handlers.values()) {
            handler.readUpdateTag(tag, registries);
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
            this.activeMegastructureId = CelestialRefactorRegistry.findLegacyIdByIndex(
                this.legacyActiveMegastructureIndex,
                be.getCelestialBodyData(),
                be.isAmplify(),
                be.getPlanetaryResourceSet()
            );
        }
        if (this.activeMegastructureId != null) {
            this.legacyActiveMegastructureName = null;
            this.legacyActiveMegastructureRing = -1;
        }
    }

    private void loadActiveIdentity(String id, String legacyName, int legacyRing, int legacyIndex) {
        ResourceLocation parsed = id.isEmpty() ? null : ResourceLocation.tryParse(id);
        this.activeMegastructureId = parsed;
        this.legacyActiveMegastructureIndex = legacyIndex;
        if (parsed != null) {
            this.legacyActiveMegastructureName = null;
            this.legacyActiveMegastructureRing = -1;
        } else {
            this.legacyActiveMegastructureName = legacyName.isEmpty() ? null : legacyName;
            this.legacyActiveMegastructureRing = legacyRing;
        }
    }

    private void clearLegacyIdentity() {
        this.legacyActiveMegastructureIndex = -1;
        this.legacyActiveMegastructureName = null;
        this.legacyActiveMegastructureRing = -1;
    }

    // === Power ===

    public int getInputPower(CelestialForgingAnvilBlockEntity be) {
        IMegastructureHandler handler = this.getActiveHandler(be);
        return handler == null ? 0 : handler.getInputPower(be);
    }

    public int getOutputPower(CelestialForgingAnvilBlockEntity be) {
        IMegastructureHandler handler = this.getActiveHandler(be);
        return handler == null ? 0 : handler.getOutputPower(be);
    }

    public boolean isInfinitePower(CelestialForgingAnvilBlockEntity be) {
        if (!be.isAcceleratorActive() || be.getAcceleratorStage() != 1 || !be.isAmplifierPresent()) return false;
        ResourceLocation id = this.getActiveId(be);
        return AnvilCraft.of("dyson_sphere_small").equals(id)
            || AnvilCraft.of("dyson_sphere_large").equals(id);
    }

    public PowerComponentType getComponentType(CelestialForgingAnvilBlockEntity be) {
        IMegastructureHandler handler = this.getActiveHandler(be);
        return handler == null ? PowerComponentType.CONSUMER : handler.getComponentType();
    }

    public void gridTick(CelestialForgingAnvilBlockEntity be) {
        IMegastructureHandler active = this.getActiveHandler(be);
        if (active != null) active.gridTick(be);
        boolean acceleratorTicked = false;
        for (Megastructure definition : ModRegistries.MEGASTRUCTURE) {
            if (!definition.auxiliary()) continue;
            IMegastructureHandler handler = this.handlers.get(definition.id());
            if (handler != null && handler != active && handler.isAuxiliaryActive(be)) {
                handler.gridTick(be);
                acceleratorTicked |= handler == this.acceleratorHandler;
            }
        }
        if (!acceleratorTicked
            && this.acceleratorHandler != active
            && this.acceleratorHandler.isAuxiliaryActive(be)) {
            this.acceleratorHandler.gridTick(be);
        }
    }
}
