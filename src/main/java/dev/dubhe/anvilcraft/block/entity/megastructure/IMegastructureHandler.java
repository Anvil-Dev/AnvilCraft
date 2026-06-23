package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Interface for CFA megastructure handlers.
 * <p>
 * In 26.1, NBT persistence uses {@link ValueOutput}/{@link ValueInput} for disk save/load.
 * Network sync still uses {@link CompoundTag} via {@code getUpdateTag}.
 * {@code readUpdateTag} is kept for Phase 6 client sync integration.
 */
public interface IMegastructureHandler {

    String name();

    void serverTick(CelestialForgingAnvilBlockEntity be);

    void onBuild(CelestialForgingAnvilBlockEntity be);

    void onClear(CelestialForgingAnvilBlockEntity be);

    /** Disk persistence — 26.1 uses ValueOutput */
    void saveAdditional(ValueOutput output);

    /** Disk persistence — 26.1 uses ValueInput */
    void loadAdditional(ValueInput input);

    /** Network sync (server) — included in CFA BE's getUpdateTag */
    void writeUpdateTag(CompoundTag tag, HolderLookup.Provider registries);

    /** Network sync (client) — called from Phase 6 CFA BE sync path */
    void readUpdateTag(CompoundTag tag, HolderLookup.Provider registries);

    default int getInputPower(CelestialForgingAnvilBlockEntity be) {
        return 0;
    }

    default int getOutputPower(CelestialForgingAnvilBlockEntity be) {
        return 0;
    }

    default PowerComponentType getComponentType() {
        return PowerComponentType.CONSUMER;
    }

    default void gridTick(CelestialForgingAnvilBlockEntity be) {
    }
}
