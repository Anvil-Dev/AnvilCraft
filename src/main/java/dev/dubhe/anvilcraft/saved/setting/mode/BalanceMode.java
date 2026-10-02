package dev.dubhe.anvilcraft.saved.setting.mode;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * 物品均衡模式：控制手持物品用完自动补货、背包物品超一组自动存入存储站。
 * 智能为同时启用补货与存入；仅补货/仅存入只启用其一；关为全部禁用。
 */
public enum BalanceMode implements StringRepresentable {
    SMART,
    RESTOCK,
    DEPOSIT,
    OFF,
    ;

    public static final Codec<BalanceMode> CODEC = StringRepresentable.fromEnum(BalanceMode::values);
    public static final StreamCodec<ByteBuf, BalanceMode> STREAM_CODEC = StreamCodecUtil.enumStreamCodec(BalanceMode.class);

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public Component getModeName() {
        return Component.translatable("screen.anvilcraft.balance_mode." + this.getSerializedName());
    }

    /** 是否启用补货（手持物品用完时从存储站补一组）。 */
    public boolean restockEnabled() {
        return this == SMART || this == RESTOCK;
    }

    /** 是否启用自动存入（物品超一组时把多余部分存入存储站）。 */
    public boolean depositEnabled() {
        return this == SMART || this == DEPOSIT;
    }
}
