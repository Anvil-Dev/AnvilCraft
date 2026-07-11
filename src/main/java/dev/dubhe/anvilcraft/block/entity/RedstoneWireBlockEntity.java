package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.injection.tooltip.ITooltipProviderExtension;
import dev.dubhe.anvilcraft.block.RedstoneWireBlock;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

@Getter
public class RedstoneWireBlockEntity extends BlockEntity implements ITooltipProviderExtension {
    private int nonDustPower;

    public RedstoneWireBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean setNonDustPower(int power) {
        if (this.nonDustPower == power) {
            return false;
        }
        this.nonDustPower = power;
        this.setChanged();
        return true;
    }

    @Override
    public List<Component> anvilcraft$getTooltip() {
        return List.of(
            Component.translatable("tooltip.anvilcraft.redstone.title").withStyle(ChatFormatting.BLUE),
            Component.translatable(
                "tooltip.anvilcraft.redstone.power",
                this.getBlockState().getValue(RedstoneWireBlock.POWER)
            ).withStyle(ChatFormatting.GRAY),
            Component.translatable("tooltip.anvilcraft.redstone.output_to_redstone", this.nonDustPower)
                .withStyle(ChatFormatting.GRAY)
        );
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        tag.putInt("NonDustPower", this.nonDustPower);
        return tag;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("NonDustPower", this.nonDustPower);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.nonDustPower = tag.getInt("NonDustPower");
    }
}
