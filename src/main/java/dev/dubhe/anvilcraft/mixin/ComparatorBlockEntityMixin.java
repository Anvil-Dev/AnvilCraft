package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.injection.tooltip.ITooltipProviderExtension;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ComparatorBlockEntity.class)
public abstract class ComparatorBlockEntityMixin extends BlockEntity implements ITooltipProviderExtension {
    public ComparatorBlockEntityMixin(
        BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Shadow
    public abstract int getOutputSignal();

    @Shadow
    private int output;

    @Override
    public List<Component> anvilcraft$getTooltip() {
        final ArrayList<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("tooltip.anvilcraft.redstone.title").withStyle(ChatFormatting.BLUE));
        lines.add(Component.translatable("tooltip.anvilcraft.redstone.output_power", this.getOutputSignal())
            .withStyle(ChatFormatting.GRAY));

        Component mode = switch (this.getBlockState().getValue(ComparatorBlock.MODE)) {
            case COMPARE -> Component.translatable("tooltip.anvilcraft.redstone.output_mode.compare");
            case SUBTRACT -> Component.translatable("tooltip.anvilcraft.redstone.output_mode.subtract");
        };
        lines.add(Component.translatable("tooltip.anvilcraft.redstone.output_mode", mode).withStyle(ChatFormatting.GRAY));
        return lines;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("signal", this.getOutputSignal());
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Inject(method = "setOutputSignal", at = @At("HEAD"))
    private void sendChangesWhenChanged(int value, CallbackInfo ci) {
        if (this.output == value) return;
        this.setChanged();
        if (this.level == null) return;
        BlockState state = this.getBlockState();
        this.level.sendBlockUpdated(this.getBlockPos(), state, state, Block.UPDATE_CLIENTS);
    }
}
