package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public interface IMegastructureHandler {

    String name();

    void serverTick(CelestialForgingAnvilBlockEntity be);

    void onBuild(CelestialForgingAnvilBlockEntity be);

    void onClear(CelestialForgingAnvilBlockEntity be);

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
}
