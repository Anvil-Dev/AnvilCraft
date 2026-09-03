package dev.dubhe.anvilcraft.client.event;

import com.mojang.blaze3d.platform.InputConstants;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.sound.SoundHelper;
import dev.dubhe.anvilcraft.api.thought.ThoughtManager;
import dev.dubhe.anvilcraft.api.tooltip.HudTooltipManager;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.client.gui.tooltip.FilterContentHoverWindow;
import dev.dubhe.anvilcraft.client.init.ModKeyMappings;
import dev.dubhe.anvilcraft.client.rpc.StorageTerminalClientStub;
import dev.dubhe.anvilcraft.client.rpc.TerminalReachabilityCache;
import dev.dubhe.anvilcraft.client.support.AmuletSelectorSupport;
import dev.dubhe.anvilcraft.client.support.StructureDiskPreviewSupport;
import dev.dubhe.anvilcraft.client.support.TerminalRemoteOverlay;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.HammerOpenedAnvilMenu;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import dev.dubhe.anvilcraft.item.TerminalItem;
import dev.dubhe.anvilcraft.network.DragonRodStopDevourPacket;
import dev.dubhe.anvilcraft.network.OpenHammerAnvilPacket;
import dev.dubhe.anvilcraft.network.UsePillBoxPacket;
import dev.dubhe.anvilcraft.util.BlockHighlightUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;
import javax.annotation.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class ClientEventListener {
    private static boolean wasAttackDown = false;

    /**
     * 按下时被创造背包 BundleLike 分支消费过的标记：松开时需取消 vanilla
     * mouseReleased 的第二次 slotClicked（指针有物品时 vanilla 会在松开时
     * 再执行一次 PICKUP，触发 fallback 交换/放回，与已完成的 RPC 结果冲突）。
     */
    private static boolean creativeBundlePressed = false;

    @SubscribeEvent
    public static void onScreenMouseReleasedTerminal(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!ClientEventListener.creativeBundlePressed) {
            return;
        }
        ClientEventListener.creativeBundlePressed = false;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void blockHighlight(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (BlockHighlightUtil.SUBCHUNKS.isEmpty()) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        BlockHighlightUtil.render(
            level,
            Minecraft.getInstance().renderBuffers().bufferSource(),
            event.getPoseStack(),
            event.getCamera()
        );
    }

    @SubscribeEvent
    public static void onRenderBlockOverlay(RenderBlockScreenEffectEvent event) {
        if (event.getOverlayType() == RenderBlockScreenEffectEvent.OverlayType.BLOCK
            && (event.getBlockState().is(ModBlocks.ACCELERATION_RING) || event.getBlockState().is(ModBlocks.DEFLECTION_RING))
        ) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientPlayerDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        SoundHelper.INSTANCE.clear();
        StorageTerminalClientStub.clear();
        TerminalReachabilityCache.clear();
        TerminalRemoteOverlay.setHovering(ItemStack.EMPTY);
        HudTooltipManager.INSTANCE.resetClientState();
    }

    @SubscribeEvent
    public static void onClientLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel) {
            HudTooltipManager.INSTANCE.resetClientState();
        }
    }

    @SubscribeEvent
    public static void onKeyPress(InputEvent.Key event) {
        if (ModKeyMappings.TOGGLE_GOGGLE.get().isDown()) AnvilHammerItem.goggleEnabled = !AnvilHammerItem.goggleEnabled;
        if (Minecraft.getInstance().level == null) return;

        // 以下是界面部分

        if (event.getKey() == ModKeyMappings.USE_PILL_BOX.get().getKey().getValue()) {
            if (event.getAction() == InputConstants.PRESS) {
                ClientPacketListener connection = Minecraft.getInstance().getConnection();
                if (connection != null) {
                    connection.send(new UsePillBoxPacket());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Post event) {
        if (event.getKeyCode() == ModKeyMappings.THOUGHT.get().getKey().getValue()) {
            ThoughtManager.onThought();
        }
    }

    @SubscribeEvent
    public static void onScreenKeyPressedTerminal(ScreenEvent.KeyPressed.Pre event) {
        if (event.getScreen() instanceof StorageScreen) return;
        if (TerminalRemoteOverlay.isHovering()
            && TerminalRemoteOverlay.keyPressed(
                event.getKeyCode(),
                event.getScanCode(),
                event.getModifiers()
            )) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScreenCharTypedTerminal(ScreenEvent.CharacterTyped.Pre event) {
        if (event.getScreen() instanceof StorageScreen) return;
        if (TerminalRemoteOverlay.isHovering()
            && TerminalRemoteOverlay.charTyped(event.getCodePoint(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScreenKeyReleased(ScreenEvent.KeyReleased.Post event) {
        if (event.getKeyCode() == ModKeyMappings.THOUGHT.get().getKey().getValue()) {
            ThoughtManager.onEndThought();
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        handleAttackKeyRelease();

        long lastThoughtTime = ThoughtManager.getLastThoughtTime();
        if (lastThoughtTime < 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        long curTime = minecraft.gui.getGuiTicks();
        long deltaTime = curTime - lastThoughtTime;
        if (deltaTime > ThoughtManager.getMAX_SECONDS() * 20) {
            ThoughtManager.onPostThought();
        }
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getAction() == InputConstants.RELEASE && minecraft.options.keyAttack.matchesMouse(event.getButton())) {
            sendDragonRodStopDevourPacket(minecraft);
            wasAttackDown = false;
        }
    }

    private static void handleAttackKeyRelease() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean attackDown = minecraft.options.keyAttack.isDown();
        if (wasAttackDown && !attackDown) {
            sendDragonRodStopDevourPacket(minecraft);
        }
        wasAttackDown = attackDown;
    }

    private static void sendDragonRodStopDevourPacket(Minecraft minecraft) {
        if (minecraft.player != null && minecraft.getConnection() != null) {
            PacketDistributor.sendToServer(new DragonRodStopDevourPacket());
        }
    }

    @SubscribeEvent
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (!(event.getScreen() instanceof StorageScreen) && TerminalRemoteOverlay.isHovering()) {
            int amount = (int) event.getScrollDeltaY();
            if (TerminalRemoteOverlay.mouseScrolled(amount)) {
                event.setCanceled(true);
                return;
            }
        }
        if (AmuletSelectorSupport.hasHoveringItem()) {
            int amount = (int) event.getScrollDeltaY();
            AmuletSelectorSupport.mouseScrolled(-amount);
            event.setCanceled(true);
        } else if (AnvilCraftClient.pillSelectorSupport.hasItem()) {
            int amount = (int) event.getScrollDeltaY();
            AnvilCraftClient.pillSelectorSupport.mouseScrolled(-amount);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScreenMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.options.keyUse.matchesMouse(event.getButton())) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) return;
        if (minecraft.player == null || minecraft.getConnection() == null) return;
        if (!containerScreen.getMenu().getCarried().isEmpty()) return;
        Slot slot = containerScreen.getSlotUnderMouse();
        if (slot == null) return;
        if (containerScreen.getMenu() instanceof HammerOpenedAnvilMenu menu
            && slot.container == minecraft.player.getInventory()
            && menu.anvilcraft$getOpenedHammerSlot() == slot.getContainerSlot()) {
            return;
        }
        ItemStack stack = slot.getItem();
        if (!(stack.getItem() instanceof AnvilHammerItem)) return;
        int menuSlotId = containerScreen.getMenu().slots.indexOf(slot);
        if (menuSlotId < 0) return;
        PacketDistributor.sendToServer(new OpenHammerAnvilPacket(menuSlotId));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onScreenMousePressedTerminal(ScreenEvent.MouseButtonPressed.Pre event) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) return;
        if (event.getScreen() instanceof StorageScreen) return;
        if (minecraft.player == null || minecraft.getConnection() == null) return;
        // 同时用渲染帧的 hoveredSlot 与事件坐标定位，避免任一路径漏判
        // （hoveredSlot 滞后或坐标换算偏差都会导致 overTerminal 误判为 false，
        // 点击落到 vanilla 槽位逻辑上发生交换/捏起终端）
        Slot slot = containerScreen.getSlotUnderMouse();
        if (slot == null || !TerminalRemoteOverlay.isBoundTerminal(slot.getItem())) {
            slot = findSlotAt(containerScreen, event.getMouseX(), event.getMouseY());
        }
        boolean overTerminal = slot != null && TerminalRemoteOverlay.isBoundTerminal(slot.getItem());
        if (overTerminal) {
            boolean carriedEmpty = containerScreen.getMenu().getCarried().isEmpty();
            if (carriedEmpty && !TerminalRemoteOverlay.isDismissed()) {
                // 空手且未按 Esc 屏蔽：已通过滚轮选择过物品时取出（左键一组、右键一个），
                // 点击搜索框时聚焦；未选择则返回 false——不拦截点击，允许 vanilla 将终端
                // 拿起（正常槽位交互）。浮窗未激活时也放行。
                boolean consumed = TerminalRemoteOverlay.mouseClicked(
                    (int) event.getMouseX(),
                    (int) event.getMouseY(),
                    event.getButton()
                );
                if (consumed) {
                    event.setCanceled(true);
                }
                return;
            } else if (!carriedEmpty) {
                // 捏着物品：根据终端的 inverted 状态决定放入存储的按键（默认右键，反向后左键）
                boolean inverted = InvertedActionEventListener.isInverted();
                boolean insertClick = inverted
                    ? minecraft.options.keyAttack.matchesMouse(event.getButton())
                    : minecraft.options.keyUse.matchesMouse(event.getButton());
                if (insertClick) {
                    // 捏着物品点击：把整组放入对应存储站
                    UUID targetId = TerminalRemoteOverlay.terminalIdOf(slot.getItem());
                    if (targetId != null) {
                        // 服务端 terminalInsert 会修改 carried 并经 broadcastChanges 广播同步到
                        // 客户端当前活动菜单，客户端不应手动 setCarried，否则会与服务端广播竞态
                        // （这也是取出/关界面物品重复的根源）。失败时事件已取消、客户端 carried 未变。
                        StorageTerminalClientStub.insert(
                            targetId,
                            containerScreen.getMenu().getCarried()
                        ).whenComplete((result, error) -> Minecraft.getInstance().execute(() -> {
                            if (error != null || !result.changed()) {
                                return;
                            }
                            // 创造背包界面的 ItemPickerMenu 是纯客户端菜单，服务端 carried 广播
                            // （containerId == -1）会被客户端忽略，需手动写回指针
                            TerminalRemoteOverlay.applyCarriedIfCreative(result.carried());
                        }));
                    }
                }
                // 非放入按键：阻止交换（不把终端捏起，也不放入），保持终端不动
                event.setCanceled(true);
                return;
            }
            // 空手但已按 Esc 屏蔽浮窗：阻止 vanilla 拿起终端，保持终端不动
            event.setCanceled(true);
            return;
        }

        // 创造背包的 BUNDLE_HOVER_ITEM（捏着终端点击背包/快捷栏槽）：
        // vanilla 的 slotClicked 只做本地预测（不发网络包），服务端无法执行
        // TerminalItem 的存储操作。仅拦截 BundleLike 按键（空槽/有物品均为右键取出/放入），
        // 取消事件后走 RPC 完成操作；左键放回/交换放行给 vanilla，保持 BundleItem 语义。
        if (!containerScreen.getMenu().getCarried().isEmpty()
            && event.getScreen() instanceof CreativeModeInventoryScreen creative
            && containerScreen.getMenu().getCarried().getItem() instanceof TerminalItem
            && slot != null
            && slot.container == minecraft.player.getInventory()
        ) {
            ItemStack carried = containerScreen.getMenu().getCarried();
            UUID targetId = TerminalRemoteOverlay.terminalIdOf(carried);
            boolean inverted = InvertedActionEventListener.isInverted();
            // 仅真正匹配 BundleLike 右键 + 槽内容组合才拦截：
            // 空槽=取出、有物品=放入；其余（左键放回/交换）放行 vanilla fallback，
            // 保证终端能放下/交换。
            boolean removeClick = ClientEventListener.isBundleClick(event.getButton(), inverted, true)
                && slot.getItem().isEmpty();
            boolean insertClick = ClientEventListener.isBundleClick(event.getButton(), inverted, false)
                && !slot.getItem().isEmpty();
            if (targetId != null && (removeClick || insertClick)) {
                ClientEventListener.handleCreativeBundleHover(
                    creative,
                    containerScreen,
                    slot,
                    event.getButton(),
                    carried
                );
                // 消费本次按下：松开时取消 vanilla 的第二次 slotClicked
                ClientEventListener.creativeBundlePressed = true;
                event.setCanceled(true);
            }
            return;
        }

        // 浮窗悬停中但鼠标不在终端槽位上（如面板/搜索框区域）：浮窗接管点击，
        // 交由浮窗处理（聚焦搜索框等），不落到下层 GUI。
        // 仅空手时接管：捏着物品（如把终端拖到空槽上取出/放入）时点击是明确的
        // 拖放意图，应放行给 vanilla（BUNDLE_HOVER_ITEM 路径）。
        if (containerScreen.getMenu().getCarried().isEmpty()
            && TerminalRemoteOverlay.isHovering()
            && !TerminalRemoteOverlay.isDismissed()) {
            TerminalRemoteOverlay.mouseClicked(
                (int) event.getMouseX(),
                (int) event.getMouseY(),
                event.getButton()
            );
            event.setCanceled(true);
        }
    }

    /**
     * Prevents Mouse Tweaks from intercepting shift+left-drag quick-move in StorageScreen.
     * Mouse Tweaks listens on {@code ScreenEvent.MouseDragged.Pre} at default priority and issues
     * a vanilla QUICK_MOVE for every hovered inventory slot. Handling the event at HIGHEST priority
     * and cancelling it lets StorageScreen's own quick-move-to-storage logic take over.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScreenMouseDraggedStorage(ScreenEvent.MouseDragged.Pre event) {
        if (!(event.getScreen() instanceof StorageScreen storageScreen)) {
            return;
        }
        if (!storageScreen.isQuickMoveDragging() || event.getMouseButton() != 0 || !Screen.hasShiftDown()) {
            return;
        }
        storageScreen.quickMoveDrag(event.getMouseX(), event.getMouseY());
        event.setCanceled(true);
    }

    private static void handleCreativeBundleHover(
        CreativeModeInventoryScreen creative,
        AbstractContainerScreen<?> screen,
        Slot slot,
        int button,
        ItemStack carried
    ) {
        UUID targetId = TerminalRemoteOverlay.terminalIdOf(carried);
        if (targetId == null) {
            return;
        }
        boolean inverted = InvertedActionEventListener.isInverted();
        ItemStack slotItem = slot.getItem();
        if (slotItem.isEmpty()) {
            // 取出：空槽 + 右键（非 inverted）
            if (!ClientEventListener.isBundleClick(button, inverted, true)) {
                return;
            }
            StorageTerminalClientStub.extractFirst(targetId, 64, carried).whenComplete((result, error) ->
                Minecraft.getInstance().execute(() -> {
                    if (error != null || result == null) {
                        return;
                    }
                    if (!result.changed()) {
                        // 取不出（无绑定/不可达/存储空）：放回终端（vanilla fallback 语义）
                        slot.set(carried);
                        screen.getMenu().setCarried(ItemStack.EMPTY);
                        screen.getMenu().broadcastChanges();
                        return;
                    }
                    slot.set(result.carried());
                    screen.getMenu().broadcastChanges();
                    ClientEventListener.playTerminalSound(carried, true);
                })
            );
        } else {
            // 放入：有物品槽 + 右键（非 inverted）
            if (!ClientEventListener.isBundleClick(button, inverted, false)) {
                return;
            }
            StorageTerminalClientStub.insertFirst(targetId, slotItem, carried).whenComplete((remain, error) ->
                Minecraft.getInstance().execute(() -> {
                    if (error != null || remain == null) {
                        return;
                    }
                    slot.set(remain);
                    screen.getMenu().broadcastChanges();
                    ClientEventListener.playTerminalSound(carried, false);
                })
            );
        }
    }

    /**
     * 按终端类型播放与生存模式一致的音效：超维→传送、潜影→潜影盒开/关、其他→收纳袋。
     */
    private static void playTerminalSound(ItemStack terminal, boolean remove) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        SoundEvent sound;
        if (terminal.is(ModItems.HYPERDIMENSION_TERMINAL)) {
            sound = SoundEvents.ENDERMAN_TELEPORT;
        } else if (terminal.is(ModItems.SHULKER_TERMINAL)) {
            sound = remove ? SoundEvents.SHULKER_BOX_OPEN : SoundEvents.SHULKER_BOX_CLOSE;
        } else {
            sound = remove ? SoundEvents.BUNDLE_REMOVE_ONE : SoundEvents.BUNDLE_INSERT;
        }
        player.playSound(sound, 0.8F, 0.8F + player.getRandom().nextFloat() * 0.4F);
    }

    /** BundleLike 按键判定：取出与放入均为右键（inverted 时均为左键）。 */
    private static boolean isBundleClick(int button, boolean inverted, boolean remove) {
        boolean wantRemoveClick = inverted ? button == 0 : button == 1;
        boolean wantInsertClick = inverted ? button == 0 : button == 1;
        return remove ? wantRemoveClick : wantInsertClick;
    }

    /**
     * 根据 GUI 坐标在容器菜单槽位中查找鼠标悬停的槽位，复刻
     * AbstractContainerScreen.isHovering 的判定，不依赖渲染帧的 hoveredSlot。
     */
    private static @Nullable Slot findSlotAt(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        double x = mouseX - screen.getGuiLeft();
        double y = mouseY - screen.getGuiTop();
        for (Slot slot : screen.getMenu().slots) {
            if (
                slot.isActive()
                && x >= (double) (slot.x - 1)
                && x < (double) (slot.x + 17)
                && y >= (double) (slot.y - 1)
                && y < (double) (slot.y + 17)
            ) {
                return slot;
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void renderContainerScreenEvent(ContainerScreenEvent.Render.Background event) {
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        Slot slot = screen.getSlotUnderMouse();
        ItemStack item = slot != null ? slot.getItem() : ItemStack.EMPTY;
        if (item.is(ModItems.PILL_BOX)) {
            AnvilCraftClient.pillSelectorSupport.setPillBox(item);
        } else {
            AnvilCraftClient.pillSelectorSupport.setPillBox(ItemStack.EMPTY);
        }

        // 创造模式：仅标签栏（非槽位区域）不启用终端收纳袋浮窗，背包槽位内的终端正常触发
        if (screen instanceof CreativeModeInventoryScreen && slot == null) {
            TerminalRemoteOverlay.setHovering(ItemStack.EMPTY);
            return;
        }

        // 该事件仅对调用 super.render 的 AbstractContainerScreen 触发；StorageScreen
        // 全量自绘（不调用 super.render），不会触发本事件，无需额外排除。
        // 浮窗的显示/隐藏仅取决于鼠标是否仍在绑定终端槽位上：取出后指针非空时若立即
        // 关闭浮窗会清空 storageId 与已加载内容，导致后续点击无法取出/放入，
        // 因此指针是否为空不参与判定（取出/放入由点击处理按 carried 区分）。
        boolean overTerminal = TerminalRemoteOverlay.isBoundTerminal(item);
        if (overTerminal && !TerminalRemoteOverlay.isDismissed()) {
            TerminalRemoteOverlay.setHovering(item);
            // 浮窗锚点默认跟随槽位 tooltip；但指针上有物品时槽位 tooltip 会被抑制
            // （RenderTooltipEvent.Pre 不触发），此时用鼠标位置作为锚点，
            // 避免浮窗停留在左上角初始位置。
            TerminalRemoteOverlay.updateForTooltip(event.getMouseX(), event.getMouseY());
        } else if (!overTerminal) {
            // 鼠标离开终端：清除 Esc 屏蔽并隐藏浮窗
            TerminalRemoteOverlay.setDismissed(false);
            TerminalRemoteOverlay.setHovering(ItemStack.EMPTY);
        }
    }

    @SubscribeEvent
    public static void onRenderTooltip(RenderTooltipEvent.Pre event) {
        // 鼠标在终端上时，浮窗跟随 tooltip 渲染在其左上方；原 tooltip 保留
        if (TerminalRemoteOverlay.isHovering()) {
            TerminalRemoteOverlay.updateForTooltip(event.getX(), event.getY());
        }
        GuiGraphics guiGraphics = event.getGraphics();
        int x = event.getX();
        int y = event.getY();

        ItemStack itemStack = event.getItemStack();
        if (itemStack.is(ModItems.AMULET_BOX)) {
            event.setY(y + 13);
            AmuletSelectorSupport.setCurrentHoveringItemStack(itemStack);
            AmuletSelectorSupport.render(guiGraphics, x, y);
        } else if (itemStack.is(ModItems.PILL_BOX)) {
            event.setY(y + 13);
            AnvilCraftClient.pillSelectorSupport.render(guiGraphics, x, y);
        } else if (itemStack.is(ModItems.FILTER)) {
            // 过滤内容以"外挂窗口"（裁剪过滤器 GUI 贴图）显示在 tooltip 上方
            event.setY(y + 13);
            FilterContentHoverWindow.render(guiGraphics, itemStack, x, y, event.getFont());
        } else {
            AmuletSelectorSupport.setCurrentHoveringItemStack(ItemStack.EMPTY);
        }
    }
    
    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        TerminalRemoteOverlay.setHovering(ItemStack.EMPTY);
    }

    @SubscribeEvent
    public static void onContainerScreenRenderPost(ScreenEvent.Render.Post event) {
        // 在容器屏幕渲染完成后，渲染结构磁盘的3D预览窗口
        // 这样可以确保预览窗口在所有物品和UI之上
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) {
            return;
        }

        TerminalRemoteOverlay.tick();
        if (!(event.getScreen() instanceof StorageScreen)) {
            TerminalRemoteOverlay.render(
                event.getGuiGraphics(),
                Minecraft.getInstance().font,
                event.getPartialTick()
            );
        }

        // 获取鼠标悬停的slot
        Slot slot = containerScreen.getSlotUnderMouse();
        if (slot == null || !slot.hasItem()) {
            return;
        }
        
        ItemStack itemStack = slot.getItem();
        if (!itemStack.is(ModItems.STRUCTURE_DISK.get())) {
            return;
        }
        
        // 获取真实鼠标位置
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        int mouseX = (int) (
            minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth());
        int mouseY = (int) (
            minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight());
        
        // 渲染预览窗口
        StructureDiskPreviewSupport.renderPreviewAt(
            event.getGuiGraphics(),
            itemStack,
            mouseX,
            mouseY
        );
    }
}
