package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.SliderMenu;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
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

import java.util.Objects;
import javax.annotation.Nullable;

@Getter
public class CreativeGeneratorBlockEntity extends BlockEntity implements IPowerProducer, IPowerConsumer, MenuProvider {
    private @Nullable PowerGrid grid = null;

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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("power", power);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.power = tag.getInt("power");
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
        return new SliderMenu(i, this::setPower);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.getBlockPos());
    }

    public void setPower(int power) {
        this.power = power;
        if (level instanceof ServerLevel) {
            if (grid != null) {
                this.grid.markChanged();
                return;
            }
            previousSyncFailed = true;
        }
    }

    public void tick() {
        if (level instanceof ServerLevel) {
            if (previousSyncFailed && grid != null) {
                previousSyncFailed = false;
                grid.markChanged();
            }
        }
        time++;
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
