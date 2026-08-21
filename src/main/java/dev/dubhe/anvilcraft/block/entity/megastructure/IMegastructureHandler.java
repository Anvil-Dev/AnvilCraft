package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public interface IMegastructureHandler {
    LaserRequirement NO_LASER_REQUIREMENT = new LaserRequirement(0, false);

    String name();

    void serverTick(CelestialForgingAnvilBlockEntity be);

    void onBuild(CelestialForgingAnvilBlockEntity be);

    void onClear(CelestialForgingAnvilBlockEntity be);

    /**
     * Reports whether an auxiliary definition currently owns its ring.
     * Auxiliary handlers must persist and synchronize the state used here.
     */
    default boolean isAuxiliaryActive(CelestialForgingAnvilBlockEntity be) {
        return false;
    }

    /** Releases runtime registrations without clearing persistent megastructure state. */
    default void onUnload(CelestialForgingAnvilBlockEntity be) {
    }

    void saveAdditional(CompoundTag tag, HolderLookup.Provider registries);

    void loadAdditional(CompoundTag tag, HolderLookup.Provider registries);

    void writeUpdateTag(CompoundTag tag, HolderLookup.Provider registries);

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

    /** Laser level and mode required by every interface connected to this structure. */
    default LaserRequirement getLaserRequirement() {
        return IMegastructureHandler.NO_LASER_REQUIREMENT;
    }

    record LaserRequirement(int level, boolean gamma) {
    }
}
