package dev.dubhe.anvilcraft.api.tooltip.providers;

import com.google.errorprone.annotations.DoNotCall;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/**
 * 头戴铁砧锤时显示的tooltip
 */
public interface ITooltipProvider<T> {
    boolean accepts(T value);

    List<Component> tooltip(T value);

    ItemStack icon(T value);

    int priority();

    abstract class BlockTooltipProvider implements ITooltipProvider<Block> {
        public BlockTooltipProvider() {
        }

        @ApiStatus.OverrideOnly
        public abstract boolean accepts(Block value);

        public boolean accepts(BlockState value) {
            return accepts(value.getBlock());
        }

        @DoNotCall
        @Override
        public List<Component> tooltip(Block value) {
            return tooltip(value.defaultBlockState());
        }

        public abstract List<Component> tooltip(BlockState value);

        @DoNotCall
        @Override
        public ItemStack icon(Block value) {
            return value.asItem().getDefaultInstance();
        }

        public ItemStack icon(BlockState value) {
            return value.getBlock().asItem().getDefaultInstance();
        }

        public abstract int priority();
    }

    abstract class BlockEntityTooltipProvider implements ITooltipProvider<BlockEntity> {
        public BlockEntityTooltipProvider() {
        }

        public abstract boolean accepts(BlockEntity value);

        public abstract List<Component> tooltip(BlockEntity value);

        @Override
        public ItemStack icon(BlockEntity value) {
            return value.getBlockState().getBlock().asItem().getDefaultInstance();
        }

        public abstract int priority();
    }
}
