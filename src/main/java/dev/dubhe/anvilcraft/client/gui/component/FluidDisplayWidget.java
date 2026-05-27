package dev.dubhe.anvilcraft.client.gui.component;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.util.TriConsumer;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public class FluidDisplayWidget extends AbstractWidget {
    @Getter
    private final IFluidHandler fluidHandler;
    private final TriConsumer<Double, Double, Integer> onClick;

    public FluidDisplayWidget(
        int x, int y,
        int width, int height,
        IFluidHandler fluidHandler,
        Function<IFluidHandler, Component> message,
        TriConsumer<Double, Double, Integer> onClick
    ) {
        super(x, y, width, height, message.apply(fluidHandler));
        this.fluidHandler = fluidHandler;
        this.onClick = onClick;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation fluidTexture = getFluidTexture();
        if (fluidTexture != null) {
            guiGraphics.blit(
                fluidTexture,
                this.getX(), this.getY() + this.height - getCapacity(),
                0, 0,
                this.width, getCapacity(),
                16, 256
            );
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    @Override
    public boolean isValidClickButton(int button) {
        return button == 1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.active && this.visible) {
            if (this.isValidClickButton(button)) {
                boolean flag = this.clicked(mouseX, mouseY);
                if (flag) {
                    this.onClick(mouseX, mouseY, button);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        this.onClick.accept(mouseX, mouseY, button);
    }

    @Nullable
    private ResourceLocation getFluidTexture() {
        FluidStack fluidStack = this.fluidHandler.getFluidInTank(0);
        if (fluidStack.isEmpty()) {
            return null;
        }
        AtomicReference<ResourceLocation> texture = new AtomicReference<>();
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            Optional<Holder.Reference<Fluid>> holder = level.holder(Objects.requireNonNull(fluidStack.getFluidHolder().getKey()));
            holder.ifPresent((fluid) -> {
                String registeredName = fluid.getRegisteredName().split(":")[1];
                texture.set(AnvilCraft.of("textures/block/" + registeredName + ".png"));
            });
        }
        return texture.get();
    }

    private int getCapacity() {
        if (this.fluidHandler.getFluidInTank(0).isEmpty()) {
            return 0;
        }
        final int stored = this.fluidHandler.getFluidInTank(0).getAmount();
        if (stored > 0 && stored < this.fluidHandler.getTankCapacity(0) / this.height) {
            return 1;
        } else if (
            stored < this.fluidHandler.getTankCapacity(0)
            && stored > (this.fluidHandler.getTankCapacity(0) / this.height) * this.height - 1
        ) {
            return this.height - 1;
        } else if (stored >= this.fluidHandler.getTankCapacity(0)) {
            return this.height;
        }
        return (stored * this.height / this.fluidHandler.getTankCapacity(0));
    }
}
