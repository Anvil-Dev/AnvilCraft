package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.tooltip.impl.RedstoneWireTooltipProvider;
import dev.dubhe.anvilcraft.block.RedstoneWireClientPowerCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

/**
 * 服务端返回指定导线非红石粉输入强度的数据包。
 *
 * @param pos 查询对应的导线位置
 * @param power 网络总信号强度
 * @param nonDustPower 排除原版红石粉输入后的网络信号强度
 */
public record RedstoneWirePowerResponsePacket(BlockPos pos, int power, int nonDustPower) implements IClientboundPacket {
    public static final Type<RedstoneWirePowerResponsePacket> TYPE = IPacket.type(
        AnvilCraft.of("redstone_wire_power_response")
    );
    public static final StreamCodec<ByteBuf, RedstoneWirePowerResponsePacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        RedstoneWirePowerResponsePacket::pos,
        // 合法强度只有 0 到 15，VAR_INT 在正常响应中只占一个字节。
        ByteBufCodecs.VAR_INT,
        RedstoneWirePowerResponsePacket::power,
        ByteBufCodecs.VAR_INT,
        RedstoneWirePowerResponsePacket::nonDustPower,
        RedstoneWirePowerResponsePacket::new
    );

    @Override
    public Type<RedstoneWirePowerResponsePacket> type() {
        return RedstoneWirePowerResponsePacket.TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (RedstoneWireClientPowerCache.update(player.level(), this.pos, this.power)) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == player.level()) {
                minecraft.levelRenderer.setBlockDirty(this.pos, false);
            }
        }
        // 只更新 HUD 的短期非粉线缓存；权威红石计算始终保留在服务端 Manager 中。
        RedstoneWireTooltipProvider.receive(player.level(), this.pos, this.nonDustPower);
    }
}
