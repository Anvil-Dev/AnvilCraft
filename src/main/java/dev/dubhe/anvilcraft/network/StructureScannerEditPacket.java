package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.StructureScannerMenu;
import dev.dubhe.anvilcraft.util.StructureScannerFiles;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** 客户端只提交编辑动作，蓝图内容由服务端修改并回传。 */
public record StructureScannerEditPacket(int containerId, UUID id, Action action, BlockPos pos, ItemStack replacement)
    implements IServerboundPacket {
    public enum Action {
        REPLACE, REPLACE_ALL, RESIZE, UNDO
    }

    public static final Type<StructureScannerEditPacket> TYPE = IPacket.type(AnvilCraft.of("structure_scanner_edit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StructureScannerEditPacket> STREAM_CODEC = StreamCodec.of(
        (buffer, packet) -> {
            buffer.writeVarInt(packet.containerId);
            buffer.writeUUID(packet.id);
            buffer.writeEnum(packet.action);
            buffer.writeBlockPos(packet.pos);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, packet.replacement);
        },
        buffer -> new StructureScannerEditPacket(buffer.readVarInt(), buffer.readUUID(), buffer.readEnum(Action.class),
            buffer.readBlockPos(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer))
    );

    @Override
    public Type<StructureScannerEditPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (player.isSpectator() || !(player instanceof ServerPlayer serverPlayer)
            || !(player.containerMenu instanceof StructureScannerMenu menu)
            || menu.containerId != this.containerId || !menu.stillValid(player)) {
            return;
        }
        StructureScannerFiles.edit(serverPlayer, menu, this);
    }
}
