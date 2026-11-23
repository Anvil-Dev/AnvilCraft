package dev.dubhe.anvilcraft.api.injection.entity;

public interface IFallingBlockEntityExtension {
    default double anvilcraft$getFallDistance() {
        throw new AssertionError();
    }

    /** 末地门转化的产物在本次下落中不再造成砸击或高速碰撞伤害。 */
    default void anvilcraft$disablePortalImpactDamage() {
        throw new AssertionError();
    }

    default boolean anvilcraft$isSpectral() {
        return false;
    }
}
