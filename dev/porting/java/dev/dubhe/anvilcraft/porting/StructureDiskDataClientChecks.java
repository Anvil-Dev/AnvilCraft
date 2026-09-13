package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.StructureDiskPreviewSupport;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.StructureDiskData;
import dev.dubhe.anvilcraft.util.StructureLoadUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

public final class StructureDiskDataClientChecks {
    private static final StructureDiskData DATA = StructureDiskDataTests.disk("client_full");
    private static final StructureDiskData MISSING = StructureDiskDataTests.disk("client_missing");
    private static boolean started;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    private static boolean requested;
    private static boolean received;
    private static long startedAt;

    public static void frame(Minecraft client) {
        if (!started) {
            started = true;
            startedAt = System.currentTimeMillis();
            StructureDiskPreviewSupport.clearCache();
            client.getSingleplayerServer().execute(() -> {
                var directory = client.getSingleplayerServer().getWorldPath(LevelResource.ROOT).resolve("anvilcraft/structures");
                try {
                    Files.createDirectories(directory);
                    NbtIo.writeCompressed(StructureDiskDataTests.structure(), directory.resolve(DATA.file()));
                    prepared = true;
                } catch (IOException exception) {
                    failure = exception;
                }
            });
        }
        if (failure != null) throw new IllegalStateException("结构磁盘客户端检查失败", failure);
        if (System.currentTimeMillis() - startedAt > 30000) throw new IllegalStateException("结构磁盘网络同步超时");
        if (!prepared) return;
        if (!requested) {
            requested = true;
            if (StructureLoadUtil.getStructureNbtForPreview(client.level, DATA).isPresent()) {
                throw new IllegalStateException("首次客户端查询必须等待服务端数据，不可回退读取本地文件");
            }
            return;
        }
        if (!received) {
            var cached = StructureLoadUtil.getStructureNbtForPreview(client.level, DATA);
            if (cached.isEmpty()) return;
            CompoundTag tag = cached.orElseThrow();
            if (!tag.equals(StructureDiskDataTests.structure())) throw new IllegalStateException("完整结构网络数据不一致");
            var stack = ModItems.STRUCTURE_DISK.asStack();
            stack.set(ModComponents.STRUCTURE_DISK_DATA, DATA);
            var preview = StructureLoadUtil.loadStructureFromDiskForPreview(client.level, stack);
            if (preview == null || preview.blocks.size() != 5000) throw new IllegalStateException("客户端结构预览被截断");
            received = true;
        }
        StructureLoadUtil.getStructureNbtForPreview(client.level, MISSING);
        if (!map("MISSING_STRUCTURE_FILES").containsKey(MISSING.file())) return;
        StructureLoadUtil.getStructureNbtForPreview(client.level, MISSING);
        if (map("LAST_REQUEST_TIME").containsKey(MISSING.file())) throw new IllegalStateException("缺失响应后不应再次发送请求");
        StructureDiskPreviewSupport.clearCache();
        if (!map("MISSING_STRUCTURE_FILES").isEmpty() || !map("STRUCTURE_NBT_CACHE").isEmpty()
            || !map("LAST_REQUEST_TIME").isEmpty()) throw new IllegalStateException("会话缓存清理不完整");
        AnvilCraft.LOGGER.info("PORT_STRUCTURE_DISK_DATA_CLIENT_PASSED: full 5000 blocks, block NBT, missing response, session cache");
        client.stop();
    }

    private static Map<?, ?> map(String name) {
        try {
            var field = StructureLoadUtil.class.getDeclaredField(name);
            field.setAccessible(true);
            return (Map<?, ?>) field.get(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
