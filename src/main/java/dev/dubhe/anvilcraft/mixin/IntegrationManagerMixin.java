package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.anvilcraft.lib.v2.integration.IntegrationInstance;
import dev.anvilcraft.lib.v2.integration.IntegrationManager;
import dev.anvilcraft.lib.v2.integration.IntegrationType;
import net.neoforged.fml.loading.FMLLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collection;

@Mixin(value = IntegrationManager.class, remap = false)
abstract class IntegrationManagerMixin {
    @ModifyExpressionValue(method = "load", at = @At(value = "INVOKE",
        target = "Lcom/google/common/collect/Multimap;get(Ljava/lang/Object;)Ljava/util/Collection;"))
    private Collection<IntegrationInstance> anvilcraft$serverCandidates(Collection<IntegrationInstance> original) {
        if (!FMLLoader.getCurrent().getDist().isDedicatedServer()) return original;
        // 当前加载器会在遇到非服务端条目时直接返回，先过滤它们，保留其余加载流程。
        return original.stream().filter(instance -> instance.containsType(IntegrationType.DEDICATED_SERVER)).toList();
    }
}
