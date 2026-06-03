
package dev.dubhe.anvilcraft.block.entity;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import dev.dubhe.anvilcraft.api.entity.fakeplayer.AnvilCraftFakePlayers;
import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.AccelerationRingBlock;
import dev.dubhe.anvilcraft.block.DeflectionRingBlock;
import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import dev.dubhe.anvilcraft.block.LargeCakeBlock;
import dev.dubhe.anvilcraft.block.OverseerBlock;
import dev.dubhe.anvilcraft.block.RemoteTransmissionPoleBlock;
import dev.dubhe.anvilcraft.block.SmartBlockPlacerBlock;
import dev.dubhe.anvilcraft.block.TeslaTowerBlock;
import dev.dubhe.anvilcraft.block.TransmissionPoleBlock;
import dev.dubhe.anvilcraft.block.state.Orientation;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.SmartBlockPlacerMenu;
import dev.dubhe.anvilcraft.item.property.component.StructureDiskData;
import dev.dubhe.anvilcraft.util.StructureBookUtil;
import dev.dubhe.anvilcraft.util.StructureLoadUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import javax.annotation.Nullable;

@Getter
@Setter
public class SmartBlockPlacerBlockEntity extends BlockEntity implements IPowerConsumer, MenuProvider, IDiskCloneable, IItemHandlerHolder {
    private static final int POWER = 8;
    private static final int PLACEMENT_INTERVAL = 20;
    private static final int PLACEMENT_DELAY = 6;

    // 白名单：蓝图中需要保留的方块状态属性
    private static final Set<Property<?>> INHERITED_PROPERTIES = ImmutableSet.of(
        // 方块朝向
        BlockStateProperties.FACING,
        BlockStateProperties.HORIZONTAL_FACING,
        BlockStateProperties.AXIS,
        BlockStateProperties.HORIZONTAL_AXIS,
        BlockStateProperties.ROTATION_16,
        BlockStateProperties.ORIENTATION,
        BlockStateProperties.VERTICAL_DIRECTION,
        BlockStateProperties.ATTACH_FACE,
        BlockStateProperties.RAIL_SHAPE,
        BlockStateProperties.RAIL_SHAPE_STRAIGHT,
        // 方块是top/bottom
        BlockStateProperties.HALF,
        BlockStateProperties.DOUBLE_BLOCK_HALF,
        BlockStateProperties.BED_PART,
        BlockStateProperties.HANGING,
        BlockStateProperties.BELL_ATTACHMENT,
        // 门的手动开启状态和铰链方向
        BlockStateProperties.OPEN,
        BlockStateProperties.DOOR_HINGE,
        // 中继器的挡位
        BlockStateProperties.DELAY,
        // 比较器的模式
        ComparatorBlock.MODE,
        // 可以堆叠放置的方块的数量
        BlockStateProperties.FLOWER_AMOUNT,
        BlockStateProperties.CANDLES,
        BlockStateProperties.EGGS,
        BlockStateProperties.PICKLES,
        BlockStateProperties.LAYERS,
        BlockStateProperties.SLAB_TYPE,
        BlockStateProperties.LEVEL_CAULDRON,
        // 多面方向属性（发光地衣、藤蔓等）
        BlockStateProperties.NORTH,
        BlockStateProperties.SOUTH,
        BlockStateProperties.EAST,
        BlockStateProperties.WEST,
        BlockStateProperties.UP,
        BlockStateProperties.DOWN,
        // 多方块方块的部件标识
        GiantAnvilBlock.CUBE,
        GiantAnvilBlock.HALF,
        RemoteTransmissionPoleBlock.HALF,
        TransmissionPoleBlock.HALF,
        TeslaTowerBlock.HALF,
        OverseerBlock.HALF,
        LargeCakeBlock.HALF,
        AccelerationRingBlock.HALF,
        DeflectionRingBlock.HALF
    );

    // 基于属性名称的白名单集合（按名称匹配而非对象相等，解决不同方块同名Property非同一实例的问题）
    private static final Set<String> INHERITED_PROPERTY_NAMES =
        INHERITED_PROPERTIES.stream().map(Property::getName).collect(ImmutableSet.toImmutableSet());

    // 标记当前是否有方块正在被智能放置器移动
    private static final ThreadLocal<Boolean> IS_BEING_MOVED_BY_PLACER = ThreadLocal.withInitial(() -> false);

    private PowerGrid grid = null;
    private boolean isPowered = false;
    private boolean hasRedstoneSignal = false;
    private int selectedLayer = 0;
    private int placeCooldown = 0;
    private long lastTickGameTime = -1;
    private ItemStack currentHeldBlock = ItemStack.EMPTY;
    private int currentPlacementIndex = 0;
    private final Map<Integer, Set<Integer>> layerPositions = new HashMap<>();
    private boolean isPickupMode = true;
    private boolean isSkipMissingMode = true;  // true=跳过缺少方块, false=停止在缺少方块

    // 已加载的原始结构数据(未旋转)
    @Nullable
    private StructureLoadUtil.StructureData loadedStructure = null;
    /**
     * -- GETTER --
     * 获取已加载的结构名称
     */
    private String loadedStructureName = "";

    // 结构加载状态追踪 - 用于避免重复加载
    private @Nullable UUID loadedStructureUuid = null;
    private boolean hasStructureDisk = false;
    private boolean hasInvalidStructure = false;  // 标记磁盘是否包含无效结构

    // 记录上次检查的磁盘物品，用于检测变化
    private ItemStack lastDiskItem = ItemStack.EMPTY;

    /**
     * -- GETTER --
     * 获取当前缺失的方块物品
     */
    // 当前缺失的方块物品（服务端计算，客户端渲染）
    private ItemStack missingBlockItem = ItemStack.EMPTY;

    // Disk物品栏
    private final SimpleContainer diskInventory = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            SmartBlockPlacerBlockEntity.this.setChanged();
            // 检查磁盘物品是否真正变化
            SmartBlockPlacerBlockEntity.this.checkDiskAndLoad();
        }
    };

    /**
     * -- GETTER --
     * 获取书物品栏(输入)
     */
    // 蓝图模式书物品栏(输入)
    private final SimpleContainer bookInventory = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            SmartBlockPlacerBlockEntity.this.setChanged();
            // 当输入书时,自动生成材料清单到输出槽位
            SmartBlockPlacerBlockEntity.this.onBookInputChanged();
        }
    };

    /**
     * -- GETTER --
     * 获取输出书物品栏
     */
    // 蓝图模式输出书物品栏(输出材料清单)
    private final SimpleContainer outputBookInventory = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            SmartBlockPlacerBlockEntity.this.setChanged();
            // 当输出书被取走时,消耗输入书
            SmartBlockPlacerBlockEntity.this.onOutputBookTaken();
        }
    };

    // 客户端动画状态
    private long clientAnimationStartTime = 0;
    @Nullable
    private BlockPos clientLastTargetPos = null;
    private int lastPlaceCooldown = 0;

    // 客户端收回动画状态
    private boolean clientIsRetracting = false;
    private long clientRetractStartTime = 0;
    private float[] clientRetractStartAngles = new float[4];
    private float clientRetractStartProgress = 0f;

    // 比较器信号状态
    private int lastComparatorSignal = 0;


    @SuppressWarnings("checkstyle:EmptyLineSeparator")
    public SmartBlockPlacerBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.SMART_BLOCK_PLACER.get(), pos, blockState);
    }

    private SmartBlockPlacerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static SmartBlockPlacerBlockEntity createBlockEntity(
        BlockEntityType<?> type,
        BlockPos pos,
        BlockState blockState
    ) {
        return new SmartBlockPlacerBlockEntity(type, pos, blockState);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putBoolean("isPowered", isPowered);
        tag.putBoolean("hasRedstoneSignal", hasRedstoneSignal);
        tag.putInt("selectedLayer", selectedLayer);
        tag.putInt("currentPlacementIndex", currentPlacementIndex);
        tag.putInt("placeCooldown", placeCooldown);
        tag.putBoolean("isPickupMode", isPickupMode);
        tag.putBoolean("isSkipMissingMode", isSkipMissingMode);
        if (!missingBlockItem.isEmpty()) {
            tag.put("missingBlockItem", missingBlockItem.save(provider));
        }
        if (!currentHeldBlock.isEmpty()) {
            tag.put("currentHeldBlock", currentHeldBlock.save(provider));
        }
        saveLayerPositions(tag);
        // 保存Disk物品栏
        tag.put("diskInventory", this.diskInventory.createTag(provider));
        // 保存书物品栏
        tag.put("bookInventory", this.bookInventory.createTag(provider));
        // 保存输出书物品栏
        tag.put("outputBookInventory", this.outputBookInventory.createTag(provider));

        // 保存结构缓存(保存原始未旋转的数据)
        if (this.loadedStructure != null && !this.loadedStructure.isEmpty()) {
            tag.put("cachedStructure", this.saveStructureData(this.loadedStructure, provider));
            tag.putString("cachedStructureName", this.loadedStructureName);
            if (this.loadedStructureUuid != null) {
                tag.putUUID("cachedStructureUuid", this.loadedStructureUuid);
            }
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.isPowered = tag.getBoolean("isPowered");
        this.hasRedstoneSignal = tag.getBoolean("hasRedstoneSignal");
        this.selectedLayer = tag.getInt("selectedLayer");
        this.currentPlacementIndex = tag.getInt("currentPlacementIndex");
        this.placeCooldown = tag.getInt("placeCooldown");
        this.isPickupMode = tag.getBoolean("isPickupMode");
        this.isSkipMissingMode = tag.getBoolean("isSkipMissingMode");
        this.missingBlockItem = tag.contains("missingBlockItem", Tag.TAG_COMPOUND)
                                ? ItemStack.parse(provider, tag.getCompound("missingBlockItem")).orElse(ItemStack.EMPTY)
                                : ItemStack.EMPTY;
        this.currentHeldBlock = tag.contains("currentHeldBlock", Tag.TAG_COMPOUND)
                                ? ItemStack.parse(provider, tag.getCompound("currentHeldBlock")).orElse(ItemStack.EMPTY)
                                : ItemStack.EMPTY;
        loadLayerPositions(tag);
        // 加载Disk物品栏
        this.diskInventory.fromTag(tag.getList("diskInventory", Tag.TAG_COMPOUND), provider);
        // 加载书物品栏
        this.bookInventory.fromTag(tag.getList("bookInventory", Tag.TAG_COMPOUND), provider);
        // 加载输出书物品栏
        this.outputBookInventory.fromTag(tag.getList("outputBookInventory", Tag.TAG_COMPOUND), provider);

        // 优先从缓存加载结构数据(原始未旋转的数据)
        if (tag.contains("cachedStructure", Tag.TAG_COMPOUND)) {
            this.loadedStructure = this.loadStructureData(tag.getCompound("cachedStructure"), provider);
            this.loadedStructureName = tag.getString("cachedStructureName");
            if (tag.contains("cachedStructureUuid")) {
                this.loadedStructureUuid = tag.getUUID("cachedStructureUuid");
            }
            this.hasStructureDisk = true;
        }

        // NBT加载后尝试从磁盘更新结构（如果有磁盘的话）
        this.tryLoadStructure();
    }

    @Override
    public IItemHandler getItemHandler() {
        return new InvWrapper(diskInventory) {
            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (!stack.is(ModItems.STRUCTURE_DISK.get())) {
                    return stack;
                }

                // 检查结构大小是否超过 5x5x5
                if (SmartBlockPlacerBlockEntity.this.level != null && !SmartBlockPlacerBlockEntity.this.level.isClientSide) {
                    StructureDiskData structureDiskData = stack.get(ModComponents.STRUCTURE_DISK_DATA);
                    if (structureDiskData != null) {
                        // 如果结构大小超过 5x5x5，拒绝插入
                        if (structureDiskData.sizeX() > 5 || structureDiskData.sizeY() > 5 || structureDiskData.sizeZ() > 5) {
                            return stack;
                        }
                    }
                }

                return super.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return super.extractItem(slot, amount, simulate);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (!stack.is(ModItems.STRUCTURE_DISK.get())) {
                    return false;
                }

                // 检查结构大小是否超过 5x5x5
                if (SmartBlockPlacerBlockEntity.this.level != null && !SmartBlockPlacerBlockEntity.this.level.isClientSide) {
                    StructureDiskData structureDiskData = stack.get(ModComponents.STRUCTURE_DISK_DATA);
                    if (structureDiskData != null) {
                        // 如果结构大小超过 5x5x5，拒绝插入
                        return structureDiskData.sizeX() <= 5 && structureDiskData.sizeY() <= 5 && structureDiskData.sizeZ() <= 5;
                    }
                }

                return true;
            }
        };
    }

    /**
     * 保存结构数据到NBT
     *
     * @param data     结构数据
     * @param provider 注册表访问器（当前未使用，保留用于未来扩展）
     */
    @SuppressWarnings("unused")
    private CompoundTag saveStructureData(StructureLoadUtil.StructureData data, HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        StructureDiskData.CODEC.encode(data.diskData, NbtOps.INSTANCE, new CompoundTag())
            .result()
            .ifPresent(nbt -> tag.put("DiskData", nbt));
        // 保存方块列表
        CompoundTag blocksTag = new CompoundTag();
        for (int i = 0; i < data.blocks.size(); i++) {
            StructureLoadUtil.BlockPosition blockPos = data.blocks.get(i);
            CompoundTag blockTag = new CompoundTag();
            blockTag.putInt("x", blockPos.x());
            blockTag.putInt("y", blockPos.y());
            blockTag.putInt("z", blockPos.z());
            // 保存方块状态
            try {
                BlockState.CODEC.encodeStart(
                    NbtOps.INSTANCE, blockPos.state()
                ).result().ifPresent(encoded -> blockTag.put("state", encoded));
            } catch (Exception e) {
                LoggerFactory.getLogger(SmartBlockPlacerBlockEntity.class)
                    .warn("Failed to save block state: {}", e.getMessage());
            }
            blocksTag.put(String.valueOf(i), blockTag);
        }
        tag.put("blocks", blocksTag);

        return tag;
    }

    /**
     * 从NBT加载结构数据
     *
     * @param tag      NBT标签
     * @param provider 注册表访问器（当前未使用，保留用于未来扩展）
     */
    @SuppressWarnings("unused")
    private StructureLoadUtil.StructureData loadStructureData(CompoundTag tag, HolderLookup.Provider provider) {
        Pair<StructureDiskData, Tag> orThrow = StructureDiskData.CODEC.decode(NbtOps.INSTANCE, tag.get("DiskData")).getOrThrow();
        StructureLoadUtil.StructureData data = new StructureLoadUtil.StructureData(orThrow.getFirst());

        // 加载方块列表
        CompoundTag blocksTag = tag.getCompound("blocks");
        for (String key : blocksTag.getAllKeys()) {
            CompoundTag blockTag = blocksTag.getCompound(key);
            int x = blockTag.getInt("x");
            int y = blockTag.getInt("y");
            int z = blockTag.getInt("z");

            // 加载方块状态
            final BlockState[] stateHolder =
                new BlockState[]{Blocks.AIR.defaultBlockState()};
            if (blockTag.contains("state", Tag.TAG_COMPOUND)) {
                try {
                    BlockState.CODEC.parse(
                        NbtOps.INSTANCE, blockTag.getCompound("state")
                    ).result().ifPresent(s -> stateHolder[0] = s);
                } catch (Exception e) {
                    LoggerFactory.getLogger(SmartBlockPlacerBlockEntity.class)
                        .warn("Failed to load block state: {}", e.getMessage());
                }
            }

            data.blocks.add(new StructureLoadUtil.BlockPosition(x, y, z, stateHolder[0]));
        }

        return data;
    }

    /**
     * 检查磁盘物品是否变化，如果变化则加载结构
     */
    private void checkDiskAndLoad() {
        if (this.level == null) {
            return;
        }

        ItemStack currentDisk = this.diskInventory.getItem(0);

        // 快速检查：物品是否相同
        if (ItemStack.isSameItemSameComponents(currentDisk, this.lastDiskItem)) {
            return;
        }

        // 更新缓存
        this.lastDiskItem = currentDisk.copy();

        // 客户端和服务端都调用，但各自独立处理
        this.tryLoadStructure();
    }

    /**
     * 尝试加载结构 - 只在磁盘物品变化时调用
     * 注意：加载的是原始未旋转的数据，旋转在使用时动态计算
     */
    private void tryLoadStructure() {
        if (this.level == null) {
            return;
        }

        ItemStack diskStack = this.diskInventory.getItem(0);
        boolean nowHasDisk = !diskStack.isEmpty() && diskStack.is(ModItems.STRUCTURE_DISK.get());

        // 获取当前磁盘的UUID
        UUID currentUuid = null;
        if (nowHasDisk) {
            StructureDiskData customData = diskStack.get(ModComponents.STRUCTURE_DISK_DATA);
            if (customData != null) {
                currentUuid = customData.uuid();
            }
        }

        // 状态没有变化，跳过加载
        if (nowHasDisk == this.hasStructureDisk && currentUuid != null && currentUuid.equals(this.loadedStructureUuid)) {
            return;
        }

        // 更新状态
        this.hasStructureDisk = nowHasDisk;
        this.loadedStructureUuid = currentUuid;

        boolean structureChanged = false;

        if (!nowHasDisk) {
            // 磁盘被移除
            if (this.loadedStructure != null) {
                this.loadedStructure = null;
                this.loadedStructureName = "";
                this.hasInvalidStructure = false;
                structureChanged = true;
            }
        } else {
            // 加载新结构（客户端和服务端都加载）- 不旋转，保存原始数据
            StructureLoadUtil.StructureData data = StructureLoadUtil.loadStructureFromDisk(this.level, diskStack);
            if (data != null && !data.isEmpty()) {
                this.loadedStructure = data;
                this.loadedStructureName = data.diskData.name();
                this.hasInvalidStructure = false;
                structureChanged = true;

                // 清空选区（仅服务端）
                if (!this.level.isClientSide) {
                    this.layerPositions.clear();
                    this.currentPlacementIndex = 0;
                }
            } else {
                // 加载失败，标记为无效结构
                if (this.loadedStructure != null) {
                    this.loadedStructure = null;
                    this.loadedStructureName = "";
                    this.hasInvalidStructure = true;
                    structureChanged = true;
                } else {
                    // 之前就没有结构，现在也加载失败
                    this.hasInvalidStructure = true;
                }
            }
        }

        // 只在结构数据真正变化时才同步
        if (structureChanged) {
            this.onChanged();
        }
    }

    /**
     * 获取已加载的结构数据
     */
    @Nullable
    public StructureLoadUtil.StructureData getLoadedStructure() {
        return this.loadedStructure;
    }

    /**
     * 检查是否包含无效结构（磁盘存在但结构数据无效）
     */
    public boolean hasInvalidStructure() {
        return this.hasInvalidStructure;
    }

    /**
     * 获取比较器输出信号强度（基于放置进度）
     *
     * @return 红石信号强度 0-15，0表示未开始，15表示完成
     */
    public int getComparatorOutput() {
        if (this.level == null || this.level.isClientSide) {
            return 0;
        }

        int newSignal = calculateComparatorSignal();
        
        // 如果信号强度发生变化，立即发送更新通知比较器
        if (newSignal != this.lastComparatorSignal) {
            this.lastComparatorSignal = newSignal;
            this.notifyComparatorUpdate();
        }
        
        return newSignal;
    }
    
    /**
     * 计算比较器信号强度（内部方法）
     */
    private int calculateComparatorSignal() {
        if (this.level == null || this.level.isClientSide) {
            return 0;
        }

        // 蓝图模式：基于结构数据计算进度
        if (this.loadedStructure != null && !this.loadedStructure.isEmpty()) {
            // 获取旋转后的结构数据
            StructureLoadUtil.StructureData rotatedData = rotateStructureDataStatic(this.loadedStructure);

            // 获取所有位置
            Direction facing = this.getFacing(this.getBlockPos(), this.level);
            boolean upsideDown = this.level.getBlockState(this.getBlockPos()).getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);
            List<BlockPos> allPositions = buildBlueprintPositions(this.getBlockPos(), facing, upsideDown, rotatedData);

            if (allPositions.isEmpty()) {
                return 0;
            }

            // 获取有序索引列表（只包含主体部件，多方块结构的次要部件已被过滤）
            List<Integer> orderedIndices = buildOrderedBlueprintIndices(rotatedData, upsideDown);
            int totalBlocks = orderedIndices.size();

            if (totalBlocks == 0) {
                return 0;
            }

            // 计算已放置的方块数量
            int placedCount = 0;

            for (int index : orderedIndices) {
                BlockPos targetPos = allPositions.get(index);

                // 蓝图模式：只检查位置是否有方块（不检查状态）
                if (this.isBlueprintPositionOccupied(this.level, targetPos)) {
                    placedCount++;
                }
            }

            // 计算信号强度 (0-15)
            if (placedCount >= totalBlocks) {
                return 15;  // 完全完成时输出15
            }

            // 根据进度计算信号强度
            double progress = (double) placedCount / totalBlocks;
            return (int) Math.round(progress * 15.0);  // 最大输出15
        }

        // 普通模式（PICKUP/MOVE）：基于选区位置计算进度
        if (!this.layerPositions.isEmpty()) {
            Direction facing = this.level.getBlockState(this.getBlockPos()).getValue(HorizontalDirectionalBlock.FACING);
            boolean upsideDown = this.level.getBlockState(this.getBlockPos()).getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);

            // 使用与放置逻辑相同的方法构建位置列表
            List<BlockPos> allPositions = buildOrderedPositionsFromLayers(this.getBlockPos(), facing, upsideDown);

            if (allPositions.isEmpty()) {
                return 0;
            }

            // 计算已放置的方块数量
            int placedCount = 0;
            int totalCount = allPositions.size();

            for (BlockPos targetPos : allPositions) {
                // 如果位置不可以放置，说明已经放置了方块
                if (this.isPositionOccupied(this.level, targetPos, null)) {
                    placedCount++;
                }
            }

            // 计算信号强度 (0-15)
            if (placedCount >= totalCount) {
                return 15;  // 完全完成时输出15
            }

            // 根据进度计算信号强度
            double progress = (double) placedCount / totalCount;
            return (int) Math.round(progress * 15.0);  // 最大输出15
        }

        return 0;
    }
    
    /**
     * 通知比较器更新
     */
    private void notifyComparatorUpdate() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        
        // 发送方块更新，让比较器重新查询信号强度
        BlockState currentState = this.level.getBlockState(this.getBlockPos());
        this.level.updateNeighbourForOutputSignal(this.getBlockPos(), currentState.getBlock());
    }

    /**
     * 当书输入槽位变化时调用,生成材料清单书到输出槽位
     */
    private void onBookInputChanged() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        // 检查输入槽位是否有书
        ItemStack inputBook = this.bookInventory.getItem(0);
        if (inputBook.isEmpty()) {
            // 清空输出槽位
            this.outputBookInventory.setItem(0, ItemStack.EMPTY);
            return;
        }

        // 检查输出槽位是否已经有书
        ItemStack outputBook = this.outputBookInventory.getItem(0);
        if (!outputBook.isEmpty()) {
            // 如果输出槽位已有书,不再生成
            return;
        }

        // 生成材料清单书(不消耗输入书)
        try {
            StructureBookUtil.generateMaterialListBookToOutput(
                this.level,
                this.getBlockPos(),
                this
            );
        } catch (Exception e) {
            StructureBookUtil.LOGGER.error("Failed to generate material list book: {}", e.getMessage());
        }
    }

    /**
     * 当输出书被取走时调用,消耗输入书
     */
    private void onOutputBookTaken() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        // 检查输出槽位是否为空(被取走)
        ItemStack outputBook = this.outputBookInventory.getItem(0);
        if (outputBook.isEmpty()) {
            // 消耗输入书
            ItemStack inputBook = this.bookInventory.getItem(0);
            if (!inputBook.isEmpty()) {
                this.bookInventory.setItem(0, ItemStack.EMPTY);
            }
        }
    }

    /**
     * 更新缺失方块信息（服务端调用）
     */
    private void updateMissingBlockInfo(Level level, BlockPos pos) {
        // 只在停止模式下检测
        if (this.isSkipMissingMode) {
            if (!this.missingBlockItem.isEmpty()) {
                this.missingBlockItem = ItemStack.EMPTY;
                this.onChanged();
            }
            return;
        }

        // 只在蓝图模式下检测
        if (this.loadedStructure == null || this.loadedStructure.isEmpty()) {
            if (!this.missingBlockItem.isEmpty()) {
                this.missingBlockItem = ItemStack.EMPTY;
                this.onChanged();
            }
            return;
        }

        // 获取旋转后的结构数据
        StructureLoadUtil.StructureData rotatedData = rotateStructureDataStatic(this.loadedStructure);

        // 获取所有位置
        Direction facing = this.getFacing(pos, level);
        boolean upsideDown = level.getBlockState(pos).getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);
        List<BlockPos> allPositions = buildBlueprintPositions(pos, facing, upsideDown, rotatedData);

        if (allPositions.isEmpty() || rotatedData.blocks.isEmpty()) {
            if (!this.missingBlockItem.isEmpty()) {
                this.missingBlockItem = ItemStack.EMPTY;
                this.onChanged();
            }
            return;
        }

        // 获取有序索引列表
        List<Integer> orderedIndices = buildOrderedBlueprintIndices(rotatedData, upsideDown);

        // 遍历所有位置，找到第一个未放置且缺少材料的位置
        for (int index : orderedIndices) {
            BlockPos targetPos = allPositions.get(index);

            // 蓝图模式：只检查位置是否有方块（不检查状态）
            if (this.isBlueprintPositionOccupied(level, targetPos)) {
                continue;  // 已经放置了，跳过
            }

            // 获取这个位置需要的方块
            Block requiredBlock = getRequiredBlockForPosition(index);
            if (requiredBlock == null) {
                continue;
            }

            // 检查是否有该方块（根据模式检查不同来源）
            boolean hasBlock;
            if (this.isPickupMode) {
                // Pickup模式：检查容器和掉落物实体
                // 先获取蓝图状态，检查是否是可堆叠方块
                BlockState blueprintState = getBlueprintBlockState(index, level);
                int stackCount = getStackCountFromState(blueprintState);

                if (stackCount > 1) {
                    // 可堆叠方块：检查数量是否足够
                    int availableCount = countBlockItemInContainer(level, pos, requiredBlock);
                    hasBlock = availableCount >= stackCount;
                } else {
                    // 普通方块：检查是否有存量
                    ItemStack blockItem = this.peekSpecificBlockItemFromContainer(level, pos, requiredBlock);
                    hasBlock = !blockItem.isEmpty();
                }
            } else {
                // Move模式：检查源位置的方块
                BlockPos sourcePos = pos.relative(facing.getOpposite());
                BlockState sourceState = level.getBlockState(sourcePos);
                hasBlock = !sourceState.isAir() && sourceState.is(requiredBlock);
            }

            if (!hasBlock) {
                // 找到了缺失的方块
                ItemStack newMissingItem = new ItemStack(requiredBlock);
                if (!ItemStack.isSameItemSameComponents(this.missingBlockItem, newMissingItem)) {
                    this.missingBlockItem = newMissingItem;
                    this.onChanged();
                }
                return;
            }
        }

        // 所有位置都已放置完成或材料充足
        if (!this.missingBlockItem.isEmpty()) {
            this.missingBlockItem = ItemStack.EMPTY;
            this.onChanged();
        }
    }

    /**
     * 静态方法：根据放置器和扫描器的相对朝向旋转结构数据
     * 完全使用Minecraft原生的Rotation API
     * 注意：当前实现返回原始数据，旋转逻辑由调用方（如buildBlueprintPositions、getBlueprintBlockState）单独处理
     */
    @SuppressWarnings("checkstyle:OperatorWrap")
    public static StructureLoadUtil.StructureData rotateStructureDataStatic(
        StructureLoadUtil.StructureData originalData
    ) {
        return originalData;
    }

    public void tickServer(Level level, BlockPos pos) {
        final boolean previousPowered = this.isPowered;
        final boolean previousRedstoneSignal = this.hasRedstoneSignal;

        this.isPowered = this.grid != null && this.grid.isWorking();
        this.hasRedstoneSignal = level.hasNeighborSignal(pos);

        this.flushState(level, pos);

        boolean stateChanged = this.isPowered != previousPowered || this.hasRedstoneSignal != previousRedstoneSignal;

        boolean previousAbleToWork = previousPowered && !previousRedstoneSignal;
        boolean currentAbleToWork = this.isPowered && !this.hasRedstoneSignal;

        boolean indexReset = !previousAbleToWork && currentAbleToWork && this.currentPlacementIndex != 0;
        if (indexReset) {
            this.currentPlacementIndex = 0;
        }

        if (this.isPowered && !this.hasRedstoneSignal) {
            // 根据模式选择工作逻辑
            WorkMode mode = this.loadedStructure != null && !this.loadedStructure.isEmpty()
                            ? WorkMode.BLUEPRINT
                            : (this.isPickupMode ? WorkMode.PICKUP : WorkMode.MOVE);
            this.tickWorkMode(level, pos, mode);

            // 更新缺失方块信息（仅服务端）
            if (!level.isClientSide) {
                this.updateMissingBlockInfo(level, pos);
            }
        } else {
            boolean cooldownReset = this.placeCooldown != 0;
            if (cooldownReset) {
                this.placeCooldown = 0;
            }
            boolean heldItemCleared = !this.currentHeldBlock.isEmpty();
            if (heldItemCleared) {
                this.currentHeldBlock = ItemStack.EMPTY;
            }

            boolean shutdownIndexReset = this.currentPlacementIndex != 0;
            if (shutdownIndexReset) {
                this.currentPlacementIndex = 0;
            }

            // 断电时也更新缺失方块信息（清空显示）
            if (!level.isClientSide) {
                this.updateMissingBlockInfo(level, pos);
            }

            if (stateChanged || cooldownReset || heldItemCleared || shutdownIndexReset) {
                this.onChanged();
            }
        }
        
        // 无论是否工作，都要更新比较器信号（红石锁定时放置进度可能因外部因素变化）
        this.getComparatorOutput();
    }

    public void tickClient() {
        boolean isNewCycle = this.placeCooldown > this.lastPlaceCooldown
                             && this.placeCooldown >= PLACEMENT_INTERVAL;

        boolean wasIdle = this.lastPlaceCooldown == 0;
        boolean isNowWorking = this.placeCooldown > 0;
        boolean becameActive = wasIdle && isNowWorking;

        if (isNewCycle || becameActive) {
            this.clientAnimationStartTime = 0;
            this.clientLastTargetPos = null;
            this.clientIsRetracting = false;
        }

        this.lastPlaceCooldown = this.placeCooldown;
    }

    /**
     * 更新客户端动画状态
     */
    @SuppressWarnings("unused")
    public void updateClientAnimationState(boolean isCurrentlyPowered, boolean hasRedstoneSignal) {
        this.tickClient();
    }

    /**
     * 工作模式枚举
     */
    private enum WorkMode {
        PICKUP,     // 拾取模式：从容器获取方块并放置
        MOVE,       // 移动模式：从源位置移动方块到目标位置
        BLUEPRINT   // 蓝图模式：从结构文件放置方块
    }

    /**
     * 统一的工作模式tick逻辑
     */
    private void tickWorkMode(Level level, BlockPos pos, WorkMode mode) {
        boolean canWork = this.checkCanWork(level, pos, mode);
        boolean isResourceDepleted = this.checkResourceDepleted(level, pos, mode);

        if (stopWorkCycleIfResourceDepleted(isResourceDepleted)) {
            return;
        }

        tickCommonCooldownLogic(
            level,
            canWork,
            () -> this.executePlacement(level, pos, mode),
            () -> this.prepareHeldBlock(level, pos, mode)
        );
    }

    /**
     * 检查是否可以工作
     */
    private boolean checkCanWork(Level level, BlockPos pos, WorkMode mode) {
        return switch (mode) {
            case PICKUP -> this.hasEmptyPositions(level, pos) && this.hasBlockItemsInContainer(level, pos);
            case MOVE -> this.hasTargetPositions(level, pos);
            case BLUEPRINT -> {
                // 蓝图模式：根据isPickupMode区分检查逻辑
                if (this.isPickupMode) {
                    // Pickup模式：检查是否有可放置的位置且有物品
                    yield this.hasBlueprintPositions(level, pos) && this.hasBlockItemsInContainer(level, pos);
                } else {
                    // Move模式：检查是否有可放置的位置且源位置有方块
                    yield this.hasBlueprintMoveTargets(level, pos);
                }
            }
        };
    }

    /**
     * 检查资源是否耗尽
     */
    private boolean checkResourceDepleted(Level level, BlockPos pos, WorkMode mode) {
        return switch (mode) {
            case PICKUP -> !this.hasBlockItemsInContainer(level, pos);
            case MOVE -> !this.hasTargetPositions(level, pos);
            case BLUEPRINT -> {
                if (this.loadedStructure == null || this.loadedStructure.isEmpty()) {
                    yield true;
                }

                // 使用旋转后的结构数据
                StructureLoadUtil.StructureData rotatedData = rotateStructureDataStatic(this.loadedStructure);

                // 获取倒挂状态
                boolean upsideDown = level.getBlockState(pos).getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);

                // 获取有序索引列表
                List<Integer> orderedIndices = buildOrderedBlueprintIndices(rotatedData, upsideDown);
                boolean indexExhausted = this.currentPlacementIndex >= orderedIndices.size();

                if (indexExhausted) {
                    yield true;
                }

                // 根据isPickupMode选择不同的资源检查逻辑
                if (this.isPickupMode) {
                    // Pickup模式：检查容器是否为空
                    boolean containerEmpty = !this.hasBlockItemsInContainer(level, pos);

                    // 停止模式下，额外检查当前索引位置的方块是否有存量
                    if (!this.isSkipMissingMode && !containerEmpty) {
                        // 获取当前有序位置对应的原始索引
                        int actualIndex = orderedIndices.get(this.currentPlacementIndex);
                        Block requiredBlock = getRequiredBlockForPosition(actualIndex);
                        if (requiredBlock != null) {
                            // 检查是否是可堆叠方块
                            BlockState blueprintState = getBlueprintBlockState(actualIndex, level);
                            int stackCount = getStackCountFromState(blueprintState);

                            if (stackCount > 1) {
                                // 可堆叠方块：检查数量是否足够
                                int availableCount = countBlockItemInContainer(level, pos, requiredBlock);
                                yield availableCount < stackCount;  // 数量不足视为资源耗尽
                            } else {
                                // 普通方块：检查是否有存量
                                ItemStack blockItem = this.peekSpecificBlockItemFromContainer(level, pos, requiredBlock);
                                yield blockItem.isEmpty();  // 没有存量就视为资源耗尽
                            }
                        }
                    }

                    yield containerEmpty;
                } else {
                    // Move模式：检查源位置是否有方块
                    Direction facing = this.getFacing(pos, level);
                    BlockPos sourcePos = pos.relative(facing.getOpposite());

                    // 检查源位置是否有方块
                    BlockState sourceState = level.getBlockState(sourcePos);
                    boolean sourceEmpty = sourceState.isAir() || isBlockNotPushable(sourceState, level, sourcePos, facing);

                    // 停止模式下，额外检查当前索引位置的方块是否匹配
                    if (!this.isSkipMissingMode && !sourceEmpty) {
                        // 获取当前有序位置对应的原始索引
                        int actualIndex = orderedIndices.get(this.currentPlacementIndex);
                        Block requiredBlock = getRequiredBlockForPosition(actualIndex);
                        if (requiredBlock != null) {
                            // 源方块与当前位置需要的方块不匹配，视为资源耗尽
                            yield !sourceState.is(requiredBlock);
                        }
                    }

                    yield sourceEmpty;
                }
            }
        };
    }

    /**
     * 执行放置操作
     */
    private void executePlacement(Level level, BlockPos pos, WorkMode mode) {
        switch (mode) {
            case PICKUP -> this.placeBlocks(level, pos);
            case MOVE -> this.moveBlocks(level, pos);
            case BLUEPRINT -> {
                // 蓝图模式也根据isPickupMode决定使用pickup还是move逻辑
                if (this.isPickupMode) {
                    this.placeBlueprintBlocks(level, pos);  // 从容器提取物品
                } else {
                    this.moveBlueprintBlocks(level, pos);   // 直接从源位置拿方块
                }
            }
            default -> {
            }
        }
    }

    /**
     * 准备钳子中的方块（用于动画显示）
     */
    private void prepareHeldBlock(Level level, BlockPos pos, WorkMode mode) {
        switch (mode) {
            case PICKUP -> this.currentHeldBlock = this.peekBlockItemFromContainer(level, pos);
            case MOVE -> this.prepareMoveModeHeldBlock(level, pos);
            case BLUEPRINT -> this.prepareBlueprintModeHeldBlock(level, pos);
            default -> {
            }
        }
    }

    /**
     * 准备移动模式的钳子方块
     */
    private void prepareMoveModeHeldBlock(Level level, BlockPos pos) {
        Direction facing = level.getBlockState(pos).getValue(HorizontalDirectionalBlock.FACING);
        BlockPos sourcePos = pos.relative(facing.getOpposite());
        BlockState sourceState = level.getBlockState(sourcePos);
        ItemStack sourceItem = sourceState.getBlock().asItem().getDefaultInstance();
        if (!sourceItem.isEmpty() && sourceItem.getItem() instanceof BlockItem) {
            this.currentHeldBlock = sourceItem.copy();
        } else {
            this.currentHeldBlock = ItemStack.EMPTY;
        }
    }

    /**
     * 准备蓝图模式的钳子方块
     */
    private void prepareBlueprintModeHeldBlock(Level level, BlockPos pos) {
        if (this.loadedStructure == null || this.loadedStructure.isEmpty()) {
            this.currentHeldBlock = ItemStack.EMPTY;
            return;
        }

        Direction facing = this.getFacing(pos, level);
        boolean upsideDown = level.getBlockState(pos).getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);

        StructureLoadUtil.StructureData rotatedData = rotateStructureDataStatic(this.loadedStructure);
        List<BlockPos> allPositions = buildBlueprintPositions(pos, facing, upsideDown, rotatedData);

        if (allPositions.isEmpty()) {
            this.currentHeldBlock = ItemStack.EMPTY;
            return;
        }

        // 获取有序索引列表
        List<Integer> orderedIndices = buildOrderedBlueprintIndices(rotatedData, upsideDown);
        if (orderedIndices.isEmpty()) {
            this.currentHeldBlock = ItemStack.EMPTY;
            return;
        }

        // 重置索引（如果超出范围）
        if (this.currentPlacementIndex >= orderedIndices.size()) {
            this.currentPlacementIndex = 0;
        }

        // 根据isPickupMode选择不同的预览逻辑
        if (this.isPickupMode) {
            // Pickup模式：从容器中查找物品
            for (int i = 0; i < orderedIndices.size(); i++) {
                int orderIndex = (this.currentPlacementIndex + i) % orderedIndices.size();
                int index = orderedIndices.get(orderIndex);

                // 跳过多方块方块的次要部件
                if (this.loadedStructure != null && index < this.loadedStructure.blocks.size()
                    && StructureLoadUtil.isMultiblockSecondaryPart(
                        this.loadedStructure.blocks.get(index).state())) {
                    continue;
                }

                BlockPos targetPos = allPositions.get(index);

                // 检查目标位置是否可以放置（蓝图模式：只检查是否有方块，不检查状态）
                if (this.isBlueprintPositionOccupied(level, targetPos)) {
                    continue;
                }

                // 获取当前索引需要的方块
                Block requiredBlock = getRequiredBlockForPosition(index);
                if (requiredBlock == null) {
                    continue;
                }

                // 获取蓝图状态，检查是否是可堆叠方块
                BlockState blueprintState = getBlueprintBlockState(index, level);
                int stackCount = getStackCountFromState(blueprintState);

                if (stackCount > 1) {
                    // 可堆叠方块：检查容器中是否有足够的数量
                    int availableCount = countBlockItemInContainer(level, pos, requiredBlock);
                    if (availableCount < stackCount) {
                        // 数量不足，跳过
                        continue;
                    }
                } else {
                    // 普通方块：检查是否有物品
                    ItemStack blockItem = this.peekSpecificBlockItemFromContainer(level, pos, requiredBlock);
                    if (blockItem.isEmpty()) {
                        continue;
                    }
                    this.currentHeldBlock = blockItem.copy();
                    return;
                }

                // 可堆叠方块数量充足，设置 currentHeldBlock
                ItemStack blockItem = this.peekSpecificBlockItemFromContainer(level, pos, requiredBlock);
                if (!blockItem.isEmpty()) {
                    this.currentHeldBlock = blockItem.copy();
                    return;
                }
            }
        } else {
            // Move模式：从放置器后方1格预览方块
            BlockPos sourcePos = pos.relative(facing.getOpposite());
            BlockState sourceState = level.getBlockState(sourcePos);

            if (sourceState.isAir() || isBlockNotPushable(sourceState, level, sourcePos, facing)) {
                this.currentHeldBlock = ItemStack.EMPTY;
                return;
            }

            // 遍历查找第一个可以放置的位置，并且源方块与蓝图需要匹配
            for (int i = 0; i < orderedIndices.size(); i++) {
                int orderIndex = (this.currentPlacementIndex + i) % orderedIndices.size();
                int index = orderedIndices.get(orderIndex);

                // 跳过多方块方块的次要部件
                if (this.loadedStructure != null && index < this.loadedStructure.blocks.size()
                    && StructureLoadUtil.isMultiblockSecondaryPart(
                        this.loadedStructure.blocks.get(index).state())) {
                    continue;
                }

                BlockPos targetPos = allPositions.get(index);

                // 检查目标位置是否可以放置（蓝图模式：只检查是否有方块，不检查状态）
                if (this.isBlueprintPositionOccupied(level, targetPos)) {
                    continue;
                }

                // 获取蓝图需要的方块
                Block requiredBlock = getRequiredBlockForPosition(index);
                if (requiredBlock == null) {
                    continue;
                }

                // 检查源方块是否与蓝图需要的方块匹配
                if (sourceState.is(requiredBlock)) {
                    ItemStack sourceItem = sourceState.getBlock().asItem().getDefaultInstance();
                    if (!sourceItem.isEmpty()) {
                        this.currentHeldBlock = sourceItem.copy();
                        return;
                    }
                }
            }
        }

        // 没有找到有物品的方块
        this.currentHeldBlock = ItemStack.EMPTY;
    }

    /**
     * 资源耗尽时停止工作周期
     */
    private boolean stopWorkCycleIfResourceDepleted(boolean isResourceDepleted) {
        if (!isResourceDepleted) {
            return false;
        }

        // 清空钳子中的物品（停止动画）
        if (!this.currentHeldBlock.isEmpty()) {
            this.currentHeldBlock = ItemStack.EMPTY;
        }

        // 重置冷却，让下一个 tick 可以立即检查是否可以继续工作
        if (this.placeCooldown > 0) {
            this.placeCooldown = 0;
        }

        // 任何模式下，资源耗尽时都重置索引，从第一个位置开始重新检索
        if (this.currentPlacementIndex != 0) {
            this.currentPlacementIndex = 0;
        }

        this.onChanged();
        return true;
    }

    /**
     * 方块操作成功回调接口
     */
    @FunctionalInterface
    private interface BlockOperationSuccessHandler {
        boolean handle(ItemStack blockItem, BlockItem blockItemObj, BlockPos targetPos);
    }

    /**
     * 方块操作成功回调接口（支持ExtractionResult）
     */
    @FunctionalInterface
    private interface BlockOperationSuccessHandlerWithExtraction {
        boolean handle(ItemStack blockItem, BlockItem blockItemObj, BlockPos targetPos, @Nullable ExtractionResult extractionResult);
    }

    /**
     * 通用冷却控制逻辑
     */
    private void tickCommonCooldownLogic(
        Level level, boolean shouldExecute,
        Runnable executeAction, Runnable onCycleStart
    ) {
        long currentGameTime = level.getGameTime();
        boolean shouldDecrementCooldown = currentGameTime != this.lastTickGameTime;

        if (this.placeCooldown > 0 && shouldDecrementCooldown) {
            if (this.placeCooldown == PLACEMENT_DELAY && shouldExecute) {
                if (this.currentHeldBlock.isEmpty()) {
                    this.currentPlacementIndex = 0;
                }
                executeAction.run();
            }

            this.placeCooldown--;
        }

        if (shouldDecrementCooldown) {
            this.lastTickGameTime = currentGameTime;
        }

        if (this.placeCooldown == 0 && shouldExecute) {
            onCycleStart.run();

            this.placeCooldown = PLACEMENT_INTERVAL;
            this.lastTickGameTime = currentGameTime;
            this.onChanged();
        }
    }

    private boolean hasEmptyPositions(Level level, BlockPos placerPos) {
        Direction facing = level.getBlockState(placerPos).getValue(HorizontalDirectionalBlock.FACING);
        boolean upsideDown = level.getBlockState(placerPos).getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);
        BlockPos basePos = placerPos.relative(facing.getOpposite(), -4);

        return hasValidTargetPositions(level, basePos, facing, upsideDown);
    }

    private boolean hasTargetPositions(Level level, BlockPos placerPos) {
        if (this.layerPositions.isEmpty()) {
            return false;
        }

        Direction facing = level.getBlockState(placerPos).getValue(HorizontalDirectionalBlock.FACING);
        boolean upsideDown = level.getBlockState(placerPos).getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);
        BlockPos basePos = placerPos.relative(facing.getOpposite(), -4);
        BlockPos sourcePos = placerPos.relative(facing.getOpposite());

        BlockState sourceState = level.getBlockState(sourcePos);
        if (sourceState.isAir() || isBlockNotPushable(sourceState, level, sourcePos, facing)) {
            return false;
        }

        return hasValidTargetPositions(level, basePos, facing, upsideDown);
    }

    /**
     * 检查是否有有效的目标位置
     *
     * @param level      世界
     * @param basePos    基准位置
     * @param facing     朝向
     * @param upsideDown 是否倒挂
     * @return 是否存在有效位置
     */
    private boolean hasValidTargetPositions(
        Level level, BlockPos basePos, Direction facing,
        boolean upsideDown
    ) {
        for (Map.Entry<Integer, Set<Integer>> entry : this.layerPositions.entrySet()) {
            int layer = entry.getKey();
            for (int position : entry.getValue()) {
                BlockPos targetPos = SmartBlockPlacerBlockEntity
                    .calculateTargetPosition(basePos, facing, position / 5, position % 5, layer, upsideDown);
                BlockState targetState = level.getBlockState(targetPos);

                // 空位可以放置
                if (targetState.isAir()) {
                    return true;
                }
                
                // 流体可以放置
                if (!targetState.getFluidState().isEmpty()) {
                    return true;
                }
                
                // 可堆叠方块：检查是否还可以继续堆叠（不检查具体方块类型）
                int currentStack = getStackCountFromState(targetState);
                int maxStack = getMaxStackCountForState(targetState);
                if (currentStack < maxStack) {
                    return true;  // 还可以堆叠
                }
            }
        }
        return false;
    }

    /**
     * 检查蓝图模式是否有可放置的位置
     */
    private boolean hasBlueprintPositions(Level level, BlockPos placerPos) {
        if (this.loadedStructure == null || this.loadedStructure.isEmpty()) {
            return false;
        }

        Direction facing = this.getFacing(placerPos, level);
        boolean upsideDown = level.getBlockState(placerPos).getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);

        StructureLoadUtil.StructureData rotatedData = rotateStructureDataStatic(this.loadedStructure);
        List<BlockPos> allPositions = buildBlueprintPositions(placerPos, facing, upsideDown, rotatedData);

        if (allPositions.isEmpty()) {
            return false;
        }

        // 检查是否还有空位（按有序索引遍历，跳过次要部件）
        List<Integer> orderedIndices = buildOrderedBlueprintIndices(rotatedData, upsideDown);
        for (int index : orderedIndices) {
            // 跳过多方块方块的次要部件
            if (index < rotatedData.blocks.size()
                && StructureLoadUtil.isMultiblockSecondaryPart(
                    rotatedData.blocks.get(index).state())) {
                continue;
            }
            BlockPos targetPos = allPositions.get(index);
            // 蓝图模式：只检查位置是否为空（是否有方块）
            BlockState targetState = level.getBlockState(targetPos);
            if (targetState.isAir()) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查蓝图move模式是否有可移动的目标
     * 检查源位置是否有方块 且 蓝图位置是否有可放置的空位
     */
    private boolean hasBlueprintMoveTargets(Level level, BlockPos placerPos) {
        if (this.loadedStructure == null || this.loadedStructure.isEmpty()) {
            return false;
        }

        Direction facing = this.getFacing(placerPos, level);
        boolean upsideDown = level.getBlockState(placerPos).getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);
        BlockPos sourcePos = placerPos.relative(facing.getOpposite());

        // 检查源位置是否有方块
        BlockState sourceState = level.getBlockState(sourcePos);
        if (sourceState.isAir() || isBlockNotPushable(sourceState, level, sourcePos, facing)) {
            return false;
        }

        StructureLoadUtil.StructureData rotatedData = rotateStructureDataStatic(this.loadedStructure);
        List<BlockPos> allPositions = buildBlueprintPositions(placerPos, facing, upsideDown, rotatedData);

        if (allPositions.isEmpty()) {
            return false;
        }

        // 检查是否还有可放置的蓝图位置（跳过次要部件）
        List<Integer> orderedIndices = buildOrderedBlueprintIndices(rotatedData, upsideDown);
        for (int index : orderedIndices) {
            // 跳过多方块方块的次要部件
            if (index < rotatedData.blocks.size()
                && StructureLoadUtil.isMultiblockSecondaryPart(
                    rotatedData.blocks.get(index).state())) {
                continue;
            }
            BlockPos targetPos = allPositions.get(index);
            // 蓝图模式：只检查位置是否为空（是否有方块）
            BlockState targetState = level.getBlockState(targetPos);
            if (targetState.isAir()) {
                return true;
            }
        }

        return false;
    }

    private boolean isBlockNotPushable(BlockState state, Level level, BlockPos pos, Direction facing) {
        return !PistonBaseBlock.isPushable(
            state, level, pos, facing, false, facing
        );
    }

    /**
     * 判断是否可以放置方块（覆盖已有方块）
     */
    private boolean canBePlaced(Level level, BlockState blockState, @Nullable BlockItem blockItem) {
        if (level instanceof ServerLevel) {
            if (!blockState.getFluidState().isEmpty()) {
                return true;
            }
            if (blockState.is(Blocks.TURTLE_EGG)
                && blockState.getValue(TurtleEggBlock.EGGS) < 4) {
                return blockItem != null && blockState.getBlock() == blockItem.getBlock();
            }
            if (blockState.is(Blocks.SEA_PICKLE)
                && blockState.getValue(SeaPickleBlock.PICKLES) < 4) {
                return blockItem != null && blockState.getBlock() == blockItem.getBlock();
            }
            if (blockState.getBlock() instanceof CandleBlock) {
                if (blockState.getValue(CandleBlock.CANDLES) >= 4) {
                    return false;
                }
                return blockItem != null && blockState.getBlock() == blockItem.getBlock();
            }
            if (blockState.is(Blocks.PINK_PETALS)) {
                if (blockState.getValue(BlockStateProperties.FLOWER_AMOUNT) >= 4) {
                    return false;
                }
                return blockItem != null && blockState.getBlock() == blockItem.getBlock();
            }
        }
        return false;
    }

    /**
     * 检查位置是否可以放置方块
     *
     * @param level     世界
     * @param targetPos 目标位置
     * @param blockItem 要放置的物品（可为 null）
     * @return 是否可以放置
     */
    public boolean canPlaceAtPosition(Level level, BlockPos targetPos, @Nullable BlockItem blockItem) {
        BlockState targetState = level.getBlockState(targetPos);
        if (targetState.isAir()) {
            return true;
        }
        
        // 如果传入 null，只检查流体和可堆叠方块（不检查具体方块类型）
        if (blockItem == null) {
            // 流体可以放置
            if (!targetState.getFluidState().isEmpty()) {
                return true;
            }
            // 可堆叠方块：允许通过，具体匹配检查在预提取后进行
            if (targetState.is(Blocks.TURTLE_EGG)
                && targetState.getValue(TurtleEggBlock.EGGS) < 4) {
                return true;  // 还可以堆叠，允许通过
            }
            if (targetState.is(Blocks.SEA_PICKLE)
                && targetState.getValue(SeaPickleBlock.PICKLES) < 4) {
                return true;  // 还可以堆叠，允许通过
            }
            if (targetState.getBlock() instanceof CandleBlock
                && targetState.getValue(CandleBlock.CANDLES) < 4) {
                return true;  // 还可以堆叠，允许通过
            }
            return targetState.is(Blocks.PINK_PETALS)
                   && targetState.getValue(BlockStateProperties.FLOWER_AMOUNT) < 4;  // 还可以堆叠，允许通过
        }
        
        // 传入具体物品时，检查方块匹配
        return this.canBePlaced(level, targetState, blockItem);
    }

    /**
     * 检查位置是否已被占据（不能放置）
     *
     * @param level     世界
     * @param targetPos 目标位置
     * @param blockItem 要放置的物品（可为 null）
     * @return 如果位置已被占据返回 true
     */
    private boolean isPositionOccupied(Level level, BlockPos targetPos, @Nullable BlockItem blockItem) {
        return !this.canPlaceAtPosition(level, targetPos, blockItem);
    }

    /**
     * 检查蓝图位置是否已被占据（只检查是否有方块，不检查状态）
     * 用于蓝图模式，防止覆盖已有方块
     *
     * @param level     世界
     * @param targetPos 目标位置
     * @return 如果位置已有方块（非空气）返回 true
     */
    private boolean isBlueprintPositionOccupied(Level level, BlockPos targetPos) {
        BlockState targetState = level.getBlockState(targetPos);
        return !targetState.isAir();
    }

    private boolean hasBlockItemsInContainer(Level level, BlockPos placerPos) {
        return !getBlockItemFromContainer(level, placerPos).isEmpty();
    }

    private Direction getFacing(BlockPos pos, Level level) {
        return level.getBlockState(pos).getValue(HorizontalDirectionalBlock.FACING);
    }

    /**
     * 放置方块（pickup模式）
     */
    private void placeBlocks(Level level, BlockPos placerPos) {
        Direction facing = this.getFacing(placerPos, level);
        boolean upsideDown = level.getBlockState(placerPos).getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);

        // 使用预提取逻辑，放置成功后才真正删除ItemEntity
        executeUnifiedBlockOperationWithExtraction(
            level, facing, upsideDown,
            () -> buildOrderedPositionsFromLayers(placerPos, facing, upsideDown),
            (index) -> {
                // 获取当前位置的方块状态
                List<BlockPos> allPositions = buildOrderedPositionsFromLayers(placerPos, facing, upsideDown);
                if (index >= 0 && index < allPositions.size()) {
                    BlockPos targetPos = allPositions.get(index);
                    BlockState currentState = level.getBlockState(targetPos);
                    
                    // 如果是可堆叠方块，必须提取匹配的方块物品
                    int currentStack = getStackCountFromState(currentState);
                    int maxStack = getMaxStackCountForState(currentState);
                    if (currentStack < maxStack) {
                        // 可堆叠方块：提取与当前位置匹配的方块物品
                        Block requiredBlock = currentState.getBlock();
                        return this.preExtractSpecificBlockItemFromContainer(level, placerPos, requiredBlock);  // 找到了匹配的方块
                        // 没找到匹配的方块，返回 null 阻止放置
                    }
                }
                
                // 空位或其他情况：提取容器中的第一个方块物品
                return this.preExtractBlockItemFromContainer(level, placerPos);
            },
            (blockItem, blockItemObj, targetPos, extractionResult) -> {
                BlockState newState = level.getBlockState(targetPos);
                
                // 关键修复：基于放置后的方块状态判断是否可堆叠，而不是基于预提取的物品
                if (!newState.isAir()) {
                    // 检查是否是可堆叠方块
                    int currentStack = getStackCountFromState(newState);
                    int maxStack = getMaxStackCountForState(newState);
                    
                    // 如果当前数量小于最大数量，说明还可以继续堆叠
                    if (currentStack < maxStack) {
                        // 可堆叠方块且未达到上限，设置 heldBlock 用于动画显示
                        this.currentHeldBlock = blockItem.copy();
                        // 返回 true 保持当前索引，下次 tick 继续放置
                        return true;
                    }
                }
                
                // 不可堆叠或已达上限，清空 heldBlock
                this.currentHeldBlock = ItemStack.EMPTY;
                // 返回 false 移动到下一个位置
                return false;
            }
        );
    }

    /**
     * 移动方块（move模式）
     */
    private void moveBlocks(Level level, BlockPos placerPos) {
        Direction facing = this.getFacing(placerPos, level);
        boolean upsideDown = level.getBlockState(placerPos).getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);
        BlockPos sourcePos = placerPos.relative(facing.getOpposite());

        // 检查源方块
        BlockState sourceState = level.getBlockState(sourcePos);
        if (sourceState.isAir() || isBlockNotPushable(sourceState, level, sourcePos, facing)) {
            return;
        }

        // 保存源BlockEntity的NBT数据（如果有）
        final CompoundTag sourceBlockEntityData;
        BlockEntity sourceBlockEntity = level.getBlockEntity(sourcePos);
        if (sourceBlockEntity != null) {
            sourceBlockEntityData = sourceBlockEntity.saveWithFullMetadata(level.registryAccess());
        } else {
            sourceBlockEntityData = null;
        }

        final ItemStack sourceItem = sourceState.getBlock().asItem().getDefaultInstance();

        executeUnifiedBlockOperation(
            level, facing, upsideDown,
            () -> buildOrderedPositionsFromLayers(placerPos, facing, upsideDown),
            (index) -> sourceItem,  // 忽略 index，总是源方块
            () -> sourceItem,
            (blockItem, blockItemObj, targetPos) -> {
                BlockState stateToPlace = sourceState;

                // 侦测器不继承POWERED状态
                if (stateToPlace.is(Blocks.OBSERVER)
                    && stateToPlace.hasProperty(BlockStateProperties.POWERED)) {
                    stateToPlace = stateToPlace.setValue(
                        BlockStateProperties.POWERED,
                        false
                    );
                }

                if (sourceState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                    stateToPlace = sourceState.setValue(
                        BlockStateProperties.WATERLOGGED,
                        false
                    );
                }

                // 先删除源方块
                IS_BEING_MOVED_BY_PLACER.set(true);
                try {
                    level.removeBlock(sourcePos, false);
                } finally {
                    IS_BEING_MOVED_BY_PLACER.set(false);
                }

                // 放置方块到目标位置
                level.setBlock(targetPos, stateToPlace, Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);

                if (sourceBlockEntityData != null) {
                    BlockEntity targetBlockEntity = level.getBlockEntity(targetPos);
                    if (targetBlockEntity != null) {
                        targetBlockEntity.loadWithComponents(sourceBlockEntityData, level.registryAccess());
                        targetBlockEntity.setChanged();
                    }
                }

                // 在目标位置发送方块更新通知，让红石灯等方块根据新位置的红石信号更新状态
                level.neighborChanged(targetPos, stateToPlace.getBlock(), targetPos);

                this.currentHeldBlock = ItemStack.EMPTY;
                return false;
            }
        );
    }

    /**
     * 放置蓝图中的方块（pickup逻辑：从容器提取物品）
     */
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    private void placeBlueprintBlocks(Level level, BlockPos placerPos) {
        Direction facing = this.getFacing(placerPos, level);
        boolean upsideDown = level.getBlockState(placerPos).getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);

        if (this.loadedStructure == null || this.loadedStructure.isEmpty()) {
            return;
        }

        // 获取旋转后的结构数据
        StructureLoadUtil.StructureData rotatedData = rotateStructureDataStatic(this.loadedStructure);

        List<BlockPos> allPositions = buildBlueprintPositions(placerPos, facing, upsideDown, rotatedData);
        if (allPositions.isEmpty()) {
            return;
        }

        // 获取有序索引列表（按 y → z → x 排序）
        List<Integer> orderedIndices = buildOrderedBlueprintIndices(rotatedData, upsideDown);
        if (orderedIndices.isEmpty()) {
            return;
        }

        // 重置索引（如果超出范围）
        if (this.currentPlacementIndex >= orderedIndices.size()) {
            this.currentPlacementIndex = 0;
        }

        // 按有序索引列表遍历
        for (int i = 0; i < orderedIndices.size(); i++) {
            int orderIndex = (this.currentPlacementIndex + i) % orderedIndices.size();
            int index = orderedIndices.get(orderIndex);  // 获取原始数据中的真实索引
            BlockPos targetPos = allPositions.get(index);  // 使用真实索引获取位置

            // 获取当前索引需要的方块
            Block requiredBlock = getRequiredBlockForPosition(index);
            if (requiredBlock == null) {
                continue;
            }

            // 跳过多方块方块的次要部件（部件会在主体部件放置时自动创建并修正朝向）
            if (this.loadedStructure != null && index < this.loadedStructure.blocks.size()) {
                BlockState originalState = this.loadedStructure.blocks.get(index).state();
                if (StructureLoadUtil.isMultiblockSecondaryPart(originalState)) {
                    continue;
                }
            }

            // 先获取物品，用于后续的位置检查
            ItemStack blockItem = this.peekSpecificBlockItemFromContainer(level, placerPos, requiredBlock);
            if (blockItem.isEmpty() || !(blockItem.getItem() instanceof BlockItem)) {
                // Stop mode：容器中没有需要的方块，停止在当前位置
                if (!this.isSkipMissingMode) {
                    this.currentHeldBlock = ItemStack.EMPTY;
                    this.onChanged();
                    return;
                }
                // Skip mode：容器中没有这个方块，继续查找下一个
                if (!this.currentHeldBlock.isEmpty()) {
                    this.currentHeldBlock = ItemStack.EMPTY;
                    this.onChanged();
                }
                continue;
            }

            // 获取蓝图状态，检查是否是可堆叠方块
            BlockState blueprintState = getBlueprintBlockState(index, level);
            int stackCount = getStackCountFromState(blueprintState);

            if (stackCount > 1) {
                // 可堆叠方块：先检查容器中是否有足够的物品
                int availableCount = countBlockItemInContainer(level, placerPos, requiredBlock);

                if (availableCount < stackCount) {
                    if (!this.isSkipMissingMode) {
                        // Stop mode：停止在当前位置
                        this.currentHeldBlock = ItemStack.EMPTY;
                        this.onChanged();
                        return;
                    }
                    // Skip mode：跳过这个位置
                    continue;
                }
            }

            // 检查目标位置是否可以放置（蓝图模式：只检查是否有方块，不检查状态）
            if (this.isBlueprintPositionOccupied(level, targetPos)) {
                continue;
            }

            // 物品充足且位置可以放置，设置 currentHeldBlock 用于动画显示
            this.currentHeldBlock = blockItem.copy();
            this.onChanged();

            if (stackCount > 1) {
                // 可堆叠方块：需要提取 stackCount 个物品
                if (!extractAndPlaceStackableBlock(
                    level, placerPos, targetPos, facing, upsideDown,
                    requiredBlock, stackCount, index, orderIndex, orderedIndices.size()
                )) {
                    // 提取或放置失败
                    this.currentHeldBlock = ItemStack.EMPTY;
                    this.onChanged();
                    return;
                }
                // 成功，已经在 extractAndPlaceStackableBlock 中更新了索引
                return;
            }

            // 普通方块：提取并放置单个物品
            ItemStack extractedItem = this.extractSpecificBlockItemFromContainer(level, placerPos, requiredBlock);
            if (extractedItem.isEmpty() || !(extractedItem.getItem() instanceof BlockItem extractedBlockItemObj)) {
                // 提取失败，清空并继续
                this.currentHeldBlock = ItemStack.EMPTY;
                this.onChanged();
                continue;
            }

            // 多方块方块使用蓝图中的朝向进行放置，确保所有部件位置正确
            Direction placementFacing = facing;
            if (StructureLoadUtil.isMultiblockBlock(requiredBlock)) {
                Direction desiredFacing = extractDesiredHorizontalFacing(blueprintState);
                if (desiredFacing != null) {
                    placementFacing = desiredFacing;
                }
            }

            // 放置方块
            boolean placeSuccess = this.tryPlaceBlockWithFakePlayer(
                level, targetPos, placementFacing, upsideDown, extractedBlockItemObj, extractedItem);

            if (!placeSuccess) {
                // 放置失败，回滚物品
                this.rollbackExtractedItem(level, placerPos, extractedItem);
                this.currentHeldBlock = ItemStack.EMPTY;
                this.currentPlacementIndex = (orderIndex + 1) % orderedIndices.size();
                this.onChanged();
                return;
            }

            // 放置成功后，修正方块的朝向为蓝图中的朝向
            if (StructureLoadUtil.isMultiblockBlock(requiredBlock)) {
                // 多方块方块：应用蓝图状态到所有部件（包括 setPlacedBy 创建的次要部件）
                applyMultiBlockBlueprintStates(level, allPositions, rotatedData, index, requiredBlock);
            } else {
                this.applyBlueprintBlockFacing(level, targetPos, index);
            }

            // 在目标位置发送方块更新通知，让红石灯等方块根据新位置的状态更新
            level.neighborChanged(targetPos, level.getBlockState(targetPos).getBlock(), targetPos);

            // 放置成功
            this.currentHeldBlock = ItemStack.EMPTY;
            this.currentPlacementIndex = (orderIndex + 1) % orderedIndices.size();
            this.onChanged();
            return;
        }

        // 所有位置都遍历完了，没有找到可以放置的
        this.currentHeldBlock = ItemStack.EMPTY;
        this.onChanged();
    }

    /**
     * 移动蓝图中的方块（move逻辑：从放置器后方1格提取方块，放置到蓝图位置）
     * 按有序索引列表遍历，与pickup模式保持一致
     */
    private void moveBlueprintBlocks(Level level, BlockPos placerPos) {
        Direction facing = this.getFacing(placerPos, level);
        boolean upsideDown = level.getBlockState(placerPos).getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);
        BlockPos sourcePos = placerPos.relative(facing.getOpposite());

        if (this.loadedStructure == null || this.loadedStructure.isEmpty()) {
            return;
        }

        // 检查源方块
        BlockState sourceState = level.getBlockState(sourcePos);
        if (sourceState.isAir() || isBlockNotPushable(sourceState, level, sourcePos, facing)) {
            return;
        }

        // 获取旋转后的结构数据
        StructureLoadUtil.StructureData rotatedData = rotateStructureDataStatic(this.loadedStructure);

        List<BlockPos> allPositions = buildBlueprintPositions(placerPos, facing, upsideDown, rotatedData);
        if (allPositions.isEmpty()) {
            return;
        }

        // 获取有序索引列表（按 y → z → x 排序）
        List<Integer> orderedIndices = buildOrderedBlueprintIndices(rotatedData, upsideDown);
        if (orderedIndices.isEmpty()) {
            return;
        }

        // 重置索引（如果超出范围）
        if (this.currentPlacementIndex >= orderedIndices.size()) {
            this.currentPlacementIndex = 0;
        }

        // 保存源BlockEntity的NBT数据（如果有）
        final CompoundTag sourceBlockEntityData;
        BlockEntity sourceBlockEntity = level.getBlockEntity(sourcePos);
        if (sourceBlockEntity != null) {
            sourceBlockEntityData = sourceBlockEntity.saveWithFullMetadata(level.registryAccess());
        } else {
            sourceBlockEntityData = null;
        }

        final ItemStack sourceItem = sourceState.getBlock().asItem().getDefaultInstance();

        // 按有序索引列表遍历
        for (int i = 0; i < orderedIndices.size(); i++) {
            int orderIndex = (this.currentPlacementIndex + i) % orderedIndices.size();
            int index = orderedIndices.get(orderIndex);  // 获取原始数据中的真实索引
            BlockPos targetPos = allPositions.get(index);  // 使用真实索引获取位置

            // 检查目标位置是否可以放置（蓝图模式：只检查是否有方块，不检查状态）
            if (this.isBlueprintPositionOccupied(level, targetPos)) {
                continue;
            }

            // 获取蓝图需要的方块
            Block requiredBlock = getRequiredBlockForPosition(index);
            if (requiredBlock == null) {
                continue;
            }

            // 跳过多方块方块的次要部件（部件会在主体部件放置时自动创建并修正朝向）
            if (this.loadedStructure != null && index < this.loadedStructure.blocks.size()) {
                BlockState originalState = this.loadedStructure.blocks.get(index).state();
                if (StructureLoadUtil.isMultiblockSecondaryPart(originalState)) {
                    continue;
                }
            }

            // 检查源方块是否与蓝图需要的方块匹配
            if (!sourceState.is(requiredBlock)) {
                // Stop mode：源方块与蓝图不匹配，停止在当前位置
                if (!this.isSkipMissingMode) {
                    this.currentHeldBlock = ItemStack.EMPTY;
                    this.onChanged();
                    return;
                }
                // Skip mode：跳过这个位置，继续查找下一个
                continue;
            }

            // 设置 currentHeldBlock 用于动画显示
            this.currentHeldBlock = sourceItem.copy();
            this.onChanged();

            // 获取蓝图中的目标状态（已经包含旋转、倒挂和状态过滤处理）
            BlockState blueprintState = getBlueprintBlockState(index, level);

            // 多方块方块使用蓝图中的朝向进行放置，确保所有部件位置正确
            Direction placementFacing = facing;
            if (StructureLoadUtil.isMultiblockBlock(requiredBlock)) {
                Direction desiredFacing = extractDesiredHorizontalFacing(blueprintState);
                if (desiredFacing != null) {
                    placementFacing = desiredFacing;
                }
            }

            // 使用 FakePlayer 放置方块
            boolean placeSuccess = this.tryPlaceBlockWithFakePlayer(
                level, targetPos, placementFacing, upsideDown,
                (BlockItem) sourceItem.getItem(), sourceItem
            );

            if (!placeSuccess) {
                // 放置失败，回退索引
                this.currentHeldBlock = ItemStack.EMPTY;
                this.currentPlacementIndex = (orderIndex + 1) % orderedIndices.size();
                this.onChanged();
                return;
            }

            // 先删除源方块
            IS_BEING_MOVED_BY_PLACER.set(true);
            try {
                level.removeBlock(sourcePos, false);
            } finally {
                IS_BEING_MOVED_BY_PLACER.set(false);
            }

            // 放置成功后，修正方块的朝向为蓝图中的朝向
            if (StructureLoadUtil.isMultiblockBlock(requiredBlock)) {
                // 多方块方块：应用蓝图状态到所有部件（包括 setPlacedBy 创建的次要部件）
                applyMultiBlockBlueprintStates(level, allPositions, rotatedData, index, requiredBlock);
            } else {
                this.applyBlueprintBlockFacing(level, targetPos, index);
                // 使用蓝图的状态覆盖目标位置的方块（包含正确的朝向），只更新客户端
                level.setBlock(targetPos, blueprintState, Block.UPDATE_CLIENTS);
            }

            if (sourceBlockEntityData != null) {
                BlockEntity targetBlockEntity = level.getBlockEntity(targetPos);
                if (targetBlockEntity != null) {
                    targetBlockEntity.loadWithComponents(sourceBlockEntityData, level.registryAccess());
                    targetBlockEntity.setChanged();
                }
            }

            // 在目标位置发送方块更新通知
            level.neighborChanged(targetPos, blueprintState.getBlock(), targetPos);

            // 放置成功
            this.currentHeldBlock = ItemStack.EMPTY;
            this.currentPlacementIndex = (orderIndex + 1) % orderedIndices.size();
            this.onChanged();
            return;
        }

        // 所有位置都遍历完了，没有找到可以放置的
        this.currentHeldBlock = ItemStack.EMPTY;
        this.onChanged();
    }

    /**
     * 统一方块操作执行方法（用于move模式）
     * 注意：move模式放置失败时不回滚物品，因为源方块仍在原位
     *
     * @param level            世界
     * @param facing           朝向
     * @param upsideDown       是否倒挂
     * @param positionProvider 位置列表提供者
     * @param itemExtractor    物品提取器（接收位置索引，返回物品）
     * @param itemPeeker       物品预览器
     * @param onSuccess        成功回调
     */
    private void executeUnifiedBlockOperation(
        Level level,
        Direction facing,
        boolean upsideDown,
        Supplier<List<BlockPos>> positionProvider,
        IntFunction<ItemStack> itemExtractor,
        @Nullable Supplier<ItemStack> itemPeeker,
        BlockOperationSuccessHandler onSuccess
    ) {

        List<BlockPos> allPositions = positionProvider.get();

        if (allPositions.isEmpty()) {
            return;
        }

        // 重置索引（如果超出范围）
        if (this.currentPlacementIndex >= allPositions.size()) {
            this.currentPlacementIndex = 0;
        }

        // 从当前索引开始查找空位
        for (int i = 0; i < allPositions.size(); i++) {
            int index = (this.currentPlacementIndex + i) % allPositions.size();
            BlockPos targetPos = allPositions.get(index);

            // 检查目标位置是否可以放置
            if (this.isPositionOccupied(level, targetPos, null)) {
                continue;
            }

            // 如果有预览器，先预览检查（pickup/move 模式）
            if (itemPeeker != null) {
                ItemStack peekedBlockItem = itemPeeker.get();
                if (peekedBlockItem.isEmpty() || !(peekedBlockItem.getItem() instanceof BlockItem peekedBlockItemObj)) {
                    this.currentPlacementIndex = (index + 1) % allPositions.size();
                    this.onChanged();
                    return;
                }

                if (this.isPositionOccupied(level, targetPos, peekedBlockItemObj)) {
                    this.currentPlacementIndex = (index + 1) % allPositions.size();
                    this.onChanged();
                    return;
                }
            }

            // 提取物品（传入当前位置索引）
            ItemStack blockItem = itemExtractor.apply(index);
            if (blockItem.isEmpty() || !(blockItem.getItem() instanceof BlockItem blockItemObj)) {
                // 容器中没有物品或物品类型不对，立即停止
                this.currentHeldBlock = ItemStack.EMPTY;  // 清空动画显示
                this.currentPlacementIndex = (index + 1) % allPositions.size();
                this.onChanged();
                return;
            }

            // 使用 FakePlayer 放置方块
            boolean placeSuccess = this.tryPlaceBlockWithFakePlayer(level, targetPos, facing, upsideDown, blockItemObj, blockItem);

            // 放置失败时直接返回（move模式不需要回滚，源方块仍在原位）
            if (!placeSuccess) {
                this.onChanged();
                return;
            }

            // 执行成功回调
            boolean canStack = onSuccess.handle(blockItem, blockItemObj, targetPos);

            if (canStack) {
                this.onChanged();
                return;
            }

            // 更新索引
            this.currentPlacementIndex = (index + 1) % allPositions.size();
            this.onChanged();
            return;
        }
    }

    /**
     * 统一方块操作执行方法（支持预提取逻辑）
     * 用于pickup模式，在放置成功后才真正删除ItemEntity
     */
    private void executeUnifiedBlockOperationWithExtraction(
        Level level,
        Direction facing,
        boolean upsideDown,
        Supplier<List<BlockPos>> positionProvider,
        IntFunction<ExtractionResult> itemExtractor,
        BlockOperationSuccessHandlerWithExtraction onSuccess
    ) {

        List<BlockPos> allPositions = positionProvider.get();

        if (allPositions.isEmpty()) {
            return;
        }

        // 重置索引（如果超出范围）
        if (this.currentPlacementIndex >= allPositions.size()) {
            this.currentPlacementIndex = 0;
        }

        // 从当前索引开始查找空位
        for (int i = 0; i < allPositions.size(); i++) {
            int index = (this.currentPlacementIndex + i) % allPositions.size();
            BlockPos targetPos = allPositions.get(index);

            // 使用 null 检查位置（只检查空位和流体，不检查具体方块类型）
            if (this.isPositionOccupied(level, targetPos, null)) {
                continue;
            }

            // 预提取物品（不真正删除ItemEntity）
            ExtractionResult extractionResult = itemExtractor.apply(index);
            if (extractionResult == null || extractionResult.getItemStack().isEmpty()
                || !(extractionResult.getItemStack().getItem() instanceof BlockItem)) {
                // 容器中没有物品或物品类型不对，立即停止
                this.currentHeldBlock = ItemStack.EMPTY;  // 清空动画显示
                this.currentPlacementIndex = (index + 1) % allPositions.size();
                this.onChanged();
                return;
            }

            ItemStack blockItem = extractionResult.getItemStack();
            BlockItem blockItemObj = (BlockItem) blockItem.getItem();

            // 使用 FakePlayer 放置方块
            boolean placeSuccess = this.tryPlaceBlockWithFakePlayer(level, targetPos, facing, upsideDown, blockItemObj, blockItem);

            // 放置失败时，不需要回滚（因为ItemEntity还没被删除）
            if (!placeSuccess) {
                this.onChanged();
                return;
            }

            // 放置成功后，修正方块的朝向为蓝图中的朝向
            this.applyBlueprintBlockFacing(level, targetPos, index);

            // 在目标位置发送方块更新通知，让红石灯等方块根据新位置的状态更新
            level.neighborChanged(targetPos, level.getBlockState(targetPos).getBlock(), targetPos);

            // 放置成功，确认提取（真正删除或修改ItemEntity）
            extractionResult.confirmExtraction();

            // 执行成功回调
            boolean canStack = onSuccess.handle(blockItem, blockItemObj, targetPos, extractionResult);

            if (canStack) {
                this.onChanged();
                return;
            }

            // 更新索引
            this.currentPlacementIndex = (index + 1) % allPositions.size();
            this.onChanged();
            return;
        }
    }

    /**
     * 从 layerPositions 构建有序位置列表
     */
    private List<BlockPos> buildOrderedPositionsFromLayers(BlockPos placerPos, Direction facing, boolean upsideDown) {
        BlockPos basePos = placerPos.relative(facing.getOpposite(), -4);
        return buildOrderedPositions(basePos, facing, this.layerPositions, upsideDown);
    }

    /**
     * 从结构数据构建位置列表（静态公共方法，供渲染器调用）
     * 恢复相对位置计算，让蓝图中心点位于放置范围中心
     * 但不应用旋转
     */
    @SuppressWarnings("unused")
    public static List<BlockPos> buildBlueprintPositions(
        BlockPos placerPos,
        Direction forward,
        boolean upsideDown,
        StructureLoadUtil.StructureData rotatedData
    ) {
        if (rotatedData.blocks.isEmpty()) {
            return List.of();
        }

        // 计算结构中心点
        int centerX = rotatedData.diskData.sizeX() / 2;
        int centerZ = rotatedData.diskData.sizeZ() / 2;

        // 计算基准位置（放置器前方4格）
        BlockPos basePos = placerPos.relative(forward.getOpposite(), -4);
        List<BlockPos> positions = new ArrayList<>();

        // 根据放置器朝向和Scanner朝向计算实际旋转
        // Scanner和Placer南北相反，需要修正
        Rotation rotation = getRotationForPlacement(forward, rotatedData.diskData.direction());

        // 使用原始坐标，但相对于中心点偏移
        for (StructureLoadUtil.BlockPosition blueprintBlock : rotatedData.blocks) {
            // 计算相对于中心的偏移
            int offsetX = blueprintBlock.x() - centerX;
            int offsetZ = blueprintBlock.z() - centerZ;

            // 倒挂情况下，y轴需要下移（与普通模式保持一致）
            int offsetY = upsideDown ? -blueprintBlock.y() : blueprintBlock.y();

            // 根据放置器朝向计算目标位置
            Direction left = forward.getCounterClockWise();

            BlockPos targetPos = basePos
                .relative(left, offsetX)
                .relative(forward, offsetZ)
                .above(offsetY);

            positions.add(targetPos);
        }

        return positions;
    }

    /**
     * 计算放置器朝向和Scanner朝向的旋转步数
     *
     * @param forward       放置器朝向
     * @param scannerFacing Scanner朝向值
     * @return 旋转步数 (0-3)
     */
    private static int calculateRotationSteps(Direction forward, Direction scannerFacing) {
        // 1. 计算放置器朝向的基础旋转
        int placerRotation = switch (forward) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };

        // 2. 根据Scanner朝向计算额外修正
        int scannerCorrection = switch (scannerFacing) {
            case Direction.NORTH -> 2;  // Scanner北 → +180度
            case Direction.SOUTH -> 2;  // Scanner南 → +180度
            case Direction.WEST -> 3;  // Scanner西 → +270度
            case Direction.EAST -> 1;  // Scanner东 → +90度
            default -> 0;
        };

        // 3. Scanner朝南时额外+180度（在修正基础上再翻180）
        int extraFlip = (scannerFacing == Direction.SOUTH) ? 2 : 0;

        // 4. 总旋转步数 = 基础旋转 + Scanner修正 + Scanner朝南额外翻转
        return (placerRotation + scannerCorrection + extraFlip) % 4;
    }

    /**
     * 获取放置器朝向和Scanner朝向对应的Minecraft原生Rotation
     *
     * @param forward       放置器朝向
     * @param scannerFacing Scanner朝向值
     * @return Minecraft Rotation对象
     */
    private static Rotation getRotationForPlacement(Direction forward, Direction scannerFacing) {
        int rotationSteps = calculateRotationSteps(forward, scannerFacing);
        return switch (rotationSteps) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /**
     * 翻转方块状态的 half 属性（top <-> bottom）
     * 用于倒挂情况下正确放置楼梯、台阶等有 half 属性的方块
     *
     * @param state 原始方块状态
     * @return 翻转 half 属性后的方块状态
     */
    @SuppressWarnings("unused")
    public static BlockState flipHalfPropertyStatic(BlockState state) {
        // 处理 Minecraft 原生的 Half 属性（楼梯等）
        EnumProperty<Half> halfProperty = BlockStateProperties.HALF;
        if (state.hasProperty(halfProperty)) {
            Half
                currentHalf = state.getValue(halfProperty);
            Half
                flippedHalf = currentHalf == Half.TOP
                              ? Half.BOTTOM
                              : Half.TOP;
            return state.setValue(halfProperty, flippedHalf);
        }

        // 处理台阶方块的 SlabType 属性(BOTTOM, TOP, DOUBLE)
        if (state.hasProperty(BlockStateProperties.SLAB_TYPE)) {
            SlabType currentType = state.getValue(BlockStateProperties.SLAB_TYPE);
            SlabType flippedType = switch (currentType) {
                case BOTTOM -> SlabType.TOP;
                case TOP -> SlabType.BOTTOM;
                case DOUBLE -> SlabType.DOUBLE; // DOUBLE 不需要翻转
            };
            return state.setValue(BlockStateProperties.SLAB_TYPE, flippedType);
        }

        // 处理六向方块的 VERTICAL_DIRECTION 属性(UP, DOWN)
        if (state.hasProperty(BlockStateProperties.VERTICAL_DIRECTION)) {
            Direction currentDir = state.getValue(BlockStateProperties.VERTICAL_DIRECTION);
            Direction flippedDir = switch (currentDir) {
                case UP -> Direction.DOWN;
                case DOWN -> Direction.UP;
                default -> currentDir; // 水平方向不需要翻转
            };
            return state.setValue(BlockStateProperties.VERTICAL_DIRECTION, flippedDir);
        }

        // 处理六向方块的 ORIENTATION 属性(Minecraft 原生的 FrontAndTop,用于钟等方块)
        if (state.hasProperty(BlockStateProperties.ORIENTATION)) {
            net.minecraft.core.FrontAndTop currentOrientation =
                state.getValue(BlockStateProperties.ORIENTATION);
            // 倒挂时,翻转垂直方向:UP <-> DOWN,水平方向旋转180度
            net.minecraft.core.FrontAndTop flippedOrientation = switch (currentOrientation) {
                case DOWN_EAST -> net.minecraft.core.FrontAndTop.UP_EAST;
                case DOWN_NORTH -> net.minecraft.core.FrontAndTop.UP_NORTH;
                case DOWN_SOUTH -> net.minecraft.core.FrontAndTop.UP_SOUTH;
                case DOWN_WEST -> net.minecraft.core.FrontAndTop.UP_WEST;
                case UP_EAST -> net.minecraft.core.FrontAndTop.DOWN_EAST;
                case UP_NORTH -> net.minecraft.core.FrontAndTop.DOWN_NORTH;
                case UP_SOUTH -> net.minecraft.core.FrontAndTop.DOWN_SOUTH;
                case UP_WEST -> net.minecraft.core.FrontAndTop.DOWN_WEST;
                // 侧向附着不需要垂直翻转
                case WEST_UP, EAST_UP, NORTH_UP, SOUTH_UP -> currentOrientation;
            };
            return state.setValue(BlockStateProperties.ORIENTATION, flippedOrientation);
        }

        // 处理 FACING 属性(6个方向:UP, DOWN, NORTH, SOUTH, EAST, WEST)
        if (state.hasProperty(BlockStateProperties.FACING)) {
            Direction currentFacing = state.getValue(BlockStateProperties.FACING);
            Direction flippedFacing = switch (currentFacing) {
                case UP -> Direction.DOWN;
                case DOWN -> Direction.UP;
                case NORTH -> Direction.SOUTH;
                case SOUTH -> Direction.NORTH;
                case EAST -> Direction.WEST;
                case WEST -> Direction.EAST;
            };
            return state.setValue(BlockStateProperties.FACING, flippedFacing);
        }

        // 处理 ATTACH_FACE 属性(墙面附着方块,如按钮、压力板等)
        if (state.hasProperty(BlockStateProperties.ATTACH_FACE)) {
            net.minecraft.world.level.block.state.properties.AttachFace currentFace =
                state.getValue(BlockStateProperties.ATTACH_FACE);
            net.minecraft.world.level.block.state.properties.AttachFace flippedFace =
            switch (currentFace) {
                case CEILING -> net.minecraft.world.level.block.state.properties.AttachFace.FLOOR;
                case FLOOR -> net.minecraft.world.level.block.state.properties.AttachFace.CEILING;
                case WALL -> net.minecraft.world.level.block.state.properties.AttachFace.WALL;
            };
            return state.setValue(BlockStateProperties.ATTACH_FACE, flippedFace);
        }

        // 处理 HANGING 属性(悬挂方块,如灯笼、锁链等)
        if (state.hasProperty(BlockStateProperties.HANGING)) {
            Boolean currentHanging = state.getValue(BlockStateProperties.HANGING);
            // 倒挂时翻转悬挂状态
            return state.setValue(BlockStateProperties.HANGING, !currentHanging);
        }

        // 如果没有需要翻转的属性，返回原状态
        return state;
    }

    /**
     * 翻转方块状态的 half 属性（top <-> bottom）
     * 用于倒挂情况下正确放置楼梯、台阶等有 half 属性的方块
     *
     * @param state 原始方块状态
     * @return 翻转 half 属性后的方块状态
     */
    private static BlockState flipHalfProperty(BlockState state) {
        return flipHalfPropertyStatic(state);
    }

    /**
     * 获取蓝图放置的有序索引列表
     * 按 y → z → x 排序（对应 layer → row → col），保持与正常模式一致的放置顺序
     *
     * @param rotatedData 旋转后的结构数据
     * @param upsideDown  是否倒挂（倒挂时从上到下放置）
     * @return 排序后的索引列表
     */
    public static List<Integer> buildOrderedBlueprintIndices(StructureLoadUtil.StructureData rotatedData, boolean upsideDown) {
        if (rotatedData.blocks.isEmpty()) {
            return List.of();
        }

        // 创建索引列表，过滤掉多方块方块的次要部件
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < rotatedData.blocks.size(); i++) {
            // 跳过多方块方块的次要部件，它们会在主体部件放置时自动创建
            if (!StructureLoadUtil.isMultiblockSecondaryPart(rotatedData.blocks.get(i).state())) {
                indices.add(i);
            }
        }

        // 按方块坐标的 y → z(降序) → x(降序) 排序
        // 正常模式：y升序=从下到上
        // 倒挂模式：y降序=从上到下（因为倒挂后最高层先放置）
        // z降序=从远到近, x降序=从右到左
        final boolean finalUpsideDown = upsideDown;
        indices.sort((i, j) -> {
            StructureLoadUtil.BlockPosition a = rotatedData.blocks.get(i);
            StructureLoadUtil.BlockPosition b = rotatedData.blocks.get(j);
            if (a.y() != b.y()) {
                return finalUpsideDown ? Integer.compare(b.y(), a.y()) : Integer.compare(a.y(), b.y());
            }
            if (a.z() != b.z()) return Integer.compare(b.z(), a.z());  // z降序
            return Integer.compare(b.x(), a.x());  // x降序
        });

        return indices;
    }

    /**
     * 获取指定索引位置需要的方块类型
     * 注意：使用旋转后的结构数据
     */
    @Nullable
    public Block getRequiredBlockForPosition(int index) {
        if (this.loadedStructure == null) {
            return null;
        }

        // 获取旋转后的结构数据
        StructureLoadUtil.StructureData rotatedData = rotateStructureDataStatic(this.loadedStructure);
        if (index < 0 || index >= rotatedData.blocks.size()) {
            return null;
        }
        return rotatedData.blocks.get(index).state().getBlock();
    }

    /**
     * 获取当前放置索引对应的实际目标位置（用于渲染器）
     * 注意：currentPlacementIndex 是有序索引列表中的位置，需要转换为原始索引
     *
     * @return 当前应该放置的方块的实际位置，如果没有则返回 null
     */
    @Nullable
    public BlockPos getCurrentBlueprintTargetPosition() {
        if (this.loadedStructure == null || this.loadedStructure.isEmpty() || this.level == null) {
            return null;
        }

        Direction facing = this.getFacing(this.getBlockPos(), this.level);
        boolean upsideDown = this.level.getBlockState(this.getBlockPos()).getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);

        StructureLoadUtil.StructureData rotatedData = rotateStructureDataStatic(this.loadedStructure);
        List<BlockPos> allPositions = buildBlueprintPositions(this.getBlockPos(), facing, upsideDown, rotatedData);

        if (allPositions.isEmpty()) {
            return null;
        }

        // 获取有序索引列表
        List<Integer> orderedIndices = buildOrderedBlueprintIndices(rotatedData, upsideDown);
        if (orderedIndices.isEmpty() || this.currentPlacementIndex >= orderedIndices.size()) {
            return null;
        }

        // 获取当前有序位置对应的原始索引
        int actualIndex = orderedIndices.get(this.currentPlacementIndex);

        // 返回该索引对应的位置
        return allPositions.get(actualIndex);
    }

    /**
     * 尝试使用 FakePlayer 放置方块
     *
     * @return 是否放置成功
     */
    private boolean tryPlaceBlockWithFakePlayer(
        Level level, BlockPos targetPos, Direction facing,
        boolean upsideDown, BlockItem blockItemObj, ItemStack blockItem
    ) {
        Orientation orientation = this.calculatePlacementOrientation(facing, upsideDown);
        return AnvilCraftFakePlayers.anvilcraftBlockPlacer.placeBlock(
            level, targetPos, orientation, blockItemObj, blockItem) != InteractionResult.FAIL;
    }

    /**
     * 从方块状态中获取堆叠数量
     *
     * @param state 方块状态
     * @return 堆叠数量，1表示不可堆叠
     */
    private int getStackCountFromState(BlockState state) {
        if (state.is(Blocks.TURTLE_EGG)) {
            return state.getValue(TurtleEggBlock.EGGS);
        } else if (state.is(Blocks.SEA_PICKLE)) {
            return state.getValue(SeaPickleBlock.PICKLES);
        } else if (state.getBlock() instanceof CandleBlock) {
            return state.getValue(CandleBlock.CANDLES);
        } else if (state.is(Blocks.PINK_PETALS)) {
            // 花簇：获取花朵数量（1-4）
            return state.getValue(BlockStateProperties.FLOWER_AMOUNT);
        } else if (state.is(Blocks.SNOW)) {
            return 1;  // 雪片不作为可堆叠方块
        } else if (state.is(Blocks.GLOW_LICHEN)) {
            int count = 0;
            if (state.getValue(BlockStateProperties.NORTH)) count++;
            if (state.getValue(BlockStateProperties.SOUTH)) count++;
            if (state.getValue(BlockStateProperties.EAST)) count++;
            if (state.getValue(BlockStateProperties.WEST)) count++;
            if (state.getValue(BlockStateProperties.UP)) count++;
            if (state.getValue(BlockStateProperties.DOWN)) count++;
            return Math.max(count, 1);  // 至少为1
        } else if (state.is(Blocks.VINE)) {
            // 藤蔓：统计启用的方向面数量（只有四面，没有UP和DOWN）
            int count = 0;
            if (state.getValue(BlockStateProperties.NORTH)) count++;
            if (state.getValue(BlockStateProperties.SOUTH)) count++;
            if (state.getValue(BlockStateProperties.EAST)) count++;
            if (state.getValue(BlockStateProperties.WEST)) count++;
            return Math.max(count, 1);  // 至少为1
        }
        return 1;
    }

    /**
     * 获取方块的最大堆叠数量
     *
     * @param state 方块状态
     * @return 最大堆叠数量，1表示不可堆叠
     */
    private int getMaxStackCountForState(BlockState state) {
        if (state.is(Blocks.TURTLE_EGG)) {
            return 4;  // 海龟蛋最大4个
        } else if (state.is(Blocks.SEA_PICKLE)) {
            return 4;  // 海泡菜最大4个
        } else if (state.getBlock() instanceof CandleBlock) {
            return 4;  // 蜡烛最夑4个
        } else if (state.is(Blocks.PINK_PETALS)) {
            return 4;  // 花簇最多4个
        } else if (state.is(Blocks.SNOW)) {
            return 1;  // 雪片不作为可堆叠方块
        } else if (state.is(Blocks.GLOW_LICHEN)) {
            return 6;  // 发光地衣最夑6个面
        } else if (state.is(Blocks.VINE)) {
            return 4;  // 藤蔓最大4个面
        }
        return 1;  // 其他方块不可堆叠
    }

    /**
     * 统计容器中指定方块物品的数量
     *
     * @param level       世界
     * @param placerPos   放置器位置
     * @param targetBlock 目标方块
     * @return 可用数量
     */
    private int countBlockItemInContainer(Level level, BlockPos placerPos, Block targetBlock) {
        Direction facing = this.getFacing(placerPos, level);
        BlockPos inputPos = placerPos.relative(facing.getOpposite());

        int totalCount = 0;

        // 检查容器
        IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, inputPos, null);
        if (itemHandler != null) {
            for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                ItemStack stack = itemHandler.extractItem(slot, Integer.MAX_VALUE, true);
                if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                    if (blockItem.getBlock() == targetBlock) {
                        totalCount += stack.getCount();
                    }
                }
            }
            return totalCount;
        }

        // 检查 ItemEntity
        AABB aabb = new AABB(inputPos);
        List<ItemEntity> entities = level.getEntities(
            EntityTypeTest.forClass(ItemEntity.class),
            aabb,
            Entity::isAlive
        );

        for (ItemEntity entity : entities) {
            if (entity.getItem().getItem() instanceof BlockItem blockItem) {
                if (blockItem.getBlock() == targetBlock) {
                    totalCount += entity.getItem().getCount();
                }
            }
        }

        return totalCount;
    }

    /**
     * 提取并放置可堆叠方块
     *
     * @return 是否成功
     */
    private boolean extractAndPlaceStackableBlock(
        Level level, BlockPos placerPos, BlockPos targetPos, Direction facing, boolean upsideDown,
        Block requiredBlock, int stackCount, int index, int orderIndex, int orderedSize
    ) {
        // 先从容器中提取 stackCount 个物品
        for (int i = 0; i < stackCount; i++) {
            ItemStack extracted = this.extractSpecificBlockItemFromContainer(level, placerPos, requiredBlock);
            if (extracted.isEmpty()) {
                // 物品不足，回滚已经提取的物品
                for (int j = 0; j < i; j++) {
                    this.rollbackExtractedItem(level, placerPos, new ItemStack(requiredBlock));
                }
                return false;
            }
        }

        // 提取成功，放置第1个方块
        ItemStack firstItem = new ItemStack(requiredBlock);
        if (firstItem.getItem() instanceof BlockItem firstBlockItemObj) {
            boolean placeSuccess = this.tryPlaceBlockWithFakePlayer(level, targetPos, facing, upsideDown, firstBlockItemObj, firstItem);
            if (!placeSuccess) {
                // 放置失败，回滚所有物品
                for (int i = 0; i < stackCount; i++) {
                    this.rollbackExtractedItem(level, placerPos, new ItemStack(requiredBlock));
                }
                return false;
            }
        }

        // 放置成功，应用蓝图状态（包含正确的堆叠数量）
        this.applyBlueprintBlockFacing(level, targetPos, index);

        // 在目标位置发送方块更新通知，让红石灯等方块根据新位置的状态更新
        level.neighborChanged(targetPos, level.getBlockState(targetPos).getBlock(), targetPos);

        // 更新索引
        this.currentPlacementIndex = (orderIndex + 1) % orderedSize;
        this.currentHeldBlock = ItemStack.EMPTY;
        this.onChanged();

        return true;
    }

    /**
     * 获取蓝图中指定索引的方块状态（已应用旋转和倒挂处理）
     *
     * @param index 索引
     * @param level 世界
     * @return 蓝图中的方块状态
     */
    private BlockState getBlueprintBlockState(int index, Level level) {
        if (this.loadedStructure == null || this.loadedStructure.isEmpty()) {
            return Blocks.AIR.defaultBlockState();
        }

        // 获取放置器朝向
        Direction facing = this.getFacing(this.getBlockPos(), level);

        // 计算旋转（与buildBlueprintPositions保持一致）
        Rotation rotation = getRotationForPlacement(facing, this.loadedStructure.diskData.direction());

        // 获取原始状态并应用旋转
        StructureLoadUtil.StructureData originalData = this.loadedStructure;
        if (index < 0 || index >= originalData.blocks.size()) {
            return Blocks.AIR.defaultBlockState();
        }

        BlockState originalState = originalData.blocks.get(index).state();
        @SuppressWarnings("deprecation")
        BlockState rotatedState = originalState.rotate(rotation);

        // 倒挂情况下，翻转 half 属性
        boolean upsideDown = level.getBlockState(this.getBlockPos()).getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);
        if (upsideDown) {
            rotatedState = flipHalfProperty(rotatedState);
        }

        // 应用白名单过滤：只保留白名单中的状态属性
        rotatedState = applyWhitelistFilter(rotatedState);

        return rotatedState;
    }

    /**
     * 应用蓝图中方块的朝向到已放置的方块
     * 用于修正 FakePlayer 放置后的方块朝向
     */
    private void applyBlueprintBlockFacing(Level level, BlockPos targetPos, int index) {
        if (this.loadedStructure == null || this.loadedStructure.isEmpty()) return;

        // 获取放置器朝向
        Direction facing = this.getFacing(this.getBlockPos(), level);

        // 计算旋转（与buildBlueprintPositions保持一致）
        Rotation rotation = getRotationForPlacement(facing, this.loadedStructure.diskData.direction());

        // 使用旋转后的结构数据；index 按旋转后结构数据的索引空间解释
        StructureLoadUtil.StructureData originalData = this.loadedStructure;
        if (index < 0 || index >= originalData.blocks.size()) return;

        BlockState originalState = originalData.blocks.get(index).state();
        @SuppressWarnings("deprecation")
        BlockState rotatedState = originalState.rotate(rotation);

        // 倒挂情况下，翻转 half 属性
        boolean upsideDown = level.getBlockState(this.getBlockPos()).getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);
        if (upsideDown) {
            rotatedState = flipHalfProperty(rotatedState);
        }

        BlockState worldState = level.getBlockState(targetPos);

        if (worldState.getBlock() != rotatedState.getBlock()) return;

        // 应用白名单过滤：只保留白名单中的状态属性
        rotatedState = applyWhitelistFilter(rotatedState);

        // 海泡菜特殊处理:手动将 waterlogged 设置为 false
        if (rotatedState.is(Blocks.SEA_PICKLE)
            && rotatedState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            rotatedState = rotatedState.setValue(BlockStateProperties.WATERLOGGED, false);
        }
        
        // 树叶方块特殊处理:蓝图模式下默认设置 persistent=true
        if (rotatedState.is(net.minecraft.tags.BlockTags.LEAVES)
            && rotatedState.hasProperty(BlockStateProperties.PERSISTENT)) {
            rotatedState = rotatedState.setValue(BlockStateProperties.PERSISTENT, true);
        }

        if (!worldState.equals(rotatedState)) {
            // 只更新客户端，不触发邻居更新（由调用方统一处理）
            level.setBlock(targetPos, rotatedState, Block.UPDATE_CLIENTS);
        }
    }

    /**
     * 从方块状态中提取期望的水平朝向（用于多方块方块放置时的朝向控制）
     *
     * @param state 蓝图旋转后的方块状态
     * @return 水平朝向，如果没有水平朝向属性则返回 null
     */
    @Nullable
    private static Direction extractDesiredHorizontalFacing(BlockState state) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            Direction f = state.getValue(BlockStateProperties.FACING);
            if (f.getAxis() != Direction.Axis.Y) {
                return f;
            }
        }
        return null;
    }

    /**
     * 应用蓝图状态到多方块结构的所有部件
     * 在主体部件通过 FakePlayer 放置（setPlacedBy 创建所有次要部件）后调用，
     * 将蓝图中所有部件的正确朝向和属性应用到已放置的方块上
     *
     * @param level         世界
     * @param allPositions  所有蓝图位置列表
     * @param rotatedData   旋转后的结构数据
     * @param mainIndex     主体部件在结构数据中的索引
     * @param placedBlock   已放置的方块类型
     */
    private void applyMultiBlockBlueprintStates(
        Level level,
        List<BlockPos> allPositions,
        StructureLoadUtil.StructureData rotatedData,
        int mainIndex,
        Block placedBlock
    ) {
        // 首先应用主体部件自身的蓝图状态
        BlockPos mainPos = allPositions.get(mainIndex);
        this.applyBlueprintBlockFacing(level, mainPos, mainIndex);

        // 遍历结构数据中的所有方块，找到属于同一多方块结构的其他部件并应用状态
        for (int i = 0; i < rotatedData.blocks.size(); i++) {
            if (i == mainIndex) continue;

            Block otherBlock = rotatedData.blocks.get(i).state().getBlock();
            if (otherBlock != placedBlock) continue;

            BlockPos otherWorldPos = allPositions.get(i);
            BlockState otherWorldState = level.getBlockState(otherWorldPos);

            // 确认该位置已被 setPlacedBy 填充了正确的方块类型
            if (otherWorldState.getBlock() == placedBlock) {
                this.applyBlueprintBlockFacing(level, otherWorldPos, i);
            }
        }
    }

    /**
     * 应用白名单过滤：只保留白名单中的状态属性，其他属性重置为默认值
     * 使用属性名称进行匹配（而非对象相等），确保不同方块中同名属性（如 "half"）都能正确保留
     *
     * @param state 原始方块状态
     * @return 过滤后的方块状态
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private BlockState applyWhitelistFilter(BlockState state) {
        BlockState resultState = state.getBlock().defaultBlockState();

        // 遍历状态的属性，按名称匹配白名单（解决不同方块同名Property对象不同的问题）
        for (Property<?> property : state.getProperties()) {
            if (INHERITED_PROPERTY_NAMES.contains(property.getName())) {
                resultState = setAllowedValue(
                    (Property) property, resultState, state);
            }
        }

        return resultState;
    }

    public static <T extends Comparable<T>> BlockState setAllowedValue(
        Property<T> property, BlockState targetState, BlockState sourceState) {
        T value = sourceState.getValue(property);
        return targetState.setValue(property, value);
    }

    /**
     * 检查方块是否正在被智能放置器移动
     *
     * @return 是否正在被移动
     */
    public static boolean isBlockBeingMovedByPlacer() {
        return IS_BEING_MOVED_BY_PLACER.get();
    }

    /**
     * 构建有序的放置位置列表
     * 顺序：从最下面一层开始，每一层从最远离放置器的位置开始，从左到右，然后逐渐向下
     *
     * @param basePos        基准位置
     * @param facing         朝向
     * @param layerPositions 层位置映射
     * @param upsideDown     是否倒挂
     * @return 有序的位置列表
     */
    public static List<BlockPos> buildOrderedPositions(
        BlockPos basePos,
        Direction facing,
        Map<Integer, Set<Integer>> layerPositions,
        boolean upsideDown
    ) {
        if (layerPositions.isEmpty()) {
            return List.of();
        }

        List<BlockPos> positions = new ArrayList<>();
        List<Integer> sortedLayers = new ArrayList<>(layerPositions.keySet());
        sortedLayers.sort(Integer::compareTo);

        for (int layer : sortedLayers) {
            Set<Integer> layerPosSet = layerPositions.get(layer);
            if (layerPosSet == null || layerPosSet.isEmpty()) {
                continue;
            }

            List<int[]> rowColList = new ArrayList<>(layerPosSet.size());
            for (int position : layerPosSet) {
                rowColList.add(new int[]{
                    position / 5,
                    position % 5
                });
            }

            rowColList.sort((a, b) -> {
                if (a[0] != b[0]) {
                    return Integer.compare(a[0], b[0]);
                }
                return Integer.compare(a[1], b[1]);
            });

            for (int[] rowCol : rowColList) {
                positions.add(calculateTargetPosition(basePos, facing, rowCol[0], rowCol[1], layer, upsideDown));
            }
        }
        return positions;
    }

    private ItemStack peekBlockItemFromContainer(Level level, BlockPos placerPos) {
        return this.getBlockItemFromContainer(level, placerPos);
    }

    /**
     * 预提取物品：不真正删除ItemEntity，只返回物品信息和来源引用
     * 在放置成功后调用 ExtractionResult.confirmExtraction() 才真正删除
     */
    @Nullable
    private ExtractionResult preExtractBlockItemFromContainer(Level level, BlockPos placerPos) {
        Direction facing = this.getFacing(placerPos, level);
        BlockPos inputPos = placerPos.relative(facing.getOpposite());

        IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, inputPos, null);
        int slot;
        for (slot = 0; itemHandler != null && slot < itemHandler.getSlots(); slot++) {
            ItemStack blockItemStack = itemHandler.extractItem(slot, 1, true);
            if (!blockItemStack.isEmpty() && blockItemStack.getItem() instanceof BlockItem) {
                // 从容器预提取：先模拟提取，返回物品信息，放置成功后再真正提取
                // 细雪桶特殊处理：预提取时就返还桶
                if (blockItemStack.is(Items.POWDER_SNOW_BUCKET)) {
                    itemHandler.insertItem(slot, new ItemStack(Items.BUCKET), false);
                }
                return new ContainerExtractionResult(blockItemStack.copy(), itemHandler, slot);
            }
        }

        if (itemHandler == null) {
            AABB aabb = new AABB(inputPos);
            List<Entity> rawEntities = level.getEntitiesOfClass(
                Entity.class, aabb, e -> e instanceof ContainerEntity && !((ContainerEntity) e).isEmpty()
            );

            for (Entity rawEntity : rawEntities) {
                if (rawEntity instanceof ContainerEntity containerEntity) {
                    IItemHandler entityHandler = ((Entity) containerEntity).getCapability(
                        Capabilities.ItemHandler.ENTITY, null
                    );
                    if (entityHandler != null) {
                        for (slot = 0; slot < entityHandler.getSlots(); slot++) {
                            ItemStack blockItemStack = entityHandler.extractItem(slot, 1, true);
                            if (!blockItemStack.isEmpty() && blockItemStack.getItem() instanceof BlockItem) {
                                // 从实体容器预提取：先模拟提取，返回物品信息，放置成功后再真正提取
                                // 细雪桶特殊处理：预提取时就返还桶
                                if (blockItemStack.is(Items.POWDER_SNOW_BUCKET)) {
                                    entityHandler.insertItem(slot, new ItemStack(Items.BUCKET), false);
                                }
                                return new ContainerExtractionResult(blockItemStack.copy(), entityHandler, slot);
                            }
                        }
                    }
                    break;
                }
            }
        }

        // 从 ItemEntity 预提取：不真正删除，只记录引用
        AABB aabb = new AABB(inputPos);
        List<ItemEntity> entities = level.getEntities(
            EntityTypeTest.forClass(ItemEntity.class),
            aabb,
            Entity::isAlive
        );
        if (entities.isEmpty()) {
            return null;
        }

        ItemEntity itemEntity = null;
        for (ItemEntity entity : entities) {
            if (entity.getItem().getItem() instanceof BlockItem) {
                itemEntity = entity;
                break;
            }
        }

        if (itemEntity == null) {
            return null;
        }

        // 返回物品信息和ItemEntity引用，但不真正删除
        ItemStack extracted = itemEntity.getItem().copyWithCount(1);

        // 细雪桶特殊处理：预提取时就生成空桶掉落物
        if (extracted.is(Items.POWDER_SNOW_BUCKET)) {
            // 生成空桶掉落物
            ItemEntity bucketEntity = new ItemEntity(
                level,
                itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(),
                new ItemStack(Items.BUCKET)
            );
            bucketEntity.setDeltaMovement(0, 0, 0);
            level.addFreshEntity(bucketEntity);
        }

        return new ExtractionResult(extracted, itemEntity, true);
    }

    /**
     * 预提取容器中特定方块物品（不真正删除）
     *
     * @param level       世界
     * @param placerPos   放置器位置
     * @param targetBlock 目标方块
     * @return 预提取结果，如果没有找到则返回 null
     */
    @Nullable
    private ExtractionResult preExtractSpecificBlockItemFromContainer(
        Level level, BlockPos placerPos, Block targetBlock) {
        Direction facing = this.getFacing(placerPos, level);
        BlockPos inputPos = placerPos.relative(facing.getOpposite());

        IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, inputPos, null);
        int slot;
        for (slot = 0; itemHandler != null && slot < itemHandler.getSlots(); slot++) {
            ItemStack blockItemStack = itemHandler.extractItem(slot, 1, true);
            if (!blockItemStack.isEmpty() && blockItemStack.getItem() instanceof BlockItem blockItem) {
                // 检查是否是需要的方块
                if (blockItem.getBlock() == targetBlock) {
                    // 细雪桶特殊处理：预提取时就返还桶
                    if (blockItemStack.is(Items.POWDER_SNOW_BUCKET)) {
                        itemHandler.insertItem(slot, new ItemStack(Items.BUCKET), false);
                    }
                    return new ContainerExtractionResult(blockItemStack.copy(), itemHandler, slot);
                }
            }
        }

        if (itemHandler == null) {
            AABB aabb = new AABB(inputPos);
            List<Entity> rawEntities = level.getEntitiesOfClass(
                Entity.class, aabb, e -> e instanceof ContainerEntity && !((ContainerEntity) e).isEmpty()
            );

            for (Entity rawEntity : rawEntities) {
                if (rawEntity instanceof ContainerEntity containerEntity) {
                    IItemHandler entityHandler = ((Entity) containerEntity).getCapability(
                        Capabilities.ItemHandler.ENTITY, null
                    );
                    if (entityHandler != null) {
                        for (slot = 0; slot < entityHandler.getSlots(); slot++) {
                            ItemStack blockItemStack = entityHandler.extractItem(slot, 1, true);
                            if (!blockItemStack.isEmpty() && blockItemStack.getItem() instanceof BlockItem blockItem) {
                                if (blockItem.getBlock() == targetBlock) {
                                    // 细雪桶特殊处理
                                    if (blockItemStack.is(Items.POWDER_SNOW_BUCKET)) {
                                        entityHandler.insertItem(slot, new ItemStack(Items.BUCKET), false);
                                    }
                                    return new ContainerExtractionResult(blockItemStack.copy(), entityHandler, slot);
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }

        // 从 ItemEntity 预提取
        AABB aabb = new AABB(inputPos);
        List<ItemEntity> entities = level.getEntities(
            EntityTypeTest.forClass(ItemEntity.class),
            aabb,
            Entity::isAlive
        );
        if (entities.isEmpty()) {
            return null;
        }

        ItemEntity itemEntity = null;
        for (ItemEntity entity : entities) {
            if (entity.getItem().getItem() instanceof BlockItem blockItem) {
                if (blockItem.getBlock() == targetBlock) {
                    itemEntity = entity;
                    break;
                }
            }
        }

        if (itemEntity == null) {
            return null;
        }

        // 返回物品信息和ItemEntity引用，但不真正删除
        ItemStack extracted = itemEntity.getItem().copyWithCount(1);

        // 细雪桶特殊处理：预提取时就生成空桶掉落物
        if (extracted.is(Items.POWDER_SNOW_BUCKET)) {
            // 生成空桶掉落物
            ItemEntity bucketEntity = new ItemEntity(
                level,
                itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(),
                new ItemStack(Items.BUCKET)
            );
            bucketEntity.setDeltaMovement(0, 0, 0);
            level.addFreshEntity(bucketEntity);
        }

        return new ExtractionResult(extracted, itemEntity, true);
    }

    /**
     * 预览容器中特定方块物品（不提取）
     *
     * @param level       世界
     * @param placerPos   放置器位置
     * @param targetBlock 目标方块
     * @return 预览的物品，如果没有找到则返回 EMPTY
     */
    public ItemStack peekSpecificBlockItemFromContainer(
        Level level, BlockPos placerPos, Block targetBlock) {
        Direction facing = this.getFacing(placerPos, level);
        BlockPos inputPos = placerPos.relative(facing.getOpposite());

        IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, inputPos, null);
        int slot;
        for (slot = 0; itemHandler != null && slot < itemHandler.getSlots(); slot++) {
            ItemStack blockItemStack = itemHandler.extractItem(slot, 1, true);
            if (!blockItemStack.isEmpty() && blockItemStack.getItem() instanceof BlockItem blockItem) {
                // 检查是否是需要的方块
                if (blockItem.getBlock() == targetBlock) {
                    return blockItemStack.copy();
                }
            }
        }

        if (itemHandler == null) {
            AABB aabb = new AABB(inputPos);
            List<Entity> rawEntities = level.getEntitiesOfClass(
                Entity.class, aabb, e -> e instanceof ContainerEntity && !((ContainerEntity) e).isEmpty()
            );

            for (Entity rawEntity : rawEntities) {
                if (rawEntity instanceof ContainerEntity containerEntity) {
                    IItemHandler entityHandler = ((Entity) containerEntity).getCapability(
                        Capabilities.ItemHandler.ENTITY, null
                    );
                    if (entityHandler != null) {
                        for (slot = 0; slot < entityHandler.getSlots(); slot++) {
                            ItemStack blockItemStack = entityHandler.extractItem(slot, 1, true);
                            if (!blockItemStack.isEmpty() && blockItemStack.getItem() instanceof BlockItem blockItem) {
                                if (blockItem.getBlock() == targetBlock) {
                                    return blockItemStack.copy();
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }

        AABB aabb = new AABB(inputPos);
        List<ItemEntity> entities = level.getEntities(
            EntityTypeTest.forClass(ItemEntity.class),
            aabb,
            Entity::isAlive
        );
        if (entities.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemEntity itemEntity = null;
        for (ItemEntity entity : entities) {
            if (entity.getItem().getItem() instanceof BlockItem blockItem) {
                if (blockItem.getBlock() == targetBlock) {
                    itemEntity = entity;
                    break;
                }
            }
        }

        if (itemEntity == null) {
            return ItemStack.EMPTY;
        }

        // 返回副本，不修改实体
        return itemEntity.getItem().copy();
    }

    /**
     * 从容器中提取特定方块物品
     *
     * @param level       世界
     * @param placerPos   放置器位置
     * @param targetBlock 目标方块
     * @return 提取的物品，如果没有找到则返回 EMPTY
     */
    private ItemStack extractSpecificBlockItemFromContainer(
        Level level, BlockPos placerPos, Block targetBlock) {
        Direction facing = this.getFacing(placerPos, level);
        BlockPos inputPos = placerPos.relative(facing.getOpposite());

        IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, inputPos, null);
        int slot;
        for (slot = 0; itemHandler != null && slot < itemHandler.getSlots(); slot++) {
            ItemStack blockItemStack = itemHandler.extractItem(slot, 1, true);
            if (!blockItemStack.isEmpty() && blockItemStack.getItem() instanceof BlockItem blockItem) {
                // 检查是否是需要的方块
                if (blockItem.getBlock() == targetBlock) {
                    // 直接提取，不需要先检查
                    ItemStack extracted = itemHandler.extractItem(slot, 1, false);
                    if (extracted.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                    if (extracted.is(Items.POWDER_SNOW_BUCKET)) {
                        itemHandler.insertItem(slot, new ItemStack(Items.BUCKET), false);
                    }
                    // 发送容器更新，让比较器能正确检测物品数量
                    level.sendBlockUpdated(inputPos, level.getBlockState(inputPos), level.getBlockState(inputPos), 3);
                    return extracted;
                }
            }
        }

        if (itemHandler == null) {
            AABB aabb = new AABB(inputPos);
            List<Entity> rawEntities = level.getEntitiesOfClass(
                Entity.class, aabb, e -> e instanceof ContainerEntity && !((ContainerEntity) e).isEmpty()
            );

            for (Entity rawEntity : rawEntities) {
                if (rawEntity instanceof ContainerEntity containerEntity) {
                    IItemHandler entityHandler = ((Entity) containerEntity).getCapability(
                        Capabilities.ItemHandler.ENTITY, null
                    );
                    if (entityHandler != null) {
                        for (slot = 0; slot < entityHandler.getSlots(); slot++) {
                            ItemStack blockItemStack = entityHandler.extractItem(slot, 1, true);
                            if (!blockItemStack.isEmpty() && blockItemStack.getItem() instanceof BlockItem blockItem) {
                                if (blockItem.getBlock() == targetBlock) {
                                    return entityHandler.extractItem(slot, 1, false);
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }

        AABB aabb = new AABB(inputPos);
        List<ItemEntity> entities = level.getEntities(
            EntityTypeTest.forClass(ItemEntity.class),
            aabb,
            Entity::isAlive
        );
        if (entities.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemEntity itemEntity = null;
        for (ItemEntity entity : entities) {
            if (entity.getItem().getItem() instanceof BlockItem blockItem) {
                if (blockItem.getBlock() == targetBlock) {
                    itemEntity = entity;
                    break;
                }
            }
        }

        if (itemEntity == null) {
            return ItemStack.EMPTY;
        }

        // 提取时先保存物品副本，再修改实体数量
        ItemStack extracted = itemEntity.getItem().copyWithCount(1);
        int count = itemEntity.getItem().getCount();
        if (extracted.is(Items.POWDER_SNOW_BUCKET)) {
            itemEntity.setItem(new ItemStack(Items.BUCKET, count));
            itemEntity.setDeltaMovement(0, 0, 0);
        } else if (count > 1) {
            itemEntity.getItem().setCount(count - 1);
        } else {
            itemEntity.discard();
        }
        // 发送容器更新，让比较器能正确检测物品数量
        level.sendBlockUpdated(inputPos, level.getBlockState(inputPos), level.getBlockState(inputPos), 3);
        return extracted;
    }

    private ItemStack getBlockItemFromContainer(Level level, BlockPos placerPos) {
        Direction facing = this.getFacing(placerPos, level);
        BlockPos inputPos = placerPos.relative(facing.getOpposite());

        IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, inputPos, null);
        int slot;
        for (slot = 0; itemHandler != null && slot < itemHandler.getSlots(); slot++) {
            ItemStack blockItemStack = itemHandler.extractItem(slot, 1, true);
            if (!blockItemStack.isEmpty() && blockItemStack.getItem() instanceof BlockItem) {
                return blockItemStack.copy();
            }
        }

        if (itemHandler == null) {
            AABB aabb = new AABB(inputPos);
            List<Entity> rawEntities = level.getEntitiesOfClass(
                Entity.class, aabb, e -> e instanceof ContainerEntity && !((ContainerEntity) e).isEmpty()
            );

            for (Entity rawEntity : rawEntities) {
                if (rawEntity instanceof ContainerEntity containerEntity) {
                    IItemHandler entityHandler = ((Entity) containerEntity).getCapability(
                        Capabilities.ItemHandler.ENTITY, null
                    );
                    if (entityHandler != null) {
                        for (slot = 0; slot < entityHandler.getSlots(); slot++) {
                            ItemStack blockItemStack = entityHandler.extractItem(slot, 1, true);
                            if (!blockItemStack.isEmpty() && blockItemStack.getItem() instanceof BlockItem) {
                                return blockItemStack.copy();
                            }
                        }
                    }
                    break;
                }
            }
        }

        AABB aabb = new AABB(inputPos);
        List<ItemEntity> entities = level.getEntities(
            EntityTypeTest.forClass(ItemEntity.class),
            aabb,
            Entity::isAlive
        );
        if (entities.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemEntity itemEntity = null;
        for (ItemEntity entity : entities) {
            if (entity.getItem().getItem() instanceof BlockItem) {
                itemEntity = entity;
                break;
            }
        }

        if (itemEntity == null) {
            return ItemStack.EMPTY;
        }

        return itemEntity.getItem().copy();
    }

    /**
     * 提取操作结果封装类
     * 用于支持"预提取"逻辑：先获取物品信息，放置成功后再真正删除
     */
    private static class ExtractionResult {
        @Getter
        private final ItemStack itemStack;
        @Nullable
        private final ItemEntity sourceItemEntity;  // 如果来源是ItemEntity,记录引用
        @Getter
        private final boolean fromItemEntity;

        ExtractionResult(ItemStack itemStack, @Nullable ItemEntity sourceItemEntity, boolean fromItemEntity) {
            this.itemStack = itemStack;
            this.sourceItemEntity = sourceItemEntity;
            this.fromItemEntity = fromItemEntity;
        }

        /**
         * 确认提取：在放置成功后调用，真正删除或修改ItemEntity
         * 注意：细雪桶的返还已在预提取阶段处理，这里只需删除细雪桶
         */
        public void confirmExtraction() {
            if (fromItemEntity && sourceItemEntity != null && sourceItemEntity.isAlive()) {
                int count = sourceItemEntity.getItem().getCount();
                // 不需要处理细雪桶，因为预提取时已经生成空桶掉落物了
                if (count > 1) {
                    sourceItemEntity.getItem().setCount(count - 1);
                } else {
                    sourceItemEntity.discard();
                }
            }
        }
    }

    /**
     * 容器提取结果封装类
     * 用于支持容器的预提取逻辑：先模拟提取，放置成功后再真正提取
     */
    private static class ContainerExtractionResult extends ExtractionResult {
        private final IItemHandler itemHandler;
        private final int slot;

        ContainerExtractionResult(ItemStack itemStack, IItemHandler itemHandler, int slot) {
            super(itemStack, null, false);
            this.itemHandler = itemHandler;
            this.slot = slot;
        }

        /**
         * 确认提取：在放置成功后调用，真正从容器中提取物品
         * 注意：细雪桶的返还已在预提取阶段处理，这里只需删除细雪桶
         */
        @Override
        public void confirmExtraction() {
            // 真正从容器中删除物品
            itemHandler.extractItem(slot, 1, false);
            // 不需要处理细雪桶，因为预提取时已经把桶插回去了
        }
    }

    private Orientation calculatePlacementOrientation(Direction facing, boolean upsideDown) {
        return switch (facing) {
            case NORTH -> upsideDown ? Orientation.SOUTH_UP : Orientation.NORTH_UP;
            case SOUTH -> upsideDown ? Orientation.NORTH_UP : Orientation.SOUTH_UP;
            case WEST -> upsideDown ? Orientation.EAST_UP : Orientation.WEST_UP;
            case EAST -> upsideDown ? Orientation.WEST_UP : Orientation.EAST_UP;
            default -> Orientation.NORTH_UP;
        };
    }

    /**
     * 回滚已提取的物品到原容器
     *
     * @param level         世界
     * @param placerPos     放置器位置
     * @param extractedItem 已提取的物品
     */
    private void rollbackExtractedItem(Level level, BlockPos placerPos, ItemStack extractedItem) {
        if (extractedItem.isEmpty()) {
            return;
        }

        Direction facing = this.getFacing(placerPos, level);
        BlockPos inputPos = placerPos.relative(facing.getOpposite());

        // 尝试将物品放回容器
        IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, inputPos, null);
        if (itemHandler != null) {
            ItemStack remaining = extractedItem.copy();
            for (int slot = 0; slot < itemHandler.getSlots() && !remaining.isEmpty(); slot++) {
                remaining = itemHandler.insertItem(slot, remaining, false);
            }
            // 发送容器更新，让比较器能正确检测物品数量
            level.sendBlockUpdated(inputPos, level.getBlockState(inputPos), level.getBlockState(inputPos), 3);
            // 如果还有剩余物品，生成ItemEntity
            if (!remaining.isEmpty()) {
                ItemEntity itemEntity = new ItemEntity(
                    level,
                    inputPos.getX() + 0.5, inputPos.getY() + 0.5, inputPos.getZ() + 0.5, remaining
                );
                itemEntity.setDeltaMovement(0, 0, 0);  // 清除动量
                level.addFreshEntity(itemEntity);
            }
            return;
        }

        // 检查是否已经有相同位置的ItemEntity，尝试堆叠回去
        AABB aabb = new AABB(inputPos);
        List<ItemEntity> entities = level.getEntities(
            EntityTypeTest.forClass(ItemEntity.class),
            aabb,
            Entity::isAlive
        );

        for (ItemEntity entity : entities) {
            if (entity.getItem().getItem() == extractedItem.getItem()
                && ItemStack.isSameItemSameComponents(entity.getItem(), extractedItem)) {
                // 可以堆叠，增加数量
                int newCount = entity.getItem().getCount() + extractedItem.getCount();
                entity.getItem().setCount(newCount);
                entity.setDeltaMovement(0, 0, 0);  // 清除动量
                return;
            }
        }

        // 如果没有可堆叠的ItemEntity，创建新的
        ItemEntity itemEntity = new ItemEntity(level, inputPos.getX() + 0.5, inputPos.getY() + 0.5, inputPos.getZ() + 0.5, extractedItem);
        itemEntity.setDeltaMovement(0, 0, 0);  // 清除动量
        level.addFreshEntity(itemEntity);
    }

    /**
     * 计算目标位置
     */
    @SuppressWarnings("checkstyle:LocalVariableName")
    public static BlockPos calculateTargetPosition(BlockPos basePos, Direction facing, int row, int col, int layer, boolean upsideDown) {
        Direction right = facing.getClockWise();
        int yOffset = upsideDown ? layer - 4 : layer;
        return basePos.atY(basePos.getY() + yOffset)
            .relative(right, col - 2)
            .relative(right.getClockWise(), row - 2);
    }

    public void onChanged() {
        this.setChanged();
        Level level = this.getLevel();
        if (level != null) {
            level.sendBlockUpdated(
                this.getBlockPos(),
                this.getBlockState(),
                this.getBlockState(),
                Block.UPDATE_CLIENTS
            );
        }
    }

    public void setSelectedLayer(int layer) {
        this.selectedLayer = layer;
        this.onChanged();
    }

    public void setPickupMode(boolean pickupMode) {
        this.isPickupMode = pickupMode;
        this.onChanged();
    }

    public void setSkipMissingMode(boolean skipMissingMode) {
        this.isSkipMissingMode = skipMissingMode;
        this.onChanged();
    }

    public void togglePosition(int layer, int position, boolean selected) {
        Set<Integer> positions = layerPositions.computeIfAbsent(layer, k -> new HashSet<>());
        if (selected) {
            positions.add(position);
        } else {
            positions.remove(position);
            if (positions.isEmpty()) {
                layerPositions.remove(layer);
            }
        }
        this.onChanged();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    private void saveLayerPositions(CompoundTag tag) {
        CompoundTag layerTag = new CompoundTag();
        for (Map.Entry<Integer, Set<Integer>> entry : this.layerPositions.entrySet()) {
            layerTag.putIntArray(
                "layer_" + entry.getKey(),
                entry.getValue().stream().mapToInt(Integer::intValue).toArray()
            );
        }
        tag.put("layerPositions", layerTag);
    }

    private void loadLayerPositions(CompoundTag tag) {
        this.layerPositions.clear();
        if (tag.contains("layerPositions", Tag.TAG_COMPOUND)) {
            CompoundTag layerTag = tag.getCompound("layerPositions");
            for (String key : layerTag.getAllKeys()) {
                if (key.startsWith("layer_")) {
                    int layer = Integer.parseInt(key.substring(6));
                    Set<Integer> positions = new HashSet<>();
                    for (int pos : layerTag.getIntArray(key)) {
                        positions.add(pos);
                    }
                    this.layerPositions.put(layer, positions);
                }
            }
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public int getInputPower() {
        // 蓝图模式耗电量为64kW，其他模式为16kW
        return (this.loadedStructure != null && !this.loadedStructure.isEmpty()) ? 64 : SmartBlockPlacerBlockEntity.POWER;
    }

    @Override
    public void setGrid(@Nullable PowerGrid grid) {
        this.grid = grid;
    }

    @Override
    public @Nullable Level getCurrentLevel() {
        return this.level;
    }

    @Override
    public BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.anvilcraft.smart_block_placer");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (player.isSpectator()) {
            return null;
        }
        return new SmartBlockPlacerMenu(ModMenuTypes.SMART_BLOCK_PLACER.get(), containerId, inventory, this);
    }

    @Override
    public void storeDiskData(CompoundTag tag) {
        tag.putInt("selectedLayer", this.selectedLayer);
        tag.putInt("currentPlacementIndex", this.currentPlacementIndex);
        tag.putBoolean("isPickupMode", this.isPickupMode);
        this.saveLayerPositions(tag);
    }

    @Override
    public void applyDiskData(CompoundTag tag) {
        this.selectedLayer = tag.getInt("selectedLayer");
        this.currentPlacementIndex = tag.getInt("currentPlacementIndex");
        this.isPickupMode = tag.getBoolean("isPickupMode");
        this.loadLayerPositions(tag);
        this.onChanged();
    }

}
