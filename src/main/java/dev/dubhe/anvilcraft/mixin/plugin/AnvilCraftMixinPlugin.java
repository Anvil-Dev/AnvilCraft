package dev.dubhe.anvilcraft.mixin.plugin;

import net.neoforged.fml.loading.LoadingModList;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class AnvilCraftMixinPlugin implements IMixinConfigPlugin {
    private static boolean hasZetaPiston = false;
    private static boolean hasCreate = false;
    private static boolean hasReiScreen = false;
    private static boolean hasAE2 = false;
    private static boolean hasCerbonBetterBeacons = false;
    private static boolean hasJei = false;
    private static boolean hasArchitectury = false;
    private static boolean hasSodium = false;
    private static boolean hasEmbeddium = false;

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
        hasJei = LoadingModList.get().getMods().stream().anyMatch(it -> it.getModId().equals("jei"));
        hasArchitectury = this.isLoaded("dev/architectury/neoforge/ArchitecturyNeoForge");
        hasSodium = this.isLoaded(
            "net/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockOcclusionCache.class"
        );
        hasEmbeddium = this.isLoaded(
            "org/embeddedt/embeddium/impl/render/chunk/compile/pipeline/BlockOcclusionCache.class"
        );
    }

    @Override
    public @Nullable String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("PistonStructureResolverMixin")) return !hasZetaPiston;
        if (mixinClassName.endsWith("DefaultDisplayViewingScreenMixin")) return hasReiScreen;
        if (mixinClassName.contains("Create")) return hasCreate;
        if (mixinClassName.contains("AE2")) return hasAE2;
        if (mixinClassName.contains("Cerbon")) return hasCerbonBetterBeacons;
        if (mixinClassName.contains("Jei")) return hasJei;
        if (mixinClassName.contains("Architectury")) return hasArchitectury;
        if (mixinClassName.contains(".compat.Sodium")) return hasSodium;
        if (mixinClassName.contains(".compat.Emb")) return hasEmbeddium;
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public @Nullable List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
