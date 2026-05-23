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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
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
    public static void generateMaterialListBookToOutput(
        Level level,
        BlockPos placerPos,
        SmartBlockPlacerBlockEntity blockEntity
    ) {
        var loadedStructure = blockEntity.getLoadedStructure();
        if (loadedStructure == null || loadedStructure.isEmpty()) {
            return;
        }
        
        // 第一步: 统计蓝图中需要的方块数量
        Map<Block, Integer> requiredBlocks = new LinkedHashMap<>();
        for (var blockPosition : loadedStructure.blocks) {
            Block block = blockPosition.state().getBlock();
            requiredBlocks.merge(block, 1, Integer::sum);
        }
        
        // 第二步: 统计世界中已放置的方块数量
        Map<Block, Integer> placedBlocks = countPlacedBlocksInStructure(
            level, placerPos, loadedStructure, blockEntity
        );
        
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
            LOGGER.info("Structure complete: {} (all blocks placed), output book", 
                loadedStructure.structureName);
            return;
        }
        
        // 第四步: 创建成书
        ItemStack writtenBook = new ItemStack(Items.WRITTEN_BOOK);
        
        // 生成书页内容
        java.util.List<net.minecraft.server.network.Filterable<Component>> pages = new java.util.ArrayList<>();
        
        // 第一页开始: 材料详情(只显示缺失的)
        Component currentPage = Component.translatable("book.anvilcraft.material_list.missing_header");
        int lineCount = 1;
        
        for (Map.Entry<Block, Integer> entry : neededBlocks.entrySet()) {
            Block block = entry.getKey();
            int needed = entry.getValue();
            int available = countBlockInContainer(level, placerPos, block);
            int missing = Math.max(0, needed - available);
            
            // 只显示缺少的方块
            if (missing > 0) {
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
        
        // 添加最后一页
        if (lineCount > 0) {
            pages.add(new net.minecraft.server.network.Filterable<>(currentPage, java.util.Optional.empty()));
        }
        
        // 设置书的专用组件
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
        LOGGER.info("Generated material list book for structure: {} (needed: {}/{} blocks, placed: {})", 
            loadedStructure.structureName, neededBlocks.size(), requiredBlocks.size(), 
            placedBlocks.values().stream().mapToInt(Integer::intValue).sum());
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
        
        Direction facing = level.getBlockState(placerPos).getValue(
            net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING
        );
        boolean upsideDown = level.getBlockState(placerPos).getValue(
            dev.dubhe.anvilcraft.block.SmartBlockPlacerBlock.UPSIDE_DOWN
        );
        
        // 旋转结构数据(与buildBlueprintPositions保持一致)
        StructureLoadUtil.StructureData rotatedData = 
            SmartBlockPlacerBlockEntity.rotateStructureDataStatic(
                loadedStructure, level, placerPos
            );
        
        // 计算基准位置
        BlockPos basePos = placerPos.relative(facing.getOpposite(), -4);
        
        int totalPlaced = 0;
        int totalChecked = 0;
        
        // 遍历所有结构方块,检查世界中是否已经放置了正确的方块
        // 使用与buildBlueprintPositions相同的坐标映射: x→col, z→row, y→layer
        for (var blockPosition : rotatedData.blocks) {
            int row = blockPosition.z();   // z 对应 row(纵向)
            int col = blockPosition.x();   // x 对应 col(横向)
            int layer = blockPosition.y(); // y 对应 layer
            
            BlockPos targetPos = SmartBlockPlacerBlockEntity.calculateTargetPosition(
                basePos, facing, row, col, layer, upsideDown
            );
            
            BlockState worldState = level.getBlockState(targetPos);
            BlockState expectedState = blockPosition.state();
            
            totalChecked++;
            
            // 检查世界中的方块是否与蓝图中的方块匹配
            if (!worldState.isAir() && worldState.getBlock() == expectedState.getBlock()) {
                Block worldBlock = worldState.getBlock();
                placedBlocks.merge(worldBlock, 1, Integer::sum);
                totalPlaced++;
            }
        }
        
        LOGGER.debug("Structure check: {} blocks placed out of {} total", totalPlaced, totalChecked);
        
        return placedBlocks;
    }
    
    /**
     * 计算容器中指定方块的数量
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
        IItemHandler itemHandler = level.getCapability(
            Capabilities.ItemHandler.BLOCK,
            inputPos,
            null
        );
        
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
        
        return count;
    }
}
