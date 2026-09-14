package dev.dubhe.anvilcraft.client.building;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.building.BlueprintUploadTracker;
import dev.dubhe.anvilcraft.network.BlueprintFileUploadPacket;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 客户端蓝图文件目录:{@code <gameDir>/anvilcraft/structures/}。
 * 扫描、大小校验与分块上传都在这里完成;扩展名清单含原版/Create 的 .nbt 与 Litematica 的 .litematic。
 */
public final class BlueprintClientFiles {
    private static final Pattern SAFE_FILE_NAME = Pattern.compile("[^/\\\\]{1,128}");
    private static final List<String> IMPORTABLE_EXTENSIONS = List.of(".nbt", ".litematic");

    private BlueprintClientFiles() {
    }

    public static Path importRoot() throws IOException {
        Path root = Minecraft.getInstance().gameDirectory.toPath()
            .resolve("anvilcraft")
            .resolve("structures")
            .toAbsolutePath()
            .normalize();
        Files.createDirectories(root);
        return root;
    }

    public static void openFolder() {
        try {
            Util.getPlatform().openPath(importRoot());
        } catch (IOException exception) {
            AnvilCraft.LOGGER.warn("Cannot access blueprint import directory", exception);
        }
    }

    /** 列出可导入文件名,按名称排序;跳过符号链接、超限文件与不支持的扩展名。 */
    public static List<String> listImportableFiles() {
        List<String> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(importRoot())) {
            for (Path path : stream) {
                String name = path.getFileName().toString();
                if (!SAFE_FILE_NAME.matcher(name).matches()
                    || Files.isSymbolicLink(path)
                    || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    continue;
                }
                String lower = name.toLowerCase(Locale.ROOT);
                if (IMPORTABLE_EXTENSIONS.stream().noneMatch(lower::endsWith)) continue;
                try {
                    if (Files.size(path) > BlueprintUploadTracker.MAX_UPLOAD_BYTES) continue;
                } catch (IOException exception) {
                    continue;
                }
                files.add(name);
            }
        } catch (IOException exception) {
            AnvilCraft.LOGGER.warn("Cannot access blueprint import directory", exception);
        }
        files.sort(Comparator.naturalOrder());
        return files;
    }

    /** 读取文件并按分块上传到服务端;返回是否成功发出。 */
    public static boolean upload(String fileName, boolean autoRotate) {
        try {
            Path root = importRoot();
            Path file = root.resolve(fileName).toAbsolutePath().normalize();
            if (!file.getParent().equals(root)
                || Files.isSymbolicLink(file)
                || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
                return false;
            }
            if (Files.size(file) > BlueprintUploadTracker.MAX_UPLOAD_BYTES) return false;
            byte[] bytes = Files.readAllBytes(file);
            if (bytes.length == 0 || bytes.length > BlueprintUploadTracker.MAX_UPLOAD_BYTES) return false;
            UUID sessionId = UUID.randomUUID();
            int chunkSize = BlueprintFileUploadPacket.CHUNK_BYTES;
            int totalChunks = Math.ceilDiv(bytes.length, chunkSize);
            for (int index = 0; index < totalChunks; index++) {
                int from = index * chunkSize;
                int to = Math.min(bytes.length, from + chunkSize);
                PacketDistributor.sendToServer(new BlueprintFileUploadPacket(
                    sessionId,
                    fileName,
                    index,
                    totalChunks,
                    bytes.length,
                    autoRotate,
                    Arrays.copyOfRange(bytes, from, to)
                ));
            }
            return true;
        } catch (IOException exception) {
            return false;
        }
    }
}
