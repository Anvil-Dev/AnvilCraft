package dev.dubhe.anvilcraft.integration.ageratum.component;

import dev.anvilcraft.lib.v2.font.AnvilLibFont;
import dev.anvilcraft.lib.v2.font.extension.GuiGraphicsExtractorExtension;
import dev.anvilcraft.resource.ageratum.client.constants.AgeratumConstants;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDDocument;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDBlockComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDHeaderComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import java.net.URI;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MDDirectoryComponent extends MDComponent {
    public MDDirectoryComponent() {
        super(FormattedText.EMPTY);
    }

    public static MDDocument expand(MDDocument document) {
        if (document.components().stream().noneMatch(MDDirectoryComponent.class::isInstance)) return document;
        List<Heading> headings = new ArrayList<>();
        for (MDComponent component : document.components()) {
            if (component instanceof MDHeaderComponent header) {
                String title = header.getText().getString().trim();
                if (!title.isEmpty()) headings.add(new Heading(Math.max(0, header.getLevel() - 1), title));
            }
        }
        List<MDComponent> components = new ArrayList<>();
        for (MDComponent component : document.components()) {
            if (component instanceof MDDirectoryComponent) {
                components.add(new MDHeaderComponent(2, "目录"));
                if (!headings.isEmpty()) components.add(new DirectoryList(headings));
            } else components.add(component);
        }
        return new MDDocument(document.sourceLocation(), document.frontMatter(), components);
    }

    private static String anchor(String title) {
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        StringBuilder anchor = new StringBuilder(normalized.length());
        boolean separator = false;
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            if (Character.isLetterOrDigit(current)) {
                anchor.append(current);
                separator = false;
            } else if (Character.isWhitespace(current) || current == '-' || current == '_') {
                if (!separator && !anchor.isEmpty()) {
                    anchor.append('-');
                    separator = true;
                }
            }
        }
        while (!anchor.isEmpty() && anchor.charAt(anchor.length() - 1) == '-') anchor.deleteCharAt(anchor.length() - 1);
        return anchor.toString();
    }

    private record Heading(int level, String title) {
        private FormattedText link() {
            Style style = Style.EMPTY.withUnderlined(true).withColor(AgeratumConstants.GuideScreenUI.Colors.LINK_COLOR)
                .withClickEvent(new ClickEvent.OpenUrl(URI.create("#" + anchor(this.title))));
            return MDComponent.textFormat(this.title, style);
        }
    }

    private static final class DirectoryList extends MDBlockComponent<Heading> {
        private static final String[] BULLETS = {"●", "○", "■", "□", "◆", "◇", "▸", "▹"};

        private DirectoryList(List<Heading> headings) {
            this(headings.stream().map(heading -> new CachedItem<>(heading.level(), heading, heading.link())).toList(), true);
        }

        private DirectoryList(List<CachedItem<Heading>> items, boolean prepared) {
            super(composeBlockText(items), items);
        }

        @Override
        protected int getTextX(CachedItem<Heading> item) {
            return item.level() * 10 + 12;
        }

        @Override
        protected void extractDecorationRenderState(
            GuiGraphicsExtractor graphics, Minecraft minecraft, CachedItem<Heading> item, int y, int lineHeight, int maxX
        ) {
            for (int level = 0; level <= item.level(); level++) {
                graphics.fill(level * 10, y, level * 10 + 9, y + lineHeight,
                    LEVEL_LINE_COLORS[level % LEVEL_LINE_COLORS.length] | 0x55000000);
            }
            ((GuiGraphicsExtractorExtension) graphics).anvillib$text(AnvilLibFont.getSelectFont(), BULLETS[item.level() % BULLETS.length],
                item.level() * 10, y, 0xFF000000, false);
        }
    }
}
