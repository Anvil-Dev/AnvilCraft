package dev.dubhe.anvilcraft.mixin.plugin;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class AnvilCraftMixinPlugin implements IMixinConfigPlugin {
    private static boolean hasZetaPiston = false;
    private static boolean hasCreate = false;
    private static boolean hasReiScreen = false;

    private boolean isLoaded(String clazz) {
        return AnvilCraftMixinPlugin.class.getClassLoader().getResource(clazz) != null;
    }

    @Override
    public void onLoad(String mixinPackage) {
        hasZetaPiston = AnvilCraftMixinPlugin.class.getClassLoader()
                .getResource("org/violetmoon/zeta/piston/ZetaPistonStructureResolver.class") != null;
        hasCreate = this.isLoaded("com/simibubi/create/Create.class");
        hasZetaPiston = this.isLoaded("org/violetmoon/zeta/piston/ZetaPistonStructureResolver.class");
        hasReiScreen = this.isLoaded("me/shedaniel/rei/impl/client/gui/screen/DefaultDisplayViewingScreen.class");
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, @NotNull String mixinClassName) {
        if (mixinClassName.endsWith("PistonStructureResolverMixin")) return !hasZetaPiston;
        if (mixinClassName.endsWith("DefaultDisplayViewingScreenMixin")) return hasReiScreen;
        if (mixinClassName.startsWith("dev.dubhe.anvilcraft.mixin.integration.create.")) {
            return hasCreate;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (!targetClassName.contains("dev.dubhe.anvilcraft.integration.emi.DoubleBlockIcon")) return;
        for (MethodNode methodNode : targetClass.methods) {
            if (methodNode.name.equals("method_25394")) {
                methodNode.name = "render";
            }
        }
    }
}
