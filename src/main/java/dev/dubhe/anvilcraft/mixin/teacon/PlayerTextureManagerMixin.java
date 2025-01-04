package dev.dubhe.anvilcraft.mixin.teacon;

import com.google.common.collect.Sets;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.markusbordihn.easynpc.client.texture.PlayerTextureManager;
import de.markusbordihn.easynpc.client.texture.TextureModelKey;
import de.markusbordihn.easynpc.entity.easynpc.data.SkinData;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Set;
import java.util.UUID;

@Mixin(PlayerTextureManager.class)
public class PlayerTextureManagerMixin {

    @Unique
    private static final Set<UUID> STARTED_CREATORS = Sets.newConcurrentHashSet();

    @WrapOperation(
        method = "getOrCreateTextureWithDefault",
        at = @At(
            value = "INVOKE",
            target = "Lde/markusbordihn/easynpc/client/texture/PlayerTextureManager;createTexture(Lde/markusbordihn/easynpc/client/texture/TextureModelKey;Lde/markusbordihn/easynpc/entity/easynpc/data/SkinData;Ljava/util/UUID;)Lnet/minecraft/resources/ResourceLocation;"
        )
    )
    private static ResourceLocation wrapCreateTexture(TextureModelKey textureModelKey, SkinData<?> skinData, UUID playerUUID, Operation<ResourceLocation> original) {
        if (STARTED_CREATORS.contains(playerUUID)) return null;
        Thread.ofVirtual()
            .name("WrappedCreateTexture")
            .start(() -> {
                STARTED_CREATORS.add(playerUUID);
                original.call(textureModelKey, skinData, playerUUID);
                STARTED_CREATORS.remove(playerUUID);
            });
        return null;
    }
}
