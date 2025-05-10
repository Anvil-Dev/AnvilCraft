package dev.anvilcraft.coremod.foundation;

import com.google.common.base.Preconditions;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.TargetType;
import org.objectweb.asm.tree.ClassNode;

import java.util.HashSet;
import java.util.Set;

public class TransformerBuilder {

    private ClassTransformer transformer;
    private final Set<ITransformer.Target<ClassNode>> targets = new HashSet<>();

    public TransformerBuilder transformer(ClassTransformer instance) {
        this.transformer = instance;
        return this;
    }

    public TransformerBuilder target(
        String className
    ) {
        targets.add(new ITransformer.Target<>(className, "", "", TargetType.CLASS));
        return this;
    }

    public ITransformer<ClassNode> build() {
        Preconditions.checkArgument(transformer != null, "ClassTransformer required.");
        Preconditions.checkArgument(!targets.isEmpty(), "No target specified.");
        return new TransformerImpl(
            transformer,
            targets
        );
    }

    public static TransformerBuilder builder() {
        return new TransformerBuilder();
    }
}
