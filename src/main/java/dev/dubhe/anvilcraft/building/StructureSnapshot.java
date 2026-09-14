package dev.dubhe.anvilcraft.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * 规范结构快照:所有导入格式统一转换成的内存表示,语义与原版结构 NBT 等价。
 * 方块按 Y、Z、X 排序,调色板按排序后首次出现顺序编号,实体按坐标稳定排序,
 * 因此同一内容无论来源如何都会产生相同的规范 NBT 与内容哈希。
 */
public record StructureSnapshot(
    Vec3i size,
    List<BlockState> palette,
    List<BlockEntry> blocks,
    List<EntityEntry> entities
) {
    public StructureSnapshot {
        palette = List.copyOf(palette);
        blocks = List.copyOf(blocks);
        entities = List.copyOf(entities);
    }

    /** 一个方块条目;nbt 为方块实体的完整数据(saveWithId 语义),无方块实体时为空。 */
    public record BlockEntry(BlockPos pos, int stateIndex, Optional<CompoundTag> nbt) {
    }

    /** 一个实体条目;字段语义与原版结构 entities 条目一致。 */
    public record EntityEntry(Vec3 pos, BlockPos blockPos, CompoundTag nbt) {
    }

    public BlockState stateOf(BlockEntry entry) {
        return this.palette.get(entry.stateIndex());
    }

    public boolean hasBlockEntities() {
        return this.blocks.stream().anyMatch(entry -> entry.nbt().isPresent());
    }

    public boolean hasEntities() {
        return !this.entities.isEmpty();
    }

    /** 非空气方块条目数,用于摘要显示与材料预估。 */
    public int nonAirBlockCount() {
        int count = 0;
        for (BlockEntry entry : this.blocks) {
            if (!this.stateOf(entry).isAir()) count++;
        }
        return count;
    }
}
