package dev.dubhe.anvilcraft.client.markdown;

import dev.dubhe.anvilcraft.network.HandbookStructureExportPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public final class HandbookStructureExportClient {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HandbookStructureExportClient.class);

    private HandbookStructureExportClient() {
    }

    public static void export(Identifier location, StructureTemplate template) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) return;
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            NbtIo.writeCompressed(template.save(new CompoundTag()), output);
            byte[] data = output.toByteArray();
            if (data.length > HandbookStructureExportPacket.MAX_BYTES) {
                minecraft.player.sendSystemMessage(Component.translatable("system.ageratum.structure_export.too_large"));
                return;
            }
            for (int offset = 0; offset < data.length; offset += HandbookStructureExportPacket.CHUNK_BYTES) {
                byte[] chunk = Arrays.copyOfRange(data, offset, Math.min(data.length, offset + HandbookStructureExportPacket.CHUNK_BYTES));
                ClientPacketDistributor.sendToServer(new HandbookStructureExportPacket(location, data.length, offset, chunk));
            }
            minecraft.player.sendSystemMessage(Component.translatable("system.ageratum.structure_export.sending"));
        } catch (Exception exception) {
            log.warn("Failed to send structure export {}", location, exception);
            minecraft.player.sendSystemMessage(Component.translatable("system.ageratum.structure_export.failed"));
        }
    }
}
