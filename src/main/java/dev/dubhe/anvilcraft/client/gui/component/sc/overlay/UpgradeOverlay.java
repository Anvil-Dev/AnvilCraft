package dev.dubhe.anvilcraft.client.gui.component.sc.overlay;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.api.sc.category.CategoryMode;
import dev.dubhe.anvilcraft.api.sc.category.hidden.UpgradeCategory;
import dev.dubhe.anvilcraft.api.sc.category.provider.CategoryProvider;
import dev.dubhe.anvilcraft.api.sc.upgrade.Upgrade;
import dev.dubhe.anvilcraft.api.sc.upgrade.UpgradeResult;
import dev.dubhe.anvilcraft.api.sc.upgrade.Upgrades;
import dev.dubhe.anvilcraft.client.gui.component.TexturedButton;
import dev.dubhe.anvilcraft.client.gui.screen.ShulkerContainerScreen;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.constant.TextureConstants;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class UpgradeOverlay extends BaseOverlay {
    private Map<CategoryProvider, CategoryMode> prev;
    private final Upgrade<?>[] upgrades = new Upgrade[3];

    public UpgradeOverlay(ShulkerContainerScreen screen) {
        super(screen);

        screen.getMenu().addUpgradeSlots();

        var builder = ImmutableMap.<CategoryProvider, CategoryMode>builder();
        for (
            Map.Entry<CategoryProvider, CategoryMode> entry
            : this.storage().getClientCategories().getCategories().entrySet()
        ) {
            builder.put(entry.getKey(), entry.getValue());
        }
        this.prev = builder.build();

        this.storage().getClientCategories().getCategories().clear();
        this.storage().getClientCategories().getCategories().put(new CategoryProvider(new UpgradeCategory()), CategoryMode.WHITELIST);

        Upgrades upgrades = this.storage().getUpgrades();
        this.upgrades[0] = upgrades.getEntryLimitUpgrade();
        this.upgrades[1] = upgrades.getStackPowerUpgrade();
        this.upgrades[2] = upgrades.getTransferUpgrade();

        this.addRenderableWidget(new TexturedButton(
            this.getGuiLeft() + 2,
            this.getGuiTop() + 198,
            102,
            20,
            TextureConstants.SHULKER_CONTAINER_UPGRADE_BACK,
            20,
            102,
            40,
            button -> screen.changeOverlay(new MainOverlay(screen))
        ));
    }

    @Override
    public BaseOverlay recreate() {
        var overlay = new UpgradeOverlay(this.screen);
        overlay.prev = prev;
        return overlay;
    }

    @Override
    public ResourceLocation bg() {
        return TextureConstants.SHULKER_CONTAINER_UPGRADE_BG;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        final var pose = graphics.pose();

        final int left = this.getGuiLeft() + 9;
        for (int i = 0; i < 3; i++) {
            final int top = this.getGuiTop() + 14 + i * 54;
            final var upgrade = this.upgrades[i];
            final var now = upgrade.getNow();
            final @Nullable var next = upgrade.getNext();
            pose.pushPose();
            pose.translate(left, top, 0);

            // 材料
            if (!this.screen.getMenu().getSlot(90 + i).hasItem()) {
                ItemStack material = Items.BARRIER.getDefaultInstance();
                if (next != null) {
                    List<ItemStack> materials = next.getMaterial();
                    material = materials
                        .get((int) ((System.currentTimeMillis() / 1000) % materials.size()))
                        .copyWithCount(next.getConsumedCount() - upgrade.getProgress());
                }
                this.renderTransparentItem(graphics, pose, material, 4, 4);
                if (!material.is(Items.BARRIER) && this.insideMaterial(top, mouseX, mouseY)) this.setTooltip(material);
            }

            // 进度
            int progress;
            if (next != null) {
                progress = now.ordinal() * 16 + (int) Math.floor(16 * ((double) upgrade.getProgress() / next.getConsumedCount()));
            } else {
                progress = 64;
            }
            graphics.blit(
                TextureConstants.SHULKER_CONTAINER_UPGRADE_PROGRESS,
                24,
                2,
                0,
                0,
                progress,
                8,
                64,
                8
            );

            // 确认按钮
            UpgradeResult result = upgrade.canUpgrade(
                this.minecraft().player,
                this.screen.getMenu().getSlot(90 + i).getItem()
            );
            if (this.insideConfirmButton(top, mouseX, mouseY)) this.setTooltip(result.getDesc());
            int offsetV = 0;
            if (result == UpgradeResult.CAN_UPGRADE && this.insideConfirmButton(top, mouseX, mouseY)) offsetV = 20;
            graphics.blit(
                TextureConstants.SHULKER_CONTAINER_UPGRADE_CONFIRM,
                0,
                27,
                0,
                offsetV,
                24,
                20,
                24,
                40
            );

            // 工具虚影
            List<ItemStack> tools = Objects.requireNonNullElse(next, now).getTool();
            var tool = tools
                .get((int) ((System.currentTimeMillis() / 1000) % tools.size()))
                .copyWithCount(1);
            this.renderTransparentItem(graphics, pose, tool, 4, 28);

            // 文本描述
            pose.translate(25, 11, 10);

            pose.pushPose();
            pose.scale(0.75F, 0.75F, 1);
            graphics.drawCenteredString(this.minecraft().font, now.getTypeName(), 41, 5, 0xFFFFFF);
            pose.popPose();

            pose.translate(0, 15, 10);
            pose.pushPose();
            pose.scale(0.5F, 0.5F, 1);
            var y = 0;
            for (
                FormattedCharSequence charSequence
                : this.minecraft().font.split(
                    Component.translatable("screen.anvilcraft.shulker_container.upgrade.now", now.getDesc()),
                    120
            )) {
                graphics.drawString(this.minecraft().font, charSequence, 2, y, 0xFFFFFF, true);
                y += 9;
            }
            y += 9;
            for (
                FormattedCharSequence charSequence
                : this.minecraft().font.split(
                    next == null
                    ? Component.translatable("screen.anvilcraft.shulker_container.upgrade.no_next")
                    : Component.translatable("screen.anvilcraft.shulker_container.upgrade.next", next.getDesc()),
                    120
            )) {
                graphics.drawString(this.minecraft().font, charSequence, 2, y, 0xFFFFFF, true);
                y += 9;
            }
            pose.popPose();

            pose.popPose();
        }
        ItemStack share = Items.BARRIER.getDefaultInstance();
        if (this.screen.getMenu().share.mayPlace(ModBlocks.SINGULARITY_CRYSTAL.asStack())) share = ModBlocks.SINGULARITY_CRYSTAL.asStack();
        this.renderTransparentItem(graphics, pose, share, this.getGuiLeft() + 13, this.getGuiTop() + 174);
    }

    @Override
    public boolean whenClick(double mouseX, double mouseY, int button) {
        for (int i = 0; i < 3; i++) {
            final int top = this.getGuiTop() + 14 + i * 54;
            final var upgrade = this.upgrades[i];

            Slot slot = this.screen.getMenu().getSlot(90 + i);
            ItemStack material = slot.getItem().copy();
            if (this.insideConfirmButton(top, mouseX, mouseY)) {
                ItemStack remain = upgrade.upgrade(this.minecraft().player, material);
                if (remain.getCount() != material.getCount()) {
                    slot.remove(material.getCount() - remain.getCount());
                }
                return remain.getCount() != material.getCount();
            }
        }
        return false;
    }

    @Override
    public void onClose() {
        this.storage().getClientCategories().getCategories().clear();
        this.storage().getClientCategories().getCategories().putAll(this.prev);

        this.screen.getMenu().removeUpgradeSlots();
    }

    private boolean insideMaterial(int top, int mouseX, int mouseY) {
        int left = this.getGuiLeft() + 13;
        top = top + 4;
        int right = left + 16;
        int bottom = top + 16;
        return mouseX >= (double) left
               && mouseY >= (double) top
               && mouseX < (double) right
               && mouseY < (double) bottom;
    }

    private boolean insideConfirmButton(int top, double mouseX, double mouseY) {
        int left = this.getGuiLeft() + 9;
        top = top + 28;
        int right = left + 24;
        int bottom = top + 20;
        return mouseX >= (double) left
               && mouseY >= (double) top
               && mouseX < (double) right
               && mouseY < (double) bottom;
    }

    private void renderTransparentItem(GuiGraphics graphics, PoseStack pose, ItemStack stack, int x, int y) {
        RenderSupport.renderItemWithTransparency(stack, pose, x, y, 0x55);
        if (stack.getCount() != 1) {
            pose.pushPose();
            String s = String.valueOf(stack.getCount());
            pose.translate(0.0F, 0.0F, 200.0F);
            graphics.drawString(this.minecraft().font, s, x + 19 - 2 - this.minecraft().font.width(s), y + 6 + 3, 0xFFFFFF, true);
            pose.popPose();
        }
    }

    @Override
    protected boolean clicked(double mouseX, double mouseY) {
        int left = this.getGuiLeft() + 13;
        int top = this.getGuiTop() + 18;
        int right = left + 16;
        int bottom = top + 16;
        if (mouseX >= (double) left
            && mouseY >= (double) top
            && mouseX < (double) right
            && mouseY < (double) bottom
        ) {
            return false;
        }

        top += 54;
        bottom = top + 16;
        if (mouseX >= (double) left
            && mouseY >= (double) top
            && mouseX < (double) right
            && mouseY < (double) bottom
        ) {
            return false;
        }

        top += 54;
        bottom = top + 16;
        if (mouseX >= (double) left
            && mouseY >= (double) top
            && mouseX < (double) right
            && mouseY < (double) bottom
        ) {
            return false;
        }

        top += 48;
        bottom = top + 16;
        if (mouseX >= (double) left
            && mouseY >= (double) top
            && mouseX < (double) right
            && mouseY < (double) bottom
        ) {
            return false;
        }

        left += 40;
        right = top + 16;
        if (mouseX >= (double) left
            && mouseY >= (double) top
            && mouseX < (double) right
            && mouseY < (double) bottom
        ) {
            return false;
        }

        return super.clicked(mouseX, mouseY);
    }
}
