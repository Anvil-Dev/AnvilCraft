package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.client.rpc.StorageTerminalClientStub;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.TerminalBinding;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class HyperdimensionTerminalItem extends TerminalItem {
    public HyperdimensionTerminalItem(Properties properties) {
        super(properties);
    }

    @Override
    protected void playRemoveOneSound(Entity entity) {
        BundleLikeItem.playSound(entity, SoundEvents.ENDERMAN_TELEPORT);
    }

    @Override
    protected void playInsertSound(Entity entity) {
        BundleLikeItem.playSound(entity, SoundEvents.ENDERMAN_TELEPORT);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide()) {
            HyperdimensionTerminalItem.openBoundStorage(player, stack);
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
        }
        return new InteractionResultHolder<>(InteractionResult.PASS, stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        TerminalBinding binding = stack.get(ModComponents.TERMINAL_BINDING);
        if (binding != null && binding.id().isPresent()) {
            tooltipComponents.add(Component.translatable("item.anvilcraft.hyperdimension_terminal.bound"));
        } else {
            tooltipComponents.add(Component.translatable("item.anvilcraft.hyperdimension_terminal.unbound"));
        }
    }

    public static void bindToStation(ServerPlayer player, ItemStack stack, StorageBlockEntity entity) {
        TerminalBinding binding = stack.get(ModComponents.TERMINAL_BINDING);
        if (binding != null && binding.id().isPresent()) {
            return;
        }
        UUID id = entity.getId();
        if (id == null) {
            id = UUID.randomUUID();
            entity.setId(id);
        }
        stack.set(ModComponents.TERMINAL_BINDING, new TerminalBinding(Optional.of(id)));
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        player.displayClientMessage(Component.translatable("message.anvilcraft.hyperdimension_terminal.bound"), true);
    }

    public static void openBoundStorage(Player player, ItemStack stack) {
        if (!player.level().isClientSide) {
            return;
        }
        TerminalBinding binding = stack.get(ModComponents.TERMINAL_BINDING);
        if (binding == null || binding.id().isEmpty()) {
            player.displayClientMessage(
                Component.translatable("message.anvilcraft.hyperdimension_terminal.not_bound"),
                true
            );
            return;
        }
        UUID storageId = binding.id().get();
        StorageTerminalClientStub.openRemote(storageId)
            .exceptionally(ignored -> -1L)
            .thenAccept(virtualPos -> Minecraft.getInstance().execute(() -> {
                if (virtualPos == -1L) {
                    player.displayClientMessage(
                        Component.translatable("message.anvilcraft.hyperdimension_terminal.not_found"),
                        true
                    );
                    return;
                }
                StorageScreen.openScreen(
                    BlockPos.of(virtualPos),
                    Component.translatable("block.anvilcraft.hyperdimension_storage_station")
                );
            }));
    }
}
