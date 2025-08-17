package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.injection.tooltip.IInjectedTooltipProvider;
import dev.dubhe.anvilcraft.network.ComparatorSyncPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(ComparatorBlockEntity.class)
public abstract class ComparatorBlockEntityMixin extends BlockEntity implements IInjectedTooltipProvider {
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
        lines.add(Component.translatable("tooltip.anvilcraft.redstone.output_power", this.getOutputSignal()).withStyle(ChatFormatting.GRAY));

        Component mode = switch (this.getBlockState().getValue(ComparatorBlock.MODE)) {
            case COMPARE -> Component.translatable("tooltip.anvilcraft.redstone.output_mode.compare");
            case SUBTRACT -> Component.translatable("tooltip.anvilcraft.redstone.output_mode.subtract");
        };
        lines.add(Component.translatable("tooltip.anvilcraft.redstone.output_mode", mode).withStyle(ChatFormatting.GRAY));
        return lines;
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void sendOutputToClient(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        if (!(this.level instanceof ServerLevel)) return;
        PacketDistributor.sendToAllPlayers(new ComparatorSyncPacket(this.worldPosition, this.output));
    }

    @Inject(method = "setOutputSignal", at = @At("HEAD"))
    private void sendChangesWhenChanged(int output, CallbackInfo ci) {
        if (this.output == output) return;
        if (!(this.level instanceof ServerLevel level1)) return;
        PacketDistributor.sendToPlayersInDimension(level1, new ComparatorSyncPacket(this.worldPosition, output));
    }
}
