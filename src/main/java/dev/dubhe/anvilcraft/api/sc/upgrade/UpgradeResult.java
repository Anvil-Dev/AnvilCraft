package dev.dubhe.anvilcraft.api.sc.upgrade;

import net.minecraft.network.chat.Component;

import java.util.Locale;

public enum UpgradeResult {
    CAN_UPGRADE,
    NO_MATERIAL,
    NO_TOOL,
    NO_ANY,
    ALREADY_MAX,
    ;

    public Component getDesc() {
        return Component.translatable("screen.anvilcraft.shulker_container.upgrade.result." + this.name().toLowerCase(Locale.ROOT));
    }
}
