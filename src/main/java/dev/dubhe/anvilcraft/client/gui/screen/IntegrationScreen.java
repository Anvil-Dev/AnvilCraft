package dev.dubhe.anvilcraft.client.gui.screen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.IntegrationUtil;
import dev.dubhe.anvilcraft.util.ModEventUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

import java.net.URI;
import java.util.List;
import javax.annotation.Nullable;

public class IntegrationScreen extends Screen {
    public static final Component TITLE = Component.translatable(
        "screen.anvilcraft.integration_screen.title"
    );
    public static final Component CATEGORY_GUIDE = Component.translatable(
        "screen.anvilcraft.integration_screen.category.guide"
    );
    public static final Component CATEGORY_RECIPE_QUERY = Component.translatable(
        "screen.anvilcraft.integration_screen.category.recipe_query"
    );
    public static final Component CATEGORY_INFO_HUD = Component.translatable(
        "screen.anvilcraft.integration_screen.category.info_hud"
    );
    public static final Component CATEGORY_MODIFY = Component.translatable(
        "screen.anvilcraft.integration_screen.category.modify"
    );
    public static final Component CATEGORY_INTERACTION = Component.translatable(
        "screen.anvilcraft.integration_screen.category.interaction"
    );
    public static final Component CATEGORY_COMPATIBLE = Component.translatable(
        "screen.anvilcraft.integration_screen.category.compatible"
    );
    public static final Component CATEGORY_ADDITIONAL = Component.translatable(
        "screen.anvilcraft.integration_screen.category.additional"
    );
    final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    @Nullable
    private IntegrationList integrationList;
    private final Screen lastScreen;

    public IntegrationScreen(@Nullable Screen screen) {
        super(IntegrationScreen.TITLE);
        this.lastScreen = screen;
    }

    @Override
    protected void init() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
        this.layout.addTitleHeader(TITLE, this.font);
        this.integrationList = this.layout.addToContents(new IntegrationList());
        LinearLayout linearlayout = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        linearlayout.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).build());
        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        if (this.integrationList != null) {
            this.integrationList.updateSize(this.width, this.layout);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null && this.lastScreen != null) {
            this.minecraft.setScreen(this.lastScreen);
        } else {
            super.onClose();
        }
    }

    public abstract class AbstractIntegrationEntry extends ContainerObjectSelectionList.Entry<AbstractIntegrationEntry> {
        final List<FormattedCharSequence> tooltip;

        public AbstractIntegrationEntry(@Nullable List<FormattedCharSequence> tooltip) {
            this.tooltip = tooltip;
        }

        public void renderToolTip() {
            if (this.tooltip == null) return;
            IntegrationScreen.this.setTooltipForNextRenderPass(this.tooltip);
        }
    }

    public class BaseIntegrationEntry extends AbstractIntegrationEntry {
        private final boolean hasExtra;
        private final IntegrationUtil.LoadStatus status;
        private final List<FormattedCharSequence> label;
        protected final List<ImageButton> children = Lists.newArrayList();
        protected final List<ImageButton> targetButtons = Lists.newArrayList();
        protected final List<ImageButton> extraButtons = Lists.newArrayList();

        public BaseIntegrationEntry(
            String id,
            boolean hasExtra,
            Component name,
            @Nullable Component description,
            IntegrationUtil.Links links
        ) {
            super(description == null ? null : Minecraft.getInstance().font.split(description, 175));
            this.hasExtra = hasExtra;
            this.status = ModEventUtil.checkIntegration(id, this.hasExtra);
            this.label = Minecraft.getInstance().font.split(name, 175);
            links.target().forEach(target -> {
                ImageButton button = this.createButton(target.type(), target.url());
                this.targetButtons.add(button);
                this.children.add(button);
            });
            links.extra().forEach(extra -> {
                ImageButton button = this.createButton(extra.type(), extra.url());
                this.extraButtons.add(button);
                this.children.add(button);
            });
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return this.children;
        }

        protected void renderLabel(GuiGraphics guiGraphics, int x, int y, int width) {
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();
            pose.translate(x, y, 0);
            pose.scale(1.25f, 1.25f, 1.0f);
            Minecraft minecraft = Minecraft.getInstance();
            guiGraphics.drawString(minecraft.font, this.label.getFirst(), 0, 0, -1, false);
            pose.popPose();
            pose.pushPose();
            pose.translate(x + width, y + 2, 0);
            pose.scale(0.8f, 0.8f, 1.0f);
            Component component = switch (this.status) {
                case LOADED -> Component.translatable("screen.anvilcraft.integration_screen.loaded").withStyle(ChatFormatting.GREEN);
                case NOT_LOADED -> Component.translatable("screen.anvilcraft.integration_screen.not_loaded").withStyle(ChatFormatting.RED);
                case NOT_FOUND -> Component.translatable("screen.anvilcraft.integration_screen.not_found").withStyle(ChatFormatting.YELLOW);
            };
            pose.translate(-minecraft.font.width(component), 0, 0);
            guiGraphics.drawString(minecraft.font, component, 0, 0, -1, false);
            pose.popPose();
        }

        public ImageButton createButton(String type, String url) {
            MutableComponent component = Component.translatable("screen.anvilcraft.integration_screen.url." + type);
            ImageButton imageButton = new ImageButton(
                18,
                18,
                new WidgetSprites(
                    AnvilCraft.of("widget/integration_screen/" + type),
                    AnvilCraft.of("widget/integration_screen/" + type + "_highlighted")
                ),
                button -> {
                    try {
                        URI uri = Util.parseAndValidateUntrustedUri(url);
                        if (Minecraft.getInstance().options.chatLinksPrompt().get()) {
                            Minecraft.getInstance().setScreen(new ConfirmLinkScreen(
                                flag -> {
                                    if (flag) {
                                        Util.getPlatform().openUri(uri);
                                    }
                                    Minecraft.getInstance().setScreen(IntegrationScreen.this);
                                }, url, false
                            ));
                        } else {
                            Util.getPlatform().openUri(uri);
                        }
                    } catch (Exception e) {
                        AnvilCraft.LOGGER.error(e.getMessage(), e);
                    }
                },
                component
            );
            imageButton.setTooltip(Tooltip.create(component));
            return imageButton;
        }

        @Override
        public void render(
            GuiGraphics guiGraphics,
            int index,
            int top,
            int left,
            int width,
            int height,
            int mouseX,
            int mouseY,
            boolean hovering,
            float partialTick
        ) {
            this.renderLabel(guiGraphics, left, top, width);
            int offsetY = this.hasExtra ? 24 : 36;
            guiGraphics.drawString(
                Minecraft.getInstance().font,
                Component.translatable("screen.anvilcraft.integration_screen.target"),
                left,
                top + offsetY,
                -1,
                false
            );
            for (int i = this.targetButtons.size(); i > 0; i--) {
                ImageButton button = this.targetButtons.get(i - 1);
                button.setX(left + width - 19 * (i));
                button.setY(top + offsetY - 9);
                button.render(guiGraphics, mouseX, mouseY, partialTick);
            }
            if (!this.hasExtra) return;
            guiGraphics.drawString(
                Minecraft.getInstance().font,
                Component.translatable("screen.anvilcraft.integration_screen.extra"),
                left,
                top + 48,
                -1,
                false
            );
            for (int i = this.extraButtons.size(); i > 0; i--) {
                ImageButton button = this.extraButtons.get(i - 1);
                button.setX(left + width - 19 * (i));
                button.setY(top + 48 - 9);
                button.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        @Override
        public void renderToolTip() {
            for (ImageButton child : this.children) {
                if (child.isHovered()) return;
            }
            super.renderToolTip();
        }
    }

    public class CategoryIntegrationEntry extends AbstractIntegrationEntry {
        final Component label;

        public CategoryIntegrationEntry(Component label) {
            super(null);
            this.label = label;
        }

        @Override
        public void render(
            GuiGraphics guiGraphics,
            int index,
            int top,
            int left,
            int width,
            int height,
            int mouseX,
            int mouseY,
            boolean hovering,
            float partialTick
        ) {
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();
            pose.translate(left + width / 2.0f, top + height - 36.0f, 0);
            pose.scale(2.0f, 2.0f, 1.0f);
            guiGraphics.drawCenteredString(
                Minecraft.getInstance().font,
                this.label,
                0,
                0,
                -1
            );
            pose.popPose();
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return ImmutableList.of();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return ImmutableList.of(new NarratableEntry() {
                @Override
                public NarrationPriority narrationPriority() {
                    return NarrationPriority.HOVERED;
                }

                @Override
                public void updateNarration(NarrationElementOutput output) {
                    output.add(NarratedElementType.TITLE, CategoryIntegrationEntry.this.label);
                }
            });
        }
    }

    public class IntegrationEntry extends BaseIntegrationEntry {
        public IntegrationEntry(IntegrationUtil.Integration integration) {
            super(
                integration.id(),
                integration.type() == IntegrationUtil.IntegrationType.EXTRA,
                integration.name(),
                integration.description(),
                integration.links()
            );
        }
    }

    public class AdditionalEntry extends BaseIntegrationEntry {
        public AdditionalEntry(IntegrationUtil.Additional integration) {
            super(integration.id(), false, integration.name(), integration.description(), integration.links());
        }
    }

    public class IntegrationList extends ContainerObjectSelectionList<AbstractIntegrationEntry> {
        public IntegrationList() {
            super(
                Minecraft.getInstance(),
                IntegrationScreen.this.width,
                IntegrationScreen.this.layout.getContentHeight(),
                IntegrationScreen.this.layout.getHeaderHeight(),
                72
            );
            IntegrationUtil.Root root = IntegrationUtil.load();
            IntegrationUtil.Integrations integrations = root.integration();
            this.addEntry(new CategoryIntegrationEntry(IntegrationScreen.CATEGORY_GUIDE));
            integrations.guide().forEach(integration -> this.addEntry(new IntegrationEntry(integration)));
            this.addEntry(new CategoryIntegrationEntry(IntegrationScreen.CATEGORY_RECIPE_QUERY));
            integrations.recipeQuery().forEach(integration -> this.addEntry(new IntegrationEntry(integration)));
            this.addEntry(new CategoryIntegrationEntry(IntegrationScreen.CATEGORY_INFO_HUD));
            integrations.infoHud().forEach(integration -> this.addEntry(new IntegrationEntry(integration)));
            this.addEntry(new CategoryIntegrationEntry(IntegrationScreen.CATEGORY_MODIFY));
            integrations.modify().forEach(integration -> this.addEntry(new IntegrationEntry(integration)));
            this.addEntry(new CategoryIntegrationEntry(IntegrationScreen.CATEGORY_INTERACTION));
            integrations.interaction().forEach(integration -> this.addEntry(new IntegrationEntry(integration)));
            this.addEntry(new CategoryIntegrationEntry(IntegrationScreen.CATEGORY_COMPATIBLE));
            integrations.compatible().forEach(integration -> this.addEntry(new IntegrationEntry(integration)));
            this.addEntry(new CategoryIntegrationEntry(IntegrationScreen.CATEGORY_ADDITIONAL));
            root.additional().forEach(integration -> this.addEntry(new AdditionalEntry(integration)));

        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
            AbstractIntegrationEntry entry = this.getHovered();
            if (entry == null || entry.tooltip == null) return;
            entry.renderToolTip();
        }
    }
}
