package dev.dubhe.anvilcraft.integration.jade.provider.client;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
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
        if (serverData.contains("stepCount")) {
            tooltip.add(Component.translatable(
                "tooltip.anvilcraft.wip_block.jade.step_count",
                serverData.getIntOr("stepCount", 0)
            ));
        }
        if (serverData.contains("nextStep")) {
            Identifier typeId = Identifier.parse(serverData.getStringOr("nextStep", ""));
            tooltip.add(Component.translatable(
                "tooltip.anvilcraft.wip_block.jade.next_step",
                Component.translatableWithFallback(
                    "gui." + typeId.getNamespace() + ".category." + typeId.getPath(), typeId.toString()
                )
            ));
        }
        if (serverData.contains("nextStepDetails")) {
            ComponentSerialization.CODEC.listOf().parse(
                accessor.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE), serverData.get("nextStepDetails")
            ).result().ifPresent(lines -> lines.forEach(tooltip::add));
        }
    }

    @Override
    public Identifier getUid() {
        return AnvilCraft.of("wip_block");
    }
}
