package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.TerminalBinding;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

public final class HyperdimensionTerminalItem extends TerminalItem {
    public HyperdimensionTerminalItem(Properties properties) {
        super(properties, Kind.HYPERDIMENSION);
    }

    @Override
    protected void playRemoveOneSound(Entity entity) {
        playSound(entity, SoundEvents.ENDERMAN_TELEPORT);
    }

    @Override
    protected void playInsertSound(Entity entity) {
        playSound(entity, SoundEvents.ENDERMAN_TELEPORT);
    }

    public static void bindToStation(ServerPlayer player, ItemStack stack, StorageBlockEntity entity) {
        if (stack.getOrDefault(ModComponents.TERMINAL_BINDING, TerminalBinding.EMPTY).id().isPresent()) return;
        UUID id = entity.getId();
        if (id == null) {
            id = UUID.randomUUID();
            entity.setId(id);
        }
        stack.set(ModComponents.TERMINAL_BINDING, new TerminalBinding(Optional.of(id)));
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        player.sendOverlayMessage(Component.translatable("message.anvilcraft.hyperdimension_terminal.bound"));
    }
}
