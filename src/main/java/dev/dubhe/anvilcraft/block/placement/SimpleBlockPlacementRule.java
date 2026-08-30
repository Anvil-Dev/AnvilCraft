package dev.dubhe.anvilcraft.block.placement;

import dev.dubhe.anvilcraft.api.block.IBlockPlacementRule;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.util.BlockPlacementUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.GrowingPlantBodyBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import javax.annotation.Nullable;

/**
 * 单个具体方块的 Fallback：方块状态不决定放置物（任意状态都放置 1 个自身 BlockItem），
 * 且对应方块物品与方块 ID 一致时由它接管。
 */
public final class SimpleBlockPlacementRule implements IBlockPlacementRule {
    private final Block block;
    private final Item item;

    private SimpleBlockPlacementRule(Block block, Item item) {
        this.block = block;
        this.item = item;
    }

    /**
     * 仅当该方块适合由 {@link SimpleBlockPlacementRule} 接管时才创建，否则返回 {@code null}。
     */
    @Nullable
    public static SimpleBlockPlacementRule of(Block block) {
        if (!canTakeOver(block)) {
            return null;
        }
        return new SimpleBlockPlacementRule(block, block.asItem());
    }

    /**
     * 该方块是否由 Simple 规则接管：对应方块物品与方块 ID 一致，
     * 且方块状态不决定放置物（无需要额外物品或改变数量的状态）。
     */
    public static boolean canTakeOver(Block block) {
        Item item = block.asItem();
        if (item == Items.AIR) {
            return false;
        }
        if (!BuiltInRegistries.ITEM.getKey(item).equals(BuiltInRegistries.BLOCK.getKey(block))) {
            return false;
        }
        return !isStateDependent(block);
    }

    /**
     * 方块状态决定放置物的类别：放置物随状态变化（数量、额外物品或禁止），
     * 这些方块需要数据包规则或类级 Fallback，不能由 Simple 接管。
     */
    private static boolean isStateDependent(Block block) {
        if (block instanceof SlabBlock || block instanceof StairBlock || block instanceof TrapDoorBlock) {
            return true;
        }
        if (block instanceof CropBlock
            || block instanceof FlowerPotBlock
            || block instanceof GrowingPlantBodyBlock
            || block instanceof CandleCakeBlock) {
            return true;
        }
        if (block instanceof BedBlock
            || block instanceof DoorBlock
            || block instanceof DoublePlantBlock
            || block instanceof AbstractMultiPartBlock<?>) {
            return true;
        }
        if (BlockPlacementUtil.isMultifaceLike(block)) {
            return true;
        }
        // 年龄属性（作物、地狱疣、甜浆果丛等）或计数属性：状态决定放置数量
        return hasAgeProperty(block) || hasCountProperty(block);
    }

    private static boolean hasAgeProperty(Block block) {
        return block.getStateDefinition().getProperties().stream()
            .anyMatch(property -> property.getName().equals("age"));
    }

    private static boolean hasCountProperty(Block block) {
        for (var property : BlockPlacementUtil.COUNT_PROPERTIES) {
            if (block.defaultBlockState().hasProperty(property)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean matches(BlockState state) {
        return state.is(this.block);
    }

    @Override
    public List<PlacementItem> getPlacementItems(BlockState state) {
        return List.of(new PlacementItem(new ItemStack(this.item, 1), ItemStack.EMPTY));
    }
}
