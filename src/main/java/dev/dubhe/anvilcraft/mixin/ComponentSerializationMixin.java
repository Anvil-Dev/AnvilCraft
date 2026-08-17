package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.component.ModNameContents;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.ComponentSerialization;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ComponentSerialization.class)
public class ComponentSerializationMixin {
    // ComponentSerialization 中所有的 StreamCodec 都引用了 CODEC 作为原型，无需也无法注入
    @ModifyVariable(method = "createCodec", at = @At("STORE"))
    private static ComponentContents.Type<?>[] register(ComponentContents.Type<?>[] original) {
        ComponentContents.Type<?>[] newTypes = new ComponentContents.Type[original.length + 1];
        System.arraycopy(original, 0, newTypes, 0, original.length);
        newTypes[original.length] = ModNameContents.TYPE;
        return newTypes;
    }
}
