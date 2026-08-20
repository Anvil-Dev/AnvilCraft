package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorRegistry;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialSearchHistory;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetaryResourceSet;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.entity.megastructure.ExcavatorHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.PenroseSphereHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.WormholeStabilizerHandler;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import dev.dubhe.anvilcraft.init.ModMegastructures;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.CelestialForgingAnvilMenu;
import dev.dubhe.anvilcraft.item.DiskItem;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

public class CelestialForgingAnvilBlockEntity extends BlockEntity implements MenuProvider, IPowerConsumer, IPowerProducer, IDiskCloneable {
    // 38 * 0.52 = 19.76 after Feather Falling IV.
    private static final float PLANET_CONTACT_DAMAGE = 38.0f;
    // 66 * 0.84 * 0.36 = 19.9584 after full Protection IV diamond armor.
    private static final float STAR_CONTACT_DAMAGE = 66.0f;

    /// === 巨构建造委托 ===
    @Getter
    private final CfaMegastructureManager megastructureManager = new CfaMegastructureManager();

    @Getter
    private int preRotation = 0;
    @Getter
    private int rotation = 0;

    @Getter
    private boolean isAmplify = false;

    /// 获取锻星砧 3×2×3 结构接收到的最大红石信号强度（0–15）。
    /// 遍历结构包围盒内全部 18 个方块位置，取各方块邻居信号的最大值。
    /// 结果缓存 REDSTONE_SIGNAL_CACHE_TICKS 刻，到期或 neighborChanged 触发时重算。
    public int getRedstoneSignal() {
        // Comparator output is not guaranteed to be available in a freshly loaded client chunk.
        if (level == null || level.isClientSide()) return cachedRedstoneSignal;
        long now = level.getGameTime();
        if (redstoneSignalCacheTick >= 0 && now - redstoneSignalCacheTick < REDSTONE_SIGNAL_CACHE_TICKS) {
            return cachedRedstoneSignal;
        }
        int signal = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos partPos = worldPosition.offset(dx, dy, dz);
                    signal = Math.max(signal, level.getBestNeighborSignal(partPos));
                }
            }
        }
        cachedRedstoneSignal = Math.min(signal, 15);
        redstoneSignalCacheTick = now;
        return cachedRedstoneSignal;
    }

    /// neighborChanged 回调时调用，立即失效红石信号缓存。
    public void markRedstoneSignalDirty() {
        redstoneSignalCacheTick = -1;
    }

    private void syncRedstoneSignalIfChanged() {
        if (level == null || level.isClientSide()) return;
        getRedstoneSignal();
        if (cachedRedstoneSignal != syncedRedstoneSignal) {
            syncedRedstoneSignal = cachedRedstoneSignal;
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private int cachedRedstoneSignal = 0;
    private int syncedRedstoneSignal = 0;
    private long redstoneSignalCacheTick = -1;
    private static final int REDSTONE_SIGNAL_CACHE_TICKS = 5;

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

    public boolean hasActiveMegastructure() {
        return this.megastructureManager.hasActiveMegastructure();
    }

    public @Nullable ResourceLocation getActiveMegastructureId() {
        return this.megastructureManager.getActiveId(this);
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
        if (this.searchController.isSearching() && this.searchController.ticksRemaining() > 0) {
            return isAmplify ? 32000 : 1000;
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

    /// === 超新星爆发闪光（同步到客户端，仅用于渲染）===
    /// 超新星闪光剩余刻数，从 SUPERNOVA_FLASH_TICKS 递减到 0。0 表示无闪光。
    @Getter
    private int supernovaFlashTicks = 0;
    /// 触发时捕获的天体视觉中心世界 Y（闪光中心，独立于其后生成的残骸位置）。
    @Getter
    private double supernovaCenterY = 0;
    /// 触发时捕获的天体缩放比例（相对红石 15 满级的比值），用于让闪光大小跟随红石缩放。
    @Getter
    private float supernovaScale = 1.0f;
    /// 超新星闪光总时长（刻）。8 帧 × 每帧 3 刻 = 24 刻（约 1.2 秒）。
    public static final int SUPERNOVA_FLASH_TICKS = 24;

    /// 在服务端触发超新星闪光，并同步到客户端。由 AcceleratorHandler 在超新星阶段调用。
    /// 必须在生成残骸（替换天体数据）之前调用，以便捕获爆炸恒星的中心与缩放。
    public void startSupernovaFlash() {
        this.supernovaFlashTicks = SUPERNOVA_FLASH_TICKS;
        this.supernovaCenterY = getBodyCenterWorldY();
        /// 缩放比 = 当前天体缩放 / 基础（无红石）天体缩放：无红石时为 1（基准 16×16 格），
        /// 红石越高天体越大、闪光也越大，从而"缩放倍率与天体一致"。
        if (celestialBodyData != null) {
            float redstoneFactor = getRedstoneSignal() / 15.0f;
            float rawBodyScale = celestialBodyData.bodyScale();
            float fullBodyScale = rawBodyScale * CelestialBodyData.BODY_SCALE_FACTOR;
            float bodyScaleMultiplier = rawBodyScale + (fullBodyScale - rawBodyScale) * redstoneFactor;
            this.supernovaScale = rawBodyScale > 1e-6f ? bodyScaleMultiplier / rawBodyScale : 1.0f;
        } else {
            this.supernovaScale = 1.0f;
        }
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /// 计算当前红石信号下天体视觉中心的世界 Y 坐标。
    /// 与渲染端 centerY 计算一致：baseCenterY 与完整 dynamicCenterY 之间按红石比例线性插值。
    public double getBodyCenterWorldY() {
        float redstoneFactor = getRedstoneSignal() / 15.0f;
        float fullCenterY = CelestialBodyData.dynamicCenterY(celestialBodyData, isAmplify);
        float baseCenterY = isAmplify ? 6.5f : 4.5f;
        float centerY = baseCenterY + (fullCenterY - baseCenterY) * redstoneFactor;
        return worldPosition.getY() + centerY;
    }

    /// === 渲染端缩放/高度插值平滑（仅客户端，不持久化、不同步）===
    /// 对环缩放、天体中心高度、天体缩放、光束高度做帧率无关的指数逼近，
    /// 使红石信号变化时的尺寸/高度变化丝滑过渡而非瞬间跳变。
    @Getter
    private float smoothRingScale;
    @Getter
    private float smoothCenterY;
    @Getter
    private float smoothBodyScale;
    @Getter
    private float smoothBeamHeight;
    private boolean smoothInitialized = false;
    private long lastSmoothNanos = 0L;
    /// 指数逼近时间常数（秒）。越小越快跟上目标。
    private static final float SMOOTH_TAU = 0.18f;

    /// 推进一帧的平滑插值，返回帧率无关的逼近系数。首帧直接吸附到目标值。
    private float advanceSmoothFactor() {
        long now = Util.getNanos();
        if (!smoothInitialized) {
            lastSmoothNanos = now;
            return 1.0f;
        }
        float dt = (now - lastSmoothNanos) / 1.0e9f;
        lastSmoothNanos = now;
        if (dt <= 0f) return 0f;
        if (dt > 0.25f) dt = 0.25f; /// 防止卡顿/暂停后跳变
        return 1.0f - (float) Math.exp(-dt / SMOOTH_TAU);
    }

    /// 更新平滑后的渲染缩放/高度值。由渲染器每帧调用，传入当前红石信号下的目标值。
    public void updateRenderSmoothing(float targetRingScale, float targetCenterY, float targetBodyScale, float targetBeamHeight) {
        float f = advanceSmoothFactor();
        if (!smoothInitialized) {
            smoothRingScale = targetRingScale;
            smoothCenterY = targetCenterY;
            smoothBodyScale = targetBodyScale;
            smoothBeamHeight = targetBeamHeight;
            smoothInitialized = true;
            return;
        }
        smoothRingScale += (targetRingScale - smoothRingScale) * f;
        smoothCenterY += (targetCenterY - smoothCenterY) * f;
        smoothBodyScale += (targetBodyScale - smoothBodyScale) * f;
        smoothBeamHeight += (targetBeamHeight - smoothBeamHeight) * f;
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
        if (!this.locked) return;
        if (this.isSearching() || this.isAcceleratorActive()) return;
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

    /// 搜索状态委托给独立状态机；旧 getter/setter 在下方保留。
    private final CfaSearchController searchController = new CfaSearchController();

    /// 电网
    @Setter
    @Nullable
    private PowerGrid grid;

    /// 引力源状态委托给独立控制器。
    private final CfaGravityController gravityController = new CfaGravityController();

    public void startSearch() {
        if (this.locked || this.isSearching() || this.isAcceleratorActive()) return;
        this.searchController.start(this);
    }

    public boolean isSearching() {
        return this.searchController.isSearching();
    }

    public int getSearchTicksRemaining() {
        return this.searchController.ticksRemaining();
    }

    public boolean isSearchFailed() {
        return this.searchController.hasFailed();
    }

    public boolean isPowerInsufficient() {
        return this.searchController.isPowerInsufficient();
    }

    /** Marks search state dirty and sends the small block-entity update used by the CFA screen. */
    void markSearchStateChanged() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public void serverTick() {
        syncRedstoneSignalIfChanged();
        /// 超新星闪光计时（服务端）——递减以免同步出陈旧的激活状态。
        if (supernovaFlashTicks > 0) {
            supernovaFlashTicks--;
        }
        this.searchController.serverTick(this);

        /// 管理恒星引力源
        this.updateGravitySource();

        /// 巨构建造逻辑（委托给处理类）
        megastructureManager.serverTick(this);
    }

    /// Updates the physical source from the same geometry used by rendering and contact damage.
    private void updateGravitySource() {
        this.gravityController.tick(
            this.level,
            this.worldPosition,
            this.isAmplify,
            this.amplifierPresent,
            this.celestialBodyData,
            this.stellarMass,
            this.getRedstoneSignal()
        );
    }

    /// 强制移除引力源。当增幅器被拆除时调用，确保引力立即消失。
    public void removeGravitySource() {
        this.gravityController.remove(this.level, this.worldPosition);
    }

    public void handleEntityContact(Entity entity) {
        this.gravityController.handleEntityContact(this.level, this.celestialBodyData, entity);
    }

    /// 搜索历史，最多 10 条。索引 0 = 最新。
    @Getter
    private final List<SearchHistoryEntry> searchHistory = new ArrayList<>();
    private final CelestialSearchHistory searchHistoryController = new CelestialSearchHistory(this.searchHistory);
    private @Nullable CompoundTag cachedDropData;
    private boolean permanentRemovalPrepared;

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
        /// 超新星闪光计时（客户端）。服务端在阶段 3 期间也会递减并同步，
        /// 但客户端独立递减以保证流畅；二者都到 0 即结束。
        if (supernovaFlashTicks > 0) {
            supernovaFlashTicks--;
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
            this.removeGravitySource();
            this.megastructureManager.unload(this);
        }
    }

    public void prepareForPermanentRemoval() {
        if (this.permanentRemovalPrepared
            || this.level == null
            || this.level.isClientSide()
            || PowerGrid.isServerClosing) {
            return;
        }
        this.cachedDropData = this.saveCustomOnly(this.level.registryAccess());
        this.permanentRemovalPrepared = true;
        this.removeGravitySource();
        this.megastructureManager.clearAllMegastructures(this);
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        this.cachedDropData = null;
        this.permanentRemovalPrepared = false;
    }

    public CompoundTag saveForDrop(HolderLookup.Provider registries) {
        return this.cachedDropData == null ? this.saveCustomOnly(registries) : this.cachedDropData.copy();
    }

    /// 从 bodySeed 派生出的可复现的 ±5% 随机偏移百分比。仅用于 UI 显示年龄/半径/质量值。index 为 0=时元，1=空间，2=质量。返回在 [-0.05, +0.05] 范围内的偏移值。
    public float getDisplayOffset(int index) {
        if (bodySeed == 0) return 0f;
        net.minecraft.util.RandomSource rand = net.minecraft.util.RandomSource.create(bodySeed + index * 7919L);
        return (rand.nextFloat() - 0.5f) * 0.1f;
    }

    public void tryMatchCelestialBody() {
        this.searchController.match(this);
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
        return dev.dubhe.anvilcraft.block.entity.celestial.CelestialSnapshotCodec.extract(stack);
    }

    /// 从磁盘或奇点晶体中加载天体快照。
    @javax.annotation.Nullable
    public static CompoundTag loadSnapshotFromStack(ItemStack stack) {
        return dev.dubhe.anvilcraft.block.entity.celestial.CelestialSnapshotCodec.load(stack);
    }

    /// 将快照保存到磁盘或奇点晶体中。
    public static void saveSnapshotToStack(ItemStack stack, CompoundTag snapshot) {
        dev.dubhe.anvilcraft.block.entity.celestial.CelestialSnapshotCodec.save(stack, snapshot);
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
            if (ModMegastructures.WORMHOLE_STABILIZER.getId().equals(megastructureManager.getActiveId(this))) {
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
        tag.putInt("redstoneSignal", this.cachedRedstoneSignal);
        tag.putLong("bodySeed", this.bodySeed);
        tag.putInt("stellarMass", this.stellarMass);

        tag.putBoolean("locked", this.locked);
        tag.putBoolean("amplifierPresent", this.amplifierPresent);
        tag.putBoolean("searching", this.isSearching());
        tag.putInt("searchTicks", this.getSearchTicksRemaining());
        tag.putBoolean("searchFailed", this.isSearchFailed());
        tag.putBoolean("powerInsufficient", this.isPowerInsufficient());
        this.searchController.save(tag, registries);
        if (celestialBodyData != null) {
            tag.put("celestialBody", celestialBodyData.toTag());
        }
        /// 搜索历史
        tag.put("searchHistory", this.searchHistoryController.toTag());
        /// 砧子物品栏
        tag.put("anvils", CfaInventoryCodec.save(this.anvilInventory, registries));
        /// 材料槽
        if (!materialFilter.isEmpty()) {
            tag.put("materialFilter", materialFilter.save(registries));
        }
        tag.putInt("materialLimit", materialLimit);
        tag.putInt("ageAnvilCount", this.ageAnvilCount);
        if (planetaryResourceSet != null) {
            tag.put("planetaryResources", planetaryResourceSet.toTag());
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
        tag.putInt("historyBrowseIndex", this.searchHistoryController.browseIndex());
        /// 将巨构建造 NBT 委托给管理器
        megastructureManager.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.isAmplify = tag.getBoolean("amplified");
        this.cachedRedstoneSignal = Math.max(0, Math.min(tag.getInt("redstoneSignal"), 15));
        this.syncedRedstoneSignal = this.cachedRedstoneSignal;
        this.redstoneSignalCacheTick = -1;
        this.stellarMass = tag.getInt("stellarMass");
        this.locked = tag.getBoolean("locked");
        this.amplifierPresent = tag.getBoolean("amplifierPresent");
        this.searchController.loadPersistent(
            tag.getBoolean("searching"),
            tag.getInt("searchTicks"),
            tag.getBoolean("searchFailed"),
            tag.getBoolean("powerInsufficient"),
            tag,
            registries
        );
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
        if (tag.contains("searchHistory")) {
            this.searchHistoryController.load(tag.getCompound("searchHistory"));
        } else {
            this.searchHistoryController.clear();
        }
        this.searchHistoryController.setBrowseIndex(tag.getInt("historyBrowseIndex"));
        if (tag.contains("anvils")) {
            CfaInventoryCodec.load(tag.getCompound("anvils"), this.anvilInventory, registries);
        } else {
            CfaInventoryCodec.load(new CompoundTag(), this.anvilInventory, registries);
        }
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
        /// 超新星闪光（客户端渲染）——运行时同步走 loadAdditional（onDataPacket→loadWithComponents），
        /// 故必须在此读取；仅在收到更大 ticks 时重启，避免覆盖客户端流畅递减。
        if (tag.contains("supernovaFlashTicks")) {
            int incomingFlash = tag.getInt("supernovaFlashTicks");
            if (incomingFlash > this.supernovaFlashTicks) {
                this.supernovaFlashTicks = incomingFlash;
            }
            this.supernovaCenterY = tag.getDouble("supernovaCenterY");
            this.supernovaScale = tag.contains("supernovaScale") ? tag.getFloat("supernovaScale") : 1.0f;
        }
        /// 将巨构建造 NBT 委托给管理器（必须放在最后，以便管理器覆盖 BE 字段）
        megastructureManager.loadAdditional(tag, registries);
        // Resolve legacy name/index data once the celestial context and handlers are loaded.
        megastructureManager.getActiveId(this);
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("amplified", this.isAmplify);
        tag.putInt("redstoneSignal", getRedstoneSignal());
        this.syncedRedstoneSignal = this.cachedRedstoneSignal;
        tag.putLong("bodySeed", this.bodySeed);
        tag.putInt("stellarMass", this.stellarMass);

        tag.putBoolean("locked", this.locked);
        tag.putBoolean("amplifierPresent", this.amplifierPresent);
        tag.putBoolean("searching", this.isSearching());
        tag.putInt("searchTicks", this.getSearchTicksRemaining());
        tag.putBoolean("searchFailed", this.isSearchFailed());
        tag.putBoolean("powerInsufficient", this.isPowerInsufficient());
        if (celestialBodyData != null) {
            tag.put("celestialBody", celestialBodyData.toTag());
        }
        /// 搜索历史
        tag.put("searchHistory", this.searchHistoryController.toTag());
        /// 同步全部 5 个槽位，供客户端界面显示。
        tag.put("anvils", CfaInventoryCodec.save(this.anvilInventory, registries));
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
        tag.putInt("historyBrowseIndex", this.searchHistoryController.browseIndex());
        /// 超新星闪光（客户端渲染）
        tag.putInt("supernovaFlashTicks", supernovaFlashTicks);
        tag.putDouble("supernovaCenterY", supernovaCenterY);
        tag.putFloat("supernovaScale", supernovaScale);
        /// 将巨构建造 NBT 委托给管理器
        megastructureManager.writeUpdateTag(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.handleUpdateTag(tag, lookupProvider);
        this.isAmplify = tag.getBoolean("amplified");
        this.cachedRedstoneSignal = Math.max(0, Math.min(tag.getInt("redstoneSignal"), 15));
        this.syncedRedstoneSignal = this.cachedRedstoneSignal;
        this.redstoneSignalCacheTick = -1;
        this.stellarMass = tag.getInt("stellarMass");
        this.locked = tag.getBoolean("locked");
        this.amplifierPresent = tag.getBoolean("amplifierPresent");
        this.searchController.loadSynced(
            tag.getBoolean("searching"),
            tag.getInt("searchTicks"),
            tag.getBoolean("searchFailed"),
            tag.getBoolean("powerInsufficient")
        );
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
        if (tag.contains("searchHistory")) {
            this.searchHistoryController.load(tag.getCompound("searchHistory"));
        } else {
            this.searchHistoryController.clear();
        }
        this.searchHistoryController.setBrowseIndex(tag.getInt("historyBrowseIndex"));
        if (tag.contains("anvils")) {
            CfaInventoryCodec.load(tag.getCompound("anvils"), this.anvilInventory, lookupProvider);
        } else {
            CfaInventoryCodec.load(new CompoundTag(), this.anvilInventory, lookupProvider);
        }
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
        /// 超新星闪光（客户端渲染）——仅在收到更大值时重启，避免覆盖客户端流畅递减
        int incomingFlash = tag.getInt("supernovaFlashTicks");
        if (incomingFlash > this.supernovaFlashTicks) {
            this.supernovaFlashTicks = incomingFlash;
        }
        this.supernovaCenterY = tag.getDouble("supernovaCenterY");
        this.supernovaScale = tag.contains("supernovaScale") ? tag.getFloat("supernovaScale") : 1.0f;
        /// 将巨构建造 NBT 委托给管理器
        megastructureManager.readUpdateTag(tag, lookupProvider);
    }

    public int getAnvilCount(int slot) {
        return this.anvilInventory.getItem(slot).getCount();
    }

    public void addToSearchHistory(CelestialBodyData data, @Nullable PlanetaryResourceSet resources) {
        this.searchHistoryController.add(data, resources);
    }

    public void clearSearchHistory() {
        this.searchHistoryController.clear();
    }

    /// === 搜索历史浏览（服务端）===

    public boolean hasPreviousHistory() {
        return this.searchHistoryController.hasPrevious();
    }

    public boolean hasNextHistory() {
        return this.searchHistoryController.hasNext();
    }

    public void browseHistoryPrev() {
        if (level == null || level.isClientSide()) return;
        if (this.locked || this.isSearching() || this.isAcceleratorActive()) return;
        SearchHistoryEntry entry = this.searchHistoryController.previous(celestialBodyData, planetaryResourceSet);
        if (entry != null) this.applyHistoryEntry(entry);
    }

    public void browseHistoryNext() {
        if (level == null || level.isClientSide()) return;
        if (this.locked || this.isSearching() || this.isAcceleratorActive()) return;
        SearchHistoryEntry entry = this.searchHistoryController.next();
        if (entry != null) this.applyHistoryEntry(entry);
    }

    private void applyHistoryEntry(SearchHistoryEntry entry) {
        this.celestialBodyData = entry.body();
        this.planetaryResourceSet = entry.resources();
        setChanged();
        syncToClient();
    }

    private void syncToClient() {
        CfaBlockEntitySync.sendToTracking(this, this.getUpdatePacket());
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
            clearAuxiliaryMegastructures();
        }
        this.setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private void clearAuxiliaryMegastructures() {
        megastructureManager.clearAuxiliaryMegastructures(this);
    }

    /// 清除活动巨构及所有相关状态，恢复为束星环。
    public void clearMegastructure() {
        megastructureManager.clearMegastructure(this);
        /// 清除材料过滤器（仍由 BE 持有）
        this.materialFilter = new ItemStack(Items.BARRIER);
        this.materialLimit = 0;
        /// 重新注册到电网以恢复 CONSUMER 类型
        PowerGrid.addComponent(this);
    }

    /// 获取与客户端看到的匹配的选项列表（应用相同的过滤）。当主巨构已建造时，仅辅助巨构可见。
    public List<CelestialRefactorOption> getClientVisibleOptions() {
        List<CelestialRefactorOption> options = CelestialRefactorRegistry.getOptions(
            celestialBodyData,
            isAmplify,
            this.planetaryResourceSet
        );
        if (megastructureManager.hasActiveMegastructure()) {
            options = options.stream().filter(CelestialRefactorOption::auxiliary).toList();
        }
        return options;
    }

    /// 获取当前活动的巨构选项，如果未建造则返回 null。
    @Nullable
    public CelestialRefactorOption getActiveMegastructureOption() {
        return megastructureManager.getActiveOption(this);
    }

    public boolean hasActiveAuxiliaryMegastructure() {
        return this.megastructureManager.hasActiveAuxiliary(this);
    }

    @Nullable
    public CelestialRefactorOption getActiveAuxiliaryMegastructureOption(int ring) {
        return this.megastructureManager.getActiveAuxiliaryOptionForRing(this, ring);
    }

    /// 获取放置在此 CFA 各侧的传送门（不可修改）。
    public Map<Cube323PartHalf, BlockPos> getPortals() {
        WormholeStabilizerHandler wh = megastructureManager.getWormholeHandler();
        return wh.getPortals();
    }

    /// 尝试建造巨构。玩家点击"开始重构"时在服务器端调用。optionIndex 为选中的重构选项索引。
    public void buildMegastructure(int optionIndex) {
        if (level == null || level.isClientSide()) return;
        if (!this.locked) return;
        if (this.isSearching() || this.isAcceleratorActive()) return;
        if (celestialBodyData == null) return;
        List<CelestialRefactorOption> options = getClientVisibleOptions();
        if (optionIndex < 0 || optionIndex >= options.size()) return;

        CelestialRefactorOption option = options.get(optionIndex);

        // Validate the server-side option and handler before consuming any material.
        if (!megastructureManager.canBuild(option, this)) return;

        /// 先检查材料
        if (option.needsMaterial()) {
            ItemStack contained = materialContainer.getItem(0);
            ItemStack required = option.material().copyWithCount(option.materialCount());
            if (!ItemStack.isSameItem(contained, required)
                || contained.getCount() < required.getCount()) {
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
