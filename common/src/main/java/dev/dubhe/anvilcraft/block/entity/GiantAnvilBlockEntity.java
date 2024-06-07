package dev.dubhe.anvilcraft.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class GiantAnvilBlockEntity extends BlockEntity {
    protected ArrayList<BlockPos> blockGroups = new ArrayList<>();

    public GiantAnvilBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag nbt) {
        ListTag blocksTag = new ListTag();
        for (BlockPos pos : blockGroups) {
            CompoundTag blockTag = new CompoundTag();
            blockTag.putInt("X", pos.getX());
            blockTag.putInt("Y", pos.getY());
            blockTag.putInt("Z", pos.getZ());
            blocksTag.add(blockTag);
        }
        nbt.put("blocks", blocksTag);

        super.saveAdditional(nbt);
    }

    @Override
    public void load(@NotNull CompoundTag nbt) {
        super.load(nbt);

        Tag blocksTag = nbt.get("blocks");
        ArrayList<BlockPos> blocks = new ArrayList<>();
        if (blocksTag instanceof ListTag blocksListTag) {
            for (Tag blockTag : blocksListTag) {
                CompoundTag blockCompoundTag = (CompoundTag) blockTag;
                int x = blockCompoundTag.getInt("X");
                int y = blockCompoundTag.getInt("Y");
                int z = blockCompoundTag.getInt("Z");
                blocks.add(new BlockPos(x, y, z));
            }
        }
        blockGroups = blocks;
    }

    public ArrayList<BlockPos> getBlockGroups() {
        return blockGroups;
    }

    public void setBlockGroups(ArrayList<BlockPos> blockGroups) {
        this.blockGroups = blockGroups;
    }
}
