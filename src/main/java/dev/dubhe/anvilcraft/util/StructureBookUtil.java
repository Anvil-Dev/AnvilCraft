package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 结构材料清单书生成工具
 */
public class StructureBookUtil {
    public static final Logger LOGGER = LoggerFactory.getLogger(StructureBookUtil.class);

    /**
     * 生成材料清单到输出书槽位
     * 逻辑: 蓝图需求 - 世界中已放置 = 还需要放置
     */
    public static void generateMaterialListBookToOutput(Level level, BlockPos placerPos, SmartBlockPlacerBlockEntity blockEntity) {
        var loadedStructure = blockEntity.getLoadedStructure();
        if (loadedStructure == null || loadedStructure.isEmpty()) {
            return;
        }

        // 第一步: 统计蓝图中需要的方块数量
        Map<Block, Integer> requiredBlocks = new LinkedHashMap<>();
        for (var blockPosition : loadedStructure.blocks) {
            Block block = blockPosition.state().getBlock();
            // 检查是否是可堆叠方块，如果是则累加堆叠数量
            int stackCount = getStackCountFromState(blockPosition.state());
            requiredBlocks.merge(block, stackCount, Integer::sum);
        }

        // 第二步: 统计世界中已放置的方块数量
        Map<Block, Integer> placedBlocks = countPlacedBlocksInStructure(level, placerPos, loadedStructure, blockEntity);

        // 第三步: 计算还需要的方块 = 需求 - 已放置
        Map<Block, Integer> neededBlocks = new LinkedHashMap<>();
        for (Map.Entry<Block, Integer> entry : requiredBlocks.entrySet()) {
            Block block = entry.getKey();
            int required = entry.getValue();
            int placed = placedBlocks.getOrDefault(block, 0);
            int needed = Math.max(0, required - placed);
            if (needed > 0) {
                neededBlocks.put(block, needed);
            }
        }

        // 如果所有方块都已放置完成，输出普通的书
        if (neededBlocks.isEmpty()) {
            ItemStack book = new ItemStack(Items.BOOK);
            blockEntity.getOutputBookInventory().setItem(0, book);
            LOGGER.info("Structure complete: {} (all blocks placed), output book", loadedStructure.diskData.name());
            return;
        }

        // 生成书页内容
        java.util.List<net.minecraft.server.network.Filterable<Component>> pages = new java.util.ArrayList<>();

        // 第一页开始: 材料详情(只显示缺失的)
        Component currentPage = Component.translatable("book.anvilcraft.material_list.missing_header");
        int lineCount = 1;
        boolean hasMissingContent = false;  // 标记是否有缺失内容

        // 获取是否为蓝图move模式
        boolean isPickupMode = blockEntity.isPickupMode();
        boolean isBlueprintMode = !blockEntity.getDiskInventory().getItem(0).isEmpty();
        boolean isBlueprintMoveMode = isBlueprintMode && !isPickupMode;

        for (Map.Entry<Block, Integer> entry : neededBlocks.entrySet()) {
            Block block = entry.getKey();
            int needed = entry.getValue();
            int available;

            if (isBlueprintMoveMode) {
                // 蓝图move模式：检查源位置的方块
                available = countBlockAtSourcePosition(level, placerPos, block);
            } else {
                // 蓝图pickup模式或普通模式：检查容器中的方块
                available = countBlockInContainer(level, placerPos, block);
            }

            int missing = Math.max(0, needed - available);

            // 只显示缺少的方块
            if (missing > 0) {
                hasMissingContent = true;  // 标记有缺失内容

                Component line = Component.literal("\n")
                    .append(Component.literal(block.getName().getString()))
                    .append(Component.literal(" "))
                    .append(Component.literal("§c×" + missing));

                // 检查是否需要分页(每页约14行)
                if (lineCount >= 13) {
                    pages.add(new net.minecraft.server.network.Filterable<>(currentPage, java.util.Optional.empty()));
                    currentPage = Component.literal("");
                    lineCount = 0;
                }

                currentPage = currentPage.copy().append(line);
                lineCount++;
            }
        }

        // 添加最后一页（只有当有缺失内容时才添加）
        if (hasMissingContent && lineCount > 0) {
            pages.add(new net.minecraft.server.network.Filterable<>(currentPage, java.util.Optional.empty()));
        }

        // 如果pages为空（所有需要的方块都有足够存量），输出普通的书
        if (pages.isEmpty()) {
            ItemStack book = new ItemStack(Items.BOOK);
            blockEntity.getOutputBookInventory().setItem(0, book);
            LOGGER.info("Structure material available: {} (all needed blocks available), output book", loadedStructure.diskData.name());
            return;
        }

        // 设置书的专用组件
        final ItemStack writtenBook = new ItemStack(Items.WRITTEN_BOOK);
        var bookContent = new net.minecraft.world.item.component.WrittenBookContent(
            new net.minecraft.server.network.Filterable<>("Material List", java.util.Optional.empty()),  // resolved title
            "Smart Block Placer",  // owner
            0,  // generation
            pages,  // pages
            false  // filtered
        );
        writtenBook.set(DataComponents.WRITTEN_BOOK_CONTENT, bookContent);

        // 放入输出槽位
        blockEntity.getOutputBookInventory().setItem(0, writtenBook);
        LOGGER.info(
            "Generated material list book for structure: {} (needed: {}/{} blocks, placed: {})",
            loadedStructure.diskData.name(),
            neededBlocks.size(),
            requiredBlocks.size(),
            placedBlocks.values().stream().mapToInt(Integer::intValue).sum()
        );
    }

    /**
     * 统计结构中已放置的方块数量
     */
    @SuppressWarnings("unused")
    private static Map<Block, Integer> countPlacedBlocksInStructure(
        Level level,
        BlockPos placerPos,
        StructureLoadUtil.StructureData loadedStructure,
        SmartBlockPlacerBlockEntity blockEntity
    ) {
        Map<Block, Integer> placedBlocks = new LinkedHashMap<>();

        Direction facing = level.getBlockState(placerPos).getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
        boolean upsideDown = level.getBlockState(placerPos).getValue(dev.dubhe.anvilcraft.block.SmartBlockPlacerBlock.UPSIDE_DOWN);

        // 使用 buildBlueprintPositions 获取所有实际位置
        List<net.minecraft.core.BlockPos> allPositions = SmartBlockPlacerBlockEntity.buildBlueprintPositions(
            placerPos,
            facing,
            upsideDown,
            loadedStructure
        );

        if (allPositions.isEmpty() || loadedStructure.blocks.isEmpty()) {
            return placedBlocks;
        }

        int totalPlaced = 0;
        int totalChecked = 0;

        // 遍历所有位置，检查世界中是否已经放置了正确的方块
        for (int i = 0; i < loadedStructure.blocks.size() && i < allPositions.size(); i++) {
            BlockPos targetPos = allPositions.get(i);
            BlockState worldState = level.getBlockState(targetPos);
            BlockState expectedState = loadedStructure.blocks.get(i).state();

            totalChecked++;

            // 检查世界中的方块是否与蓝图中的方块匹配
            if (!worldState.isAir() && worldState.getBlock() == expectedState.getBlock()) {
                Block worldBlock = worldState.getBlock();
                // 检查是否是可堆叠方块，如果是则累加实际堆叠数量
                int placedCount = getStackCountFromState(worldState);
                placedBlocks.merge(worldBlock, placedCount, Integer::sum);
                totalPlaced++;
            }
        }

        LOGGER.debug("Structure check: {} blocks placed out of {} total", totalPlaced, totalChecked);

        return placedBlocks;
    }

    /**
     * 计算容器中指定方块的数量（包括掉落物实体）
     */
    private static int countBlockInContainer(Level level, BlockPos placerPos, Block targetBlock) {
        // 获取放置器朝向
        BlockState state = level.getBlockState(placerPos);
        if (!state.hasProperty(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)) {
            return 0;
        }

        Direction facing = state.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
        BlockPos inputPos = placerPos.relative(facing.getOpposite());

        int count = 0;

        // 使用 level.getCapability 获取物品处理器
        IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, inputPos, null);

        if (itemHandler != null) {
            for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                ItemStack stack = itemHandler.getStackInSlot(slot);
                if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                    if (blockItem.getBlock() == targetBlock) {
                        count += stack.getCount();
                    }
                }
            }
        }

        // 检查掉落物实体
        net.minecraft.world.phys.AABB aabb = new net.minecraft.world.phys.AABB(inputPos);
        java.util.List<net.minecraft.world.entity.item.ItemEntity> entities = level.getEntities(
            EntityTypeTest.forClass(net.minecraft.world.entity.item.ItemEntity.class),
            aabb,
            net.minecraft.world.entity.Entity::isAlive
        );

        for (net.minecraft.world.entity.item.ItemEntity entity : entities) {
            ItemStack stack = entity.getItem();
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                if (blockItem.getBlock() == targetBlock) {
                    count += stack.getCount();
                }
            }
        }

        return count;
    }

    /**
     * 计算源位置指定方块的数量（用于蓝图move模式）
     */
    private static int countBlockAtSourcePosition(Level level, BlockPos placerPos, Block targetBlock) {
        // 获取放置器朝向
        BlockState state = level.getBlockState(placerPos);
        if (!state.hasProperty(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)) {
            return 0;
        }

        Direction facing = state.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
        BlockPos sourcePos = placerPos.relative(facing.getOpposite());

        // 检查源位置的方块是否匹配
        BlockState sourceState = level.getBlockState(sourcePos);
        if (!sourceState.isAir() && sourceState.getBlock() == targetBlock) {
            return 1;  // 源位置只有一个方块
        }

        return 0;
    }

    /**
     * 从方块状态中获取堆叠数量
     *
     * @param state 方块状态
     * @return 堆叠数量，1表示不可堆叠
     */
    private static int getStackCountFromState(BlockState state) {
        if (state.is(net.minecraft.world.level.block.Blocks.TURTLE_EGG)) {
            return state.getValue(net.minecraft.world.level.block.TurtleEggBlock.EGGS);
        } else if (state.is(net.minecraft.world.level.block.Blocks.SEA_PICKLE)) {
            return state.getValue(net.minecraft.world.level.block.SeaPickleBlock.PICKLES);
        } else if (state.getBlock() instanceof net.minecraft.world.level.block.CandleBlock) {
            return state.getValue(net.minecraft.world.level.block.CandleBlock.CANDLES);
        }
        return 1;
    }
}
