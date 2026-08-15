package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.dubhe.anvilcraft.api.component.ModNameContents;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.ComponentSerialization;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ComponentSerialization.class)
public class ComponentSerializationMixin {
    @Definition(id = "Type", type = ComponentContents.Type.class)
    @Expression("new Type[]{?, ?, ?, ?, ?, ?, ?}")
    @ModifyExpressionValue(method = "createCodec", at = @At("MIXINEXTRAS:EXPRESSION"))
    private static ComponentContents.Type<?>[] register(ComponentContents.Type<?>[] original) {
        ComponentContents.Type<?>[] newTypes = new ComponentContents.Type[original.length + 1];
        System.arraycopy(original, 0, newTypes, 0, original.length);
        newTypes[original.length] = ModNameContents.TYPE;
        return newTypes;
    }
}
