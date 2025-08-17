package dev.dubhe.anvilcraft.api.tooltip.providers;

import com.google.errorprone.annotations.DoNotCall;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/**
 * 头戴铁砧锤时显示的tooltip
 */
public interface ITooltipProvider<T> {
    MutableComponent INDENTATION = Component.literal("  ");

    boolean accepts(T value);

    List<Component> tooltip(T value);

    ItemStack icon(T value);

    int priority();

    abstract class BlockTooltipProvider implements ITooltipProvider<Triple<Level, BlockPos, BlockState>> {
        public BlockTooltipProvider() {
        }

        @ApiStatus.OverrideOnly
        public abstract boolean accepts(Level level, BlockPos pos, BlockState value);

        public boolean accepts(Triple<Level, BlockPos, BlockState> value) {
            return accepts(value.getLeft(), value.getMiddle(), value.getRight());
        }

        @DoNotCall
        @Override
        public List<Component> tooltip(Triple<Level, BlockPos, BlockState> value) {
            return tooltip(value.getLeft(), value.getMiddle(), value.getRight());
        }

        public abstract List<Component> tooltip(Level level, BlockPos pos, BlockState value);

        @DoNotCall
        @Override
        public ItemStack icon(Triple<Level, BlockPos, BlockState> value) {
            return icon(value.getLeft(), value.getMiddle(), value.getRight());
        }

        public ItemStack icon(Level level, BlockPos pos, BlockState value) {
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

    static Component withIndentAndMerge(Component... components) {
        MutableComponent indentation = INDENTATION.copy();
        for (Component component : components) {
            indentation.append(component);
        }
        return indentation;
    }
}
