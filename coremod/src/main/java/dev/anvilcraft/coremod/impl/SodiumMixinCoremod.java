package dev.anvilcraft.coremod.impl;

import cpw.mods.modlauncher.api.ITransformerVotingContext;
import dev.anvilcraft.coremod.foundation.ClassTransformer;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

public class SodiumMixinCoremod implements ClassTransformer {
    @Override
    public @Nullable ClassNode accept(ClassNode input, ITransformerVotingContext context) {
        return null;
    }
}
