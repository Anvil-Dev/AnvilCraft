package dev.dubhe.anvilcraft.integration.jade.provider;

import com.google.common.collect.ImmutableList;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.fluid.InfinityFluidTank;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
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
import snownee.jade.util.FluidTextHelper;

import java.util.Collections;
import java.util.List;

public class LargeFluidTankProvider extends FluidStorageProvider.Extension {
    public static final LargeFluidTankProvider INSTANCE = new LargeFluidTankProvider();

    private LargeFluidTankProvider() {
    }

    @Override
    public @Nullable List<ViewGroup<FluidView.Data>> getGroups(Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor blockAccessor)) return null;
        ResourceHandler<FluidResource> handler = accessor.getLevel().getCapability(
            Capabilities.Fluid.BLOCK,
            blockAccessor.getHitResult().getBlockPos(),
            null
        );
        if (handler == null) return null;
        FluidResource resource = handler.getResource(0);
        long amount = handler.getAmountAsLong(0);
        if (!(handler instanceof InfinityFluidTank infinity)) {
            return Collections.singletonList(new ViewGroup<>(Collections.singletonList(new FluidView.Data(
                JadeFluidObject.of(resource.getFluid(), amount),
                handler.getCapacityAsInt(0, resource)
            ))));
        }
        return Collections.singletonList(new ViewGroup<>(Collections.singletonList(new FluidView.Data(
            JadeFluidObject.of(resource.getFluid(), amount),
            infinity.isInfinity() ? Integer.MAX_VALUE : infinity.getCapacityAsInt(0, resource)
        ))));
    }

    @Override
    public List<ClientViewGroup<FluidView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<FluidView.Data>> groups) {
        if (groups.isEmpty() || groups.getFirst().views.isEmpty()) return ImmutableList.of();
        FluidView.Data data = groups.getFirst().views.getFirst();
        JadeFluidObject fluid = data.fluids().getFirst();
        if (fluid.isEmpty()) return ImmutableList.of();
        long amount = fluid.getAmount();
        long capacity = data.capacity();
        MutableComponent infinity = Component.translatable("tooltip.anvilcraft.jade.infinity").withStyle(ChatFormatting.GRAY);
        Component current = capacity == Integer.MAX_VALUE ? infinity : FluidTextHelper.getMillibuckets(amount, true);
        Component max = capacity == Integer.MAX_VALUE ? infinity : FluidTextHelper.getMillibuckets(capacity, true);
        FluidView view = new FluidView(JadeUI.fluid(fluid), current, max);
        view.fluidName = fluid.getDisplayName();
        view.ratio = capacity == Integer.MAX_VALUE ? 1.0F : Math.clamp((float) amount / capacity, 0.0F, 1.0F);
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
        return AnvilCraft.of("large_fluid_tank");
    }

    @Override
    public int getDefaultPriority() {
        return 1;
    }
}
