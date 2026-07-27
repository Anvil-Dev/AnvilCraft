package dev.dubhe.anvilcraft.inventory;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** 玩家置顶的锻造模板。 */
public record SmithingTemplateFavorites(List<ResourceLocation> templates) {
    public static final SmithingTemplateFavorites EMPTY = new SmithingTemplateFavorites(List.of());
    public static final Codec<SmithingTemplateFavorites> CODEC = ResourceLocation.CODEC.listOf()
        .xmap(SmithingTemplateFavorites::new, SmithingTemplateFavorites::templates);

    public SmithingTemplateFavorites {
        templates = List.copyOf(templates);
    }

    /**
     * 切换模板的置顶状态。
     *
     * @param template 模板物品 id
     * @return 新的置顶数据
     */
    public SmithingTemplateFavorites toggle(ResourceLocation template) {
        List<ResourceLocation> result = new ArrayList<>(this.templates);
        if (!result.remove(template)) {
            result.add(template);
        }
        return new SmithingTemplateFavorites(result);
    }
}
