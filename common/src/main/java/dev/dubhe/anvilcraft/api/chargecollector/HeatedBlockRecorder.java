package dev.dubhe.anvilcraft.api.chargecollector;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class HeatedBlockRecorder {
    public static final HeatedBlockRecorder INSTANCE = new HeatedBlockRecorder();

    private final Map<BlockPos, AtomicInteger> record = new HashMap<>();

    /**
     * 记录方块照射
     */
    public void addOrIncrease(BlockPos pos) {
        if (!record.containsKey(pos)) {
            record.put(pos, new AtomicInteger(0));
        }
        record.get(pos).addAndGet(1);
    }

    /**
     * 移除方块照射
     */
    public void remove(BlockPos pos) {
        if (!record.containsKey(pos)) {
            record.put(pos, new AtomicInteger(0));
            return;
        }
        record.get(pos).decrementAndGet();
    }

    public void delete(BlockPos pos) {
        record.remove(pos);
    }

    /**
     * 检查光照强度
     */
    public boolean requireLightLevel(BlockPos pos, int level) {
        if (!record.containsKey(pos)) {
            return false;
        }
        return record.get(pos).get() >= level;
    }

    public void clear() {
        record.clear();
    }

}
