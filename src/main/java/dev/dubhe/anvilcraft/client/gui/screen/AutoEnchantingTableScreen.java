package dev.dubhe.anvilcraft.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.anvilcraft.lib.v2.util.ListUtil;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.Scrollable;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.AutoEnchantingTableBlockEntity;
import dev.dubhe.anvilcraft.client.gui.component.FluidDisplayWidget;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.inventory.AutoEnchantingTableMenu;
import dev.dubhe.anvilcraft.network.AutoEnchantingTableFluidPacket;
import dev.dubhe.anvilcraft.network.AutoEnchantingTableLevelPacket;
import dev.dubhe.anvilcraft.network.AutoEnchantingTableSyncPacket;
import dev.dubhe.anvilcraft.util.EnchantmentData;
import dev.dubhe.anvilcraft.util.LiquidEnchantmentUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class AutoEnchantingTableScreen extends AbstractContainerScreen<AutoEnchantingTableMenu> {
    private static final Identifier BACKGROUND = SharedTextures.bg("machine", "auto_enchanting_table");
    private static final Identifier PROGRESS = SharedTextures.textureGui("machine/auto_enchanting_table/progress");

    private final List<Integer> filteredIndexes = new ArrayList<>();
    private final Scrollable scrollable = new Scrollable() {
        @Override
        public int row() {
            return 2;
        }

        @Override
        public int column() {
            return 5;
        }

        @Override
        public int size() {
            return AutoEnchantingTableScreen.this.filteredIndexes.size();
        }

        @Override
        public void setHead(int head) {
            AutoEnchantingTableScreen.this.head = head;
        }
    };
    private int head = 0;
    private String filterText = "";
    private List<EnchantmentData> lastFilteredEnchantments = List.of();
    private @Nullable EditBox searchBox;
    private @Nullable ItemStack renderingTooltipEnchantedBook;
    private @Nullable ItemStack ghostOutput;
    /// 引物模式下选中附魔全部无法应用到输入物品（如木棍）时置位，用于 GUI 提示
    private boolean primerResultEmpty = false;
    private static final int WARNING_DURATION_TICKS = 80;
    private static final int WARNING_TEXT_COLOR = 0xDFFF2222;
    private @Nullable Component warningMessage;
    private int warningTicks = 0;
    private int warningIndex = -1;

    public AutoEnchantingTableScreen(
        AutoEnchantingTableMenu menu,
        Inventory playerInventory,
        Component title
    ) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        this.titleLabelX = (this.getImageWidth() - this.font.width(this.title)) / 2;
        this.titleLabelY = Constant.SCREEN_TITLE_Y;

        this.searchBox = new EditBox(
            this.font,
            this.leftPos + 59,
            this.topPos + 19,
            83,
            9,
            Component.translatable("screen.anvilcraft.auto_enchanting_table.search")
        );
        this.searchBox.setBordered(false);
        this.searchBox.setMaxLength(64);
        this.searchBox.setValue(this.filterText);
        this.searchBox.setResponder(this::onSearchTextChange);
        this.searchBox.setCanLoseFocus(true);
        this.addRenderableWidget(this.searchBox);

        // 打开屏幕时立即填充自选附魔过滤结果，否则 filteredIndexes 为空导致无法显示
        this.refreshFilter();

        this.addRenderableWidget(new FluidDisplayWidget(
            this.leftPos + 151,
            this.topPos + 16,
            18,
            56,
            this.menu.getBlockEntity().getFluidHandler(),
            () ->
                ClientPacketDistributor.sendToServer(new AutoEnchantingTableFluidPacket(this.menu.getBlockEntity().getBlockPos()))
        ));
    }

    private void onSearchTextChange(String text) {
        this.filterText = text;
        this.refreshFilter();
        this.scrollable.reset();
    }

    private void refreshFilter() {
        this.filteredIndexes.clear();
        String query = this.filterText.toLowerCase();
        List<EnchantmentData> enchantments = this.menu.getEnchantments();
        for (int i = 0; i < enchantments.size(); i++) {
            EnchantmentData data = enchantments.get(i);
            String name = data.enchantment().value().description().getString().toLowerCase();
            String id = data.enchantment().getRegisteredName().toLowerCase();
            if (query.isEmpty() || name.contains(query) || id.contains(query)) {
                this.filteredIndexes.add(i);
            }
        }
    }

    @Override
    protected void containerTick() {
        this.renderingTooltipEnchantedBook = null;
        this.ghostOutput = this.computeGhostOutput();
        // 引物变化（或首次进入）时重新计算自选附魔过滤结果，使 filteredIndexes 与菜单附魔列表保持一致
        List<EnchantmentData> enchantments = this.menu.getEnchantments();
        if (!enchantments.equals(this.lastFilteredEnchantments)) {
            this.lastFilteredEnchantments = List.copyOf(enchantments);
            this.refreshFilter();
            this.scrollable.reset();
        }
        // 引物模式下输入物品无法承载选中附魔时持续提示（否则物品会静默卡在输入槽）
        if (this.primerResultEmpty) {
            this.showWarning(-1, Component.translatable("screen.anvilcraft.auto_enchanting_table.warning.item_incompatible"));
        }
        if (this.warningTicks > 0) {
            this.warningTicks--;
            if (this.warningTicks == 0) {
                this.warningMessage = null;
                this.warningIndex = -1;
            }
        }
        super.containerTick();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        guiGraphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(guiGraphics, mouseX, mouseY, partialTick);
        // 引物模式下悬停输出栏：显示即将输出物品的虚影（不可交互）
        if (
            this.ghostOutput != null
            && !this.ghostOutput.isEmpty()
        ) {
            Slot outputSlot = this.menu.getSlot(AutoEnchantingTableBlockEntity.SLOT_OUTPUT);
            int ghostX = this.leftPos + outputSlot.x;
            int ghostY = this.topPos + outputSlot.y;
            RenderSupport.renderItemWithTransparency(this.ghostOutput, guiGraphics, ghostX, ghostY, 0.52f);
        }
        int progressPassed = Mth.ceil(
            14 * (1 - ((this.menu.getBlockEntity().getCooldownTicks() + partialTick) / AnvilCraft.CONFIG.autoEnchantingTableInterval))
        );
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED,
            AutoEnchantingTableScreen.PROGRESS,
            this.leftPos + 12,
            this.topPos + 36,
            0,
            0,
            7,
            progressPassed,
            7,
            14
        );
        this.extractTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor guiGraphics, int x, int y) {
        // 引物模式下悬停输出栏：显示即将输出物品的虚影 tooltip（含产物附魔）
        if (
            this.ghostOutput != null
            && !this.ghostOutput.isEmpty()
            && this.hoveredSlot == this.menu.getSlot(AutoEnchantingTableBlockEntity.SLOT_OUTPUT)
            && this.menu.getSlot(AutoEnchantingTableBlockEntity.SLOT_OUTPUT).getItem().isEmpty()
        ) {
            guiGraphics.setTooltipForNextFrame(
                this.font,
                this.getTooltipFromContainerItem(this.ghostOutput),
                this.ghostOutput.getTooltipImage(),
                this.ghostOutput,
                x,
                y
            );
            return;
        }
        if (
            this.hoveredSlot == this.menu.getSlot(AutoEnchantingTableBlockEntity.SLOT_PRIMER)
            && Objects.requireNonNull(this.hoveredSlot).getItem().isEmpty()
        ) {
            guiGraphics.setTooltipForNextFrame(this.font, Component.translatable("screen.anvilcraft.auto_enchanting_table.primer"), x, y);
        } else if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            ItemStack stack = this.hoveredSlot.getItem();
            guiGraphics.setTooltipForNextFrame(this.font, this.getTooltipFromContainerItem(stack), stack.getTooltipImage(), stack, x, y);
        } else if (this.renderingTooltipEnchantedBook != null) {
            guiGraphics.setTooltipForNextFrame(
                this.font,
                this.getTooltipFromContainerItem(this.renderingTooltipEnchantedBook),
                this.renderingTooltipEnchantedBook.getTooltipImage(),
                this.renderingTooltipEnchantedBook,
                x,
                y
            );
        } else if (this.insideBookshelf(x, y)) {
            guiGraphics.setTooltipForNextFrame(
                this.font,
                Component.translatable(
                    "screen.anvilcraft.auto_enchanting_table.enchant_power_bonus",
                    this.getMenu().getBlockEntity().getShelfLevel()
                ),
                x,
                y
            );
        }
    }

    protected void renderEnchantmentSelectingArea(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderingTooltipEnchantedBook = null;
        // 未放置允许的引物时，不显示自选附魔
        ItemStack primer = this.menu.getBlockEntity().getItem(AutoEnchantingTableBlockEntity.SLOT_PRIMER);
        if (primer.isEmpty() || !this.menu.getBlockEntity().isAllowedPrimer(primer)) {
            return;
        }
        if (this.filteredIndexes.isEmpty()) return;
        int visible = Math.min(this.filteredIndexes.size() - this.head, 10);
        for (int j = 0; j < visible; j++) {
            int index = this.head + j;
            int original = this.filteredIndexes.get(index);
            int x = this.leftPos + 47 + 18 * (j % 5);
            int y = this.topPos + 32 + 18 * ((j / 5) % 2);

            EnchantmentData data = ListUtil.safelyGet(this.menu.getEnchantments(), original).orElse(null);
            if (data == null) continue;

            ItemStack willRender = EnchantmentHelper.createBook(data.toEnchantmentInst());

            int offsetV = 0;

            if (MathUtil.isInRange(mouseX, mouseY, x, y, x + 18, y + 18)) {
                offsetV = 36;
                this.renderingTooltipEnchantedBook = willRender;
            }

            boolean selected = this.menu.getSelectedIndexes().contains(original);
            if (selected) offsetV = 18;

            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SharedTextures.SWITCH_TABLE_BUTTON, x, y, 0, offsetV, 18, 18, 18, 54);
            guiGraphics.item(willRender, x + 1, y + (selected ? 1 : 0), (int) (partialTick * 100));

            // 条件不满足被拒的按钮：红色呼吸灯效果
            if (this.warningTicks > 0 && index == this.warningIndex) {
                long gameTime = this.getMinecraft().level != null ? this.getMinecraft().level.getGameTime() : 0;
                float breathe = (Mth.sin(gameTime * 0.2f) + 1.0f) / 2.0f;
                int alpha = 0x80 + (int) (0x40 * breathe);
                int color = (alpha << 24) | 0xFF5555;
                guiGraphics.fillGradient(x, y, x + 18, y + 18, color, color);
            }
        }
        // 条件不满足时的警告文字（自选附魔区中央，75% 不透明红色，4 秒）
        if (this.warningTicks > 0 && this.warningMessage != null) {
            int centerX = this.leftPos + this.getImageWidth() / 2;
            int height = this.font.lineHeight;
            int centerY = this.topPos - Mth.ceil(height * 1.15);
            int width = this.font.width(this.warningMessage);
            int x = centerX - width / 2;
            int y = centerY - height / 2;
            guiGraphics.fillGradient(x - 1, y - 1, x + width, y + height, 0x88000000, 0x88000000);
            guiGraphics.text(
                this.font,
                this.warningMessage,
                x,
                y,
                WARNING_TEXT_COLOR,
                false
            );
        }
    }

    private boolean isLiquidEnchantmentMode() {
        return this.getLiquidEnchantment().isPresent();
    }

    private Optional<Holder<Enchantment>> getLiquidEnchantment() {
        FluidStack fluid = this.menu.getBlockEntity().getFluid();
        return LiquidEnchantmentUtil.getEnchantment(fluid);
    }

    private int getLiquidMaxLevel() {
        Optional<Holder<Enchantment>> enchantment = this.getLiquidEnchantment();
        if (enchantment.isEmpty()) return 0;
        AutoEnchantingTableBlockEntity be = this.menu.getBlockEntity();
        return AutoEnchantingTableBlockEntity.computeLiquidMaxLevel(
            be.getItem(AutoEnchantingTableBlockEntity.SLOT_INPUT),
            be.getItem(AutoEnchantingTableBlockEntity.SLOT_PRIMER),
            enchantment.get()
        );
    }

    /**
     * 液态魔咒模式：渲染唯一的附魔书（等级随滚轮改变），并给出当前等级与限制提示。
     */
    private void renderLiquidEnchantmentArea(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        Optional<Holder<Enchantment>> enchantment = this.getLiquidEnchantment();
        if (enchantment.isEmpty()) return;
        AutoEnchantingTableBlockEntity be = this.menu.getBlockEntity();
        int level = be.getLiquidEnchantmentLevel();

        int x = this.leftPos + 47;
        int y = this.topPos + 32;
        ItemStack willRender = EnchantmentHelper.createBook(new EnchantmentInstance(enchantment.get(), Math.max(1, level)));

        int offsetV = 0;
        boolean hovered = MathUtil.isInRange(mouseX, mouseY, x, y, x + 18, y + 18);
        if (hovered) {
            offsetV = 36;
            this.renderingTooltipEnchantedBook = willRender;
        }
        if (level > 0) offsetV = 18;

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SharedTextures.SWITCH_TABLE_BUTTON, x, y, 0, offsetV, 18, 18, 18, 54);
        guiGraphics.item(willRender, x + 1, y + (level > 0 ? 1 : 0), 0);

        if (level > 0) {
            guiGraphics.text(
                this.font,
                AutoEnchantingTableScreen.getLiquidLevelText(level),
                x + 20,
                y + 5,
                0xFFFFFFFF,
                false
            );
        }
        var pose = guiGraphics.pose();
        pose.pushMatrix();
        pose.translate(x + 1, y + 20);
        pose.scale(0.59F, 0.59F);
        guiGraphics.text(
            this.font,
            Component.translatableEscape("screen.anvilcraft.auto_enchanting_table.liquid_enchantment.0"),
            0,
            0,
            0xFFFFFFFF,
            false
        );
        guiGraphics.text(
            this.font,
            Component.translatableEscape("screen.anvilcraft.auto_enchanting_table.liquid_enchantment.1"),
            0,
            13,
            0xFFFFFFFF,
            false
        );
        pose.popMatrix();

        int maxLevel = this.getLiquidMaxLevel();
        // 皇家模式下与物品不兼容：红色呼吸灯提示不可选择
        if (maxLevel <= 0) {
            long gameTime = this.getMinecraft().level != null ? this.getMinecraft().level.getGameTime() : 0;
            float breathe = (Mth.sin(gameTime * 0.2f) + 1.0f) / 2.0f;
            int alpha = 0x80 + (int) (0x40 * breathe);
            int color = (alpha << 24) | 0xFF5555;
            int centerX = this.leftPos + this.getImageWidth() / 2;
            int height = this.font.lineHeight;
            int centerY = this.topPos - Mth.ceil(height * 1.15);
            Component warning = Component.translatable("screen.anvilcraft.auto_enchanting_table.warning.liquid_incompatible");
            int width = this.font.width(warning);
            guiGraphics.fillGradient(x, y, x + 18, y + 18, color, color);
            x = centerX - width / 2;
            y = centerY - height / 2;
            guiGraphics.fillGradient(x - 1, y - 1, x + width, y + height, 0x88000000, 0x88000000);
            guiGraphics.text(
                this.font,
                warning,
                x,
                y,
                WARNING_TEXT_COLOR,
                false
            );
        }
    }

    private boolean insideLiquidBook(double mouseX, double mouseY) {
        int left = this.leftPos + 47;
        int top = this.topPos + 32;
        // 覆盖整个自选附魔区域（5 列 × 2 行），滚轮在区域内即可调整等级
        return MathUtil.isInRange(mouseX, mouseY, left, top, left + 90, top + 36);
    }

    /**
     * 液态魔咒模式下等级文字：≤10 级始终罗马数字；>10 级是否罗马数字由客户端配置决定。
     */
    private static Component getLiquidLevelText(int level) {
        if (level <= 10) {
            return Component.translatable("enchantment.level." + level);
        }
        if (AnvilCraft.CLIENT_CONFIG.liquidEnchantmentRomanNumerals) {
            // 11-15 级已有翻译（XI-XV），更高等级回退为代码计算的罗马数字
            if (level <= 15) {
                return Component.translatable("enchantment.level." + level);
            }
            return Component.literal(AutoEnchantingTableScreen.toRomanNumeral(level));
        }
        return Component.literal(String.valueOf(level));
    }

    private static String toRomanNumeral(int value) {
        int[] numbers = {100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] symbols = {"C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < numbers.length; i++) {
            while (value >= numbers[i]) {
                value -= numbers[i];
                builder.append(symbols[i]);
            }
        }
        return builder.toString();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos,
            0, 0, this.getImageWidth(), this.getImageHeight(), 256, 256);

        // 自选附魔区绘制在背景之上、控件之下，避免悬停时遮挡流体槽 tooltip
        switch (this.getMenu().getBlockEntity().getWorkMode()) {
            case PRIMER -> this.renderEnchantmentSelectingArea(guiGraphics, mouseX, mouseY, partialTick);
            case LIQUID_ENCHANTMENT -> this.renderLiquidEnchantmentArea(guiGraphics, mouseX, mouseY);
            default -> {}
        }

        ItemStack stack = Items.BOOKSHELF.getDefaultInstance();
        guiGraphics.fakeItem(stack, this.leftPos + 27, this.topPos + 55);
        guiGraphics.itemDecorations(
            this.getMinecraft().font,
            stack,
            this.leftPos + 27,
            this.topPos + 55,
            "" + this.getMenu().getBlockEntity().getShelfLevel()
        );

        if (this.scrollable.canScroll()) {
            int left = this.leftPos + 140;
            int top = this.topPos + 32;
            int down = top + 36;
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED,
                SharedTextures.SWITCH_TABLE_SLIDER,
                left,
                top + (int) ((down - top - 12) * this.scrollable.getScrollOffs()),
                0,
                0,
                4,
                12,
                8,
                12
            );
        }
    }

    @Override
    public void resize(int width, int height) {
        this.scrollable.calculateScroll(this.head / 5);
        this.init(width, height);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == 0) {
            if (this.searchBox != null && !this.insideSearchBox(mouseX, mouseY)) {
                this.searchBox.setFocused(false);
            }
            if (this.insideScrollbar(mouseX, mouseY)) {
                this.scrollable.scrolling();
                return true;
            }
            // 液态魔咒模式下自选附魔区被液态附魔书占用，点击不应切换引物附魔
            if (!this.isLiquidEnchantmentMode()) {
                int visible = Math.min(this.filteredIndexes.size() - this.head, 10);
                for (int j = 0; j < visible; j++) {
                    int index = this.head + j;
                    int x = this.leftPos + 47 + 18 * (j % 5);
                    int y = this.topPos + 32 + 18 * ((j / 5) % 2);
                    if (!MathUtil.isInRange(mouseX, mouseY, x, y, x + 18, y + 18)) continue;
                    int original = this.filteredIndexes.get(index);
                    if (this.menu.getSelectedIndexes().contains(original)) {
                        this.menu.unselect(original);
                        ClientPacketDistributor.sendToServer(new AutoEnchantingTableSyncPacket(original, false));
                    } else {
                        EnchantmentData data = ListUtil.safelyGet(this.menu.getEnchantments(), original).orElse(null);
                        if (data != null && !this.canSelect(index, data)) {
                            // 条件不满足：按钮不被按下，显示警告
                            return true;
                        }
                        this.menu.select(original);
                        ClientPacketDistributor.sendToServer(new AutoEnchantingTableSyncPacket(original, true));
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (this.searchBox != null && this.searchBox.isFocused()) {
            // 搜索框聚焦时按一次 Esc：取消聚焦；再按一次才退出
            if (keyCode == InputConstants.KEY_ESCAPE) {
                this.searchBox.setFocused(false);
                return true;
            }
            // 搜索框聚焦时完全接替键盘输入
            this.searchBox.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.searchBox == null) return super.charTyped(event);
        if (this.searchBox.isFocused()) {
            return this.searchBox.charTyped(event);
        }
        return super.charTyped(event);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        int button = event.button();
        if (button == 0 && this.scrollable.isScrolling()) {
            this.scrollable.notScrolling();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseY = event.y();
        if (this.scrollable.isScrolling()) {
            int top = this.topPos + 32;
            this.scrollable.scrollOnDrag(12, mouseY, top, top + 36);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isLiquidEnchantmentMode()) {
            if (!this.insideLiquidBook(mouseX, mouseY)) return false;
            AutoEnchantingTableBlockEntity be = this.menu.getBlockEntity();
            int current = be.getLiquidEnchantmentLevel();
            int maxLevel = this.getLiquidMaxLevel();
            int delta = scrollY > 0 ? 1 : -1;
            int target = Math.clamp(current + delta, 0, maxLevel);
            if (target != current) {
                this.menu.setLiquidLevel(target);
                ClientPacketDistributor.sendToServer(new AutoEnchantingTableLevelPacket(target));
            }
            return true;
        }
        if (!this.scrollable.canScroll()) return false;
        this.scrollable.scrollOnScroll(scrollY / 1.2);
        return true;
    }

    private boolean canSelect(int index, EnchantmentData data) {
        int newTotal = this.getSelectedTotalLevel() + data.level();
        // 附魔总等级不能超过书架数量
        if (newTotal > this.menu.getBlockEntity().getShelfLevel()) {
            this.showWarning(index, Component.translatable("screen.anvilcraft.auto_enchanting_table.warning.bookshelf"));
            return false;
        }
        // 消耗经验流体不能超过罐内最大储量
        if (newTotal * AutoEnchantingTableBlockEntity.EXP_COST_PER_SHELF
            > AutoEnchantingTableBlockEntity.FLUID_CAPACITY) {
            this.showWarning(index,
                Component.translatable("screen.anvilcraft.auto_enchanting_table.warning.fluid_capacity"));
            return false;
        }
        return true;
    }

    private int getSelectedTotalLevel() {
        int total = 0;
        for (int i : this.menu.getSelectedIndexes()) {
            EnchantmentData data = ListUtil.safelyGet(this.menu.getEnchantments(), i).orElse(null);
            if (data != null) total += data.level();
        }
        return total;
    }

    /**
     * 计算引物模式下即将输出物品的预览（服务端条件全部满足时才返回，否则为空）。
     */
    private @Nullable ItemStack computeGhostOutput() {
        this.primerResultEmpty = false;
        // 液态魔咒模式下待附魔物品直接位于输出槽，无需虚影预览
        if (this.isLiquidEnchantmentMode()) return null;
        AutoEnchantingTableBlockEntity be = this.menu.getBlockEntity();
        ItemStack primer = be.getItem(AutoEnchantingTableBlockEntity.SLOT_PRIMER);
        if (primer.isEmpty() || !be.isAllowedPrimer(primer)) return null;
        ItemStack input = this.menu.getSlot(AutoEnchantingTableBlockEntity.SLOT_INPUT).getItem();
        if (input.isEmpty()) return null;
        if (!this.menu.getSlot(AutoEnchantingTableBlockEntity.SLOT_OUTPUT).getItem().isEmpty()) return null;
        // 输入已有附魔时直接透传预览（不消耗经验流体，无需选择附魔）
        if (EnchantmentHelper.hasAnyEnchantments(input)) return input.copy();
        List<Holder<Enchantment>> selected = this.getSelectedEnchantments();
        if (selected.isEmpty()) return null;
        int totalLevel = 0;
        for (Holder<Enchantment> holder : selected) {
            totalLevel += holder.value().getMaxLevel();
        }
        if (totalLevel > be.getShelfLevel()) return null;
        int cost = totalLevel * AutoEnchantingTableBlockEntity.EXP_COST_PER_SHELF;
        if (cost <= 0 || cost > AutoEnchantingTableBlockEntity.FLUID_CAPACITY) return null;
        FluidStack fluid = be.getFluid();
        if (!fluid.is(ModFluids.EXP_FLUID) || fluid.getAmount() < cost) return null;
        ItemStack result = AutoEnchantingTableBlockEntity.computePrimerEnchantResult(input, selected);
        this.primerResultEmpty = result.isEmpty();
        return result;
    }

    private List<Holder<Enchantment>> getSelectedEnchantments() {
        List<Holder<Enchantment>> selected = new ArrayList<>();
        for (int i : this.menu.getSelectedIndexes()) {
            ListUtil.safelyGet(this.menu.getEnchantments(), i).ifPresent(data -> selected.add(data.enchantment()));
        }
        return selected;
    }

    private void showWarning(int index, Component message) {
        this.warningIndex = index;
        this.warningMessage = message;
        this.warningTicks = WARNING_DURATION_TICKS;
    }

    protected boolean insideSearchBox(double mouseX, double mouseY) {
        int left = this.leftPos + 46;
        int top = this.topPos + 17;
        int right = left + 99;
        int down = top + 12;
        return MathUtil.isInRange(mouseX, mouseY, left, top, right, down);
    }

    protected boolean insideBookshelf(double mouseX, double mouseY) {
        int left = this.leftPos + 27;
        int top = this.topPos + 55;
        int right = left + 16;
        int down = top + 16;
        return MathUtil.isInRange(mouseX, mouseY, left, top, right, down);
    }

    protected boolean insideScrollbar(double mouseX, double mouseY) {
        int left = this.leftPos + 140;
        int top = this.topPos + 32;
        int right = left + 4;
        int down = top + 36;
        return MathUtil.isInRange(mouseX, mouseY, left, top, right, down);
    }
}
