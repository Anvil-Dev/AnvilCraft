package dev.dubhe.anvilcraft.block.state;

import lombok.Getter;
import net.minecraft.util.StringRepresentable;

/**
 * 仓储端口的外观类型：端口连通组件接入的核心是潜影集装箱还是超维存储站。
 */
public enum StoragePortType implements StringRepresentable {
    /** 接入潜影集装箱（或未接入 / 接入多个核心） */
    SHULKER_CONTAINER("sc"),
    /** 接入超维存储站 */
    HYPERDIMENSION("hd");

    @Getter
    private final String name;

    StoragePortType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
