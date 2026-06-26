package dev.dubhe.anvilcraft.integration.jade.provider;

import com.google.common.collect.ImmutableList;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jspecify.annotations.Nullable;
import snownee.jade.addon.universal.FluidStorageProvider;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.ui.NarratableComponent;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.ViewGroup;

import java.util.Collections;
import java.util.List;

public class CreativeFluidTankProvider extends FluidStorageProvider.Extension {
    public static final CreativeFluidTankProvider INSTANCE = new CreativeFluidTankProvider();

    private CreativeFluidTankProvider() {
    }

    @Override
    public @Nullable List<ViewGroup<FluidView.Data>> getGroups(Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor blockAccessor)) return null;
        return Collections.singletonList(new ViewGroup<>(Collections.singletonList(
            new FluidView.Data(
                JadeFluidObject.of(
                    accessor.getLevel().getCapability(
                        Capabilities.Fluid.BLOCK,
                        blockAccessor.getHitResult().getBlockPos(),
                        null
                    ).getResource(0).getFluid()
                ),
                Integer.MAX_VALUE
            )
        )));
    }

    @Override
    public List<ClientViewGroup<FluidView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<FluidView.Data>> groups) {
        JadeFluidObject fluid = groups.getFirst().views.getFirst().fluids().getFirst();
        if (fluid.isEmpty()) return ImmutableList.of();
        FluidView view = new FluidView(
            JadeUI.fluid(fluid),
            Component.translatable("tooltip.anvilcraft.jade.infinity").withStyle(ChatFormatting.GRAY),
            Component.translatable("tooltip.anvilcraft.jade.infinity").withStyle(ChatFormatting.GRAY)
        );
        view.fluidName = fluid.getDisplayName();
        view.ratio = 1.0F;
        if (fluid.is(Fluids.EMPTY)) {
            view.overrideText = NarratableComponent.translatable(
                "jade.fluid",
                FluidView.EMPTY_FLUID,
                NarratableComponent.attach(Component.literal(view.max.getString()), view.max)
            );
        }
        return Collections.singletonList(new ClientViewGroup<>(Collections.singletonList(view)));
    }

    @Override
    public boolean shouldRequestData(Accessor<?> accessor) {
        return true;
    }

    @Override
    public Identifier getUid() {
        return AnvilCraft.of("creative_fluid_tank");
    }

    @Override
    public int getDefaultPriority() {
        return 1;
    }
}
