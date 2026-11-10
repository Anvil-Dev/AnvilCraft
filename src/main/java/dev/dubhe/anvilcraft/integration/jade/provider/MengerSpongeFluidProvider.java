package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.Accessor;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import java.util.List;

public enum MengerSpongeFluidProvider implements IServerExtensionProvider<FluidView.Data> {
    INSTANCE;

    public static final Identifier UID = AnvilCraft.of("menger_sponge_fluid");

    @Override
    public @Nullable List<ViewGroup<FluidView.Data>> getGroups(Accessor<?> accessor) {
        return List.of();
    }

    @Override
    public boolean shouldRequestData(Accessor<?> accessor) {
        return false;
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
