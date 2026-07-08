package dev.dubhe.anvilcraft.client.gui.component;

import dev.dubhe.anvilcraft.AnvilCraft;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class FluidDisplayWidget extends AbstractWidget {
    @Getter
    private final FluidStacksResourceHandler fluidHandler;

    public FluidDisplayWidget(
        int x,
        int y,
        int width,
        int height,
        FluidStacksResourceHandler fluidHandler,
        Function<FluidStacksResourceHandler, Component> message
    ) {
        super(x, y, width, height, message.apply(fluidHandler));
        this.fluidHandler = fluidHandler;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        Identifier fluidTexture = getFluidTexture();
        if (fluidTexture != null) {
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                fluidTexture,
                this.getX(), this.getY() + this.height - this.getCapacity(),
                0, 0,
                this.width, this.getCapacity(),
                16, 256
            );
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    @Override
    protected boolean isValidClickButton(MouseButtonInfo buttonInfo) {
        return false;
    }

    @Nullable
    private Identifier getFluidTexture() {
        FluidResource resource = this.fluidHandler.getResource(0);
        if (resource.isEmpty()) {
            return null;
        }
        AtomicReference<@org.jspecify.annotations.Nullable Identifier> texture = new AtomicReference<>();
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            Optional<Holder.Reference<Fluid>> holder = level.holder(Objects.requireNonNull(resource.typeHolder().getKey()));
            holder.ifPresent((fluid) -> {
                String registeredName = fluid.getRegisteredName().split(":")[1];
                texture.set(AnvilCraft.of("textures/block/" + registeredName + ".png"));
            });
        }
        return texture.get();
    }

    private int getCapacity() {
        if (this.fluidHandler.getResource(0).isEmpty()) {
            return 0;
        }
        final int stored = this.fluidHandler.getAmountAsInt(0);
        return (stored * this.height / this.fluidHandler.getCapacityAsInt(0, FluidResource.EMPTY));
    }
}
