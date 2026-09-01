package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.platform.InputConstants;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.client.rpc.StorageClientStub;
import dev.dubhe.anvilcraft.client.rpc.StorageTerminalClientStub;
import dev.dubhe.anvilcraft.client.rpc.TerminalReachabilityCache;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.TerminalBinding;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * 超维终端的远端浮窗：空手悬停在绑定的终端物品上时，在当前 GUI 内显示一个可交互面板，
 * 展示绑定存储站的内容物（与存储站一致的排序），支持滚轮翻页、左右键取出、搜索过滤与
 * Alt 切换 remote_4/5/6 三种尺寸。
 *
 * <p>与 {@link AmuletSelectorSupport} 类似，为静态状态；渲染在
 * {@code ScreenEvent.Render.Post}，交互在对应的 Screen 事件中分发。面板渲染在
 * 原 tooltip 的左上方（紧挨但不重叠），跟随 tooltip 移动；鼠标移入面板后固定，
 * 离开终端与面板后隐藏。</p>
 */
public final class TerminalRemoteOverlay {
    private static final int SLOT_SIZE = 18;
    private static final int GRID_X = 4;
    private static final int GRID_Y = 18;
    private static final int SEARCH_Y = 6;
    private static final int REFRESH_INTERVAL = 20;
    /** 内容尚未加载完成时的快速重试间隔（tick）。 */
    private static final int FAST_RELOAD_INTERVAL = 5;
    /** 浮窗与 tooltip 之间的间隙（px），紧挨但不重叠。 */
    private static final int TOOLTIP_GAP = 2;
    /** 浮窗相对 tooltip 左上方再向右上偏移的像素（右 20、上 14）。 */
    private static final int PANEL_OFFSET_X = 20;
    private static final int PANEL_OFFSET_Y = 14;

    private static @Nullable UUID storageId;
    private static int sizeMode;
    private static @Nullable EditBox searchBox;
    private static String search = "";
    private static IntList order = new IntArrayList();
    private static final Int2ObjectMap<UnlimitedItemStack> CONTENTS = new Int2ObjectOpenHashMap<>();
    private static final Int2LongMap COUNTS = new Int2LongOpenHashMap();
    private static int cursor;
    private static int left;
    private static int top;
    private static int frameCounter;
    private static int fastRetries;
    private static boolean taking;
    /** 是否已通过滚轮选择过物品：未选择时点击终端允许 vanilla 拿起终端，不拦截。 */
    private static boolean selected;
    /** 是否已按 Tab 接管键盘输入：未接管时键盘事件全部放行给下层 GUI（热键栏、E 等照常生效）。 */
    private static boolean keyboardActive;
    /** 最近一次 tooltip 左上角位置，用于 Alt 切换尺寸时保持右下角原点不变。 */
    private static int lastTooltipX;
    private static int lastTooltipY;
    /** 按下 Esc 后屏蔽再次弹出，直到鼠标离开终端。 */
    private static boolean dismissed;

    private TerminalRemoteOverlay() {
    }

    public static void setHovering(ItemStack stack) {
        UUID newId = TerminalRemoteOverlay.storageIdOf(stack);
        if (newId != null && !stack.is(ModItems.HYPERDIMENSION_TERMINAL)) {
            // 本地 / 潜影终端：仅当当前可连接目标时激活浮窗（超出范围 / 无目标则不可用）
            TerminalReachabilityCache.ensure(newId);
            if (!TerminalReachabilityCache.isReachable(newId)) {
                newId = null;
            }
        }
        if (Objects.equals(newId, TerminalRemoteOverlay.storageId)) {
            return;
        }
        TerminalRemoteOverlay.reset();
        if (newId == null) {
            return;
        }
        TerminalRemoteOverlay.storageId = newId;
        TerminalRemoteOverlay.dismissed = false;
        TerminalRemoteOverlay.fastRetries = 0;
        TerminalRemoteOverlay.startLoad();
    }

    public static boolean isHovering() {
        return TerminalRemoteOverlay.storageId != null;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isDismissed() {
        return TerminalRemoteOverlay.dismissed;
    }

    public static void setDismissed(boolean dismissed) {
        TerminalRemoteOverlay.dismissed = dismissed;
    }

    public static boolean isBoundTerminal(ItemStack stack) {
        return TerminalRemoteOverlay.storageIdOf(stack) != null;
    }

    /** 返回任意终端（超维 / 本地 / 潜影）当前会话的存储标识；非终端物品返回 null。 */
    public static @Nullable UUID terminalIdOf(ItemStack stack) {
        return TerminalRemoteOverlay.storageIdOf(stack);
    }

    public static void render(GuiGraphics graphics, Font font, float partialTick) {
        if (!TerminalRemoteOverlay.isHovering()) {
            return;
        }
        int w = TerminalRemoteOverlay.panelW();
        int h = TerminalRemoteOverlay.panelH();
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 400.0F);
        graphics.blit(
            TerminalRemoteOverlay.background(),
            TerminalRemoteOverlay.left,
            TerminalRemoteOverlay.top,
            0,
            0,
            w,
            h,
            w,
            h
        );
        TerminalRemoteOverlay.renderSearchBox(graphics, font, partialTick);
        int gridSize = TerminalRemoteOverlay.gridSize();
        int pageStart = TerminalRemoteOverlay.pageStart();
        for (int i = 0; i < TerminalRemoteOverlay.pageSize(); i++) {
            int orderIndex = pageStart + i;
            if (orderIndex >= TerminalRemoteOverlay.order.size()) {
                break;
            }
            int slot = TerminalRemoteOverlay.order.getInt(orderIndex);
            UnlimitedItemStack stack = TerminalRemoteOverlay.CONTENTS.getOrDefault(slot, UnlimitedItemStack.EMPTY);
            int x = TerminalRemoteOverlay.left + TerminalRemoteOverlay.GRID_X + i % gridSize * TerminalRemoteOverlay.SLOT_SIZE;
            int y = TerminalRemoteOverlay.top + TerminalRemoteOverlay.GRID_Y + i / gridSize * TerminalRemoteOverlay.SLOT_SIZE;
            if (!stack.isEmpty()) {
                ItemStack itemStack = stack.toStack();
                graphics.renderFakeItem(itemStack, x + 1, y + 1);
                StorageScreen.renderItemDecorations(
                    graphics,
                    font,
                    itemStack,
                    TerminalRemoteOverlay.COUNTS.get(slot),
                    x + 1,
                    y + 1
                );
            }
            if (TerminalRemoteOverlay.selected && orderIndex == TerminalRemoteOverlay.cursor) {
                graphics.blit(SharedTextures.BOX_SELECTION, x, y, 0, 0, 18, 18, 18, 18);
            }
        }
        graphics.pose().popPose();
    }

    private static void renderSearchBox(GuiGraphics graphics, Font font, float partialTick) {
        TerminalRemoteOverlay.ensureSearchBox();
        if (TerminalRemoteOverlay.searchBox == null) {
            return;
        }
        TerminalRemoteOverlay.searchBox.setX(TerminalRemoteOverlay.left + TerminalRemoteOverlay.GRID_X + 2);
        TerminalRemoteOverlay.searchBox.setY(TerminalRemoteOverlay.top + TerminalRemoteOverlay.SEARCH_Y);
        TerminalRemoteOverlay.searchBox.setWidth(TerminalRemoteOverlay.maxSearchWidth());
        // 浮窗内坐标已整体平移到 z=400，EditBox 渲染在自身层级即可；
        // 用真实鼠标坐标渲染以保证 hover 状态与光标显示正确
        Minecraft minecraft = Minecraft.getInstance();
        int mouseX = (int) (minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth()
                            / minecraft.getWindow().getScreenWidth());
        int mouseY = (int) (minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight()
                            / minecraft.getWindow().getScreenHeight());
        TerminalRemoteOverlay.searchBox.render(graphics, mouseX, mouseY, partialTick);
        // 未激活（未聚焦）且内容为空时显示灰色 "Tab" 提示，提示可按 Tab 键激活搜索
        if (!TerminalRemoteOverlay.searchBox.isFocused() && TerminalRemoteOverlay.searchBox.getValue().isEmpty()) {
            Component hint = Component.translatable("screen.anvilcraft.storage.search.tab").withStyle(ChatFormatting.GRAY);
            graphics.drawString(font, hint, TerminalRemoteOverlay.searchBox.getX(), TerminalRemoteOverlay.searchBox.getY(), 0xFFFFFFFF);
        }
    }

    public static boolean mouseClicked(int mouseX, int mouseY, int button) {
        // 点击搜索框区域时交给 EditBox 处理（聚焦/定位光标），不取出物品；
        // 同时激活键盘接管（点击搜索框的意图就是输入，与按 Tab 效果一致）。
        // 返回 true 表示点击已被浮窗消费，调用方应取消事件，避免落到下层 GUI。
        if (TerminalRemoteOverlay.searchBox != null
            && TerminalRemoteOverlay.searchBox.isMouseOver(mouseX, mouseY)) {
            TerminalRemoteOverlay.searchBox.mouseClicked(mouseX, mouseY, button);
            TerminalRemoteOverlay.searchBox.setFocused(true);
            TerminalRemoteOverlay.keyboardActive = true;
            return true;
        }
        if (button != 0 && button != 1) {
            return false;
        }
        if (!TerminalRemoteOverlay.isHovering() || TerminalRemoteOverlay.taking) {
            return false;
        }
        // 未通过滚轮选择过物品时，不拦截点击——允许 vanilla 将终端拿起
        if (!TerminalRemoteOverlay.selected) {
            return false;
        }
        // 鼠标通常位于终端槽位上（面板跟随 tooltip 显示在其左上方），点击时取出
        // 滚轮选中的物品；若恰好点在面板格子上则取该格物品。左键一组，右键一个。
        int slot = TerminalRemoteOverlay.determineSlot(mouseX, mouseY);
        if (slot < 0) {
            // 内容尚未加载：触发一次加载并吞掉点击，避免把终端捏起
            TerminalRemoteOverlay.refresh();
            return false;
        }
        TerminalRemoteOverlay.taking = true;
        TerminalRemoteOverlay.take(slot, button);
        return true;
    }

    public static boolean mouseScrolled(int amount) {
        if (!TerminalRemoteOverlay.isHovering()) {
            return false;
        }
        // 悬停即聚焦：滚动始终被面板接管（内容为空时无事可做，仍吞掉）
        if (TerminalRemoteOverlay.order.isEmpty()) {
            return true;
        }
        // 第一次滚动先选中第一格（启用左右键取出）；其后的滚动才进行其它选择操作。
        if (!TerminalRemoteOverlay.selected) {
            TerminalRemoteOverlay.selected = true;
            TerminalRemoteOverlay.cursor = 0;
            TerminalRemoteOverlay.syncVisible();
            return true;
        }
        // 普通滚动选择相邻物品；按住 Shift 按行滚动；按住 Ctrl 翻页。
        // 所有滚动形式在首/末格边界都无条件循环回绕到对面。
        int step = 1;
        if (Screen.hasControlDown()) {
            step = TerminalRemoteOverlay.pageSize();
        } else if (Screen.hasShiftDown()) {
            step = TerminalRemoteOverlay.gridSize();
        }
        int size = TerminalRemoteOverlay.order.size();
        int next = Math.floorMod(amount > 0 ? TerminalRemoteOverlay.cursor - step : TerminalRemoteOverlay.cursor + step, size);
        if (next != TerminalRemoteOverlay.cursor) {
            TerminalRemoteOverlay.cursor = next;
            TerminalRemoteOverlay.syncVisible();
        }
        return true;
    }

    private static int determineSlot(int mouseX, int mouseY) {
        Integer gridIndex = TerminalRemoteOverlay.getSlotAt(mouseX, mouseY);
        if (gridIndex != null && gridIndex >= 0 && gridIndex < TerminalRemoteOverlay.order.size()) {
            return TerminalRemoteOverlay.order.getInt(gridIndex);
        }
        if (TerminalRemoteOverlay.cursor >= 0 && TerminalRemoteOverlay.cursor < TerminalRemoteOverlay.order.size()) {
            return TerminalRemoteOverlay.order.getInt(TerminalRemoteOverlay.cursor);
        }
        return -1;
    }

    public static boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!TerminalRemoteOverlay.isHovering()) {
            return false;
        }
        // Esc / Alt 始终由浮窗处理（无论是否按 Tab 接管键盘）：关闭浮窗 / 切换尺寸
        if (keyCode == InputConstants.KEY_ESCAPE) {
            TerminalRemoteOverlay.dismissed = true;
            TerminalRemoteOverlay.setHovering(ItemStack.EMPTY);
            return true;
        }
        if (keyCode == InputConstants.KEY_LALT || keyCode == InputConstants.KEY_RALT) {
            TerminalRemoteOverlay.sizeMode = (TerminalRemoteOverlay.sizeMode + 1) % 3;
            // 保持右下角原点不变，按最近一次 tooltip 位置重新锚定
            TerminalRemoteOverlay.reanchor();
            return true;
        }
        // 未按 Tab 接管键盘前不拦截其它按键：Tab 键本身除外（首次按 Tab 激活接管并聚焦搜索框）
        if (!TerminalRemoteOverlay.keyboardActive) {
            if (keyCode == InputConstants.KEY_TAB) {
                TerminalRemoteOverlay.keyboardActive = true;
                TerminalRemoteOverlay.ensureSearchBox();
                if (TerminalRemoteOverlay.searchBox != null) {
                    TerminalRemoteOverlay.searchBox.setFocused(true);
                }
                return true;
            }
            return false;
        }
        // 已接管键盘：Tab 取消接管，其余按键交给搜索框（聚焦时）
        if (keyCode == InputConstants.KEY_TAB) {
            // 再次按 Tab：取消接管，键盘放行给下层 GUI
            TerminalRemoteOverlay.keyboardActive = false;
            if (TerminalRemoteOverlay.searchBox != null) {
                TerminalRemoteOverlay.searchBox.setFocused(false);
            }
            return true;
        }
        TerminalRemoteOverlay.ensureSearchBox();
        if (TerminalRemoteOverlay.searchBox != null) {
            // 仅聚焦时把键入交给搜索框；未聚焦的其它按键一律吞掉，避免触发下层 GUI
            if (TerminalRemoteOverlay.searchBox.isFocused()) {
                TerminalRemoteOverlay.searchBox.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return true;
    }

    public static boolean charTyped(char codePoint, int modifiers) {
        if (!TerminalRemoteOverlay.isHovering() || !TerminalRemoteOverlay.keyboardActive) {
            return false;
        }
        // 已接管：拦截所有字符输入；仅聚焦状态才交给搜索框
        TerminalRemoteOverlay.ensureSearchBox();
        if (TerminalRemoteOverlay.searchBox != null && TerminalRemoteOverlay.searchBox.isFocused()) {
            TerminalRemoteOverlay.searchBox.charTyped(codePoint, modifiers);
        }
        return true;
    }

    /** 是否已通过滚轮选择过物品；未选择时点击终端允许 vanilla 拿起终端。 */
    public static boolean hasSelection() {
        return TerminalRemoteOverlay.selected;
    }

    /** 确保搜索框已创建（渲染/按键/字符输入前调用），位置与宽度随后由渲染更新。 */
    private static void ensureSearchBox() {
        if (TerminalRemoteOverlay.searchBox != null) {
            return;
        }
        TerminalRemoteOverlay.searchBox = new EditBox(
            Minecraft.getInstance().font,
            TerminalRemoteOverlay.left + TerminalRemoteOverlay.GRID_X + 1,
            TerminalRemoteOverlay.top + TerminalRemoteOverlay.SEARCH_Y,
            TerminalRemoteOverlay.maxSearchWidth(),
            9,
            // 占位提示由 renderSearchBox 在未激活时手动绘制（灰色 "Tab"），这里保持为空
            Component.empty()
        );
        TerminalRemoteOverlay.searchBox.setBordered(false);
        TerminalRemoteOverlay.searchBox.setTextColor(0xFFFFFFFF);
        TerminalRemoteOverlay.searchBox.setMaxLength(50);
        TerminalRemoteOverlay.searchBox.setValue(TerminalRemoteOverlay.search);
        TerminalRemoteOverlay.searchBox.setResponder(text -> {
            if (!text.equals(TerminalRemoteOverlay.search)) {
                TerminalRemoteOverlay.search = text;
                TerminalRemoteOverlay.reorder();
            }
        });
        // 默认未激活：按 Tab 或点击搜索框才会聚焦；可失去焦点以便 Tab 再次取消激活
        TerminalRemoteOverlay.searchBox.setCanLoseFocus(true);
    }

    public static void tick() {
        if (!TerminalRemoteOverlay.isHovering()) {
            return;
        }
        // 本地 / 潜影终端：目标脱离连接范围（走远 / 目标消失）后停用浮窗
        UUID id = TerminalRemoteOverlay.storageId;
        Player player = Minecraft.getInstance().player;
        if (id != null
            && player != null
            && (id.equals(StorageTerminalClientStub.localTerminalId())
                || id.equals(StorageTerminalClientStub.shulkerTerminalId()))) {
            TerminalReachabilityCache.ensure(id);
            if (!TerminalReachabilityCache.isReachable(id)) {
                TerminalRemoteOverlay.reset();
                return;
            }
        }
        // 内容尚未加载完成时更频繁地重试（有次数上限），之后按固定周期刷新，
        // 保证即使某次加载/同步响应被丢弃，也会在下个周期补上，数据最终一致。
        boolean fast = TerminalRemoteOverlay.order.isEmpty()
                       && TerminalRemoteOverlay.fastRetries < 12;
        int interval = fast ? TerminalRemoteOverlay.FAST_RELOAD_INTERVAL
                            : TerminalRemoteOverlay.REFRESH_INTERVAL;
        if (++TerminalRemoteOverlay.frameCounter % interval == 0) {
            if (fast) {
                TerminalRemoteOverlay.fastRetries++;
            }
            TerminalRemoteOverlay.refresh();
        }
    }

    private static void startLoad() {
        // 直接刷新：reorder/sync 内部都通过 ensureVirtualPos 获取并缓存虚拟位置，
        // 不再依赖单独的 virtualPos 字段，避免响应被悬停抖动丢弃后永远无法加载
        TerminalRemoteOverlay.refresh();
    }

    private static void refresh() {
        UUID id = TerminalRemoteOverlay.storageId;
        if (id == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        StorageTerminalClientStub.reorder(id, TerminalRemoteOverlay.search).whenComplete((newOrder, error) -> minecraft.execute(() -> {
            if (error != null || !TerminalRemoteOverlay.isHoveringSame(id)) {
                return;
            }
            IntList next = new IntArrayList(newOrder);
            boolean orderChanged = !next.equals(TerminalRemoteOverlay.order);
            TerminalRemoteOverlay.order = next;
            TerminalRemoteOverlay.cursor = Mth.clamp(
                TerminalRemoteOverlay.cursor,
                0,
                Math.max(0, TerminalRemoteOverlay.order.size() - 1)
            );
            // 仅当排序真正变化时才清空缓存，避免周期性刷新导致物品闪烁
            if (orderChanged) {
                TerminalRemoteOverlay.CONTENTS.clear();
                TerminalRemoteOverlay.COUNTS.clear();
            }
            TerminalRemoteOverlay.syncVisible();
        }));
    }

    private static void reorder() {
        TerminalRemoteOverlay.cursor = 0;
        // 搜索是显式操作：搜索后进入选择模式，点击终端可取出搜索结果
        TerminalRemoteOverlay.selected = true;
        TerminalRemoteOverlay.refresh();
    }

    private static void syncVisible() {
        UUID id = TerminalRemoteOverlay.storageId;
        if (id == null) {
            return;
        }
        IntList slots = TerminalRemoteOverlay.getVisibleSlots();
        if (slots.isEmpty()) {
            TerminalRemoteOverlay.CONTENTS.clear();
            TerminalRemoteOverlay.COUNTS.clear();
            return;
        }
        // 通过 ensureVirtualPos 获取虚拟位置（缓存或触发 openRemote），不依赖独立字段
        Minecraft minecraft = Minecraft.getInstance();
        StorageTerminalClientStub.ensureVirtualPos(id).whenComplete((virtualPos, error) -> minecraft.execute(() -> {
            if (error != null || !TerminalRemoteOverlay.isHoveringSame(id)) {
                return;
            }
            StorageClientStub.sync(BlockPos.of(virtualPos), slots).whenComplete((result, syncError) -> minecraft.execute(() -> {
                if (syncError != null || !TerminalRemoteOverlay.isHoveringSame(id)) {
                    return;
                }
                TerminalRemoteOverlay.applySync(result);
            }));
        }));
    }

    private static void applySync(StorageServerStub.SyncResult result) {
        for (StorageServerStub.StackUpdate update : result.updates()) {
            if (update.stack().isEmpty()) {
                TerminalRemoteOverlay.CONTENTS.remove(update.index());
                TerminalRemoteOverlay.COUNTS.remove(update.index());
            } else {
                TerminalRemoteOverlay.CONTENTS.put(update.index(), update.stack());
                TerminalRemoteOverlay.COUNTS.put(update.index(), update.count());
            }
        }
    }

    private static void take(int slot, int button) {
        UUID id = TerminalRemoteOverlay.storageId;
        if (id == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ItemStack carried = minecraft.player != null
                            ? minecraft.player.containerMenu.getCarried()
                            : ItemStack.EMPTY;
        // 按住 Shift 时直接移入背包（放得下多少移多少，完全放不下不移动也不取到鼠标）；
        // 否则取到鼠标指针上。
        if (Screen.hasShiftDown()) {
            StorageTerminalClientStub.takeToInventory(id, slot, button).whenComplete((result, error) -> minecraft.execute(() -> {
                TerminalRemoteOverlay.taking = false;
                if (error != null || !result.changed()) {
                    return;
                }
                // 移入背包动的是玩家背包，指针未变，仅需刷新面板显示
                if (TerminalRemoteOverlay.isHoveringSame(id)) {
                    TerminalRemoteOverlay.refresh();
                }
            }));
            return;
        }
        StorageTerminalClientStub.take(id, slot, button, carried).whenComplete((result, error) -> minecraft.execute(() -> {
            TerminalRemoteOverlay.taking = false;
            if (error != null || !result.changed()) {
                return;
            }
            // 取出的物品已由服务端放到指针并广播同步到当前活动菜单，客户端无需手动 setCarried；
            // 创造背包界面的 ItemPickerMenu 是纯客户端菜单，服务端 carried 广播（containerId == -1）
            // 会被客户端忽略，需手动写回指针；其余界面由广播同步。
            TerminalRemoteOverlay.applyCarriedIfCreative(result.carried());
            // 只需刷新面板显示让被取物品从存储列表中消失。
            if (TerminalRemoteOverlay.isHoveringSame(id)) {
                TerminalRemoteOverlay.refresh();
            }
        }));
    }

    /**
     * 若当前处于创造背包界面，把指针物品写回创造界面的菜单。
     * 创造界面的 CreativeModeMenu 是纯客户端菜单（服务端 carried 广播 containerId == -1
     * 会被客户端忽略），且并非 player.inventoryMenu——必须写回当前屏幕菜单才能生效；
     * 其余界面由服务端广播同步，无需（也不应）手动设置，避免与服务端广播竞态导致物品重复。
     */
    public static void applyCarriedIfCreative(ItemStack carried) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof CreativeModeInventoryScreen creativeScreen
            && minecraft.player != null) {
            creativeScreen.getMenu().setCarried(carried);
        }
    }

    private static IntList getVisibleSlots() {
        int pageStart = TerminalRemoteOverlay.pageStart();
        IntArrayList slots = new IntArrayList();
        for (int i = pageStart; i < Math.min(pageStart + TerminalRemoteOverlay.pageSize(), TerminalRemoteOverlay.order.size()); i++) {
            slots.add(TerminalRemoteOverlay.order.getInt(i));
        }
        return slots;
    }

    private static int pageStart() {
        if (TerminalRemoteOverlay.order.isEmpty()) {
            return 0;
        }
        return TerminalRemoteOverlay.cursor / TerminalRemoteOverlay.pageSize() * TerminalRemoteOverlay.pageSize();
    }

    private static @Nullable Integer getSlotAt(int mouseX, int mouseY) {
        int gridSize = TerminalRemoteOverlay.gridSize();
        for (int row = 0; row < gridSize; row++) {
            for (int column = 0; column < gridSize; column++) {
                int x = TerminalRemoteOverlay.left + TerminalRemoteOverlay.GRID_X + column * TerminalRemoteOverlay.SLOT_SIZE;
                int y = TerminalRemoteOverlay.top + TerminalRemoteOverlay.GRID_Y + row * TerminalRemoteOverlay.SLOT_SIZE;
                if (
                    mouseX >= x - 1
                    && mouseX < x + TerminalRemoteOverlay.SLOT_SIZE - 1
                    && mouseY >= y - 1
                    && mouseY < y + TerminalRemoteOverlay.SLOT_SIZE - 1
                ) {
                    return row * gridSize + column;
                }
            }
        }
        return null;
    }

    public static void updateForTooltip(int tooltipX, int tooltipY) {
        // 浮窗渲染在 tooltip 左上方（紧挨但不重叠），再向右上偏移 (20,14)。
        // 记录 tooltip 位置，供 Alt 切换尺寸时保持右下角原点不变。
        TerminalRemoteOverlay.lastTooltipX = tooltipX;
        TerminalRemoteOverlay.lastTooltipY = tooltipY;
        TerminalRemoteOverlay.reanchor();
    }

    private static void reanchor() {
        // 以右下角为原点：尺寸变化时右下角相对 tooltip 保持固定
        TerminalRemoteOverlay.left = TerminalRemoteOverlay.lastTooltipX
                                    - TerminalRemoteOverlay.panelW() - TerminalRemoteOverlay.TOOLTIP_GAP
                                    + TerminalRemoteOverlay.PANEL_OFFSET_X;
        TerminalRemoteOverlay.top = TerminalRemoteOverlay.lastTooltipY
                                   - TerminalRemoteOverlay.panelH() - TerminalRemoteOverlay.TOOLTIP_GAP
                                   - TerminalRemoteOverlay.PANEL_OFFSET_Y;
        TerminalRemoteOverlay.clampAnchor();
    }

    private static void clampAnchor() {
        Minecraft minecraft = Minecraft.getInstance();
        TerminalRemoteOverlay.left = Mth.clamp(
            TerminalRemoteOverlay.left,
            2,
            Math.max(2, minecraft.getWindow().getGuiScaledWidth() - TerminalRemoteOverlay.panelW() - 2)
        );
        TerminalRemoteOverlay.top = Mth.clamp(
            TerminalRemoteOverlay.top,
            2,
            Math.max(2, minecraft.getWindow().getGuiScaledHeight() - TerminalRemoteOverlay.panelH() - 2)
        );
    }

    private static int maxSearchWidth() {
        return 60 + 18 * TerminalRemoteOverlay.sizeMode;
    }

    private static int gridSize() {
        return 4 + TerminalRemoteOverlay.sizeMode;
    }

    private static int pageSize() {
        return TerminalRemoteOverlay.gridSize() * TerminalRemoteOverlay.gridSize();
    }

    private static int panelW() {
        return 80 + 18 * TerminalRemoteOverlay.sizeMode;
    }

    private static int panelH() {
        return 96 + 18 * TerminalRemoteOverlay.sizeMode;
    }

    private static ResourceLocation background() {
        return SharedTextures.textureGui("misc/storage_station/remote_" + (4 + TerminalRemoteOverlay.sizeMode));
    }

    private static boolean isHoveringSame(UUID id) {
        return TerminalRemoteOverlay.isHovering() && Objects.equals(TerminalRemoteOverlay.storageId, id);
    }

    private static @Nullable UUID storageIdOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.is(ModItems.HYPERDIMENSION_TERMINAL)) {
            TerminalBinding binding = stack.get(ModComponents.TERMINAL_BINDING);
            if (binding == null || binding.id().isEmpty()) {
                return null;
            }
            return binding.id().get();
        }
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        if (stack.is(ModItems.LOCAL_TERMINAL)) {
            return StorageTerminalClientStub.localTerminalId();
        }
        if (stack.is(ModItems.SHULKER_TERMINAL)) {
            return StorageTerminalClientStub.shulkerTerminalId();
        }
        return null;
    }

    private static void reset() {
        TerminalRemoteOverlay.storageId = null;
        TerminalRemoteOverlay.search = "";
        TerminalRemoteOverlay.searchBox = null;
        TerminalRemoteOverlay.order = new IntArrayList();
        TerminalRemoteOverlay.CONTENTS.clear();
        TerminalRemoteOverlay.COUNTS.clear();
        TerminalRemoteOverlay.cursor = 0;
        TerminalRemoteOverlay.taking = false;
        TerminalRemoteOverlay.selected = false;
        TerminalRemoteOverlay.keyboardActive = false;
        TerminalRemoteOverlay.fastRetries = 0;
        TerminalRemoteOverlay.frameCounter = 0;
    }
}
