package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.util.HandbookStructureExporter;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class HandbookStructureExportHandler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HandbookStructureExportHandler.class);
    private static final Map<UUID, Transfer> TRANSFERS = new HashMap<>();
    private static final Map<UUID, Long> LAST_REQUEST = new HashMap<>();
    private static final long TIMEOUT_NANOS = 30000000000L;

    private HandbookStructureExportHandler() {
    }

    public static void handle(HandbookStructureExportPacket payload, ServerPlayer player) {
        UUID id = player.getUUID();
        long now = System.nanoTime();
        if (payload.offset() == 0) {
            Long last = LAST_REQUEST.get(id);
            if (last != null && now - last < 2000000000L) {
                player.sendSystemMessage(Component.translatable("system.ageratum.structure_export.busy"));
                return;
            }
            LAST_REQUEST.put(id, now);
            TRANSFERS.remove(id);
            if (payload.totalBytes() <= 0 || payload.totalBytes() > HandbookStructureExportPacket.MAX_BYTES
                || payload.location().toString().length() > 256) {
                fail(player);
                return;
            }
            TRANSFERS.put(id, new Transfer(payload.location(), payload.totalBytes(), now));
        }
        Transfer transfer = TRANSFERS.get(id);
        if (transfer == null) return;
        if (now - transfer.started > TIMEOUT_NANOS || !transfer.location.equals(payload.location())
            || transfer.totalBytes != payload.totalBytes() || transfer.bytes.size() != payload.offset()
            || payload.data().length == 0 || payload.data().length > HandbookStructureExportPacket.CHUNK_BYTES
            || payload.data().length > transfer.totalBytes - transfer.bytes.size()) {
            TRANSFERS.remove(id);
            fail(player);
            return;
        }
        transfer.bytes.writeBytes(payload.data());
        if (transfer.bytes.size() != transfer.totalBytes) return;
        TRANSFERS.remove(id);
        try {
            var root = HandbookStructureExporter.read(transfer.bytes.toByteArray());
            var output = HandbookStructureExporter.write(
                player.level().getServer().getWorldPath(LevelResource.ROOT), transfer.location, root);
            player.sendSystemMessage(Component.translatable(
                "system.ageratum.structure_export.success", "data/ageratum/" + output.getFileName()));
        } catch (Exception exception) {
            log.warn("Failed to export structure {} for {}", transfer.location, player.getGameProfile().name(), exception);
            fail(player);
        }
    }

    private static void fail(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("system.ageratum.structure_export.failed"));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        TRANSFERS.remove(event.getEntity().getUUID());
        LAST_REQUEST.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        long now = System.nanoTime();
        TRANSFERS.entrySet().removeIf(entry -> now - entry.getValue().started > TIMEOUT_NANOS);
        LAST_REQUEST.entrySet().removeIf(entry -> now - entry.getValue() > TIMEOUT_NANOS);
    }

    private static final class Transfer {
        private final Identifier location;
        private final int totalBytes;
        private final long started;
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        private Transfer(Identifier location, int totalBytes, long started) {
            this.location = location;
            this.totalBytes = totalBytes;
            this.started = started;
        }
    }
}
