package dev.dubhe.anvilcraft.mixin.plugin;

import net.neoforged.fml.loading.FMLLoader;
import org.jspecify.annotations.Nullable;
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

    private boolean isLoaded(String clazz) {
        return AnvilCraftMixinPlugin.class.getClassLoader().getResource(clazz) != null;
    }

    @Override
    public void onLoad(String mixinPackage) {
        AnvilCraftMixinPlugin.hasZetaPiston = this.isLoaded("org/violetmoon/zeta/piston/ZetaPistonStructureResolver.class");
        AnvilCraftMixinPlugin.hasReiScreen = this.isLoaded("me/shedaniel/rei/impl/client/gui/screen/DefaultDisplayViewingScreen.class");
        AnvilCraftMixinPlugin.hasCreate = this.isLoaded("com/simibubi/create/Create.class");
        AnvilCraftMixinPlugin.hasAE2 = FMLLoader.getCurrent().getLoadingModList().getMods().stream().anyMatch(it -> it.getModId().equals("ae2"));
        AnvilCraftMixinPlugin.hasCerbonBetterBeacons = this.isLoaded("com/cerbon/better_beacons/BetterBeacons.class");
        AnvilCraftMixinPlugin.hasJei = FMLLoader.getCurrent().getLoadingModList().getMods().stream().anyMatch(it -> it.getModId().equals("jei"));
        AnvilCraftMixinPlugin.hasArchitectury = this.isLoaded("dev/architectury/neoforge/ArchitecturyNeoForge");
    }

    @Override
    public @Nullable String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("PistonStructureResolverMixin")) return !AnvilCraftMixinPlugin.hasZetaPiston;
        if (mixinClassName.endsWith("DefaultDisplayViewingScreenMixin")) return AnvilCraftMixinPlugin.hasReiScreen;
        if (mixinClassName.contains("Create")) return AnvilCraftMixinPlugin.hasCreate;
        if (mixinClassName.contains("AE2")) return AnvilCraftMixinPlugin.hasAE2;
        if (mixinClassName.contains("Cerbon")) return AnvilCraftMixinPlugin.hasCerbonBetterBeacons;
        if (mixinClassName.contains("Jei")) return AnvilCraftMixinPlugin.hasJei;
        if (mixinClassName.contains("Architectury")) return AnvilCraftMixinPlugin.hasArchitectury;
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
