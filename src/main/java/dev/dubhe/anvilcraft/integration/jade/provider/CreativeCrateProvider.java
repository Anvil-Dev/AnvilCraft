package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import java.util.List;

public enum CreativeCrateProvider implements IServerExtensionProvider<ItemStack> {
    INSTANCE;
    public static final Identifier UID = AnvilCraft.of("creative_crate");

    @Override
    public @Nullable List<ViewGroup<ItemStack>> getGroups(Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor blockAccessor)) return null;
        ItemStack stack = accessor.getLevel().getCapability(
            Capabilities.Item.BLOCK,
            blockAccessor.getHitResult().getBlockPos(),
            null
        ).getResource(0).toStack();
        return List.of(new ViewGroup<>(List.of(stack)));
    }

    @Override
    public Identifier getUid() {
        return CreativeCrateProvider.UID;
    }
}
