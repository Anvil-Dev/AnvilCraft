package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 锻星砧巨构处理器接口。
 * 26.1 使用 {@link ValueOutput}/{@link ValueInput} 持久化，网络同步仍通过 {@link CompoundTag}。
 */
public interface IMegastructureHandler {
    LaserRequirement NO_LASER_REQUIREMENT = new LaserRequirement(0, false);

    String name();

    void serverTick(CelestialForgingAnvilBlockEntity be);

    void onBuild(CelestialForgingAnvilBlockEntity be);

    void onClear(CelestialForgingAnvilBlockEntity be);

    /** 使用 ValueOutput 保存巨构持久化数据。 */
    void saveAdditional(ValueOutput output);

    /** 使用 ValueInput 读取巨构持久化数据。 */
    void loadAdditional(ValueInput input);

    /** 服务端写入随锻星砧更新标签发送的同步数据。 */
    void writeUpdateTag(CompoundTag tag, HolderLookup.Provider registries);

    /** 客户端读取锻星砧更新标签中的同步数据。 */
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

    default LaserRequirement getLaserRequirement() {
        return IMegastructureHandler.NO_LASER_REQUIREMENT;
    }

    /** 所有相连激光接口需要满足的激光等级和类型。 */
    record LaserRequirement(int level, boolean gamma) {
    }
}
