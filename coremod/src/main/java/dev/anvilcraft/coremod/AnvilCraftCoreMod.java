package dev.anvilcraft.coremod;

import com.google.common.collect.ImmutableList;
import cpw.mods.modlauncher.api.ITransformer;
import dev.anvilcraft.coremod.foundation.TransformerBuilder;
import dev.anvilcraft.coremod.impl.SodiumMixinCoremod;
import lombok.extern.slf4j.Slf4j;
import net.neoforged.neoforgespi.coremod.ICoreMod;

import java.util.List;

@Slf4j
public class AnvilCraftCoreMod implements ICoreMod {
    public static final List<? extends ITransformer<?>> TRANSFORMERS;

    static {
        ImmutableList.Builder<ITransformer<?>> builder = ImmutableList.builder();
        builder.add(TransformerBuilder.builder()
            .target("")
            .transformer(new SodiumMixinCoremod())
            .build()
        );
        TRANSFORMERS = builder.build();
    }

    @Override
    public Iterable<? extends ITransformer<?>> getTransformers() {
        log.info("AnvilCraftCoremod Loaded.");
        return TRANSFORMERS;
    }
}
