package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.injection.tooltip.IInjectedTooltipProducer;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

@MethodsReturnNonnullByDefault
@Mixin(RedStoneWireBlock.class)
public abstract class RedstoneWireBlockMixin implements IInjectedTooltipProducer {
    @Shadow @Final public static IntegerProperty POWER;

    @Override
    public List<Component> anvilcraft$getTooltip(BlockState state) {
        final ArrayList<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("tooltip.anvilcraft.redstone.title").withStyle(ChatFormatting.BLUE));
        lines.add(Component.translatable("tooltip.anvilcraft.redstone.power", state.getValue(POWER)));
        return lines;
    }
}
