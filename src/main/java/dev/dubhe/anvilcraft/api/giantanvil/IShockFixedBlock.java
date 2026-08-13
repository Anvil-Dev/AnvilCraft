package dev.dubhe.anvilcraft.api.giantanvil;

import net.minecraft.world.level.block.state.BlockState;

/**
 * 树脂撼地弹跳铁砧时，被标记为固定的方块保持为方块、不生成下落实体。
 * 在服务器线程、撼地行为树执行期间调用。
 */
public interface IShockFixedBlock {
    boolean anvilcraft$isFixedDuringShockBounce(BlockState state);
}
