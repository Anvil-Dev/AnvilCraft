package dev.dubhe.anvilcraft.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.anvilcraft.resource.ageratum.client.AgeratumClient;
import dev.anvilcraft.resource.ageratum.client.constants.AgeratumConstants;
import dev.anvilcraft.resource.ageratum.client.gui.GuideScreen;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.markdown.HandbookSidebarState;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mixin(value = GuideScreen.class, remap = false)
abstract class HandbookSidebarMixin implements HandbookSidebarState {
    @Unique private @Nullable Set<Integer> anvilcraft$inheritedCollapsed;
    @Unique private int anvilcraft$rowsBeforeRebuild;
    @Unique private double anvilcraft$remainderBeforeRebuild;
    @Shadow
    @Final
    protected Identifier documentLocation;
    @Shadow
    protected List<Identifier> breadCrumbs;
    @Shadow
    protected List<?> labelEntries;
    @Shadow
    protected List<Integer> visibleLabelIndices;
    @Shadow
    @Final
    protected Set<Integer> collapsedLabelGroups;
    @Shadow
    protected int labelScrollRows;
    @Shadow
    protected int maxLabelScrollRows;
    @Shadow
    protected double labelScrollRemainder;
    @Shadow
    protected int leftPos;
    @Shadow
    protected int topPos;
    @Shadow
    protected int labelWidth;
    @Shadow
    protected int labelHeight;

    @Shadow
    public abstract int getLabelVisibleRows();

    @Shadow
    public abstract int getLabelBaseX();

    @Shadow
    private int getLabelStartY() {
        throw new AssertionError();
    }

    @Shadow
    public abstract int getLabelRowOffset();

    @Shadow
    public abstract int getContentStartX();

    @Shadow
    private void rebuildVisibleLabelIndices() {
        throw new AssertionError();
    }

    @Shadow
    private int findCurrentDocumentLabelIndex() {
        throw new AssertionError();
    }

    @Shadow
    private boolean labelGroupHasChildren(int index) {
        throw new AssertionError();
    }

    @Unique
    private boolean anvilcraft$ownGuide() {
        return this.documentLocation.getNamespace().equals(AnvilCraft.MOD_ID);
    }

    @Unique
    private HandbookLabelAccessor anvilcraft$entry(int index) {
        return (HandbookLabelAccessor) this.labelEntries.get(index);
    }

    @Unique
    private int anvilcraft$parent(int index) {
        if (this.anvilcraft$entry(index).anvilcraft$level() == 1) return -1;
        for (int i = index - 1; i >= 0; i--) {
            if (this.anvilcraft$entry(i).anvilcraft$level() == 1) return i;
        }
        return -1;
    }

    @Unique
    private int anvilcraft$pinned() {
        if (this.visibleLabelIndices.isEmpty()) return -1;
        int row = Mth.clamp(this.labelScrollRows, 0, this.visibleLabelIndices.size() - 1);
        int first = this.visibleLabelIndices.get(row);
        if (this.anvilcraft$entry(first).anvilcraft$level() == 2) return this.anvilcraft$parent(first);
        if (row == 0) return -1;
        int previous = this.visibleLabelIndices.get(row - 1);
        return this.anvilcraft$entry(previous).anvilcraft$level() == 2 ? this.anvilcraft$parent(previous) : previous;
    }

    @Unique
    private int anvilcraft$maxScroll() {
        int rows = Math.max(1, this.getLabelVisibleRows() - (this.anvilcraft$pinned() >= 0 ? 1 : 0));
        return Math.max(0, this.visibleLabelIndices.size() - rows);
    }

    @Unique
    private void anvilcraft$ensureVisible() {
        int index = this.visibleLabelIndices.indexOf(this.findCurrentDocumentLabelIndex());
        if (index < 0) return;
        for (int attempt = 0; attempt < 3; attempt++) {
            int previous = this.labelScrollRows;
            int max = this.anvilcraft$maxScroll();
            this.labelScrollRows = Mth.clamp(this.labelScrollRows, 0, max);
            int rows = Math.max(1, this.getLabelVisibleRows() - (this.anvilcraft$pinned() >= 0 ? 1 : 0));
            if (index < this.labelScrollRows) {
                this.labelScrollRows = Mth.clamp(index, 0, max);
            } else if (index >= this.labelScrollRows + rows) {
                this.labelScrollRows = Mth.clamp(index - rows + 1, 0, max);
            }
            if (previous == this.labelScrollRows) break;
        }
    }

    @Inject(method = "findPinnedParentIndex", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$pinnedParent(CallbackInfoReturnable<Integer> cir) {
        if (this.anvilcraft$ownGuide()) cir.setReturnValue(this.anvilcraft$pinned());
    }

    @Inject(method = "scrollLabelsBy", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$scroll(int delta, CallbackInfo ci) {
        if (!this.anvilcraft$ownGuide()) return;
        this.labelScrollRows = Mth.clamp(this.labelScrollRows + delta, 0, this.anvilcraft$maxScroll());
        ci.cancel();
    }

    @Inject(method = "toggleLabelGroup", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$toggle(int index, CallbackInfo ci) {
        if (!this.anvilcraft$ownGuide()) return;
        this.anvilcraft$toggleGroup(index);
        ci.cancel();
    }

    @Unique
    private void anvilcraft$toggleGroup(int index) {
        if (!this.collapsedLabelGroups.remove(index)) this.collapsedLabelGroups.add(index);
        this.rebuildVisibleLabelIndices();
        this.labelScrollRows = Mth.clamp(this.labelScrollRows, 0, this.anvilcraft$maxScroll());
        this.anvilcraft$ensureVisible();
        this.maxLabelScrollRows = this.anvilcraft$maxScroll();
    }

    @Inject(method = "scrollLabelToCurrentDocument", at = @At("TAIL"))
    private void anvilcraft$revealCurrent(CallbackInfo ci) {
        if (this.anvilcraft$ownGuide()) this.anvilcraft$ensureVisible();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void anvilcraft$bounds(CallbackInfo ci) {
        if (!this.anvilcraft$ownGuide()) return;
        this.maxLabelScrollRows = this.anvilcraft$maxScroll();
        this.labelScrollRows = Mth.clamp(this.labelScrollRows, 0, this.maxLabelScrollRows);
    }

    @Inject(method = "rebuildLabelEntries", at = @At("HEAD"))
    private void anvilcraft$rememberViewport(CallbackInfo ci) {
        this.anvilcraft$rowsBeforeRebuild = this.labelScrollRows;
        this.anvilcraft$remainderBeforeRebuild = this.labelScrollRemainder;
    }

    @Inject(method = "rebuildLabelEntries", at = @At("TAIL"))
    private void anvilcraft$restoreRebuiltSidebar(CallbackInfo ci) {
        if (!this.anvilcraft$ownGuide()) return;
        if (this.anvilcraft$inheritedCollapsed != null) {
            this.anvilcraft$restoreSidebar(this.anvilcraft$inheritedCollapsed,
                this.anvilcraft$rowsBeforeRebuild, this.anvilcraft$remainderBeforeRebuild);
        } else this.anvilcraft$bounds(ci);
    }

    @Inject(method = {"extractLabelRenderState", "extractLabelTooltipRenderState", "extractLabelScrollHintRenderState"},
        at = @At("HEAD"))
    private void anvilcraft$renderBounds(CallbackInfo ci) {
        this.anvilcraft$bounds(ci);
    }

    @ModifyExpressionValue(method = "extractLabelTooltipRenderState", at = @At(value = "INVOKE", target =
        "Ldev/anvilcraft/resource/ageratum/client/gui/GuideScreen;getLabelVisibleRows()I"))
    private int anvilcraft$tooltipRows(int rows) {
        return rows - (this.anvilcraft$ownGuide() && this.anvilcraft$pinned() >= 0 ? 1 : 0);
    }

    @Override
    public void anvilcraft$restoreSidebar(Set<Integer> collapsed, int rows, double remainder) {
        this.anvilcraft$inheritedCollapsed = Set.copyOf(collapsed);
        this.collapsedLabelGroups.clear();
        this.collapsedLabelGroups.addAll(collapsed);
        this.rebuildVisibleLabelIndices();
        this.labelScrollRows = rows;
        this.labelScrollRemainder = remainder;
        this.maxLabelScrollRows = this.anvilcraft$maxScroll();
        this.labelScrollRows = Mth.clamp(rows, 0, this.maxLabelScrollRows);
    }

    @Unique
    private boolean anvilcraft$openEntry(int index) {
        var entry = this.anvilcraft$entry(index);
        boolean parent = entry.anvilcraft$level() == 1 && this.labelGroupHasChildren(index);
        if (parent) this.anvilcraft$toggleGroup(index);
        Identifier location = entry.anvilcraft$location();
        if (!entry.anvilcraft$clickable() || location == null) return parent;
        List<Identifier> crumbs = this.breadCrumbs;
        if (AgeratumClient.CONFIG.breadCrumbsHasLabel && !location.equals(this.documentLocation)) {
            crumbs = new ArrayList<>(crumbs);
            crumbs.add(this.documentLocation);
            crumbs = List.copyOf(crumbs);
        }
        Set<Integer> collapsed = Set.copyOf(this.collapsedLabelGroups);
        int rows = this.labelScrollRows;
        double remainder = this.labelScrollRemainder;
        boolean opened = AgeratumClient.openGuideOnClient(location, crumbs);
        if (opened && Minecraft.getInstance().screen instanceof HandbookSidebarState next) {
            next.anvilcraft$restoreSidebar(collapsed, rows, remainder);
        }
        return opened;
    }

    @Unique
    private boolean anvilcraft$hit(double x, double y, int index, int row) {
        int indent = this.anvilcraft$entry(index).anvilcraft$level() == 2
            ? AgeratumConstants.GuideScreenUI.Layout.LABEL_LEVEL2_INDENT : 0;
        int left = this.getLabelBaseX() + indent;
        int top = this.getLabelStartY() + row * this.getLabelRowOffset();
        return x >= left && x < left + this.labelWidth && y >= top && y < top + this.labelHeight;
    }

    @Inject(method = "tryOpenLabelAt", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$click(double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
        if (!this.anvilcraft$ownGuide()) return;
        double x = Math.floor(mouseX - this.leftPos);
        double y = Math.floor(mouseY - this.topPos);
        if (x >= this.getContentStartX()) {
            cir.setReturnValue(false);
            return;
        }
        int pinned = this.anvilcraft$pinned();
        int pinnedRows = pinned >= 0 ? 1 : 0;
        if (pinned >= 0 && this.anvilcraft$hit(x, y, pinned, 0)) {
            cir.setReturnValue(this.anvilcraft$openEntry(pinned));
            return;
        }
        int start = Mth.clamp(this.labelScrollRows, 0, this.anvilcraft$maxScroll());
        int end = Math.min(this.visibleLabelIndices.size(), start + this.getLabelVisibleRows() - pinnedRows);
        for (int i = start; i < end; i++) {
            int index = this.visibleLabelIndices.get(i);
            if (index != pinned && this.anvilcraft$hit(x, y, index, i - start + pinnedRows)) {
                cir.setReturnValue(this.anvilcraft$openEntry(index));
                return;
            }
        }
        cir.setReturnValue(false);
    }
}
