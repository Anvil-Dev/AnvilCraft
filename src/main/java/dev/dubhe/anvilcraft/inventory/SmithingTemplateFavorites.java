package dev.dubhe.anvilcraft.inventory;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/** 玩家置顶的锻造模板。 */
public record SmithingTemplateFavorites(List<Identifier> templates) {
    public static final SmithingTemplateFavorites EMPTY = new SmithingTemplateFavorites(List.of());
    public static final MapCodec<SmithingTemplateFavorites> CODEC = Identifier.CODEC.listOf()
        .fieldOf("templates")
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
    public SmithingTemplateFavorites toggle(Identifier template) {
        List<Identifier> result = new ArrayList<>(this.templates);
        if (!result.remove(template)) {
            result.add(template);
        }
        return new SmithingTemplateFavorites(result);
    }
}
