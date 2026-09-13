package dev.dubhe.anvilcraft.api.plasma;

import dev.dubhe.anvilcraft.api.heat.HeaterInfo;
import dev.dubhe.anvilcraft.block.entity.PlasmaJetsBlockEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * 等离子喷流生命周期扩展。默认全部不处理。
 * 服务器方法仅在服务器线程调用；粒子方法仅在客户端调用。
 */
public interface PlasmaJetBehavior {
    default void onServerTickHead(PlasmaJetsBlockEntity jet, ServerLevel level) {
    }

    default void onServerTickTail(PlasmaJetsBlockEntity jet, ServerLevel level) {
    }

    default boolean shouldStopRaising(PlasmaJetsBlockEntity jet) {
        return false;
    }

    default void afterRaise(PlasmaJetsBlockEntity from, PlasmaJetsBlockEntity to) {
    }

    default boolean keepInitialJet(PlasmaJetsBlockEntity jet, Level level) {
        return false;
    }

    default HeaterInfo<?> heatInfo(PlasmaJetsBlockEntity jet, HeaterInfo<?> ordinary) {
        return ordinary;
    }

    default float modifyDamage(PlasmaJetsBlockEntity jet, float ordinary) {
        return ordinary;
    }

    /** 返回 {@code true} 表示已接管时长刷新，跳过原版燃油消耗。 */
    default boolean refreshDuration(PlasmaJetsBlockEntity jet, Level level) {
        return false;
    }

    default ParticleOptions particle(PlasmaJetsBlockEntity jet, ParticleOptions ordinary) {
        return ordinary;
    }

    default void extraParticles(PlasmaJetsBlockEntity jet, ClientLevel level) {
    }

    default void onRemoved(PlasmaJetsBlockEntity jet) {
    }

    default void save(PlasmaJetsBlockEntity jet, CompoundTag tag, HolderLookup.Provider registries) {
    }

    default void load(PlasmaJetsBlockEntity jet, CompoundTag tag, HolderLookup.Provider registries) {
    }
}
