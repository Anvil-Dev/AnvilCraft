package dev.dubhe.anvilcraft.api.power;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * 电力元件
 */
@SuppressWarnings("unused")
public interface IPowerComponent extends Comparable<IPowerComponent> {
    BooleanProperty OVERLOAD = BooleanProperty.create("overload");
    EnumProperty<Switch> SWITCH = EnumProperty.create("switch", Switch.class);

    @Nullable Level getCurrentLevel();

    default void gridTick() {
    }

    BlockPos getPos();

    default AABB getShape() {
        float range = getRange() * 2 + 1;
        return AABB.ofSize(getPos().getCenter(), range, range, range);
    }

    default int getRange() {
        return 0;
    }

    /**
     * 设置电网
     *
     * @param grid 电网
     */
    void setGrid(@Nullable PowerGrid grid);

    /**
     * 获取电网
     *
     * @return 电网
     */
    @Nullable PowerGrid getGrid();

    PowerComponentType getComponentType();

    enum Switch implements StringRepresentable {
        ON("on"),
        OFF("off");
        private final String name;

        Switch(String name) {
            this.name = name;
        }

        public String toString() {
            return this.getSerializedName();
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    default void flushState(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(OVERLOAD)) return;
        if (this.getGrid() == null) {
            if (!state.getValue(OVERLOAD)) {
                level.setBlockAndUpdate(pos, state.setValue(OVERLOAD, true));
            }
            return;
        }
        if (this.getGrid().isWorking() && state.getValue(OVERLOAD)) {
            level.setBlockAndUpdate(pos, state.setValue(OVERLOAD, false));
        } else if (!this.getGrid().isWorking() && !state.getValue(OVERLOAD)) {
            level.setBlockAndUpdate(pos, state.setValue(OVERLOAD, true));
        }
    }

    @Override
    default int compareTo(IPowerComponent powerComponent) {
        if (this.equals(powerComponent)) return 0;
        int i = getComponentType().compareTo(powerComponent.getComponentType());
        return i == 0 ? 1 : i;
    }

    default boolean isGridWorking() {
        return Optional.ofNullable(this.getGrid()).map(PowerGrid::isWorking).orElse(false);
    }

    default MutableComponent getCommandDiscription() {
        Block block = Optional.ofNullable(this.getCurrentLevel())
            .map(level -> level.getBlockState(this.getPos()).getBlock())
            .orElse(Blocks.AIR);
        int x = this.getPos().getX();
        int y = this.getPos().getY();
        int z = this.getPos().getZ();
        if (this.getComponentType() == PowerComponentType.PRODUCER && this instanceof IPowerProducer producer) {
            return Component.translatable("command.anvilcraft.powergrid.info.producer",
                block.getName(), x, y, z, producer.getOutputPower(), this.getRange()).withStyle(ChatFormatting.GREEN);
        } else if (this.getComponentType() == PowerComponentType.CONSUMER && this instanceof IPowerConsumer consumer) {
            return Component.translatable("command.anvilcraft.powergrid.info.consumer",
                block.getName(), x, y, z, consumer.getInputPower(), this.getRange()).withStyle(ChatFormatting.YELLOW);
        } else if (this.getComponentType() == PowerComponentType.TRANSMITTER) {
            return Component.translatable("command.anvilcraft.powergrid.info.transmitter",
                block.getName(), x, y, z, this.getRange()).withStyle(ChatFormatting.AQUA);
        }
        return Component.literal("");
    }
}
