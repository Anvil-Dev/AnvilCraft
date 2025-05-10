package dev.anvilcraft.coremod.foundation;

import cpw.mods.modlauncher.api.ITransformerVotingContext;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

@FunctionalInterface
public interface ClassTransformer {
    @Nullable ClassNode accept(ClassNode input, ITransformerVotingContext context);
}
