package dev.dubhe.anvilcraft.api.depository.power;

import dev.dubhe.anvilcraft.api.depository.IDepository;

/**
 * 电力存储
 */
@SuppressWarnings("unused")
public interface IPowerDepository extends IDepository<Power> {
    PowerType getType();
}
