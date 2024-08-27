package dev.dubhe.anvilcraft.integration.wthit.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import mcp.mobius.waila.api.*;
import mcp.mobius.waila.api.component.BarComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

public enum PowerBlockProvider implements IBlockComponentProvider, IDataProvider<BlockEntity> {
    INSTANCE;

    @Override
    public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
        if(!config.getBoolean(AnvilCraft.of("power_provider"))) return;

        CompoundTag serverData = accessor.getData().raw();

        if (serverData.contains("generate") && serverData.contains("consume")) {
            int generate = serverData.getInt("generate");
            int consume = serverData.getInt("consume");
            int color;
            double warningPercent = config.getDouble(AnvilCraft.of("warning_percent"));
            float percent = (float) consume / generate;
            float viewPercent = percent;
            if (percent > 1){
                color = 0xFFFF0000;
                viewPercent = 1;
            } else if (percent < warningPercent) {
                color = 0xFF32CD32;
            } else {
                color = 0xFFFFD700;
            }

            tooltip.addLine(new BarComponent(viewPercent, color, Component.translatable("tooltip.anvilcraft.jade.power_information", consume, generate)));
        }
    }

    @Override
    public void appendData(IDataWriter data, IServerAccessor<BlockEntity> accessor, IPluginConfig config) {
        if(accessor.getTarget() instanceof IPowerComponent blockEntity){
            PowerGrid powerGrid = blockEntity.getGrid();
            if (powerGrid == null){
                return;
            }
            data.raw().putInt("generate", powerGrid.getGenerate());
            data.raw().putInt("consume", powerGrid.getConsume());
        }
    }
}
