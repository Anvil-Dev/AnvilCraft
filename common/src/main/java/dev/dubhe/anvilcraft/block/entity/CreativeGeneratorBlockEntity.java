package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerComponentInfo;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.power.SimplePowerGrid;
import dev.dubhe.anvilcraft.client.renderer.PowerGridRenderer;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.inventory.SliderMenu;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Getter
public class CreativeGeneratorBlockEntity extends BlockEntity implements IPowerProducer, IPowerConsumer, MenuProvider {
    private PowerGrid grid = null;
    @Setter
    private int power = 16;
    private int time = 0;

    public static @NotNull CreativeGeneratorBlockEntity createBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState blockState
    ) {
        return new CreativeGeneratorBlockEntity(type, pos, blockState);
    }

    public CreativeGeneratorBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.CREATIVE_GENERATOR.get(), pos, blockState);
    }

    private CreativeGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("power", power);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        this.power = tag.getInt("power");
    }

    @Override
    public int getOutputPower() {
        return this.power > 0 ? this.power : 0;
    }

    @Override
    public int getInputPower() {
        return this.power < 0 ? -this.power : 0;
    }

    @Override
    public @NotNull PowerComponentType getComponentType() {
        return this.power > 0 ? PowerComponentType.PRODUCER : PowerComponentType.CONSUMER;
    }

    @Override
    public @NotNull BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public void setGrid(PowerGrid grid) {
        this.grid = grid;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return ModBlocks.CREATIVE_GENERATOR.get().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player) {
        return new SliderMenu(i, -8192, 8192, this::setPower);
    }

    public void tick() {
        time++;
    }

    @Override
    public Level getCurrentLevel() {
        return super.getLevel();
    }

    @Override
    public int getRange() {
        return 2;
    }

    /**
     * 实际电量
     */
    public int getServerPower() {
        Optional<SimplePowerGrid> s = SimplePowerGrid.findPowerGrid(getPos())
                .stream()
                .findAny();
        if (s.isPresent()) {
            if (s.get().getConsume() > s.get().getGenerate()) {
                return 0;
            }
            Optional<PowerComponentInfo> info = s.get().getInfoForPos(getBlockPos());
            return info.map(powerComponentInfo -> powerComponentInfo.type() == PowerComponentType.PRODUCER
                            ? powerComponentInfo.produces()
                            : powerComponentInfo.consumes())
                    .orElse(1);
        } else {
            return Math.abs(this.power);
        }
    }
}
