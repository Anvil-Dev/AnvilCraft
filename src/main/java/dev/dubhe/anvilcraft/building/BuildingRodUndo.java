package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.item.BuildingRodItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.CommonHooks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** 只保留最近一次放置；多方块作为一组核对和恢复，避免拆除后来放置的替代品。 */
public final class BuildingRodUndo {
    private static final Map<ServerPlayer, BuildingRodUndo> HISTORY = new WeakHashMap<>();
    private final Level level;
    private final List<PlacedGroup> groups = new ArrayList<>();
    private final Map<BlockPos, PlacedGroup> positions = new LinkedHashMap<>();

    private record Saved(BlockPos pos, BlockState state, CompoundTag nbt) {
        static Saved capture(Level level, BlockPos pos) {
            BlockEntity entity = level.getBlockEntity(pos);
            return new Saved(pos.immutable(), level.getBlockState(pos), entity == null
                ? new CompoundTag() : entity.saveWithFullMetadata(level.registryAccess()));
        }

        boolean matches(Level level) {
            return level.hasChunkAt(this.pos) && this.equals(capture(level, this.pos));
        }
    }

    private static final class PlacedGroup {
        private final List<Saved> before = new ArrayList<>();
        private final List<BlockState> expected = new ArrayList<>();
        private final List<Saved> after = new ArrayList<>();
        private final List<ItemStack> materials = new ArrayList<>();
        private final List<ItemStack> returned = new ArrayList<>();
        private boolean replaced;
    }

    BuildingRodUndo(ServerPlayer player, List<BuildingRodService.Group> planned) {
        this.level = player.level();
        for (var group : planned) {
            if (group.cells.isEmpty()) continue;
            PlacedGroup saved = new PlacedGroup();
            for (var cell : group.cells) {
                saved.before.add(Saved.capture(this.level, cell.pos()));
                saved.expected.add(cell.state());
                this.positions.put(cell.pos(), saved);
            }
            if (!player.isCreative()) group.materials.forEach(stack -> saved.materials.add(stack.copy()));
            group.returned.forEach(stack -> saved.returned.add(stack.copy()));
            this.groups.add(saved);
        }
    }

    void finish(ServerPlayer player) {
        for (PlacedGroup group : this.groups) {
            for (int index = 0; index < group.before.size(); index++) {
                Saved after = Saved.capture(this.level, group.before.get(index).pos());
                group.after.add(after);
                if (after.state().getBlock() != group.expected.get(index).getBlock()) group.replaced = true;
            }
        }
        HISTORY.put(player, this);
    }

    public static void replaced(Level level, BlockPos pos, BlockState before, BlockState after) {
        if (level.isClientSide || before.getBlock() == after.getBlock()) return;
        for (BuildingRodUndo undo : HISTORY.values()) {
            if (undo.level != level) continue;
            PlacedGroup group = undo.positions.get(pos);
            if (group != null) group.replaced = true;
        }
    }

    public static void undo(ServerPlayer player) {
        if (!BuildingRodItem.isHeld(player)) return;
        BuildingRodUndo undo = HISTORY.get(player);
        if (undo == null || undo.level != player.level()) {
            BuildingRodService.message(player, "nothing_to_undo");
            return;
        }
        List<PlacedGroup> restore = new ArrayList<>();
        BuildingMaterials materials = new BuildingMaterials(player);
        for (PlacedGroup group : undo.groups) {
            if (group.replaced || group.after.stream().anyMatch(saved -> !saved.matches(undo.level)
                || !BuildingRodService.canModify(player, saved.pos()))) {
                continue;
            }
            if (group.after.stream().anyMatch(saved -> CommonHooks.fireBlockBreak(undo.level,
                player.gameMode.getGameModeForPlayer(), player, saved.pos(), saved.state()).isCanceled())) {
                continue;
            }
            if (materials.reserve(group.returned)) restore.add(group);
        }
        if (!materials.consume()) return;
        HISTORY.remove(player);
        for (PlacedGroup group : restore) {
            for (Saved saved : group.before) BuildingCommit.set(undo.level, saved.pos(), saved.state());
            for (Saved saved : group.before) {
                BlockEntity entity = undo.level.getBlockEntity(saved.pos());
                if (entity != null && !saved.nbt().isEmpty()) {
                    entity.loadWithComponents(saved.nbt(), player.registryAccess());
                    entity.setChanged();
                    undo.level.sendBlockUpdated(saved.pos(), saved.state(), saved.state(), Block.UPDATE_CLIENTS);
                }
            }
            for (ItemStack material : group.materials) player.getInventory().placeItemBackInInventory(material);
        }
        for (PlacedGroup group : restore) {
            for (int index = 0; index < group.before.size(); index++) {
                Saved saved = group.before.get(index);
                undo.level.markAndNotifyBlock(saved.pos(), undo.level.getChunkAt(saved.pos()),
                    group.after.get(index).state(), saved.state(), Block.UPDATE_ALL, Block.UPDATE_LIMIT);
            }
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        BuildingRodService.message(player, restore.isEmpty() ? "nothing_to_undo" : "undone");
    }
}
