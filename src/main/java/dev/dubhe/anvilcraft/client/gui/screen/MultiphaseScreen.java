package dev.dubhe.anvilcraft.client.gui.screen;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import dev.dubhe.anvilcraft.api.item.property.Merciless;
import dev.dubhe.anvilcraft.api.item.property.Multiphase;
import dev.dubhe.anvilcraft.client.gui.component.WheelWidget;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.network.MultiphaseChangePacket;
import dev.dubhe.anvilcraft.util.ListUtil;
import dev.dubhe.anvilcraft.util.function.Consumer4;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MultiphaseScreen extends Screen {
    private final LocalPlayer player = Objects.requireNonNull(Minecraft.getInstance().player);
    private final InteractionHand hand;
    private final LinkedList<Multiphase.Phase> phasesCopy = new LinkedList<>();

    public WheelWidget wheel;

    public MultiphaseScreen(InteractionHand hand) {
        super(Component.translatable("screen.anvilcraft.multiphase.title"));
        this.hand = hand;
        if (!player.getItemInHand(hand).has(ModComponents.MULTIPHASE)) this.onClose();
    }

    @Override
    protected void init() {
        int leftPos = (this.width - 75) / 2;
        int topPos = (this.height - 75) / 2;
        ItemStack holding = player.getItemInHand(this.hand);
        if (this.phasesCopy.isEmpty()) {
            this.phasesCopy.addAll(holding.getOrDefault(ModComponents.MULTIPHASE, Multiphase.EMPTY).phases());
        }
        boolean hasMerciless = holding.has(ModComponents.MERCILESS);
        int size = this.phasesCopy.size();
        List<Pair<Component, Consumer4<GuiGraphics, PoseStack, Integer, Integer>>> sections = Lists.reverse(createWheelSections(
            size, this.phasesCopy, holding, false));
        if (hasMerciless) {
            sections.addAll(createWheelSections(size, this.phasesCopy, holding, true));
        }
        WheelWidget wheel = new WheelWidget(
            leftPos, topPos, 75, 75,
            12.5f, 32.5f, 0.75f, -180f / size / 2,
            sections
        ).setCurrentIndex(this.wheel != null ? this.wheel.getCurrentSectionIndex() : size - 1);
        this.clearWidgets();
        this.wheel = this.addRenderableWidget(wheel);
    }

    private static @NotNull List<Pair<Component, Consumer4<GuiGraphics, PoseStack, Integer, Integer>>> createWheelSections(
        int size, List<Multiphase.Phase> phasesCopy, ItemStack holding, boolean hasMerciless
    ) {
        return ListUtil.createWithValues(size, i -> new Pair<>(phasesCopy.get(i), renderHolding(holding, phasesCopy, i, hasMerciless)))
            .stream()
            .sorted(Comparator.comparingInt(pair -> pair.getFirst().index()))
            .<Pair<Component, Consumer4<GuiGraphics, PoseStack, Integer, Integer>>>map(pair -> new Pair<>(
                !hasMerciless ? pair.getFirst().phaseName().copy()
                    : pair.getFirst().phaseName().copy()
                    .append(Component.translatable("screen.anvilcraft.multiphase.merciless")),
                pair.getSecond()))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private static Consumer4<GuiGraphics, PoseStack, Integer, Integer> renderHolding(
        ItemStack holding, List<Multiphase.Phase> phasesCopy, int index, boolean hasMerciless
    ) {
        return (graphics, pose, width, height) -> {
            ItemStack stack = holding.copy();
            phasesCopy.get(index % phasesCopy.size()).applyToStack(stack);
            stack.set(ModComponents.MERCILESS, new Merciless(hasMerciless));
            graphics.renderItem(stack, 2, 2, 9910597);
        };
    }

    @Override
    public void removed() {
        super.removed();
        int index = this.wheel.getCurrentSectionIndex();
        index = index < this.phasesCopy.size() ? this.phasesCopy.size() - 1 - index : index - this.phasesCopy.size();
        PacketDistributor.sendToServer(new MultiphaseChangePacket(
            this.hand, (index - Objects.requireNonNull(this.phasesCopy.peek()).index() + this.phasesCopy.size()) % this.phasesCopy.size(),
            this.wheel.getCurrentSectionIndex() >= this.phasesCopy.size()));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        for (Renderable renderable : this.renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
