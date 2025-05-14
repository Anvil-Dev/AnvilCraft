package dev.dubhe.anvilcraft.mixin.plugin;

import net.neoforged.fml.loading.LoadingModList;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public class AnvilCraftMixinPlugin implements IMixinConfigPlugin {
    private static boolean hasZetaPiston = false;
    private static boolean hasCreate = false;
    private static boolean hasReiScreen = false;
    public static boolean hasAE2 = false;

    private boolean isLoaded(String clazz) {
        return AnvilCraftMixinPlugin.class.getClassLoader().getResource(clazz) != null;
    }

    @Override
    public void onLoad(String mixinPackage) {
        hasZetaPiston = this.isLoaded("org/violetmoon/zeta/piston/ZetaPistonStructureResolver.class");
        hasReiScreen = this.isLoaded("me/shedaniel/rei/impl/client/gui/screen/DefaultDisplayViewingScreen.class");
        hasCreate = this.isLoaded("com/simibubi/create/Create.class");
        hasAE2 = LoadingModList.get().getMods().stream().anyMatch(it -> it.getModId().equals("ae2"));
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, @NotNull String mixinClassName) {
        if (mixinClassName.endsWith("PistonStructureResolverMixin")) return !hasZetaPiston;
        if (mixinClassName.endsWith("DefaultDisplayViewingScreenMixin")) return hasReiScreen;
        if (mixinClassName.contains("Create")) {
            return hasCreate;
        }
        if (mixinClassName.contains("BatchCrafterBlockMixin") || mixinClassName.contains("BatchCrafterBlockEntityMixin")) {
            return hasAE2;
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
    public void postApply(@NotNull String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
