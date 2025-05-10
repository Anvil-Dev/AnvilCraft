package dev.anvilcraft.coremod.foundation;

import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TargetType;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.ClassNode;

import java.util.Set;

public class TransformerImpl implements ITransformer<ClassNode> {

    private final ClassTransformer transformer;
    private final Set<Target<ClassNode>> targets;

    public TransformerImpl(ClassTransformer transformer, Set<Target<ClassNode>> targets) {
        this.transformer = transformer;
        this.targets = targets;
    }

    @Override
    public @NotNull ClassNode transform(ClassNode input, ITransformerVotingContext context) {
        ClassNode node = transformer.accept(input, context);
        return node == null ? input : node;
    }

    @Override
    public @NotNull TransformerVoteResult castVote(ITransformerVotingContext context) {
        return TransformerVoteResult.YES;
    }

    @Override
    public @NotNull Set<Target<ClassNode>> targets() {
        return targets;
    }

    @Override
    public @NotNull TargetType<ClassNode> getTargetType() {
        return TargetType.CLASS;
    }
}
