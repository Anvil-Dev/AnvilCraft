package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.StructureScannerMenu;
import dev.dubhe.anvilcraft.util.StructureSaveUtil;
import dev.dubhe.anvilcraft.util.StructureScannerFiles;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;

public record StructureScannerSavePacket(int containerId, String name, boolean autoRotate, ItemStack marker)
    implements IServerboundPacket {
    public static final Type<StructureScannerSavePacket> TYPE = IPacket.type(AnvilCraft.of("structure_scanner_save"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StructureScannerSavePacket> STREAM_CODEC = StreamCodec.of(
        (buffer, packet) -> {
            buffer.writeVarInt(packet.containerId);
            buffer.writeUtf(packet.name, 32);
            buffer.writeBoolean(packet.autoRotate);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, packet.marker);
        },
        buffer -> new StructureScannerSavePacket(buffer.readVarInt(), buffer.readUtf(32), buffer.readBoolean(),
            ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer))
    );

    @Override
    public Type<StructureScannerSavePacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!(player.containerMenu instanceof StructureScannerMenu menu)
            || menu.containerId != this.containerId || !menu.stillValid(player)) return;
        var scanner = menu.getBlockEntity();
        if (scanner == null) return;
        if (menu.getImportedStructure() != null && player instanceof ServerPlayer serverPlayer) {
            try {
                StructureScannerFiles.saveImportedStructure(serverPlayer, menu, this.name, this.autoRotate, this.marker);
            } catch (IOException exception) {
                AnvilCraft.LOGGER.warn("Failed to record imported structure", exception);
                player.sendSystemMessage(Component.translatable("screen.anvilcraft.structure_scanner.file_failed", exception.toString()));
            }
        } else {
            StructureSaveUtil.saveStructureToDisk(player.level(), scanner, this.name, this.autoRotate, this.marker);
        }
        menu.broadcastChanges();
    }
}
