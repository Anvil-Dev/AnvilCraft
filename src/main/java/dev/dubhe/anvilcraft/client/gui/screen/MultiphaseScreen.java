package dev.dubhe.anvilcraft.client.gui.screen;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import dev.dubhe.anvilcraft.client.gui.component.WheelWidget;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.Merciless;
import dev.dubhe.anvilcraft.network.multiple.MultiphasePackets;
import dev.dubhe.anvilcraft.saved.multiphase.Multiphase;
import dev.dubhe.anvilcraft.util.ListUtil;
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
        this.clearWidgets();
        int leftPos = (this.width - 75) / 2;
        int topPos = (this.height - 75) / 2;
        ItemStack holding = this.player.getItemInHand(this.hand);
        if (this.phasesCopy.isEmpty()) {
            Multiphase multiphase = holding.get(ModComponents.MULTIPHASE).toMultiphase();
            if (multiphase != null) {
                this.phasesCopy.addAll(multiphase.phases());
            }
        }
        boolean hasMerciless = holding.has(ModComponents.MERCILESS);
        int size = this.phasesCopy.size();
        var sections = Lists.reverse(MultiphaseScreen.createWheelSections(size, this.phasesCopy, holding, false));
        if (hasMerciless) {
            sections.addAll(MultiphaseScreen.createWheelSections(size, this.phasesCopy, holding, true));
        }
        WheelWidget wheel = new WheelWidget(
            leftPos,
            topPos,
            75,
            75,
            12.5f,
            32.5f,
            0.75f,
            -180f / size / 2,
            sections
        ).setCurrentIndex(this.wheel != null ? this.wheel.getCurrentSectionIndex() : size - 1);
        this.wheel = this.addRenderableWidget(wheel);
    }

    private static @NotNull List<WheelWidget.RawSection> createWheelSections(
        int size, List<Multiphase.Phase> phasesCopy, ItemStack holding, boolean hasMerciless
    ) {
        var raw = ListUtil.createWithValues(
            size,
            i -> new Pair<>(phasesCopy.get(i), MultiphaseScreen.renderHolding(holding, phasesCopy, i, hasMerciless))
        );
        raw.sort(Comparator.comparingInt(pair -> pair.getFirst().index()));

        List<WheelWidget.RawSection> sections = new ArrayList<>();
        for (var rawPair : raw) {
            sections.add(new WheelWidget.RawSection(
                !hasMerciless
                ? rawPair.getFirst().phaseName().copy()
                : rawPair.getFirst().phaseName().copy().append(Component.translatable("screen.anvilcraft.multiphase.merciless")),
                rawPair.getSecond()
            ));
        }
        return sections;
    }

    private static WheelWidget.SectionRenderer renderHolding(
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
        PacketDistributor.sendToServer(new MultiphasePackets.ChangePhase(
            this.hand,
            index,
            this.wheel.getCurrentSectionIndex() >= this.phasesCopy.size()
        ));
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
