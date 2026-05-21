package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerComponentInfo;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.SliderMenu;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@Getter
public class CreativeGeneratorBlockEntity extends BlockEntity implements IPowerProducer, IPowerConsumer, MenuProvider {
    private PowerGrid grid = null;

    private int power = 16;

    private int time = 0;
    private boolean previousSyncFailed = false;

    public static CreativeGeneratorBlockEntity createBlockEntity(
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("power", this.power);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.power = input.getIntOr("power", 0);
    }

    @Override
    public int getOutputPower() {
        return Math.max(this.power, 0);
    }

    @Override
    public int getInputPower() {
        return this.power < 0 ? -this.power : 0;
    }

    @Override
    public PowerComponentType getComponentType() {
        return this.power > 0 ? PowerComponentType.PRODUCER : PowerComponentType.CONSUMER;
    }

    @Override
    public PowerComponentInfo toPowerComponentInfo() {
        if (this.power >= 0) {
            return IPowerProducer.super.toPowerComponentInfo();
        }
        return IPowerConsumer.super.toPowerComponentInfo();
    }

    @Override
    public BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public void setGrid(@Nullable PowerGrid grid) {
        this.grid = grid;
    }

    @Override
    public Component getDisplayName() {
        return ModBlocks.CREATIVE_GENERATOR.get().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        if (player.isSpectator()) return null;
        return new SliderMenu(i, this::setPower);
    }

    public void setPower(int power) {
        this.power = power;
        if (level instanceof ServerLevel) {
            if (this.grid != null) {
                this.grid.markChanged();
                return;
            }
            this.previousSyncFailed = true;
        }
    }

    public void tick() {
        if (level instanceof ServerLevel) {
            if (this.previousSyncFailed && this.grid != null) {
                this.previousSyncFailed = false;
                this.grid.markChanged();
            }
        }
        this.time++;
    }

    @Override
    public Level getCurrentLevel() {
        return Objects.requireNonNull(super.getLevel());
    }

    @Override
    public int getRange() {
        return 2;
    }
}
