package dev.dubhe.anvilcraft.util.registrater;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import dev.dubhe.anvilcraft.util.DangerUtil;
import net.minecraft.world.level.block.Block;

public class BlockStatProviderUtil {
    @SuppressWarnings("unused")
    public static void none(DataGenContext<?, ?> context, RegistrateBlockstateProvider provider) {
    }

    public static <E extends Block> void simple(DataGenContext<Block, E> context, RegistrateBlockstateProvider provider) {
        provider.simpleBlock(
            context.get(),
            DangerUtil.genConfiguredModel("block/" + context.getId().getPath()).get()
        );
    }
}
