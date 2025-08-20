package dev.dubhe.anvilcraft.recipe.anvil.cache;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModBlockEntityTags;
import dev.dubhe.anvilcraft.init.ModEntityTags;
import dev.dubhe.anvilcraft.recipe.anvil.cache.item.ICacheElement;
import dev.dubhe.anvilcraft.recipe.anvil.cache.item.ICacheInput;
import dev.dubhe.anvilcraft.recipe.anvil.cache.item.ICacheInputOutputImpl;
import dev.dubhe.anvilcraft.recipe.anvil.cache.item.ICacheOutput;
import dev.dubhe.anvilcraft.recipe.anvil.cache.item.ItemEntityCacheElement;
import dev.dubhe.anvilcraft.recipe.anvil.cache.item.ItemHandlerCacheElement;
import dev.dubhe.anvilcraft.recipe.anvil.cache.item.operation.SpawnOperation;
import dev.dubhe.anvilcraft.recipe.anvil.util.InWorldRecipeContext;
import dev.dubhe.anvilcraft.recipe.anvil.util.InWorldRecipeData;
import dev.dubhe.anvilcraft.recipe.anvil.util.Range;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 物品缓存类，用于在配方执行过程中缓存和管理物品
 * 该类提供了对物品的输入、输出、生成等操作的缓存和同步功能
 * 支持与方块实体和实体进行物品交互
 */
@SuppressWarnings("unused")
public class ItemCache {
    /**
     * 物品缓存的数据键
     */
    public static final InWorldRecipeData<ItemCache> ITEM_CACHE = InWorldRecipeData.of(AnvilCraft.of("item_cache"), ItemCache::of);

    /**
     * 默认接受者
     */
    public static final Consumer<InWorldRecipeContext> DEFAULT_ACCEPTOR = (ctx) -> ctx.get(ItemCache.ITEM_CACHE).endCache();

    /**
     * 世界实例
     */
    @Getter
    private final Level level;

    /**
     * 输入元素集合
     */
    private final Set<ICacheElement> inputs = new HashSet<>();

    /**
     * 输出元素集合
     */
    private final Set<ICacheElement> outputs = new HashSet<>();

    /**
     * 范围
     */
    private Range range = Range.EMPTY;

    /**
     * 生成操作列表
     */
    private final List<SpawnOperation> spawnList = new ArrayList<>();

    /**
     * 输入缓存集合
     */
    private final Set<ICacheInputOutputImpl> inputCache = new HashSet<>();

    /**
     * 输出缓存集合
     */
    private final Set<ICacheInputOutputImpl> outputCache = new HashSet<>();

    /**
     * 构造一个新的物品缓存
     *
     * @param level 世界实例
     */
    public ItemCache(Level level) {
        this.level = level;
    }

    /**
     * 创建一个新的物品缓存实例
     *
     * @param level 配方上下文
     * @param key   物品缓存数据键
     * @return 物品缓存实例
     */
    private static ItemCache of(InWorldRecipeContext level, InWorldRecipeData<ItemCache> key) {
        return new ItemCache(level.getLevel());
    }

    /**
     * 判断指定位置和范围是否在缓存范围内
     *
     * @param pos   位置
     * @param range 范围
     * @return 是否在范围内
     */
    public boolean inRange(Vec3 pos, Vec3 range) {
        return this.range.contains(pos, range);
    }

    /**
     * 将物品处理器缓存转换为缓存元素
     *
     * @param itemCache    物品缓存
     * @param cache        物品处理器缓存
     * @param input        输入元素集合
     * @param output       输出元素集合
     * @param elementPos   元素位置
     * @param elementRange 元素范围
     */
    private static void toElement(
        ItemCache itemCache,
        IItemHandlerCache cache,
        Set<ICacheElement> input,
        Set<ICacheElement> output,
        Vec3 elementPos,
        Vec3 elementRange
    ) {
        IItemHandler inputHandler = cache.getInput();
        for (int i = 0; i < inputHandler.getSlots(); i++) {
            ItemHandlerCacheElement element = new ItemHandlerCacheElement(
                itemCache,
                inputHandler,
                i,
                elementPos,
                elementRange
            );
            input.add(element);
        }
        IItemHandler outputHandler = cache.getOutput();
        for (int i = 0; i < outputHandler.getSlots(); i++) {
            ItemHandlerCacheElement element = new ItemHandlerCacheElement(
                itemCache,
                outputHandler,
                i,
                elementPos,
                elementRange
            );
            output.add(element);
        }
    }

    /**
     * 将物品处理器转换为缓存元素
     *
     * @param itemCache    物品缓存
     * @param handler      物品处理器
     * @param input        输入元素集合
     * @param output       输出元素集合
     * @param elementPos   元素位置
     * @param elementRange 元素范围
     */
    private static void toElement(
        ItemCache itemCache,
        IItemHandler handler,
        Set<ICacheElement> input,
        Set<ICacheElement> output,
        Vec3 elementPos,
        Vec3 elementRange
    ) {
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemHandlerCacheElement element = new ItemHandlerCacheElement(
                itemCache,
                handler,
                i,
                elementPos,
                elementRange
            );
            input.add(element);
            output.add(element);
        }
    }

    /**
     * 将实体转换为缓存元素
     *
     * @param itemCache 物品缓存
     * @param entity    实体
     * @return 包含输入和输出元素集合的映射条目
     */
    private static Map.Entry<Set<ICacheElement>, Set<ICacheElement>> toElement(
        ItemCache itemCache,
        Entity entity
    ) {
        Set<ICacheElement> input = new HashSet<>();
        Set<ICacheElement> output = new HashSet<>();
        if (entity instanceof ItemEntity itemEntity) {
            ItemEntityCacheElement element = new ItemEntityCacheElement(itemCache, itemEntity);
            input.add(element);
            output.add(element);
            return Map.entry(input, output);
        }
        double xRange = Math.abs(entity.getBoundingBox().maxX - entity.getBoundingBox().minX);
        double yRange = Math.abs(entity.getBoundingBox().maxY - entity.getBoundingBox().minY);
        double zRange = Math.abs(entity.getBoundingBox().maxZ - entity.getBoundingBox().minZ);
        double minRange = Math.min(xRange, Math.min(yRange, zRange));
        Vec3 elementPos = entity.position().add(0.0, yRange / 2.0, 0.0);
        Vec3 elementRange = new Vec3(minRange, minRange, minRange);
        if (entity instanceof IItemHandlerCache cache) {
            ItemCache.toElement(itemCache, cache, input, output, elementPos, elementRange);
        } else if (entity instanceof IItemHandler handler && entity.getType().is(ModEntityTags.ITEM_CACHE)) {
            ItemCache.toElement(itemCache, handler, input, output, elementPos, elementRange);
        }
        return Map.entry(input, output);
    }

    /**
     * 将方块实体转换为缓存元素
     *
     * @param itemCache 物品缓存
     * @param entity    方块实体
     * @return 包含输入和输出元素集合的映射条目
     */
    private static Map.Entry<Set<ICacheElement>, Set<ICacheElement>> toElement(
        ItemCache itemCache,
        BlockEntity entity
    ) {
        Set<ICacheElement> input = new HashSet<>();
        Set<ICacheElement> output = new HashSet<>();
        Vec3 elementPos = entity.getBlockPos().getCenter();
        Vec3 elementRange = new Vec3(0.5, 0.5, 0.5);
        Predicate<BlockEntity> inTag = blockEntity -> false;
        Optional<HolderSet.Named<BlockEntityType<?>>> holders = BuiltInRegistries.BLOCK_ENTITY_TYPE.getTag(ModBlockEntityTags.ITEM_CACHE);
        if (holders.isPresent()) {
            HolderSet.Named<BlockEntityType<?>> named = holders.get();
            List<ResourceKey<BlockEntityType<?>>> keys = new ArrayList<>();
            for (Holder<BlockEntityType<?>> holder : named) {
                ResourceKey<BlockEntityType<?>> key = holder.getKey();
                keys.add(key);
            }
            inTag = blockEntity -> {
                Optional<ResourceKey<BlockEntityType<?>>> key = BuiltInRegistries.BLOCK_ENTITY_TYPE.getResourceKey(blockEntity.getType());
                return key.filter(keys::contains).isPresent();
            };
        }
        if (entity instanceof IItemHandlerCache cache) {
            ItemCache.toElement(itemCache, cache, input, output, elementPos, elementRange);
        } else if (entity instanceof IItemHandler handler && inTag.test(entity)) {
            ItemCache.toElement(itemCache, handler, input, output, elementPos, elementRange);
        }
        return Map.entry(input, output);
    }

    /**
     * 扩展缓存范围并添加相关实体和方块实体
     *
     * @param pos   位置
     * @param range 范围
     */
    public void grow(Vec3 pos, Vec3 range) {
        Range newRange = Range.of(pos, range);
        if (this.range.contains(pos, range)) return;
        if (!this.range.isEmpty()) this.range.grow(newRange);
        else this.range = newRange;
        List<Entity> entities = this.level.getEntities(EntityTypeTest.forClass(Entity.class), newRange.toAABB(), (entity) -> true);
        for (Entity entity : entities) {
            Map.Entry<Set<ICacheElement>, Set<ICacheElement>> entry = ItemCache.toElement(this, entity);
            this.inputs.addAll(entry.getKey());
            this.outputs.addAll(entry.getValue());
        }
        for (BlockPos blockPos : newRange) {
            BlockEntity entity = level.getBlockEntity(blockPos);
            if (entity == null) continue;
            Map.Entry<Set<ICacheElement>, Set<ICacheElement>> entry = ItemCache.toElement(this, entity);
            this.inputs.addAll(entry.getKey());
            this.outputs.addAll(entry.getValue());
        }
    }

    /**
     * 获取指定物品的输入缓存
     *
     * @param itemLike 物品
     * @param pos      位置
     * @return 输入缓存
     */
    public ICacheInput getInput(ItemLike itemLike, Vec3 pos) {
        return this.getInput(stack1 -> stack1.is(itemLike.asItem()), pos);
    }

    /**
     * 获取指定物品的输入缓存
     *
     * @param itemLike 物品
     * @param pos      位置
     * @param range    范围
     * @return 输入缓存
     */
    public ICacheInput getInput(ItemLike itemLike, Vec3 pos, Vec3 range) {
        return this.getInput(stack1 -> stack1.is(itemLike.asItem()), pos, range);
    }

    /**
     * 获取指定物品堆的输入缓存
     *
     * @param stack 物品堆
     * @param pos   位置
     * @return 输入缓存
     */
    public ICacheInput getInput(ItemStack stack, Vec3 pos) {
        return this.getInput(stack1 -> ItemStack.isSameItemSameComponents(stack, stack1), pos);
    }

    /**
     * 获取指定物品堆的输入缓存
     *
     * @param stack 物品堆
     * @param pos   位置
     * @param range 范围
     * @return 输入缓存
     */
    public ICacheInput getInput(ItemStack stack, Vec3 pos, Vec3 range) {
        return this.getInput(stack1 -> ItemStack.isSameItemSameComponents(stack, stack1), pos, range);
    }

    /**
     * 获取满足谓词条件的输入缓存
     *
     * @param predicate 物品谓词
     * @param pos       位置
     * @return 输入缓存
     */
    public ICacheInput getInput(Predicate<ItemStack> predicate, Vec3 pos) {
        return this.getInput(predicate, pos, new Vec3(0.25, 0.25, 0.25));
    }

    /**
     * 获取满足谓词条件的输入缓存
     *
     * @param predicate 物品谓词
     * @param pos       位置
     * @param range     范围
     * @return 输入缓存
     */
    public ICacheInput getInput(Predicate<ItemStack> predicate, Vec3 pos, Vec3 range) {
        Range range1 = Range.of(pos, range);
        for (ICacheInputOutputImpl element : this.inputCache) {
            if (!element.equals(predicate, range1)) continue;
            return element;
        }
        this.grow(pos, range);
        Set<ICacheElement> inputs = new HashSet<>();
        for (ICacheElement input : this.inputs) {
            Range inputRange = Range.of(input.getPos(), input.getRange());
            if (!inputRange.cross(Range.of(pos, range))) continue;
            if (!input.is(predicate)) continue;
            inputs.add(input);
        }
        ICacheInputOutputImpl input = new ICacheInputOutputImpl(predicate, this, pos, range1, inputs);
        this.inputCache.add(input);
        return input;
    }

    /**
     * 获取指定物品堆的输出缓存
     *
     * @param stack 物品堆
     * @param pos   位置
     * @return 输出缓存
     */
    public ICacheOutput getOutput(ItemStack stack, Vec3 pos) {
        return this.getOutput(stack, pos, new Vec3(0.05, 0.05, 0.05));
    }

    /**
     * 获取指定物品堆的输出缓存
     *
     * @param stack 物品堆
     * @param pos   位置
     * @param range 范围
     * @return 输出缓存
     */
    public ICacheOutput getOutput(ItemStack stack, Vec3 pos, Vec3 range) {
        Range range1 = Range.of(pos, range);
        for (ICacheInputOutputImpl element : this.outputCache) {
            if (!element.equals(stack, range1)) continue;
            return element;
        }
        this.grow(pos, range);
        Set<ICacheElement> outputs = new HashSet<>();
        for (ICacheElement output : this.outputs) {
            Range outputRange = Range.of(output.getPos(), output.getRange());
            if (!outputRange.contains(range1)) continue;
            if (!output.is(stack)) continue;
            outputs.add(output);
        }
        if (outputs.isEmpty()) {
            ItemEntityCacheElement output = ItemEntityCacheElement.create(this, stack, pos);
            this.outputs.add(output);
            outputs.add(output);
        }
        ICacheInputOutputImpl output = new ICacheInputOutputImpl(stack, this, pos, range1, outputs);
        this.outputCache.add(output);
        return output;
    }

    /**
     * 添加生成操作到列表中
     *
     * @param spawnOperations 生成操作集合
     */
    public void pushSpawnList(Collection<SpawnOperation> spawnOperations) {
        this.spawnList.addAll(spawnOperations);
    }

    /**
     * 结束缓存并同步所有更改
     */
    public void endCache() {
        for (ICacheInput input : this.inputCache) {
            input.sync();
        }
        for (ICacheOutput output : this.outputCache) {
            output.sync();
        }
        Map<Map.Entry<ItemStack, Vec3>, Integer> map = new HashMap<>();
        for (SpawnOperation spawnOperation : this.spawnList) {
            ItemStack stack = spawnOperation.stack();
            if (stack.isEmpty()) continue;
            int count = spawnOperation.count();
            if (count <= 0) continue;
            stack.setCount(1);
            Vec3 spawnPos = spawnOperation.pos();
            Map.Entry<ItemStack, Vec3> key = Map.entry(stack, spawnPos);
            for (Map.Entry<ItemStack, Vec3> mapKey : map.keySet()) {
                ItemStack stack1 = mapKey.getKey();
                Vec3 pos = mapKey.getValue();
                if (!ItemStack.isSameItemSameComponents(stack, stack1)) continue;
                if (!pos.closerThan(spawnPos, 0.25)) continue;
                key = mapKey;
            }
            map.put(key, map.getOrDefault(key, 0) + count);
        }
        for (Map.Entry<ItemStack, Vec3> stackEntry : map.keySet()) {
            int count = map.get(stackEntry);
            ItemStack stack = stackEntry.getKey();
            Vec3 pos = stackEntry.getValue();
            int maxStackSize = stack.getMaxStackSize();
            while (count > 0) {
                ItemStack stack1 = stack.copy();
                int newCount = Math.min(maxStackSize, count);
                stack1.setCount(newCount);
                ItemEntity entity = new ItemEntity(this.level, pos.x, pos.y, pos.z, stack1, 0, 0, 0);
                entity.anvilcraft$setIsAdsorbable(false);
                this.level.addFreshEntity(entity);
                count -= newCount;
            }
        }
    }
}