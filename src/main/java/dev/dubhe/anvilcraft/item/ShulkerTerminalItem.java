package dev.dubhe.anvilcraft.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;

public final class ShulkerTerminalItem extends TerminalItem {
    public ShulkerTerminalItem(Properties properties) {
        super(properties, Kind.SHULKER);
    }

    @Override
    protected void playRemoveOneSound(Entity entity) {
        playSound(entity, SoundEvents.SHULKER_BOX_OPEN);
    }

    @Override
    protected void playInsertSound(Entity entity) {
        playSound(entity, SoundEvents.SHULKER_BOX_CLOSE);
    }
}
