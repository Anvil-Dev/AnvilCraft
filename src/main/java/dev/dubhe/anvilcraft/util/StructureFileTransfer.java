package dev.dubhe.anvilcraft.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

/** 顺序分块传输，每个菜单或客户端请求只保留一份有大小及超时限制的缓冲。 */
public final class StructureFileTransfer {
    public static final int CHUNK_BYTES = 24 * 1024;
    public static final int MAX_BYTES = 2 * 1024 * 1024;
    private final UUID id;
    private final String name;
    private final int total;
    private final long started = System.nanoTime();
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

    public StructureFileTransfer(UUID id, String name, int total) throws IOException {
        if (total <= 0 || total > MAX_BYTES) throw new IOException("Invalid file size");
        this.id = id;
        this.name = name;
        this.total = total;
    }

    public boolean append(UUID transferId, String fileName, int totalBytes, int offset, byte[] chunk) throws IOException {
        if (!this.id.equals(transferId) || !this.name.equals(fileName) || this.total != totalBytes
            || System.nanoTime() - this.started > 60_000_000_000L || offset != this.bytes.size()
            || chunk.length != Math.min(CHUNK_BYTES, this.total - offset)) {
            throw new IOException("Invalid file chunk");
        }
        this.bytes.write(chunk);
        return this.bytes.size() == this.total;
    }

    public byte[] finish() throws IOException {
        if (this.bytes.size() != this.total) throw new IOException("Incomplete file");
        return this.bytes.toByteArray();
    }

    public static boolean isSafeName(String name) {
        if (name.isBlank() || name.length() > 128 || name.equals(".") || name.equals("..")
            || name.endsWith(".") || name.endsWith(" ")) return false;
        return name.chars().noneMatch(character -> character < 32 || "<>:\"/\\|?*".indexOf(character) >= 0);
    }
}
