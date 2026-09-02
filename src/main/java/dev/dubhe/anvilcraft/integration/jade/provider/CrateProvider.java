package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.container.storage.CrateBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;

/**
 * 板条箱的 Jade 显示名：相邻虚空物质（dispose=true）时显示
 * 「溢出销毁板条箱」，否则保留 Jade 核心按方块名渲染的默认名称行。
 */
public class CrateProvider implements IBlockComponentProvider {
    public static final CrateProvider INSTANCE = new CrateProvider();

    private CrateProvider() {
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockState().getBlock() instanceof CrateBlock)) {
            return;
        }
        if (!accessor.getBlockState().getValue(CrateBlock.DISPOSE)) {
            return;
        }
        tooltip.replace(JadeIds.CORE_OBJECT_NAME, CrateBlock.displayName(accessor.getBlockState()).copy().withStyle(ChatFormatting.WHITE));
    }

    @Override
    public ResourceLocation getUid() {
        return AnvilCraft.of("crate");
    }
}
