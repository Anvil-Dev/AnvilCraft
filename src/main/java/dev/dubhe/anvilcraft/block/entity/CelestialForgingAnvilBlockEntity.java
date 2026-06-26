package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyMatcher;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorRegistry;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetResourceGenerator;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetaryResourceSet;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyRecipe;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.entity.megastructure.ExcavatorHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.PenroseSphereHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.WormholeStabilizerHandler;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.inventory.CelestialForgingAnvilMenu;
import dev.dubhe.anvilcraft.item.DiskItem;
import dev.dubhe.anvilcraft.util.GravityManager;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class CelestialForgingAnvilBlockEntity extends BlockEntity implements MenuProvider, IPowerConsumer, IPowerProducer, IDiskCloneable {

    /// === 巨构建造委托 ===
    @Getter
    private final CfaMegastructureManager megastructureManager = new CfaMegastructureManager();

    @Getter
    private int preRotation = 0;
    @Getter
    private int rotation = 0;

    @Getter
    private boolean isAmplify = false;

    @Getter
    @Setter
    @Nullable
    private CelestialBodyData celestialBodyData = null;

    @Getter
    @Setter
    private long bodySeed = 0;

    /// 进行天体匹配时的质量砧子数量，用于引力计算。
    @Getter
    @Setter
    private int stellarMass = 0;

    /// 为资源生成存储的时元砧子数量。
    @Getter
    @Setter
    private int ageAnvilCount = 0;

    /// 匹配到的天体所生成的资源。
    @Getter
    @Setter
    @Nullable
    private PlanetaryResourceSet planetaryResourceSet = null;

    /// 当前已建造的巨构（重构选项）索引，-1 表示未建造。委托给 CfaMegastructureManager。
    public int getActiveMegastructureIndex() {
        return megastructureManager.getActiveIndex();
    }

    /// 抽取器是否有有效的激光输入（用于模型切换）。委托给 ExcavatorHandler。
    public boolean isExcavatorLaserActive() {
        ExcavatorHandler h = megastructureManager.findHandler(ExcavatorHandler.class);
        return h != null && h.isLaserActive();
    }

    /// 彭罗斯球是否有有效的激光输入/输出对（用于模型切换）。委托给 PenroseSphereHandler。
    public boolean isPenroseSphereLaserActive() {
        PenroseSphereHandler h = megastructureManager.findHandler(PenroseSphereHandler.class);
        return h != null && h.isLaserActive();
    }

    /// === 虫洞稳定器状态 ===
    /// 黑洞参数哈希值，在稳定器建造时计算。
    @Nullable
    public UUID getWormholeParamsHash() {
        WormholeStabilizerHandler wh = megastructureManager.getWormholeHandler();
        return wh.getBodyUuid();
    }
    /// 此 CFA 当前是否已在虫洞网络中注册。
    /// 按立方体部件侧边映射到该侧放置的传送门 BlockPos。

    private final Map<Cube323PartHalf, BlockPos> portals = new EnumMap<>(Cube323PartHalf.class);
    /// 虫洞规范接口状态现已全局存储于 WormholeInterfaceStates（BetterSavedData），在整个网络组中共享。

    /// === 神殿状态 ===
    /// 三天循环中的当前位置：0=赐福，1=赐福，2=惩罚。
    @Getter
    private int templeCycleDay = 0;
    /// 上次刷新需求时的 MC 天数。
    private long templeLastDay = -1;
    /// 当前需求的物品类型（数量=1，仅标识用；同步到客户端用于提示）。
    @Getter
    private ItemStack templeDemandItem = ItemStack.EMPTY;
    /// 当前需求所需的总物品数量。
    @Getter
    private int templeDemandCount = 0;
    /// 当前需求已供奉的累计物品数量。选择新需求或需求被满足时重置。
    @Getter
    private int templeDemandProgress = 0;
    /// 当天的需求是否已被满足。
    @Getter
    private boolean templeDemandSatisfied = false;

    /// === 星体演化加速器委托 ===
    public int getAcceleratorStage() {
        return megastructureManager.getAcceleratorHandler().getStage();
    }

    public int getAcceleratorTicksRemaining() {
        return megastructureManager.getAcceleratorHandler().getTicksRemaining();
    }

    public int getAcceleratorTicksTotal() {
        return megastructureManager.getAcceleratorHandler().getTicksTotal();
    }

    /// 星体演化加速器是否处于活动状态（阶段 1-4 中的任一阶段）。

    public boolean isAcceleratorActive() {
        return megastructureManager.getAcceleratorHandler().isActive();
    }

    public CelestialForgingAnvilBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /// === 电力消费者接口 ===

    @Override
    public int getInputPower() {
        if (searching && searchTicksRemaining > 0) {
            return isAmplify ? 4000 : 1000;
        }
        return megastructureManager.getInputPower(this);
    }

    @Override
    public int getOutputPower() {
        return megastructureManager.getOutputPower(this);
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
    public int getRange() {
        return 1;
    }

    @Override
    public @Nullable PowerGrid getGrid() {
        return this.grid;
    }

    @Override
    public PowerComponentType getComponentType() {
        return megastructureManager.getComponentType(this);
    }

    @Override
    public void gridTick() {
        megastructureManager.gridTick(this);
    }

    private boolean hasEnoughPower() {
        if (grid == null) return false;
        int required = getInputPower();
        return required <= 0 || grid.isWorking();
    }

    @Getter
    private int bodyRotation = 0;

    /// === 天体动画（仅客户端，不持久化）===
    @Getter
    private int animationTicks = 0;
    @Getter
    private boolean animationForward = true;
    @Nullable
    @Getter
    private CelestialBodyData animationPreviousBodyData = null;
    private static final int ANIMATION_DURATION_TICKS = 20; /// 在 20 TPS 下为 1 秒

    /// 获取用于渲染的有效天体数据，考虑到反向动画。在反向动画期间，实际的 celestialBodyData 已经为 null（服务器已清除），因此使用缓存的前一个数据来继续渲染缩小的天体。
    @Nullable
    public CelestialBodyData getEffectiveBodyDataForRendering() {
        if (celestialBodyData != null) return celestialBodyData;
        if (animationTicks > 0 && !animationForward && animationPreviousBodyData != null) {
            return animationPreviousBodyData;
        }
        return null;
    }

    /// 获取从 0（隐藏）到 1（完全可见）的动画进度。使用 ease-in-out 三次插值。
    public float getAnimationProgress(float partialTick) {
        if (animationTicks <= 0) return animationForward ? 1.0f : 0.0f;
        float t = (ANIMATION_DURATION_TICKS - animationTicks + partialTick) / (float) ANIMATION_DURATION_TICKS;
        float eased = easeInOutCubic(t);
        return animationForward ? eased : (1.0f - eased);
    }

    /// 获取动画期间的旋转速度倍率。起始速度较快（5 倍），随着动画进行衰减到 1 倍。
    public float getAnimationRotationBoost(float partialTick) {
        float progress = getAnimationProgress(partialTick);
        return 1.0f + 4.0f * (1.0f - progress);
    }

    private static float easeInOutCubic(float t) {
        return t < 0.5f ? 4.0f * t * t * t : 1.0f - (float) Math.pow(-2.0f * t + 2.0f, 3) / 2.0f;
    }

    @Getter
    @Setter
    private boolean locked = false;

    /// 增幅器多方块结构是否已物理成型。
    @Getter
    @Setter
    private boolean amplifierPresent = false;

    /// 材料槽过滤器（选择重构选项时设置）
    @Getter
    @Setter
    private ItemStack materialFilter = new ItemStack(net.minecraft.world.item.Items.BARRIER);
    @Getter
    @Setter
    private int materialLimit = 0;

    @Getter
    private final SimpleContainer materialContainer = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            CelestialForgingAnvilBlockEntity.this.setChanged();
        }
    };

    /// 为给定的重构选项配置材料槽。玩家选择重构选项时在服务器端调用。
    public void configureMaterialSlot(int optionIndex) {
        if (level == null || level.isClientSide()) return;
        if (celestialBodyData == null) return;
        List<CelestialRefactorOption> options = getClientVisibleOptions();
        if (optionIndex < 0 || optionIndex >= options.size()) {
            setMaterialFilter(new ItemStack(net.minecraft.world.item.Items.BARRIER));
            setMaterialLimit(0);
        } else {
            CelestialRefactorOption opt = options.get(optionIndex);
            if (opt.needsMaterial()) {
                setMaterialFilter(opt.material().copy());
                setMaterialLimit(opt.materialCount());
            } else {
                setMaterialFilter(new ItemStack(net.minecraft.world.item.Items.BARRIER));
                setMaterialLimit(0);
            }
        }
        this.setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /// 搜索计时器
    @Getter
    private int searchTicksRemaining = 0;
    @Getter
    private boolean searching = false;
    @Getter
    @Setter
    private boolean searchFailed = false;
    @Getter
    private boolean powerInsufficient = false;
    private static final int SEARCH_TICKS = 200; /// 10 秒

    /// 追踪搜索开始时消耗的种子物品（用于特殊天体匹配）
    @javax.annotation.Nullable
    private Item lastConsumedSeedItem = null;
    @javax.annotation.Nullable
    private CompoundTag lastConsumedSeedNbt = null;

    /// 电网
    @Setter
    @Nullable
    private PowerGrid grid;

    /// 引力源状态
    private boolean gravitySourceActive = false;
    private double currentGravityStrength = 0;
    private int currentGravitySize = 0;
    /// 从控制器方块到渲染的恒星中心点的 Y 轴偏移。
    private static final int GRAVITY_CENTER_Y_OFFSET = 6;
    /// 引力影响半径（方块），覆盖 Ring6 的 7×7×7 区域。约为最大恒星半径（红超巨星 ~2580 R☉）的 2 倍。
    private static final int GRAVITY_RADIUS = 4;
    /// 所有天体引力计算的统一参考物理半径。5000 × R☉，且 R☉/R⊕ = 109，因此 R_ref/R⊕ = 545,000。
    private static final double GRAVITY_REFERENCE_RADIUS_RATIO = 5000.0 * 109.0;
    /// 游戏性倍率，使引力在方块尺度上可感知。
    private static final double GRAVITY_STRENGTH_MULTIPLIER = 10000000.0;

    public void startSearch() {
        this.searchFailed = false;
        this.powerInsufficient = false;

        /// 检查是否有种子物品（用于跳过预检和消耗）
        ItemStack seedStack = this.anvilInventory.getItem(4);
        boolean hasSeedItem = !seedStack.isEmpty();

        /// 服务端参数预检（有种子物品时跳过）
        if (level != null && !level.isClientSide()) {
            if (!hasSeedItem) {
                var preCheck = CelestialBodyMatcher.match(
                    getAnvilCount(0),
                    getAnvilCount(1),
                    getAnvilCount(2),
                    getAnvilCount(3),
                    this.isAmplify,
                    level.getRandom()
                );
                if (preCheck == null) {
                    this.searchFailed = true;
                    this.searching = false;
                    this.searchTicksRemaining = 0;
                    setChanged();
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                    return;
                }
            }
        }

        /// 检查电力是否足够
        if (!hasEnoughPower()) {
            this.powerInsufficient = true;
            this.searching = false;
            this.searchTicksRemaining = 0;
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
            return;
        }

        /// 捕获种子物品数据但暂不消耗（匹配成功后消耗）
        if (hasSeedItem) {
            this.lastConsumedSeedItem = seedStack.getItem();
            this.lastConsumedSeedNbt = extractSnapshot(seedStack);
        } else {
            this.lastConsumedSeedItem = null;
            this.lastConsumedSeedNbt = null;
        }

        /// 确认搜索将启动后才清除旧天体
        this.setCelestialBodyData(null);
        /// 启动搜索
        this.searchTicksRemaining = SEARCH_TICKS;
        this.searching = true;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void serverTick() {
        /// 持续刷新电力状态——电网恢复时清除过期的 powerInsufficient
        boolean hasEnoughPower = hasEnoughPower();
        if (!hasEnoughPower && !this.powerInsufficient) {
            this.powerInsufficient = true;
            setChanged();
            Objects.requireNonNull(this.level).sendBlockUpdated(this.getBlockPos(), getBlockState(), getBlockState(), 3);
        } else if (hasEnoughPower && this.powerInsufficient) {
            this.powerInsufficient = false;
            setChanged();
            Objects.requireNonNull(this.level).sendBlockUpdated(this.getBlockPos(), getBlockState(), getBlockState(), 3);
        }
        if (searchTicksRemaining > 0) {
            /// 在搜索过程中检查电力是否仍然充足
            if (!hasEnoughPower) {
                this.searching = false;
                this.searchTicksRemaining = 0;
                this.powerInsufficient = true;
                setChanged();
                if (level != null) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
            } else {
                searchTicksRemaining--;
                if (searchTicksRemaining == 0) {
                    this.searching = false;
                    tryMatchCelestialBody();
                    if (celestialBodyData == null) {
                        this.searchFailed = true;
                    }
                    setChanged();
                    if (level != null) {
                        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                    }
                }
            }
        }

        /// 管理恒星引力源
        updateGravitySource();

        /// 销毁引力中心的实体
        if (gravitySourceActive && level != null) {
            destroyEntitiesAtCenter();
        }

        /// 巨构建造逻辑（委托给处理类）
        megastructureManager.serverTick(this);
    }

    /// 为当前天体更新引力源。所有天体共享一个统一参考半径（5000 R☉，约为红超巨星半径的 2 倍），该半径对应 GRAVITY_RADIUS 的方块边界。
    /// 引力按 1/r² 从源中心衰减。引力强度为统一参考半径处的引力值，以 g⊕ 的倍数表示：
    /// M/M⊕ = 2^((massAnvilCount - 12) / 2)，参考半径 R_ref/R⊕ = 5000 × 109 = 545,000，强度 = (M/M⊕) / (R_ref/R⊕)²。
    private void updateGravitySource() {
        if (level == null || level.isClientSide()) return;

        boolean shouldHaveGravity = amplifierPresent
                                    && celestialBodyData instanceof StarData
                                    && stellarMass > 0
                                    && celestialBodyData.size() > 0;

        double newStrength = 0;
        if (shouldHaveGravity) {
            double massRatio = Math.pow(2, (stellarMass - 12) / 2.0);
            newStrength = massRatio * GRAVITY_STRENGTH_MULTIPLIER / (GRAVITY_REFERENCE_RADIUS_RATIO * GRAVITY_REFERENCE_RADIUS_RATIO);
        } /// 引力乘上这个常数得到感官合适的值 ↑
        int newSize = shouldHaveGravity ? celestialBodyData.size() : 0;

        BlockPos centerPos = worldPosition.offset(0, GRAVITY_CENTER_Y_OFFSET, 0);

        if (shouldHaveGravity) {
            if (!gravitySourceActive || newStrength != currentGravityStrength || newSize != currentGravitySize) {
                /// 如果强度/大小发生变化，则移除旧源
                if (gravitySourceActive) {
                    GravityManager.GravitySourceManager.removeSource(level, centerPos);
                }
                /// 添加新的/更新后的源
                GravityManager.GravitySourceType type = new GravityManager.GravitySourceType(newStrength, GRAVITY_RADIUS);
                GravityManager.GravitySourceManager.addSource(level, centerPos, type);
                gravitySourceActive = true;
                currentGravityStrength = newStrength;
                currentGravitySize = newSize;
            }
        } else if (gravitySourceActive) {
            GravityManager.GravitySourceManager.removeSource(level, centerPos);
            gravitySourceActive = false;
            currentGravityStrength = 0;
            currentGravitySize = 0;
        }
    }

    /// 强制移除引力源。当增幅器被拆除时调用，确保引力立即消失。
    public void removeGravitySource() {
        if (level == null || level.isClientSide()) return;
        BlockPos centerPos = worldPosition.offset(0, GRAVITY_CENTER_Y_OFFSET, 0);
        GravityManager.GravitySourceManager.removeSource(level, centerPos);
        gravitySourceActive = false;
        currentGravityStrength = 0;
        currentGravitySize = 0;
    }

    private void destroyEntitiesAtCenter() {
        BlockPos centerPos = worldPosition.offset(0, GRAVITY_CENTER_Y_OFFSET, 0);
        AABB centerBox = new AABB(centerPos);
        List<Entity> entities = Objects.requireNonNull(this.level).getEntitiesOfClass(Entity.class, centerBox);
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                if (this.celestialBodyData instanceof StarData star
                    && star.bodyClass() == CelestialBodyClass.BLACK_HOLE) {
                    living.hurt(ModDamageTypes.lostInTime(this.level), Float.MAX_VALUE);
                } else {
                    living.hurt(this.level.damageSources().inFire(), 1.0E12f);
                }
            } else {
                entity.discard();
            }
        }
    }

    /// 搜索历史，最多 10 条。索引 0 = 最新。
    @Getter
    private final List<SearchHistoryEntry> searchHistory = new ArrayList<>();
    private static final int MAX_HISTORY = 10;

    /// 一条搜索历史记录，将天体及其生成的资源捆绑在一起。
    public record SearchHistoryEntry(CelestialBodyData body, @Nullable PlanetaryResourceSet resources) {
        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.put("body", body.toTag());
            if (resources != null) {
                tag.put("resources", resources.toTag());
            }
            return tag;
        }

        public static SearchHistoryEntry fromTag(CompoundTag tag) {
            CelestialBodyData body = CelestialBodyData.fromTag(tag.getCompound("body"));
            PlanetaryResourceSet resources = null;
            if (tag.contains("resources")) {
                resources = PlanetaryResourceSet.fromTag(tag.getCompound("resources"));
            }
            return new SearchHistoryEntry(body, resources);
        }
    }

    /// 搜索历史的浏览索引：0 = 显示锁定的天体，1+ = 正在浏览。
    @Getter
    private int historyBrowseIndex = 0;
    @Nullable
    private SearchHistoryEntry historyOriginalEntry;

    @Getter
    private final SimpleContainer anvilInventory = new SimpleContainer(5) {
        @Override
        public void setChanged() {
            super.setChanged();
            CelestialForgingAnvilBlockEntity.this.setChanged();
        }
    };

    public void tick() {
        if (this.rotation == 360) this.rotation = 0;
        this.preRotation = this.rotation;
        this.rotation += 3;
        this.bodyRotation += 1;

        /// 动画计时（仅客户端）
        if (animationTicks > 0) {
            animationTicks--;
            if (animationTicks == 0 && !animationForward) {
                animationPreviousBodyData = null;
            }
        }
        /// 坍缩动画——在加速器阶段 3 期间，服务器每 tick 同步一次，
        /// 因此客户端不应独立递减以避免不同步。
        /// 在阶段 3 之外，客户端独立递减作为后备。
        var accel = megastructureManager.getAcceleratorHandler();
        if (accel.getCollapseAnimTicks() > 0 && accel.getStage() != 3) {
            accel.setCollapseAnimTicks(accel.getCollapseAnimTicks() - 1);
        }
    }

    public void setAmplify(boolean amplify) {
        if (this.isAmplify != amplify) {
            this.isAmplify = amplify;
            if (level != null && !level.isClientSide()) {
                if (celestialBodyData instanceof StarData) {
                    if (!amplify) {
                        this.locked = true; /// 移除增幅器且存在恒星天体时锁定
                    }
                }
            }
            this.setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide() && !PowerGrid.isServerClosing) {
            if (gravitySourceActive) {
                BlockPos centerPos = worldPosition.offset(0, GRAVITY_CENTER_Y_OFFSET, 0);
                GravityManager.GravitySourceManager.removeSource(level, centerPos);
                gravitySourceActive = false;
            }
            /// 注销虫洞并清除巨构，使已连接的传送门关闭。
            /// 服务器关闭期间跳过，避免保存时访问已保存数据。
            megastructureManager.clearAllMegastructures(this);
        }
    }

    /// 从 bodySeed 派生出的可复现的 ±5% 随机偏移百分比。仅用于 UI 显示年龄/半径/质量值。index 为 0=时元，1=空间，2=质量。返回在 [-0.05, +0.05] 范围内的偏移值。
    public float getDisplayOffset(int index) {
        if (bodySeed == 0) return 0f;
        net.minecraft.util.RandomSource rand = net.minecraft.util.RandomSource.create(bodySeed + index * 7919L);
        return (rand.nextFloat() - 0.5f) * 0.1f;
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void tryMatchCelestialBody() {
        if (level == null) return;
        int time = getAnvilCount(0);
        int space = getAnvilCount(1);
        int mass = getAnvilCount(2);
        int energy = getAnvilCount(3);
        this.ageAnvilCount = time;
        this.bodySeed = level.getRandom().nextLong();
        this.stellarMass = mass;

        /// 验证种子物品仍在——如果玩家在搜索期间移除了它，
        /// 清除捕获的数据，以便回退到普通匹配，而不是在未扣除种子物品的情况下
        /// 授予特殊行星。
        if (lastConsumedSeedItem != null || lastConsumedSeedNbt != null) {
            ItemStack seedStack = this.anvilInventory.getItem(4);
            if (seedStack.isEmpty()) {
                this.lastConsumedSeedItem = null;
                this.lastConsumedSeedNbt = null;
            }
        }

        /// 第一步：检查种子物品快照（磁盘 / 奇点晶体）
        if (lastConsumedSeedNbt != null && lastConsumedSeedNbt.contains("celestialBody")) {
            applySnapshot(lastConsumedSeedNbt);
            consumeSeedItem();
            return;
        }

        /// 第二步：通过种子物品检查特殊天体发现
        if (lastConsumedSeedItem != null) {
            SpecialCelestialBodyData specialBody = tryMatchSpecialCelestialBody(
                time,
                space,
                mass,
                energy,
                lastConsumedSeedItem,
                ((ServerLevel) level).getSeed()
            );
            if (specialBody != null) {
                this.celestialBodyData = specialBody;
                if (!level.isClientSide()) {
                    ResourceLocation recipeId = ResourceLocation.parse(specialBody.recipeId());
                    level.getRecipeManager().byKey(recipeId).ifPresent(holder -> {
                        if (holder.value() instanceof SpecialCelestialBodyRecipe recipe) {
                            this.planetaryResourceSet = recipe.generateResources();
                        }
                    });
                }
                addToSearchHistory(this.celestialBodyData, this.planetaryResourceSet);
                consumeSeedItem();
                if (!level.isClientSide()) {
                    this.setChanged();
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
                return;
            }
        }

        /// 回退到普通三步匹配
        this.celestialBodyData = CelestialBodyMatcher.match(time, space, mass, energy, this.isAmplify, level.getRandom());
        if (this.celestialBodyData != null) {
            /// 从 bodySeed 派生出 UUID 用于虫洞身份标识
            if (this.celestialBodyData instanceof StarData star && star.bodyUuid() == null) {
                this.celestialBodyData = star.withBodyUuid(StarData.uuidFromBodySeed(this.bodySeed));
            }
            /// 生成行星资源
            if (!level.isClientSide()) {
                ResourceLocation seedItemId = lastConsumedSeedItem != null
                    ? BuiltInRegistries.ITEM.getKey(lastConsumedSeedItem)
                    : null;
                this.planetaryResourceSet = PlanetResourceGenerator.generate(
                    this.celestialBodyData,
                    this.ageAnvilCount,
                    level,
                    this.bodySeed,
                    seedItemId
                );
            }
            addToSearchHistory(this.celestialBodyData, this.planetaryResourceSet);
        } else {
            this.planetaryResourceSet = null;
            this.searchTicksRemaining = 0; /// 失败时停止计时器
        }
        consumeSeedItem();

        if (!level.isClientSide()) {
            this.setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /// 尝试根据砧子参数和消耗的种子物品来匹配一个特殊（隐藏）天体。种子物品必须是此世界种子的有效物品（使用与 RoyalPreference 相同的模式）。
    private void consumeSeedItem() {
        if (level == null || level.isClientSide()) return;
        ItemStack seed = this.anvilInventory.getItem(4);
        if (!seed.isEmpty()) {
            this.anvilInventory.setItem(4, ItemStack.EMPTY);
        }
    }

    @javax.annotation.Nullable
    private SpecialCelestialBodyData tryMatchSpecialCelestialBody(
        int time,
        int space,
        int mass,
        int energy,
        Item consumedSeedItem,
        long worldSeed
    ) {
        if (level == null) return null;
        List<SpecialCelestialBodyRecipe> recipes = level.getRecipeManager()
            .getAllRecipesFor(ModRecipeTypes.SPECIAL_CELESTIAL_BODY_TYPE.get())
            .stream().map(RecipeHolder::value).toList();
        for (SpecialCelestialBodyRecipe recipe : recipes) {
            if (recipe.time() == time && recipe.space() == space
                && recipe.mass() == mass && recipe.energy() == energy
                && recipe.isEffectiveSeedItem(consumedSeedItem, worldSeed)
            ) {
                /// 查找配方持有者以获取完整 ID
                return level.getRecipeManager()
                    .getAllRecipesFor(ModRecipeTypes.SPECIAL_CELESTIAL_BODY_TYPE.get())
                    .stream()
                    .filter(h -> h.value() == recipe)
                    .findFirst()
                    .map(h -> SpecialCelestialBodyData.fromRecipe(recipe, h.id().toString()))
                    .orElse(null);
            }
        }
        return null;
    }

    /// 从快照（磁盘 / 奇点晶体种子物品）加载天体。快照包含所有参数——砧子数量在匹配时被忽略。
    private void applySnapshot(CompoundTag tag) {
        if (level == null) return;
        this.celestialBodyData = CelestialBodyData.fromTag(tag.getCompound("celestialBody"));
        this.bodySeed = tag.getLong("bodySeed");
        this.ageAnvilCount = tag.getInt("ageAnvilCount");
        this.stellarMass = tag.getInt("stellarMass");
        if (tag.contains("planetaryResources")) {
            this.planetaryResourceSet = PlanetaryResourceSet.fromTag(tag.getCompound("planetaryResources"));
        }
        addToSearchHistory(this.celestialBodyData, this.planetaryResourceSet);
        if (!level.isClientSide()) {
            this.setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /// === 磁盘克隆接口 ===

    @Override
    public void storeDiskData(CompoundTag tag) {
        if (celestialBodyData != null) {
            tag.put("celestialBody", celestialBodyData.toTag());
            tag.putLong("bodySeed", this.bodySeed);
            tag.putInt("ageAnvilCount", this.ageAnvilCount);
            tag.putInt("stellarMass", this.stellarMass);
            tag.putIntArray(
                "anvilCounts", new int[]{
                    getAnvilCount(0),
                    getAnvilCount(1),
                    getAnvilCount(2),
                    getAnvilCount(3)
                }
            );
            tag.putBoolean("isAmplify", this.isAmplify);
            if (planetaryResourceSet != null) {
                tag.put("planetaryResources", planetaryResourceSet.toTag());
            }
        }
    }

    @Override
    public void applyDiskData(CompoundTag tag) {
        /// 磁盘数据仅通过种子槽应用，不能通过右键。
    }

    @Override
    public InteractionResult useDisk(Level level, Player player, InteractionHand hand, ItemStack itemStack, BlockHitResult hitResult) {
        if (!player.getAbilities().mayBuild) return InteractionResult.PASS;
        if (itemStack.is(ModItems.DISK.get())) {
            /// 仅允许存储，不允许应用
            if (!DiskItem.hasDataStored(itemStack)) {
                /// 极端天体（黑洞 / 中子星）需要奇点晶体
                if (celestialBodyData instanceof StarData star && star.bodyClass().isExtreme()) {
                    player.displayClientMessage(
                        Component.translatable("message.anvilcraft.disk.extreme_body_requires_crystal")
                            .withStyle(ChatFormatting.RED),
                        true
                    );
                    return InteractionResult.FAIL;
                }
                /// 将点击重定向到主方块位置，使 DiskItem.useOn 能找到 BlockEntity
                BlockHitResult mainHit = new BlockHitResult(
                    hitResult.getLocation(),
                    hitResult.getDirection(),
                    this.getBlockPos(),
                    hitResult.isInside()
                );
                return itemStack.useOn(new UseOnContext(level, player, hand, itemStack, mainHit));
            }
        }
        return InteractionResult.PASS;
    }

    /// 从种子物品堆中提取天体快照。
    @javax.annotation.Nullable
    public static CompoundTag extractSnapshot(ItemStack stack) {
        if (stack.getItem() instanceof DiskItem && DiskItem.hasDataStored(stack)) {
            return DiskItem.getData(stack).copy();
        }
        return loadSnapshotFromStack(stack);
    }

    /// 从磁盘或奇点晶体中加载天体快照。
    @javax.annotation.Nullable
    public static CompoundTag loadSnapshotFromStack(ItemStack stack) {
        /// 磁盘
        if (stack.getItem() instanceof DiskItem && DiskItem.hasDataStored(stack)) {
            CompoundTag data = DiskItem.getData(stack);
            if (data.contains("celestialBody")) return data.copy();
        }
        /// 奇点晶体
        if (stack.is(ModBlocks.SINGULARITY_CRYSTAL.asItem())) {
            var customData = stack.getOrDefault(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY
            );
            CompoundTag tag = customData.copyTag();
            if (!tag.isEmpty() && tag.contains("celestialSnapshot")) {
                CompoundTag snapshot = tag.getCompound("celestialSnapshot");
                if (snapshot.contains("celestialBody")) return snapshot.copy();
            }
        }
        return null;
    }

    /// 将快照保存到磁盘或奇点晶体中。
    public static void saveSnapshotToStack(ItemStack stack, CompoundTag snapshot) {
        if (stack.getItem() instanceof DiskItem) {
            /// 极端天体（黑洞 / 中子星）不能存储在磁盘上
            if (snapshot.contains("celestialBody")) {
                CompoundTag bodyTag = snapshot.getCompound("celestialBody");
                String bodyClass = bodyTag.getString("bodyClass");
                if ("BLACK_HOLE".equals(bodyClass) || "NEUTRON_STAR".equals(bodyClass)) {
                    return; /// 静默拒绝——极端天体需要奇点晶体
                }
            }
            CompoundTag diskTag = DiskItem.createData(stack);
            diskTag.merge(snapshot);
        } else if (stack.is(ModBlocks.SINGULARITY_CRYSTAL.asItem())) {
            var oldCustom = stack.getOrDefault(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY
            );
            CompoundTag updated = oldCustom.copyTag();
            updated.put("celestialSnapshot", snapshot.copy());
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(updated));
        }
    }

    /// === CFA 方块交互 ===

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            /// 重新注册到电网，确保 CFA 同时位于生产者和消费者集合中
            PowerGrid.addComponent(this);
            /// 如果虫洞稳定器处于活动状态，则重新注册到虫洞网络
            /// 委托给 handler 的 onBuild 处理重新注册
            WormholeStabilizerHandler wh = megastructureManager.getWormholeHandler();
            if (megastructureManager.getActiveIndex() >= 0 && getActiveMegastructureOption() != null && "wormhole_stabilizer".equals(
                getActiveMegastructureOption().megastructure())) {
                wh.onBuild(this);
            }
            this.setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("amplified", this.isAmplify);
        tag.putLong("bodySeed", this.bodySeed);
        tag.putInt("stellarMass", this.stellarMass);

        tag.putBoolean("locked", this.locked);
        tag.putBoolean("amplifierPresent", this.amplifierPresent);
        tag.putBoolean("searching", this.searching);
        tag.putInt("searchTicks", this.searchTicksRemaining);
        tag.putBoolean("searchFailed", this.searchFailed);
        tag.putBoolean("powerInsufficient", this.powerInsufficient);
        if (celestialBodyData != null) {
            tag.put("celestialBody", celestialBodyData.toTag());
        }
        /// 搜索历史
        CompoundTag histTag = new CompoundTag();
        histTag.putInt("size", Math.min(searchHistory.size(), MAX_HISTORY));
        for (int i = 0; i < Math.min(searchHistory.size(), MAX_HISTORY); i++) {
            histTag.put("h" + i, searchHistory.get(i).toTag());
        }
        tag.put("searchHistory", histTag);
        /// 砧子物品栏
        CompoundTag invTag = new CompoundTag();
        for (int i = 0; i < 5; i++) {
            ItemStack stack = this.anvilInventory.getItem(i);
            if (!stack.isEmpty()) {
                invTag.put("s" + i, stack.save(registries));
            }
        }
        tag.put("anvils", invTag);
        /// 材料槽
        if (!materialFilter.isEmpty()) {
            tag.put("materialFilter", materialFilter.save(registries));
        }
        if (!materialFilter.isEmpty()) {
            tag.put("materialFilter", materialFilter.save(registries));
        }
        tag.putInt("materialLimit", materialLimit);
        tag.putInt("ageAnvilCount", this.ageAnvilCount);
        if (planetaryResourceSet != null) {
            tag.put("planetaryResources", planetaryResourceSet.toTag());
        }
        /// 虫洞稳定器状态（由 WormholeStabilizerHandler NBT 处理）
        if (!portals.isEmpty()) {
            CompoundTag portalTag = new CompoundTag();
            for (Map.Entry<Cube323PartHalf, BlockPos> entry : portals.entrySet()) {
                BlockPos p = entry.getValue();
                CompoundTag posTag = new CompoundTag();
                posTag.putInt("x", p.getX());
                posTag.putInt("y", p.getY());
                posTag.putInt("z", p.getZ());
                portalTag.put(entry.getKey().getSerializedName(), posTag);
            }
            tag.put("portals", portalTag);
        }
        /// 神殿状态
        tag.putInt("templeCycleDay", templeCycleDay);
        tag.putLong("templeLastDay", templeLastDay);
        if (!templeDemandItem.isEmpty()) {
            tag.put("templeDemand", templeDemandItem.save(registries));
        }
        tag.putInt("templeDemandCount", templeDemandCount);
        tag.putInt("templeDemandProgress", templeDemandProgress);
        tag.putBoolean("templeDemandSatisfied", templeDemandSatisfied);
        tag.putInt("historyBrowseIndex", historyBrowseIndex);
        /// 将巨构建造 NBT 委托给管理器
        megastructureManager.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.isAmplify = tag.getBoolean("amplified");
        this.stellarMass = tag.getInt("stellarMass");
        this.locked = tag.getBoolean("locked");
        this.amplifierPresent = tag.getBoolean("amplifierPresent");
        this.searching = tag.getBoolean("searching");
        this.searchTicksRemaining = tag.getInt("searchTicks");
        this.searchFailed = tag.getBoolean("searchFailed");
        this.powerInsufficient = tag.getBoolean("powerInsufficient");
        /// 如果 searching 为 true 但没有保存计时器（旧数据或新放置的），
        /// 重置标志以防止搜索状态卡住
        if (this.searching && this.searchTicksRemaining <= 0) {
            this.searching = false;
        }
        this.bodySeed = tag.getLong("bodySeed");
        /// 捕获旧天体数据用于动画过渡检测
        CelestialBodyData oldBodyData = this.celestialBodyData;
        if (tag.contains("celestialBody")) {
            this.celestialBodyData = CelestialBodyData.fromTag(tag.getCompound("celestialBody"));
        } else {
            this.celestialBodyData = null;
        }
        /// 检测动画过渡（仅客户端，例如单人游戏区块加载）
        /// 在加速器演化期间跳过动画
        boolean skipAnimLoad = getAcceleratorStage() >= 1;
        if (level != null && level.isClientSide() && !skipAnimLoad) {
            boolean hadBody = oldBodyData != null;
            boolean hasBody = this.celestialBodyData != null;
            if (!hadBody && hasBody) {
                this.animationTicks = ANIMATION_DURATION_TICKS;
                this.animationForward = true;
                this.animationPreviousBodyData = null;
            } else if (hadBody && !hasBody) {
                this.animationTicks = ANIMATION_DURATION_TICKS;
                this.animationForward = false;
                this.animationPreviousBodyData = oldBodyData;
            } else if (hadBody && !oldBodyData.toTag().equals(this.celestialBodyData.toTag())) {
                this.animationTicks = ANIMATION_DURATION_TICKS;
                this.animationForward = true;
                this.animationPreviousBodyData = oldBodyData;
            }
        }
        loadSearchHistory(tag);
        loadInventory(tag, registries);
        /// 材料过滤器
        if (tag.contains("materialFilter")) {
            this.materialFilter = ItemStack.parse(registries, tag.getCompound("materialFilter"))
                .orElse(new ItemStack(net.minecraft.world.item.Items.BARRIER));
        } else {
            this.materialFilter = new ItemStack(net.minecraft.world.item.Items.BARRIER);
        }
        this.materialLimit = tag.getInt("materialLimit");
        this.ageAnvilCount = tag.getInt("ageAnvilCount");
        if (tag.contains("planetaryResources")) {
            this.planetaryResourceSet = PlanetaryResourceSet.fromTag(tag.getCompound("planetaryResources"));
        } else {
            this.planetaryResourceSet = null;
        }
        /// 虫洞稳定器状态（由 WormholeStabilizerHandler NBT 处理）
        this.portals.clear();
        if (tag.contains("portals")) {
            CompoundTag portalTag = tag.getCompound("portals");
            for (String key : portalTag.getAllKeys()) {
                CompoundTag posTag = portalTag.getCompound(key);
                Cube323PartHalf side = Cube323PartHalf.valueOf(key.toUpperCase());
                BlockPos pos = new BlockPos(posTag.getInt("x"), posTag.getInt("y"), posTag.getInt("z"));
                portals.put(side, pos);
            }
        }
        /// 神殿状态
        this.templeCycleDay = tag.getInt("templeCycleDay");
        this.templeLastDay = tag.contains("templeLastDay") ? tag.getLong("templeLastDay") : -1;
        if (tag.contains("templeDemand")) {
            this.templeDemandItem = ItemStack.parse(registries, tag.getCompound("templeDemand")).orElse(ItemStack.EMPTY);
        } else {
            this.templeDemandItem = ItemStack.EMPTY;
        }
        this.templeDemandCount = tag.getInt("templeDemandCount");
        this.templeDemandProgress = tag.getInt("templeDemandProgress");
        this.templeDemandSatisfied = tag.getBoolean("templeDemandSatisfied");
        /// 对撞机运行时状态不持久化——加载时始终从干净状态开始
        this.historyBrowseIndex = tag.getInt("historyBrowseIndex");
        /// 将巨构建造 NBT 委托给管理器（必须放在最后，以便管理器覆盖 BE 字段）
        megastructureManager.loadAdditional(tag, registries);
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("amplified", this.isAmplify);
        tag.putLong("bodySeed", this.bodySeed);
        tag.putInt("stellarMass", this.stellarMass);

        tag.putBoolean("locked", this.locked);
        tag.putBoolean("amplifierPresent", this.amplifierPresent);
        tag.putBoolean("searching", this.searching);
        tag.putInt("searchTicks", this.searchTicksRemaining);
        tag.putBoolean("searchFailed", this.searchFailed);
        tag.putBoolean("powerInsufficient", this.powerInsufficient);
        if (celestialBodyData != null) {
            tag.put("celestialBody", celestialBodyData.toTag());
        }
        /// 搜索历史
        CompoundTag histTag = new CompoundTag();
        histTag.putInt("size", Math.min(searchHistory.size(), MAX_HISTORY));
        for (int i = 0; i < Math.min(searchHistory.size(), MAX_HISTORY); i++) {
            histTag.put("h" + i, searchHistory.get(i).toTag());
        }
        tag.put("searchHistory", histTag);
        /// 材料过滤器同步
        if (!materialFilter.isEmpty()) {
            tag.put("materialFilter", materialFilter.save(registries));
        }
        tag.putInt("materialLimit", materialLimit);
        tag.putInt("ageAnvilCount", this.ageAnvilCount);
        if (planetaryResourceSet != null) {
            tag.put("planetaryResources", planetaryResourceSet.toTag());
        }
        /// 神殿状态（客户端同步）
        tag.putInt("templeCycleDay", templeCycleDay);
        tag.putLong("templeLastDay", templeLastDay);
        if (!templeDemandItem.isEmpty()) {
            tag.put("templeDemand", templeDemandItem.save(registries));
        }
        tag.putInt("templeDemandCount", templeDemandCount);
        tag.putInt("templeDemandProgress", templeDemandProgress);
        tag.putBoolean("templeDemandSatisfied", templeDemandSatisfied);
        /// 对撞机运行时状态不同步到客户端
        tag.putInt("historyBrowseIndex", historyBrowseIndex);
        /// 将巨构建造 NBT 委托给管理器
        megastructureManager.writeUpdateTag(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.handleUpdateTag(tag, lookupProvider);
        this.isAmplify = tag.getBoolean("amplified");
        this.stellarMass = tag.getInt("stellarMass");
        this.locked = tag.getBoolean("locked");
        this.amplifierPresent = tag.getBoolean("amplifierPresent");
        this.searching = tag.getBoolean("searching");
        this.searchTicksRemaining = tag.getInt("searchTicks");
        this.searchFailed = tag.getBoolean("searchFailed");
        this.powerInsufficient = tag.getBoolean("powerInsufficient");
        this.bodySeed = tag.getLong("bodySeed");

        /// 捕获旧天体数据用于动画过渡检测
        CelestialBodyData oldBodyData = this.celestialBodyData;
        if (tag.contains("celestialBody")) {
            this.celestialBodyData = CelestialBodyData.fromTag(tag.getCompound("celestialBody"));
        } else {
            this.celestialBodyData = null;
        }
        /// 检测动画过渡（仅客户端）
        /// 在加速器演化期间跳过动画
        boolean skipAnim = getAcceleratorStage() >= 1;
        if (level != null && level.isClientSide() && !skipAnim) {
            boolean hadBody = oldBodyData != null;
            boolean hasBody = this.celestialBodyData != null;
            if (!hadBody && hasBody) {
                /// 天体出现——启动正向（放大淡入）动画
                this.animationTicks = ANIMATION_DURATION_TICKS;
                this.animationForward = true;
                this.animationPreviousBodyData = null;
            } else if (hadBody && !hasBody) {
                /// 天体消失——启动反向（缩小淡出）动画
                this.animationTicks = ANIMATION_DURATION_TICKS;
                this.animationForward = false;
                this.animationPreviousBodyData = oldBodyData;
            } else if (hadBody && !oldBodyData.toTag().equals(this.celestialBodyData.toTag())) {
                /// 天体变为不同类型——动画过渡
                this.animationTicks = ANIMATION_DURATION_TICKS;
                this.animationForward = true;
                this.animationPreviousBodyData = oldBodyData;
            }
        }
        loadSearchHistory(tag);
        loadInventory(tag, lookupProvider);
        /// 材料过滤器（客户端——从同步中读取）
        if (tag.contains("materialFilter")) {
            this.materialFilter = ItemStack.parse(lookupProvider, tag.getCompound("materialFilter"))
                .orElse(new ItemStack(net.minecraft.world.item.Items.BARRIER));
        } else {
            this.materialFilter = new ItemStack(net.minecraft.world.item.Items.BARRIER);
        }
        this.materialLimit = tag.getInt("materialLimit");
        this.ageAnvilCount = tag.getInt("ageAnvilCount");
        if (tag.contains("planetaryResources")) {
            this.planetaryResourceSet = PlanetaryResourceSet.fromTag(tag.getCompound("planetaryResources"));
        } else {
            this.planetaryResourceSet = null;
        }
        /// 神殿状态（客户端）
        this.templeCycleDay = tag.getInt("templeCycleDay");
        this.templeLastDay = tag.contains("templeLastDay") ? tag.getLong("templeLastDay") : -1;
        if (tag.contains("templeDemand")) {
            this.templeDemandItem = ItemStack.parse(lookupProvider, tag.getCompound("templeDemand")).orElse(ItemStack.EMPTY);
        } else {
            this.templeDemandItem = ItemStack.EMPTY;
        }
        this.templeDemandCount = tag.getInt("templeDemandCount");
        this.templeDemandProgress = tag.getInt("templeDemandProgress");
        this.templeDemandSatisfied = tag.getBoolean("templeDemandSatisfied");
        /// 对撞机运行时状态不同步到客户端
        this.historyBrowseIndex = tag.getInt("historyBrowseIndex");
        /// 将巨构建造 NBT 委托给管理器
        megastructureManager.readUpdateTag(tag, lookupProvider);
    }

    private void loadInventory(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("anvils")) {
            CompoundTag invTag = tag.getCompound("anvils");
            for (int i = 0; i < 5; i++) {
                String key = "s" + i;
                this.anvilInventory.setItem(
                    i,
                    invTag.contains(key)
                    ? ItemStack.parse(registries, Objects.requireNonNull(invTag.get(key))).orElse(ItemStack.EMPTY)
                    : ItemStack.EMPTY
                );
            }
        }
    }

    public int getAnvilCount(int slot) {
        return this.anvilInventory.getItem(slot).getCount();
    }

    public void addToSearchHistory(CelestialBodyData data, @Nullable PlanetaryResourceSet resources) {
        /// 去重：如果已是最新条目则不添加
        if (!searchHistory.isEmpty()) {
            SearchHistoryEntry latest = searchHistory.getFirst();
            if (latest.body().toTag().toString().equals(data.toTag().toString())) return;
        }
        searchHistory.addFirst(new SearchHistoryEntry(data, resources));
        while (searchHistory.size() > MAX_HISTORY) {
            searchHistory.removeLast();
        }
    }

    private void loadSearchHistory(CompoundTag tag) {
        searchHistory.clear();
        if (tag.contains("searchHistory")) {
            CompoundTag histTag = tag.getCompound("searchHistory");
            int size = Math.min(histTag.getInt("size"), MAX_HISTORY);
            for (int i = 0; i < size; i++) {
                if (histTag.contains("h" + i)) {
                    CompoundTag entryTag = histTag.getCompound("h" + i);
                    if (entryTag.contains("body")) {
                        /// 新格式：SearchHistoryEntry
                        searchHistory.add(SearchHistoryEntry.fromTag(entryTag));
                    } else {
                        /// 旧格式：裸 CelestialBodyData（未保存资源）
                        CelestialBodyData body = CelestialBodyData.fromTag(entryTag);
                        searchHistory.add(new SearchHistoryEntry(body, null));
                    }
                }
            }
        }
    }

    /// === 搜索历史浏览（服务端）===

    public boolean hasPreviousHistory() {
        int sz = searchHistory.size();
        return sz > 1 && historyBrowseIndex < sz;
    }

    public boolean hasNextHistory() {
        return historyBrowseIndex > 0;
    }

    public void browseHistoryPrev() {
        if (level == null || level.isClientSide()) return;
        int sz = searchHistory.size();
        /// 至少需要 2 条记录：索引 0 是当前锁定的天体
        if (sz <= 1 || historyBrowseIndex >= sz) return;
        if (historyBrowseIndex == 0) {
            historyOriginalEntry = new SearchHistoryEntry(Objects.requireNonNull(celestialBodyData), planetaryResourceSet);
            historyBrowseIndex = 1; /// 跳过当前天体条目
        }
        historyBrowseIndex++;
        if (historyBrowseIndex > sz) return;
        applyHistoryEntry();
    }

    public void browseHistoryNext() {
        if (level == null || level.isClientSide()) return;
        if (historyBrowseIndex <= 0) return;
        historyBrowseIndex--;
        if (historyBrowseIndex == 0) {
            if (historyOriginalEntry != null) {
                celestialBodyData = historyOriginalEntry.body();
                planetaryResourceSet = historyOriginalEntry.resources();
                historyOriginalEntry = null;
            }
            setChanged();
            syncToClient();
        } else {
            applyHistoryEntry();
        }
    }

    private void applyHistoryEntry() {
        if (historyBrowseIndex > 0 && historyBrowseIndex <= searchHistory.size()) {
            SearchHistoryEntry entry = searchHistory.get(historyBrowseIndex - 1);
            celestialBodyData = entry.body();
            planetaryResourceSet = entry.resources();
        }
        setChanged();
        syncToClient();
    }

    private void syncToClient() {
        if (level instanceof ServerLevel serverLevel) {
            Packet<?> packet = getUpdatePacket();
            for (ServerPlayer serverPlayer : serverLevel.getChunkSource().chunkMap.getPlayers(
                serverLevel.getChunkAt(worldPosition)
                    .getPos(), false
            )) {
                serverPlayer.connection.send(packet);
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("screen.anvilcraft.celestial_forging_anvil");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (this.level == null) return null;
        return new CelestialForgingAnvilMenu(ModMenuTypes.CFA.get(), containerId, inventory, this);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.getBlockPos());
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /// === 巨构建造 ===

    /// 切换锁定状态。玩家点击锁定按钮时在服务器端调用。
    public void toggleLocked() {
        if (level == null || level.isClientSide()) return;
        if (isAcceleratorActive()) {
            /// 星体演化期间无法解锁
            return;
        }
        this.locked = !this.locked;
        if (!this.locked) {
            /// 解锁：清除巨构和加速器，恢复为束星环
            clearMegastructure();
            clearAcceleratorState();
        }
        this.setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private void clearAcceleratorState() {
        megastructureManager.getAcceleratorHandler().onClear(this);
    }

    /// 清除活动巨构及所有相关状态，恢复为束星环。
    private void clearMegastructure() {
        megastructureManager.clearMegastructure(this);
        /// 清除材料过滤器（仍由 BE 持有）
        this.materialFilter = new ItemStack(Items.BARRIER);
        this.materialLimit = 0;
        /// 重新注册到电网以恢复 CONSUMER 类型
        PowerGrid.addComponent(this);
    }

    /// 获取与客户端看到的匹配的选项列表（应用相同的过滤）。当巨构已建造时，仅加速器可见。
    public List<CelestialRefactorOption> getClientVisibleOptions() {
        List<CelestialRefactorOption> options = CelestialRefactorRegistry.getOptions(
            celestialBodyData,
            isAmplify,
            this.planetaryResourceSet
        );
        if (megastructureManager.getActiveIndex() >= 0) {
            options = options.stream().filter(opt -> "stellar_evolution_accelerator".equals(opt.megastructure())).toList();
        }
        return options;
    }

    /// 获取当前活动的巨构选项，如果未建造则返回 null。
    @Nullable
    public CelestialRefactorOption getActiveMegastructureOption() {
        return megastructureManager.getActiveOption(this);
    }

    /// 获取放置在此 CFA 各侧的传送门（不可修改）。
    public Map<Cube323PartHalf, BlockPos> getPortals() {
        WormholeStabilizerHandler wh = megastructureManager.getWormholeHandler();
        return wh.getPortals();
    }

    /// 尝试建造巨构。玩家点击"开始重构"时在服务器端调用。optionIndex 为选中的重构选项索引。
    public void buildMegastructure(int optionIndex) {
        if (level == null || level.isClientSide()) return;
        if (celestialBodyData == null) return;
        List<CelestialRefactorOption> options = getClientVisibleOptions();
        if (optionIndex < 0 || optionIndex >= options.size()) return;

        CelestialRefactorOption option = options.get(optionIndex);

        /// 先检查材料
        if (option.needsMaterial()) {
            ItemStack contained = materialContainer.getItem(0);
            ItemStack required = option.material().copyWithCount(option.materialCount());
            if (!ItemStack.isSameItemSameComponents(contained, required) || contained.getCount() < required.getCount()) {
                return;
            }
            contained.shrink(required.getCount());
        }

        /// 委托给巨构建造管理器
        megastructureManager.buildMegastructure(optionIndex, this);

        /// 重新注册到电网以使组件类型变更生效
        PowerGrid.addComponent(this);
        this.setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /// === 虫洞内容同步 ===

    /// 当玩家在物流接口中放入或取出物品时立即调用。委托给 WormholeStabilizerHandler。
    public void syncLogisticsOnChange(BlockPos interfacePos, int changedSlot) {
        WormholeStabilizerHandler wh = megastructureManager.getWormholeHandler();
        wh.syncLogisticsOnChange(interfacePos, changedSlot, this);
    }

    /// 在 CFA 的特定侧面上注册一个传送门。
    public void addPortal(Cube323PartHalf side, BlockPos portalPos) {
        WormholeStabilizerHandler wh = megastructureManager.getWormholeHandler();
        wh.addPortal(side, portalPos, this);
    }

    /// 从特定侧面注销一个传送门。
    public void removePortal(Cube323PartHalf side) {
        WormholeStabilizerHandler wh = megastructureManager.getWormholeHandler();
        wh.removePortal(side, this);
    }
}
