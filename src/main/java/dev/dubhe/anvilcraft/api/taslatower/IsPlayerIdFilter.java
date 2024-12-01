package dev.dubhe.anvilcraft.api.taslatower;

import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class IsPlayerIdFilter extends TeslaFilter{
    @Getter
    private final String id = "IsPlayerIdFilter";

    @Override
    public boolean match(LivingEntity entity, String arg) {
        return entity instanceof Player player && player.getName().getString().equals(arg);
    }

    @Override
    public boolean needArg() { return true; }

    @Override
    public Component title() {
        return Component.translatable("screen.anvilcraft.tesla_tower.filter.is_player_id");
    }

    @Override
    public String tooltip(String arg) {
        return arg;
    }
}
