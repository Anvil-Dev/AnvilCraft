package dev.dubhe.anvilcraft.mixin.plugin;

import net.neoforged.fml.loading.LoadingModList;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class AnvilCraftMixinPlugin implements IMixinConfigPlugin {
    private static boolean hasZetaPiston = false;
    private static boolean hasCreate = false;
    private static boolean hasReiScreen = false;
    public static boolean hasAE2 = false;
    private static boolean hasCerbonBetterBeacons = false;

    private boolean isLoaded(String clazz) {
        return AnvilCraftMixinPlugin.class.getClassLoader().getResource(clazz) != null;
    }

    @Override
    public void onLoad(String mixinPackage) {
        hasZetaPiston = this.isLoaded("org/violetmoon/zeta/piston/ZetaPistonStructureResolver.class");
        hasReiScreen = this.isLoaded("me/shedaniel/rei/impl/client/gui/screen/DefaultDisplayViewingScreen.class");
        hasCreate = this.isLoaded("com/simibubi/create/Create.class");
        hasAE2 = LoadingModList.get().getMods().stream().anyMatch(it -> it.getModId().equals("ae2"));
        hasCerbonBetterBeacons = this.isLoaded("com/cerbon/better_beacons/BetterBeacons.class");
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, @NotNull String mixinClassName) {
        if (mixinClassName.endsWith("PistonStructureResolverMixin")) return !hasZetaPiston;
        if (mixinClassName.endsWith("DefaultDisplayViewingScreenMixin")) return hasReiScreen;
        if (mixinClassName.contains("Create")) return hasCreate;
        if (mixinClassName.contains("AE2")) return hasAE2;
        if (mixinClassName.contains("Cerbon")) return hasCerbonBetterBeacons;
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
