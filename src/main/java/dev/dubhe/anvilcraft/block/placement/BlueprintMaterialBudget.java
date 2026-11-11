package dev.dubhe.anvilcraft.block.placement;

import dev.dubhe.anvilcraft.api.block.BlockPlacementRules;
import dev.dubhe.anvilcraft.api.item.IBlockItem;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 按实际取料顺序在副本中扣减整份蓝图的消耗，检查期间不占用或取走物品。 */
public final class BlueprintMaterialBudget {
    private final ServerLevel level;
    private final List<ItemStack> inventory = new ArrayList<>();
    private final List<ItemEntity> candidates;
    private final Map<ItemEntity, ItemStack> entityItems = new HashMap<>();
    private final Map<BlockPos, List<ItemStack>> entityGroups = new HashMap<>();

    public BlueprintMaterialBudget(ServerLevel level, BlockPos sourcePos, Direction facing) {
        this.level = level;
        var handler = ItemHandlerUtil.getSourceItemHandler(sourcePos, facing, level);
        if (handler != null) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.extractItem(slot, Integer.MAX_VALUE, true);
                if (!stack.isEmpty()) {
                    this.inventory.add(stack.copy());
                }
            }
        }
        this.candidates = level.getEntitiesOfClass(ItemEntity.class, new AABB(sourcePos),
            entity -> entity.isAlive() && level.isPositionEntityTicking(entity.blockPosition()));
    }

    /** 每个目标优先使用容器，其次使用掉落物；同一目标的材料遵循现有取料边界。 */
    public boolean reserve(BlockState state) {
        for (ItemStack candidate : this.inventory) {
            if (this.reserve(this.inventory, candidate, state)) {
                return true;
            }
        }
        for (ItemEntity entity : this.candidates) {
            List<ItemStack> group = this.entityGroups.computeIfAbsent(entity.blockPosition(), pos ->
                this.level.getEntitiesOfClass(ItemEntity.class, new AABB(pos), ItemEntity::isAlive).stream()
                    .map(source -> this.entityItems.computeIfAbsent(source, item -> item.getItem().copy())).toList());
            ItemStack candidate = this.entityItems.get(entity);
            if (candidate != null && this.reserve(group, candidate, state)) {
                return true;
            }
        }
        return false;
    }

    private boolean reserve(List<ItemStack> source, ItemStack candidate, BlockState state) {
        if (candidate.isEmpty()
            || !(candidate.getItem() instanceof BlockItem || candidate.getItem() instanceof IBlockItem)) {
            return false;
        }
        int required = BlockPlacementRules.getPlacementItemCount(this.level.registryAccess(), state, candidate);
        if (required <= 0) {
            return false;
        }
        if (CompositeBlockPlacement.isConverted(state)) {
            if (!CompositeBlockPlacement.matches(candidate, state)) {
                return false;
            }
            ItemStack material = CompositeBlockPlacement.materialFor(state);
            ItemStack upgrade = source.stream().filter(stack -> !stack.isEmpty() && stack.is(material.getItem()))
                .findFirst().orElse(ItemStack.EMPTY);
            if (upgrade.isEmpty()) {
                return false;
            }
            candidate.shrink(1);
            upgrade.shrink(1);
            return true;
        }
        long available = source.stream().filter(stack -> ItemStack.isSameItemSameComponents(stack, candidate))
            .mapToLong(ItemStack::getCount).sum();
        if (available < required) {
            return false;
        }
        ItemStack material = candidate.copyWithCount(1);
        for (ItemStack stack : source) {
            if (!ItemStack.isSameItemSameComponents(stack, material)) {
                continue;
            }
            int consumed = Math.min(required, stack.getCount());
            stack.shrink(consumed);
            required -= consumed;
            if (required == 0) {
                break;
            }
        }
        return true;
    }
}
