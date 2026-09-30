package dev.dubhe.anvilcraft.porting;

import dev.anvilcraft.resource.ageratum.client.GuideBookmarkStore;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDDocument;
import dev.anvilcraft.resource.ageratum.client.gui.GuideScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

import java.util.List;
import java.util.Map;

public final class HandbookStyleProbe {
    private static List<GuideBookmarkStore.BookmarkEntry> original = List.of();
    private static List<GuideBookmarkStore.BookmarkEntry> bookmarks;
    private static String namespace;

    public static Map<String, Integer> begin(Minecraft client, MDDocument first, MDDocument second) {
        var screen = new ProbeScreen(first);
        client.setScreen(screen);
        bookmarks = screen.bookmarks();
        original = List.copyOf(bookmarks);
        namespace = first.sourceLocation().getNamespace();
        try {
            bookmarks.clear();
            screen.click(screen.coordinate("getAddButtonX") + 16, screen.coordinate("getAddButtonY") + 16);
            if (bookmarks.size() != 1 || !bookmarks.getFirst().location().equals(first.sourceLocation())) {
                throw new IllegalStateException("Add-bookmark button did not target the current document");
            }
            var next = new ProbeScreen(second);
            client.setScreen(next);
            next.click(next.coordinate("getBookmarkBaseX") + 96, next.coordinate("getBookmarkStartY") + 8);
            if (!(client.screen instanceof GuideScreen) || !location(client.screen).equals(first.sourceLocation())) {
                throw new IllegalStateException("Visible bookmark click did not navigate to its document");
            }
            return Map.of("contentWidth", screen.getContentWidth(), "labelWidth", screen.labelWidth(),
                "contentHeight", screen.getContentHeight(), "bookmarks", bookmarks.size());
        } catch (RuntimeException exception) {
            restore();
            throw exception;
        }
    }

    private static Object location(Object screen) {
        try {
            var field = GuideScreen.class.getDeclaredField("documentLocation");
            field.setAccessible(true);
            return field.get(screen);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static void restore() {
        if (bookmarks == null) return;
        bookmarks.clear();
        bookmarks.addAll(original);
        GuideBookmarkStore.save(namespace, bookmarks);
        bookmarks = null;
    }

    private static final class ProbeScreen extends GuideScreen {
        private ProbeScreen(MDDocument document) {
            super(document.sourceLocation(), document, List.of(), false);
        }

        private Object invoke(String name) {
            try {
                var method = GuideScreen.class.getDeclaredMethod(name);
                method.setAccessible(true);
                return method.invoke(this);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(exception);
            }
        }

        @SuppressWarnings("unchecked")
        private List<GuideBookmarkStore.BookmarkEntry> bookmarks() {
            return (List<GuideBookmarkStore.BookmarkEntry>) this.invoke("getBookmarks");
        }

        private int coordinate(String name) {
            return (int) this.invoke(name);
        }

        private int labelWidth() {
            return this.labelWidth;
        }

        private void click(int x, int y) {
            if (!this.mouseClicked(new MouseButtonEvent((this.leftPos + x) / this.scale, (this.topPos + y) / this.scale,
                new MouseButtonInfo(0, 0)), false)) {
                throw new IllegalStateException("Guide control rejected click");
            }
        }
    }
}
