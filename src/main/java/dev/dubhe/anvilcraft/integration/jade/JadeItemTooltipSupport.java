package dev.dubhe.anvilcraft.integration.jade;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import snownee.jade.JadeClient;

import java.util.ArrayList;
import java.util.List;

public final class JadeItemTooltipSupport {
    private JadeItemTooltipSupport() {
    }

    public static List<Component> appendModName(ItemStack stack, List<Component> lines) {
        Component modName = JadeClient.appendModName(stack);
        if (modName == null || modName.getString().isEmpty()) return lines;
        for (int index = 1; index < lines.size(); index++) {
            if (lines.get(index).getString().equals(modName.getString())) return lines;
        }
        List<Component> result = new ArrayList<>(lines);
        result.add(modName);
        return result;
    }
}
