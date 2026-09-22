package dev.dubhe.anvilcraft.item;

import dev.anvilcraft.lib.v2.util.DistExecutor;
import dev.anvilcraft.lib.v2.util.InventoryUtil;
import dev.dubhe.anvilcraft.api.TerminalSessions;
import dev.dubhe.anvilcraft.client.rpc.StorageTerminalClientStub;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import dev.dubhe.anvilcraft.item.property.component.TerminalBinding;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public abstract class TerminalItem extends BundleLikeItem {
    public enum Kind {
        LOCAL, SHULKER, HYPERDIMENSION
    }

    private final Kind kind;

    protected TerminalItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return this.kind;
    }

    public @Nullable UUID targetId(Player player, ItemStack stack) {
        return switch (this.kind) {
            case LOCAL -> TerminalSessions.localTerminalId(player.getUUID());
            case SHULKER -> TerminalSessions.shulkerTerminalId(player.getUUID());
            case HYPERDIMENSION -> stack.getOrDefault(ModComponents.TERMINAL_BINDING, TerminalBinding.EMPTY).id().orElse(null);
        };
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) return InteractionResult.PASS;
        this.openStorage(player, player.getItemInHand(hand));
        return InteractionResult.SUCCESS;
    }

    public void openStorage(Player player, ItemStack stack) {
        if (!player.level().isClientSide()) return;
        DistExecutor.run(Dist.CLIENT, () -> () -> StorageTerminalClientStub.open(player, stack, this.kind));
    }

    public static List<ItemStack> carriedItems(Player player) {
        List<ItemStack> items = PocketInventory.carriedItems(player);
        items.addAll(InventoryUtil.getCompatItems(player));
        return items;
    }

    public static List<ItemStack> getAll(Player player) {
        return carriedItems(player).stream().filter(stack -> stack.getItem() instanceof TerminalItem).toList();
    }

    @Override
    protected boolean canRemoveOne(TransferState state) {
        return state.getType() == TransferType.BUNDLE_HOVER_ITEM && this.targetId(state.getPlayer(), state.getStack()) != null;
    }

    @Override
    protected boolean canInsertInto(TransferState state) {
        return false;
    }

    @Override
    protected void removeOne(TransferState state) {
        if (!(state.getPlayer() instanceof ServerPlayer player)) return;
        UUID target = this.targetId(player, state.getStack());
        state.setOutput(target == null ? ItemStack.EMPTY : StorageServerStub.extractFromTerminal(player, target, 64, state.getSlot()));
    }

    @Override
    protected void insertOne(TransferState state) {
        if (!(state.getPlayer() instanceof ServerPlayer player)) return;
        UUID target = this.targetId(player, state.getStack());
        ItemStack remaining = state.getOther().copy();
        if (target != null) remaining.shrink(StorageServerStub.insertIntoTerminal(player, target, remaining, remaining.getCount()));
        state.setOutput(remaining);
    }

    @Override
    protected void updateStack(ItemStack stack, TransferState state) {
    }

    @Override
    public boolean canFitInsideContainerItems(ItemStack stack) {
        return false;
    }
}
