package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.FluidTankBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;

/**
 * 流体储罐的 Jade 显示名：相邻门格海绵（溢出销毁模式）时显示「过量销毁储罐」，
 * 否则保留 Jade 核心按方块名渲染的默认名称行。
 */
public class OverflowDisposalTankProvider implements IBlockComponentProvider {
    public static final OverflowDisposalTankProvider INSTANCE = new OverflowDisposalTankProvider();

    private OverflowDisposalTankProvider() {
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockState().getBlock() instanceof FluidTankBlock)) {
            return;
        }
        if (!FluidTankBlock.hasAdjacentMengerSponge(accessor.getLevel(), accessor.getPosition())) {
            return;
        }
        tooltip.replace(
            JadeIds.CORE_OBJECT_NAME,
            FluidTankBlock.displayName(accessor.getLevel(), accessor.getPosition())
                .copy()
                .withStyle(ChatFormatting.WHITE)
        );
    }

    @Override
    public ResourceLocation getUid() {
        return AnvilCraft.of("overflow_disposal_fluid_tank");
    }
}
