package dev.dubhe.anvilcraft.api.depository.power;

import dev.dubhe.anvilcraft.api.depository.IDepository;

/**
 * 电力存储
 */
public interface IPowerDepository extends IDepository<Power> {
    PowerType getType();
}
