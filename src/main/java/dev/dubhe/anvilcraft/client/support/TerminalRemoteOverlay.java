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
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
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
    /**
     * 普通文本搜索时全量内容缓存的单次同步页大小。与服务端 sync 的
     * MAX_SYNC_SLOTS 上限一致，超过部分分页请求。
     */
    private static final int FULL_SYNC_PAGE = 256;
    /**
     * 一次刷新周期内最多连续同步的全量页数。服务端同步是网络往返，几万槽位的大存储
     * 可能一次刷新就背靠背发出几十个 RPC；达到预算后暂停，等下一个刷新周期再续拉，
     * 避免单周期请求突发饿死其他 RPC / 占满网络。
     */
    private static final int FULL_SYNC_PAGE_BUDGET = 4;
    /**
     * 全量同步页失败后的退避刷新周期数：退避期间以服务端排序直接展示（未过滤），不再
     * 反复触发全量同步（避免周期性失败时每 20 tick 重发全部缺失页的流量放大）。
     */
    private static final int FULL_SYNC_RETRY_REFRESHES = 2;
    /** 浮窗与 tooltip 之间的间隙（px），紧挨但不重叠。 */
    private static final int TOOLTIP_GAP = 2;
    /** 浮窗相对 tooltip 左上方再向右上偏移的像素（右 20、上 14）。 */
    private static final int PANEL_OFFSET_X = 20;
    private static final int PANEL_OFFSET_Y = 14;

    private static @Nullable UUID storageId;
    private static int sizeMode;
    private static @Nullable EditBox searchBox;
    private static String search = "";
    /** 当前展示的排序：非普通文本搜索时为服务端全量排序，普通文本搜索时为客户端过滤后的子集。 */
    private static IntList order = new IntArrayList();
    /** 服务端返回的全量排序（服务端只按 @/# 前缀过滤）；普通文本搜索时作为客户端二次过滤的来源。 */
    private static IntList baseOrder = new IntArrayList();
    /** 普通文本搜索的全量内容同步是否进行中：避免多次 refresh 并发重复全量同步。 */
    private static boolean fullSyncing;
    /**
     * 全量同步进行期间服务端排序/搜索词再次变化：当前同步结束后需要基于最新
     * baseOrder 再补一轮，保证过滤不漏项（最终一致）。
     */
    private static boolean fullSyncReplan;
    /** 全量同步页失败后的退避剩余刷新周期数（见 {@link #FULL_SYNC_RETRY_REFRESHES}）。 */
    private static int fullSyncRetryRefresh;
    /** 本轮全量同步已连续发送的页数（达到预算后暂停，见 {@link #FULL_SYNC_PAGE_BUDGET}）。 */
    private static int fullSyncPages;
    private static final Int2ObjectMap<UnlimitedItemStack> CONTENTS = new Int2ObjectOpenHashMap<>();
    private static final Int2LongMap COUNTS = new Int2LongOpenHashMap();
    /** 当前选中的物品序号；-1 表示尚未通过滚轮选择（未选择时点击终端允许 vanilla 拿起终端）。 */
    private static int cursor = -1;
    private static int left;
    private static int top;
    private static int frameCounter;
    private static int fastRetries;
    private static boolean taking;
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
            // 本地 / 潜影终端：仅当当前可连接目标时激活浮窗（超出范围 / 无可连接潜影集装箱则不可用）
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
            if (TerminalRemoteOverlay.cursor >= 0 && orderIndex == TerminalRemoteOverlay.cursor) {
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
            return true;
        }
        if (button != 0 && button != 1) {
            return false;
        }
        if (!TerminalRemoteOverlay.isHovering() || TerminalRemoteOverlay.taking) {
            return false;
        }
        // 未通过滚轮选择过物品（cursor == -1）时，不拦截点击——允许 vanilla 将终端拿起
        if (TerminalRemoteOverlay.cursor < 0) {
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
        if (TerminalRemoteOverlay.cursor < 0) {
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
        // 未聚焦（未按 Tab 接管键盘）前不拦截其它按键：Tab 键本身除外（首次按 Tab 激活接管并聚焦搜索框）
        if (!TerminalRemoteOverlay.searchBoxFocused()) {
            if (keyCode == InputConstants.KEY_TAB) {
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
        if (!TerminalRemoteOverlay.isHovering() || !TerminalRemoteOverlay.searchBoxFocused()) {
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
        return TerminalRemoteOverlay.cursor >= 0;
    }

    /** 搜索框是否聚焦：即键盘是否被浮窗接管（按 Tab 或点击搜索框聚焦）。 */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean searchBoxFocused() {
        return TerminalRemoteOverlay.searchBox != null && TerminalRemoteOverlay.searchBox.isFocused();
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
        // 本地 / 潜影终端：目标脱离连接范围（走远 / 目标消失 / 集装箱移出背包）后停用浮窗
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
            if (TerminalRemoteOverlay.fullSyncRetryRefresh > 0) {
                // 全量同步失败退避：每过一个刷新周期衰减一次；退避期内的 refresh
                // 已降级为服务端排序展示，见 refresh()。
                TerminalRemoteOverlay.fullSyncRetryRefresh--;
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
            boolean orderChanged = !next.equals(TerminalRemoteOverlay.baseOrder);
            TerminalRemoteOverlay.baseOrder = next;
            // 普通文本搜索：服务端返回全量排序（客户端语言环境未知，不能服务端过滤）。
            // 需要按本地化名称二次过滤，但客户端只缓存了可见页内容，必须先分页全量
            // 同步缺失槽位的内容；完成后在客户端按显示名 + id path 过滤出展示排序。
            if (TerminalRemoteOverlay.isPlainTextSearch(TerminalRemoteOverlay.search)) {
                if (TerminalRemoteOverlay.fullSyncRetryRefresh > 0) {
                    // 全量同步页失败后的退避期：直接按服务端排序展示（未过滤；可见页
                    // 内容由 syncVisible 补拉），退避结束后自动恢复全量同步与过滤。
                    TerminalRemoteOverlay.applyServerOrder(next, orderChanged);
                    return;
                }
                TerminalRemoteOverlay.syncFullContents(id, orderChanged);
                return;
            }
            // @ / # 前缀搜索或空搜索：服务端已过滤，直接采用
            TerminalRemoteOverlay.applyServerOrder(next, orderChanged);
        }));
    }

    /**
     * 采用服务端排序作为展示排序并同步可见页：@/#/空搜索直接采用；
     * 普通文本搜索的失败退避期也先采用（未过滤降级展示）。
     *
     * @param next         服务端返回的排序
     * @param orderChanged 相对上次全量排序是否变化（变化时裁剪内容缓存）
     */
    private static void applyServerOrder(IntList next, boolean orderChanged) {
        TerminalRemoteOverlay.order = next;
        if (orderChanged) {
            TerminalRemoteOverlay.trimContentCache();
        }
        // 仅在选择有效时收紧到新排序范围；-1（未选择）保持不变
        if (TerminalRemoteOverlay.cursor >= 0) {
            TerminalRemoteOverlay.cursor = Mth.clamp(
                TerminalRemoteOverlay.cursor,
                0,
                Math.max(0, TerminalRemoteOverlay.order.size() - 1)
            );
        }
        TerminalRemoteOverlay.syncVisible();
    }

    /**
     * 服务端排序响应后按需分页全量同步内容：仅同步 {@code baseOrder} 中尚未缓存的槽位，
     * 每页最多 {@link #FULL_SYNC_PAGE} 个；完成后在客户端按本地化名称二次过滤。
     *
     * @param id           当前悬停的存储会话 id
     * @param orderChanged 服务端排序相对上次是否变化（变化时清空全量缓存）
     */
    private static void syncFullContents(UUID id, boolean orderChanged) {
        if (orderChanged) {
            // 排序变化：先把缓存裁剪到新全量排序范围（保留交集，复用已缓存槽）
            TerminalRemoteOverlay.trimContentCache();
        }
        if (TerminalRemoteOverlay.fullSyncing) {
            // 已有一个全量同步在进行：记录需要基于最新 baseOrder 再补一轮（若期间
            // 排序/搜索词已变化，其回调会用最新状态补同步并过滤），等它结束。
            TerminalRemoteOverlay.fullSyncReplan = true;
            return;
        }
        // 找出尚未缓存的槽位，分页请求内容
        IntArrayList missing = new IntArrayList();
        for (int i = 0; i < TerminalRemoteOverlay.baseOrder.size(); i++) {
            int slot = TerminalRemoteOverlay.baseOrder.getInt(i);
            if (!TerminalRemoteOverlay.CONTENTS.containsKey(slot)) {
                missing.add(slot);
            }
        }
        if (missing.isEmpty()) {
            // 全量内容已齐：直接过滤并同步可见页
            TerminalRemoteOverlay.applyClientFilter();
            return;
        }
        TerminalRemoteOverlay.fullSyncing = true;
        TerminalRemoteOverlay.fullSyncPages = 0;
        // 激活新批时清掉上一批残留的重排标记：置位只属于"进行中的批"，本批将基于
        // 最新 baseOrder 与搜索词拉取（无需 replan）；批处理期间的新置位由收尾消费。
        TerminalRemoteOverlay.fullSyncReplan = false;
        // 发起 RPC 时不再拍排序快照：同步期间 baseOrder / 搜索词变化时，
        // refresh 的普通文本分支会置 fullSyncReplan 标记，收尾时基于最新状态
        // 补一轮（页预算暂停的续拉同理），保证过滤不漏项（最终一致）。
        Minecraft minecraft = Minecraft.getInstance();
        StorageTerminalClientStub.ensureVirtualPos(id).whenComplete((virtualPos, error) -> minecraft.execute(() -> {
            if (error != null || !TerminalRemoteOverlay.isHoveringSame(id)) {
                TerminalRemoteOverlay.fullSyncing = false;
                return;
            }
            TerminalRemoteOverlay.syncFullPage(id, BlockPos.of(virtualPos), missing, 0);
        }));
    }

    /**
     * 分页同步一页全量内容；同步完成后若还有缺失槽位且未超页预算则请求下一页，否则
     * 暂停等下一个刷新周期续拉（预算见 {@link #FULL_SYNC_PAGE_BUDGET}）。收尾时：
     * 期间收到过重排请求（排序/搜索词变化）则基于最新 baseOrder 再补一轮，否则按
     * 本地化名称二次过滤（普通文本搜索）。
     */
    private static void syncFullPage(UUID id, BlockPos virtualPos, IntList missing, int from) {
        int to = Math.min(from + TerminalRemoteOverlay.FULL_SYNC_PAGE, missing.size());
        IntArrayList page = new IntArrayList(missing.subList(from, to));
        TerminalRemoteOverlay.fullSyncPages++;
        Minecraft minecraft = Minecraft.getInstance();
        StorageClientStub.sync(virtualPos, page).whenComplete((result, syncError) -> minecraft.execute(() -> {
            // 悬停目标已切换（换了个终端）：全量同步链作废，reset() 已清空全部状态
            if (!TerminalRemoteOverlay.isHoveringSame(id)) {
                TerminalRemoteOverlay.fullSyncing = false;
                return;
            }
            if (syncError != null) {
                TerminalRemoteOverlay.fullSyncing = false;
                // 失败：退避期间按服务端排序直接展示（未过滤），可见页内容由
                // syncVisible 补拉；退避结束后的 refresh 会用干净状态重试全量同步。
                // 已成功同步的页保留在缓存中（不是全部丢弃），重试只补缺失槽位。
                TerminalRemoteOverlay.fullSyncRetryRefresh = TerminalRemoteOverlay.FULL_SYNC_RETRY_REFRESHES;
                TerminalRemoteOverlay.applyServerOrder(
                    new IntArrayList(TerminalRemoteOverlay.baseOrder),
                    false
                );
                return;
            }
            // 已切出普通文本搜索（@/#/空）：服务端已过滤，refresh 的对应分支已接管
            // 展示，终止全量同步链，不再拉剩余页。
            if (!TerminalRemoteOverlay.isPlainTextSearch(TerminalRemoteOverlay.search)) {
                TerminalRemoteOverlay.fullSyncing = false;
                return;
            }
            TerminalRemoteOverlay.applySync(result);
            if (to < missing.size()) {
                if (TerminalRemoteOverlay.fullSyncPages >= TerminalRemoteOverlay.FULL_SYNC_PAGE_BUDGET) {
                    // 已达本刷新周期页预算：暂停本批，等下一 refresh 周期用最新 baseOrder
                    // 重算缺失槽位后继续（displayOrder 保持批处理开始前的值，全量拉齐后
                    // 才切到过滤结果，见 refresh 的普通文本分支）。
                    TerminalRemoteOverlay.fullSyncing = false;
                    return;
                }
                TerminalRemoteOverlay.syncFullPage(id, virtualPos, missing, to);
                return;
            }
            // 本批（快照）拉完：收尾
            TerminalRemoteOverlay.fullSyncing = false;
            if (TerminalRemoteOverlay.isHoveringSame(id)
                && TerminalRemoteOverlay.fullSyncReplan) {
                // 同步期间收到过新排序/搜索词（refresh 在 fullSyncing 期间到达时只
                // 置标记）：消费标记，基于最新 baseOrder 与搜索词再补一轮并过滤。
                TerminalRemoteOverlay.fullSyncReplan = false;
                TerminalRemoteOverlay.syncFullContents(id, false);
                return;
            }
            TerminalRemoteOverlay.applyClientFilter();
        }));
    }

    /**
     * 在客户端按本地化显示名与 id path 二次过滤（与 StorageScreen.applySearchFilter 一致）：
     * 仅保留显示名或 id path 包含搜索词的槽位，保持服务端排序的相对顺序。
     */
    private static void applyClientFilter() {
        String plain = TerminalRemoteOverlay.search.strip().toLowerCase(Locale.ROOT);
        IntArrayList filtered = new IntArrayList(TerminalRemoteOverlay.baseOrder.size());
        for (int i = 0; i < TerminalRemoteOverlay.baseOrder.size(); i++) {
            int slot = TerminalRemoteOverlay.baseOrder.getInt(i);
            UnlimitedItemStack stack = TerminalRemoteOverlay.CONTENTS.get(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ItemStack itemStack = stack.toStack();
            String name = itemStack.getHoverName().getString().toLowerCase(Locale.ROOT);
            String idPath = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).getPath().toLowerCase(Locale.ROOT);
            if (name.contains(plain) || idPath.contains(plain)) {
                filtered.add(slot);
            }
        }
        TerminalRemoteOverlay.order = filtered;
        // 仅在选择有效时收紧到新排序范围；-1（未选择）保持不变
        if (TerminalRemoteOverlay.cursor >= 0) {
            TerminalRemoteOverlay.cursor = Mth.clamp(
                TerminalRemoteOverlay.cursor,
                0,
                Math.max(0, TerminalRemoteOverlay.order.size() - 1)
            );
        }
        TerminalRemoteOverlay.syncVisible();
    }

    /** 是否普通文本搜索（非空且不以 @ / # 开头）；此时服务端不过滤，需客户端二次过滤。 */
    private static boolean isPlainTextSearch(String search) {
        String stripped = search.strip();
        return !stripped.isEmpty() && stripped.charAt(0) != '@' && stripped.charAt(0) != '#';
    }

    /**
     * 内容缓存与最新全量排序对齐：移除已不在 {@code baseOrder} 中的槽位，
     * 保留交集（排序局部变动时全量缓存可复用，只补缺失槽）。
     */
    private static void trimContentCache() {
        if (TerminalRemoteOverlay.CONTENTS.isEmpty()) {
            return;
        }
        IntOpenHashSet keep = new IntOpenHashSet(TerminalRemoteOverlay.baseOrder.size());
        for (int i = 0; i < TerminalRemoteOverlay.baseOrder.size(); i++) {
            keep.add(TerminalRemoteOverlay.baseOrder.getInt(i));
        }
        if (keep.size() == TerminalRemoteOverlay.CONTENTS.size()) {
            // 槽位数量相同且无重复（baseOrder 无重复）：无需裁剪
            boolean allKept = true;
            for (int slot : TerminalRemoteOverlay.CONTENTS.keySet()) {
                if (!keep.contains(slot)) {
                    allKept = false;
                    break;
                }
            }
            if (allKept) {
                return;
            }
        }
        TerminalRemoteOverlay.CONTENTS.keySet().removeIf(slot -> !keep.contains(slot));
        TerminalRemoteOverlay.COUNTS.keySet().removeIf(slot -> !keep.contains(slot));
    }

    private static void reorder() {
        // 搜索是显式操作：搜索后进入选择模式（选中第一格），点击终端可取出搜索结果。
        // 显式切词也立即结束全量同步的失败退避（用户意图明确，无需等退避周期）。
        TerminalRemoteOverlay.fullSyncRetryRefresh = 0;
        TerminalRemoteOverlay.cursor = 0;
        TerminalRemoteOverlay.refresh();
    }

    private static void syncVisible() {
        UUID id = TerminalRemoteOverlay.storageId;
        if (id == null) {
            return;
        }
        IntList slots = TerminalRemoteOverlay.getVisibleSlots();
        if (slots.isEmpty()) {
            // 展示为空：普通文本搜索时可能只是过滤结果为空，全量内容缓存仍需保留
            // （换搜索词直接复用）；仅非搜索（服务端结果即空）时清空缓存。
            if (!TerminalRemoteOverlay.isPlainTextSearch(TerminalRemoteOverlay.search)) {
                TerminalRemoteOverlay.CONTENTS.clear();
                TerminalRemoteOverlay.COUNTS.clear();
            }
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
        TerminalRemoteOverlay.baseOrder = new IntArrayList();
        TerminalRemoteOverlay.fullSyncing = false;
        TerminalRemoteOverlay.fullSyncReplan = false;
        TerminalRemoteOverlay.fullSyncRetryRefresh = 0;
        TerminalRemoteOverlay.fullSyncPages = 0;
        TerminalRemoteOverlay.CONTENTS.clear();
        TerminalRemoteOverlay.COUNTS.clear();
        TerminalRemoteOverlay.cursor = -1;
        TerminalRemoteOverlay.taking = false;
        TerminalRemoteOverlay.fastRetries = 0;
        TerminalRemoteOverlay.frameCounter = 0;
    }
}
