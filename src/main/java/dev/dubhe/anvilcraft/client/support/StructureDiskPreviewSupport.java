package dev.dubhe.anvilcraft.client.support;

import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.StructureDiskData;
import dev.dubhe.anvilcraft.util.LevelLike;
import dev.dubhe.anvilcraft.util.StructureLoadUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 结构磁盘预览支持类
 * 管理结构磁盘的缓存和3D预览渲染。
 *
 * <p>缓存策略（会话级）：</p>
 * <ul>
 *   <li>完整缓存 {@link #PREVIEW_CACHE} — 解析完毕的 LevelLike，本次游戏内永不过期，
 *       仅在超过 {@link #MAX_CACHE_SIZE} 时淘汰最旧条目</li>
 *   <li>待处理缓存 {@link #PENDING_PREVIEW_DATA} — 服务端返回的原始 NBT，
 *       等待 tooltip 渲染时获取磁盘上下文后完成解析</li>
 *   <li>完整结构文件请求与缺失状态由 {@link StructureLoadUtil} 统一缓存和节流</li>
 * </ul>
 */
public class StructureDiskPreviewSupport {
    private static final List<PreviewHandler> PREVIEW_HANDLERS = new CopyOnWriteArrayList<>();

    /** 返回 true 表示接管预览，仅客户端调用。 */
    public static void registerPreviewHandler(PreviewHandler handler) {
        PREVIEW_HANDLERS.add(handler);
    }

    @FunctionalInterface
    public interface PreviewHandler {
        boolean render(GuiGraphicsExtractor graphics, ItemStack stack, int mouseX, int mouseY);
    }

    private static final int PREVIEW_SIZE = 80;

    /**
     * 完整预览缓存（UUID → LevelLike），会话级，永不超时
     */
    private static final Map<UUID, PreviewCache> PREVIEW_CACHE = new HashMap<>();

    /**
     * 最大缓存条目数（防止内存泄漏）
     */
    private static final int MAX_CACHE_SIZE = 100;

    /**
     * 服务端返回的原始NBT预览数据（等待构建LevelLike）
     */
    private static final Map<UUID, CompoundTag> PENDING_PREVIEW_DATA = new HashMap<>();

    private record PreviewCache(
        StructureLoadUtil.StructureData structureData,
        LevelLike levelLike,
        long creationTime
    ) {
        PreviewCache(StructureLoadUtil.StructureData structureData, LevelLike levelLike) {
            this(structureData, levelLike, System.currentTimeMillis());
        }
    }

    /**
     * 在指定位置渲染预览
     */
    public static void renderPreviewAt(GuiGraphicsExtractor graphics, ItemStack diskStack, int mouseX, int mouseY) {
        for (PreviewHandler handler : PREVIEW_HANDLERS) {
            if (handler.render(graphics, diskStack, mouseX, mouseY)) return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        PreviewCache cache = StructureDiskPreviewSupport.getOrCreateCache(diskStack, minecraft.level);
        if (cache == null || cache.structureData.isEmpty()) return;

        int previewX = mouseX - StructureDiskPreviewSupport.PREVIEW_SIZE / 2;
        int previewY = mouseY - StructureDiskPreviewSupport.PREVIEW_SIZE - 16;

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();

        if (previewY < 0) {
            previewY = mouseY + 30;
        }

        if (previewX + StructureDiskPreviewSupport.PREVIEW_SIZE > screenWidth) {
            previewX = screenWidth - StructureDiskPreviewSupport.PREVIEW_SIZE - 5;
        }
        if (previewX < 0) {
            previewX = 5;
        }

        graphics.fill(
            previewX - 2, previewY - 2, previewX + StructureDiskPreviewSupport.PREVIEW_SIZE + 2,
            previewY + StructureDiskPreviewSupport.PREVIEW_SIZE + 2, 0xF0100010
        );

        graphics.fill(previewX - 2, previewY - 2, previewX + PREVIEW_SIZE + 2, previewY - 1, 0x505000ff);
        graphics.fill(previewX - 2, previewY + PREVIEW_SIZE + 1, previewX + PREVIEW_SIZE + 2, previewY + PREVIEW_SIZE + 2, 0x505000ff);
        graphics.fill(previewX - 2, previewY - 1, previewX - 1, previewY + PREVIEW_SIZE + 1, 0x505000ff);
        graphics.fill(previewX + PREVIEW_SIZE + 1, previewY - 1, previewX + PREVIEW_SIZE + 2, previewY + PREVIEW_SIZE + 1, 0x505000ff);

        StructureDiskData diskData = diskStack.get(ModComponents.STRUCTURE_DISK_DATA);
        RenderSupport.renderLevelLikeAt(cache.levelLike, graphics,
            previewX + PREVIEW_SIZE / 2, previewY + PREVIEW_SIZE / 2, 60,
            diskData == null || diskData.autoRotate() ? 2 : 0, PREVIEW_SIZE, AnvilCraftClient.CONFIG.renderScanPreviewEffect);
    }

    /**
     * 接收服务端返回的结构预览NBT数据（由 StructurePreviewResponsePacket 调用）
     * 存储原始数据，待 tooltip 渲染时再解析为 LevelLike。
     */
    public static void receiveStructureData(UUID structureUuid, CompoundTag structureData) {
        StructureDiskPreviewSupport.PENDING_PREVIEW_DATA.put(structureUuid, structureData);
    }

    /**
     * 获取或创建预览缓存
     */
    @Nullable
    private static PreviewCache getOrCreateCache(ItemStack diskStack, ClientLevel level) {
        StructureDiskData diskData = diskStack.get(ModComponents.STRUCTURE_DISK_DATA);
        if (diskData == null) return null;

        UUID uuid = diskData.uuid();

        // 1. 命中完整缓存 — 直接返回，永不过期
        PreviewCache cache = StructureDiskPreviewSupport.PREVIEW_CACHE.get(uuid);
        if (cache != null) {
            return cache;
        }

        // 2. 检查是否有服务端返回的 NBT 待处理数据
        CompoundTag pendingData = StructureDiskPreviewSupport.PENDING_PREVIEW_DATA.get(uuid);
        if (pendingData != null) {
            StructureLoadUtil.StructureData data = StructureDiskPreviewSupport.parsePreviewNbt(
                pendingData, diskData, level.registryAccess());
            if (data != null && !data.isEmpty()) {
                LevelLike levelLike = StructureDiskPreviewSupport.buildLevelLike(data);
                if (levelLike != null) {
                    cache = new PreviewCache(data, levelLike);
                    StructureDiskPreviewSupport.PREVIEW_CACHE.put(uuid, cache);
                    StructureDiskPreviewSupport.PENDING_PREVIEW_DATA.remove(uuid);
                    StructureDiskPreviewSupport.evictIfNeeded();
                    return cache;
                }
            }
            // 解析失败，清理待处理数据，后续会重新请求
            StructureDiskPreviewSupport.PENDING_PREVIEW_DATA.remove(uuid);
            return null;
        }

        // 3. 读取服务端同步的完整结构；首次访问由共享缓存发起请求。
        StructureLoadUtil.StructureData localData = StructureLoadUtil.loadStructureFromDiskForPreview(level, diskStack);
        if (localData != null && !localData.isEmpty()) {
            LevelLike levelLike = StructureDiskPreviewSupport.buildLevelLike(localData);
            if (levelLike != null) {
                cache = new PreviewCache(localData, levelLike);
                StructureDiskPreviewSupport.PREVIEW_CACHE.put(uuid, cache);
                StructureDiskPreviewSupport.evictIfNeeded();
                return cache;
            }
        }

        return null;
    }

    public static void clearCache() {
        PREVIEW_CACHE.clear();
        PENDING_PREVIEW_DATA.clear();
        StructureLoadUtil.clearClientStructureCache();
    }

    /**
     * 从NBT数据解析为 StructureData
     */
    private static StructureLoadUtil.@Nullable StructureData parsePreviewNbt(
        CompoundTag tag,
        StructureDiskData diskData,
        HolderLookup.Provider registry
    ) {
        ListTag paletteTag = tag.getListOrEmpty("palette");
        ListTag blocksTag = tag.getListOrEmpty("blocks");
        if (paletteTag.isEmpty() || blocksTag.isEmpty()) return null;

        var blockLookup = registry.lookupOrThrow(Registries.BLOCK);
        List<BlockState> palette = new ArrayList<>();
        for (int i = 0; i < paletteTag.size(); i++) {
            BlockState state = NbtUtils.readBlockState(blockLookup, paletteTag.getCompoundOrEmpty(i));
            palette.add(state);
        }
        if (palette.isEmpty()) return null;

        StructureLoadUtil.StructureData result = new StructureLoadUtil.StructureData(diskData);
        for (int i = 0; i < blocksTag.size(); i++) {
            CompoundTag blockTag = blocksTag.getCompoundOrEmpty(i);
            ListTag posTag = blockTag.getListOrEmpty("pos");
            if (posTag.size() < 3) continue;

            int x = posTag.getInt(0).orElse(0);
            int y = posTag.getInt(1).orElse(0);
            int z = posTag.getInt(2).orElse(0);
            int stateIndex = blockTag.getInt("state").orElse(-1);

            if (stateIndex >= 0 && stateIndex < palette.size()) {
                result.blocks.add(new StructureLoadUtil.BlockPosition(x, y, z, palette.get(stateIndex)));
            }
        }

        return result;
    }

    @Nullable
    private static LevelLike buildLevelLike(StructureLoadUtil.StructureData data) {
        if (data.isEmpty()) return null;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return null;

        LevelLike levelLike = new LevelLike(minecraft.level);

        Rotation rotation = switch (data.diskData.direction()) {
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.CLOCKWISE_90;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
        for (StructureLoadUtil.BlockPosition block : data.blocks) {
            int y = data.diskData.upsideDown() ? data.diskData.sizeY() - 1 - block.y() : block.y();
            levelLike.setBlockState(new BlockPos(block.x(), y, block.z()), block.state().rotate(rotation));
        }

        return levelLike;
    }

    /**
     * 缓存超过上限时淘汰最旧条目
     */
    private static void evictIfNeeded() {
        if (StructureDiskPreviewSupport.PREVIEW_CACHE.size() <= StructureDiskPreviewSupport.MAX_CACHE_SIZE) return;

        StructureDiskPreviewSupport.PREVIEW_CACHE.entrySet()
            .stream()
            .sorted(Comparator.comparingLong(e -> e.getValue().creationTime))
            .limit(StructureDiskPreviewSupport.PREVIEW_CACHE.size() - StructureDiskPreviewSupport.MAX_CACHE_SIZE)
            .forEach(e -> StructureDiskPreviewSupport.PREVIEW_CACHE.remove(e.getKey()));
    }
}
