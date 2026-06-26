package dev.dubhe.anvilcraft.integration.jade.provider;

import com.google.common.collect.ImmutableList;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jspecify.annotations.Nullable;
import snownee.jade.addon.universal.ItemStorageProvider;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.ItemView;
import snownee.jade.api.view.ViewGroup;

import java.util.Collections;
import java.util.List;

public class CreativeCrateProvider extends ItemStorageProvider.Extension {
    public static final CreativeCrateProvider INSTANCE = new CreativeCrateProvider();

    private CreativeCrateProvider() {
    }

    @Override
    public @Nullable List<ViewGroup<ItemStack>> getGroups(Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor blockAccessor)) return null;
        return Collections.singletonList(new ViewGroup<>(Collections.singletonList(
            accessor.getLevel().getCapability(
                Capabilities.Item.BLOCK,
                blockAccessor.getHitResult().getBlockPos(),
                null
            ).getResource(0).toStack()
        )));
    }

    @Override
    public List<ClientViewGroup<ItemView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<ItemStack>> groups) {
        ItemStack item = groups.getFirst().views.getFirst();
        if (item.isEmpty()) return ImmutableList.of();
        ItemView view = new ItemView(item);
        view.amountText = "∞";
        return Collections.singletonList(new ClientViewGroup<>(Collections.singletonList(view)));
    }

    @Override
    public boolean shouldRequestData(Accessor<?> accessor) {
        return true;
    }

    @Override
    public Identifier getUid() {
        return AnvilCraft.of("creative_crate");
    }

    @Override
    public int getDefaultPriority() {
        return 1;
    }
}
