package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.injection.tooltip.IInjectedTooltipProducer;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

@MethodsReturnNonnullByDefault
@Mixin(ComparatorBlockEntity.class)
public abstract class ComparatorBlockEntityMixin extends BlockEntity implements IInjectedTooltipProducer {
    public ComparatorBlockEntityMixin(
        BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Shadow
    public abstract int getOutputSignal();

    @Override
    public List<Component> anvilcraft$getTooltip() {
        final ArrayList<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("tooltip.anvilcraft.redstone.title").withStyle(ChatFormatting.BLUE));
        lines.add(Component.translatable("tooltip.anvilcraft.redstone.output_power", this.getOutputSignal()));

        Component mode = switch (this.getBlockState().getValue(ComparatorBlock.MODE)) {
            case COMPARE -> Component.translatable("tooltip.anvilcraft.redstone.output_mode.compare");
            case SUBTRACT -> Component.translatable("tooltip.anvilcraft.redstone.output_mode.subtract");
        };
        lines.add(Component.translatable("tooltip.anvilcraft.redstone.output_mode", mode));
        return lines;
    }
}
