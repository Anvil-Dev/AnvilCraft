package dev.dubhe.anvilcraft.util;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockHighlightUtil {
    private static Level level = null;
    @Getter
    private static final Map<BlockPos, Integer> highlight = new ConcurrentHashMap<>();

    /**
     * 高亮方块
     *
     * @param level 维度
     * @param pos   位置
     */
    public static void highlightBlock(Level level, BlockPos pos) {
        if (BlockHighlightUtil.level == level) BlockHighlightUtil.highlight.put(pos, 400);
        else {
            BlockHighlightUtil.level = level;
            BlockHighlightUtil.highlight.clear();
            BlockHighlightUtil.highlight.put(pos, 400);
        }
        // TODO 高亮方块
    }

    /**
     * 获取被高亮的方块列表
     *
     * @param level 维度
     * @return 被高亮的方块列表
     */
    public static Map<BlockPos, Integer> getHighlight(Level level) {
        if (BlockHighlightUtil.level != level) BlockHighlightUtil.highlight.clear();
        return BlockHighlightUtil.highlight;
    }
}
