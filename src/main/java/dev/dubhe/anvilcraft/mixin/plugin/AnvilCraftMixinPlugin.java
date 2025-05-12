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
        if (targetClassName.equals("net.minecraft.client.renderer.entity.ItemRenderer")) {
            for (MethodNode method : targetClass.methods) {
                if (method.name.equals("renderBakedItemQuads")) {
                    ListIterator<AbstractInsnNode> it = method.instructions.iterator();
                    while (it.hasNext()) {
                        AbstractInsnNode insn = it.next();
                        if (insn instanceof MethodInsnNode methodInsnNode) {
                            boolean isSodium = methodInsnNode.owner.equals("net/caffeinemc/mods/sodium/client/render/immediate/model/BakedModelEncoder")
                                && methodInsnNode.name.equals("writeQuadVertices")
                                && methodInsnNode.desc.equals("(Lnet/caffeinemc/mods/sodium/api/vertex/buffer/VertexBufferWriter;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/caffeinemc/mods/sodium/client/model/quad/ModelQuadView;IIIZ)V");
                            boolean isEmbeddium = methodInsnNode.owner.equals("org/embeddedt/embeddium/impl/render/immediate/model/BakedModelEncoder")
                                && methodInsnNode.name.equals("writeQuadVertices")
                                && methodInsnNode.desc.equals("(Lorg/embeddedt/embeddium/api/vertex/buffer/VertexBufferWriter;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lorg/embeddedt/embeddium/impl/model/quad/ModelQuadView;IIIZ)V");
                            if (!isSodium && !isEmbeddium) continue;
                            it.previous();
                            //light = SodiumHooks.modifyLightForEmissiveItems(bakedQuad, light)
                            int lightIndex = isSodium ? 5 : 6;
                            int quadIndex = isSodium ? 8 : 9;
                            it.add(
                                new VarInsnNode(
                                    Opcodes.ALOAD,
                                    quadIndex
                                )
                            );
                            it.add(
                                new VarInsnNode(
                                    Opcodes.ILOAD,
                                    lightIndex
                                )
                            );
                            it.add(
                                new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    "dev/dubhe/anvilcraft/integration/sodium/SodiumHooks",
                                    "modifyLightForEmissiveItems",
                                    "(Lnet/minecraft/client/renderer/block/model/BakedQuad;I)I"
                                )
                            );
                            it.add(
                                new VarInsnNode(
                                    Opcodes.ISTORE,
                                    lightIndex
                                )
                            );
                            break;
                        }
                    }
                }
            }
        }
    }
}
