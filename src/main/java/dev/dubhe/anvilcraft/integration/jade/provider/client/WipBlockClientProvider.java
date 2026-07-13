package dev.dubhe.anvilcraft.integration.jade.provider.client;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum WipBlockClientProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag serverData = accessor.getServerData();
        if (serverData.contains("recipe")) {
            String s = serverData.getStringOr("recipe", "");
            if (!s.isEmpty()) {
                Identifier rl = Identifier.parse(s);
                if (!rl.getPath().isEmpty()) {
                    String s1 = rl.getPath();
                    String[] splits = s1.split("/");
                    String s2 = splits[splits.length - 1];
                    tooltip.add(
                        Component.translatable(
                            "tooltip.anvilcraft.wip_block.jade.recipe",
                            s2
                        )
                    );
                }
            }
        }
        if (serverData.contains("stepCount")) {
            tooltip.add(Component.translatable(
                "tooltip.anvilcraft.wip_block.jade.step_count",
                serverData.getIntOr("stepCount", 0)
            ));
        }
    }

    @Override
    public Identifier getUid() {
        return AnvilCraft.of("wip_block");
    }
}
