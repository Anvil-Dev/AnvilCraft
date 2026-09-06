package dev.dubhe.anvilcraft.client;

import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDInlineComponentContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDInlineComponentFactory;
import dev.anvilcraft.resource.ageratum.client.registries.AgeratumRegistries;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 手册 Markdown 行内组件工厂。
 *
 * <p>手册源码中的 {@code <key id="key.anvilcraft.switch_phase"/>} 会被 Ageratum 解析为
 * {@code ageratum:key} 行内组件（不带命名空间的标签默认使用 {@code ageratum}），
 * 因此这里向 Ageratum 的注册表中补充该组件，将按键 id 渲染为当前绑定的按键。</p>
 */
@SuppressWarnings("unused")
public class AnvilCraftInlineComponentFactories {
    public static final DeferredRegister<MDInlineComponentFactory> INLINE_COMPONENT_FACTORIES =
        DeferredRegister.create(AgeratumRegistries.INLINE_COMPONENT_FACTORY_REGISTRY_KEY, Ageratum.MOD_ID);

    /**
     * 按键行内组件：{@code <key id="key.anvilcraft.switch_phase"/>}。
     */
    public static final DeferredHolder<MDInlineComponentFactory, MDInlineComponentFactory> KEY =
        INLINE_COMPONENT_FACTORIES.register("key", () -> context -> renderKey(context));

    private static MutableComponent renderKey(MDInlineComponentContext context) {
        String keyId = context.params().get("id");
        if (keyId == null || keyId.isBlank()) {
            return Component.empty().withStyle(context.baseStyle());
        }
        for (KeyMapping mapping : Minecraft.getInstance().options.keyMappings) {
            if (mapping.getName().equals(keyId)) {
                return mapping.getTranslatedKeyMessage().copy().withStyle(context.baseStyle());
            }
        }
        // 未找到对应按键映射时原样显示按键 id，方便文档作者排查
        return Component.literal(keyId).withStyle(context.baseStyle());
    }

    private AnvilCraftInlineComponentFactories() {
    }
}
